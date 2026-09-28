package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.hamyareman.ir.ui.content.ContentCatalog
import com.hamyareman.ir.ui.content.ContentItem
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.Locale

/**
 * پشتیبانی جامع برای نمایش و اجرای فایل‌های HTML در WebView:
 *  - رمزگشایی بلادرنگ فایل‌های رمزشده با HMK1 (شامل Background-music.html و صفحات بعدی/قبلی) با HtmlCodec.
 *  - اصلاح نشانی‌های موسیقی پس‌زمینه تا از سرور ایرانی آروان یا خارجی لود شوند.
 *  - رهگیری و مدیریت دکمه‌های «بعدی» و «قبلی» داخل HTMLها.
 *  - پشتیبانی از پخش خودکار مدیا بدون نیاز به User Gesture.
 */
object HamyarHtmlSupport {

    const val BG_MUSIC_FILE = "Background-music.html"

    /** نشانی سرور ایرانی برای موسیقی پس‌زمینه. */
    fun bgMusicUrl(): String = ServerResolver.internal(BG_MUSIC_FILE)

    /**
     * پیش‌پردازش متن HTML قبل از ارسال به WebView:
     * - جایگزینی ارجاعات موسیقی پس‌زمینه به سرور ایرانی.
     * - اصلاح مسیرهای نسبی رسانه‌ها (عکس‌ها، صوت و ...).
     */
    fun preprocessHtml(html: String): String {
        if (html.isBlank()) return html
        val irBgMusicUrl = bgMusicUrl()

        // ۱) جایگزینی src یا href به Background-music.html
        val bgRegex = Regex("""(?i)(src|href)\s*=\s*(["'])(?:(?:\./|\.\./|/|https?://[^"']*/)?)(?:html\s*ها/)?(?:موسیقی\s*پس\s*زمینه/)?Background[-_]music\.html(?:\?[^"']*)?\2""")
        var processed = bgRegex.replace(html) { matchResult ->
            val attr = matchResult.groupValues[1]
            val quote = matchResult.groupValues[2]
            """$attr=$quote$irBgMusicUrl$quote"""
        }

        // ۲) جایگزینی ارجاعات متنی مستقیم در جاوااسکریپت
        val literalBgRegex = Regex("""(?i)(["'])(?:(?:\./|\.\./|/|https?://[^"']*/)?)(?:html\s*ها/)?(?:موسیقی\s*پس\s*زمینه/)?Background[-_]music\.html\1""")
        processed = literalBgRegex.replace(processed) { matchResult ->
            val quote = matchResult.groupValues[1]
            """$quote$irBgMusicUrl$quote"""
        }

        // ۳) اصلاح مسیرهای عکس‌های نسبی (عکس ها/...) به سرور ایرانی
        val imgRegex = Regex("""(?i)(src\s*=\s*["'])(?:(?:\./|\.\./|/)?)(?:عکس\s*ها/)([^"']+\.(?:jpg|jpeg|png|webp|gif))(["'])""")
        processed = imgRegex.replace(processed) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val file = matchResult.groupValues[2]
            val suffix = matchResult.groupValues[3]
            val key = "عکس ها/$file"
            """$prefix${ServerResolver.internal(key)}$suffix"""
        }

        return processed
    }

    /**
     * تنظیمات بهینه وب‌ویو: اجرای JS، استوریج محلی، زوم، دسترسی فایل و پخش خودکار صدا.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun applySettings(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.defaultTextEncodingName = "utf-8"
    }

    /**
     * دریافت بایت‌ها از سرور و رمزگشایی HMK1 (در صورت وجود کلید و فرمت رمز).
     */
    fun fetchAndUnwrap(ctx: Context, vararg candidateUrls: String): ByteArray? {
        for (u in candidateUrls.distinct()) {
            if (u.isBlank()) continue
            val bytes = runCatching {
                val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                }
                conn.connect()
                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { it.readBytes() }
                } else {
                    conn.disconnect()
                    null
                }
            }.getOrNull()

            if (bytes != null && bytes.isNotEmpty()) {
                // اگر فایل با HMK1 کدگذاری شده باشد، بازگشایی می‌شود
                val plain = if (HtmlCodec.isWrapped(bytes)) {
                    runCatching { HtmlCodec.unwrap(ctx, bytes) }.getOrNull() ?: bytes
                } else {
                    bytes
                }
                return plain
            }
        }
        return null
    }

    /**
     * رهگیری درخواست‌های وب‌ویو برای لود کردن Background-music.html، فایل‌های بعدی/قبلی و مدیا.
     */
    fun interceptRequest(ctx: Context, request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url ?: return null
        val urlStr = uri.toString()
        val path = uri.path.orEmpty()
        val lastSegment = uri.lastPathSegment.orEmpty()
        val decodedLastSegment = runCatching { URLDecoder.decode(lastSegment, "UTF-8") }.getOrDefault(lastSegment)

        // ۱. بررسی آیا درخواست برای موسیقی پس‌زمینه است
        val isBgMusic = decodedLastSegment.equals(BG_MUSIC_FILE, ignoreCase = true) ||
            urlStr.contains("Background-music.html", ignoreCase = true) ||
            urlStr.contains("background-music.html", ignoreCase = true) ||
            urlStr.contains("Background_music.html", ignoreCase = true)

        if (isBgMusic) {
            val plainBytes = fetchAndUnwrap(
                ctx,
                bgMusicUrl(),
                ServerResolver.external(BG_MUSIC_FILE),
                "${ServerResolver.ARVAN_PUBLIC}/html%20%D9%87%D8%A7/Background-music.html",
            )
            if (plainBytes != null) {
                return WebResourceResponse(
                    "text/html",
                    "utf-8",
                    200,
                    "OK",
                    mapOf(
                        "Access-Control-Allow-Origin" to "*",
                        "Content-Type" to "text/html; charset=utf-8",
                    ),
                    ByteArrayInputStream(plainBytes),
                )
            }
        }

        // ۲. بررسی دکمه‌های بعدی و قبلی یا صفحات HTML دیگر
        if (decodedLastSegment.endsWith(".html", ignoreCase = true) || decodedLastSegment.endsWith(".htm", ignoreCase = true)) {
            ContentCatalog.load(ctx)
            val matchedItem = ContentCatalog.findByPathOrName(decodedLastSegment)
            if (matchedItem != null) {
                val plainBytes = fetchAndUnwrap(
                    ctx,
                    ServerResolver.pick(matchedItem.aw, matchedItem.key),
                    ServerResolver.internal(matchedItem.key),
                    ServerResolver.external(matchedItem.aw),
                )
                if (plainBytes != null) {
                    val processed = preprocessHtml(String(plainBytes, Charsets.UTF_8)).toByteArray(Charsets.UTF_8)
                    return WebResourceResponse(
                        "text/html",
                        "utf-8",
                        200,
                        "OK",
                        mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Content-Type" to "text/html; charset=utf-8",
                        ),
                        ByteArrayInputStream(processed),
                    )
                }
            }
        }

        // ۳. بررسی فایل‌های رسانه‌ای و محلی (تصاویر، فونت، صوت‌های Background Music)
        if (uri.host == "local.hamyar" || urlStr.startsWith("https://local.hamyar/") || urlStr.startsWith("http://local.hamyar/") || isMediaFile(decodedLastSegment)) {
            val cleanPath = path.trimStart('/')
            val decodedCleanPath = runCatching { URLDecoder.decode(cleanPath, "UTF-8") }.getOrDefault(cleanPath)
            if (decodedCleanPath.isNotEmpty()) {
                ContentCatalog.load(ctx)
                val candidateKey = ContentCatalog.keyFor(decodedCleanPath) ?: decodedCleanPath
                val bytes = fetchAndUnwrap(
                    ctx,
                    ServerResolver.internal(candidateKey),
                    ServerResolver.external(decodedCleanPath),
                    "${ServerResolver.ARVAN_PUBLIC}/$candidateKey",
                )
                if (bytes != null) {
                    val mime = guessMimeType(decodedCleanPath)
                    return WebResourceResponse(
                        mime,
                        null,
                        200,
                        "OK",
                        mapOf("Access-Control-Allow-Origin" to "*"),
                        ByteArrayInputStream(bytes),
                    )
                }
            }
        }

        return null
    }

    private fun isMediaFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in setOf("mp3", "wav", "ogg", "m4a", "aac", "jpg", "jpeg", "png", "webp", "gif", "svg", "woff", "woff2", "ttf", "css", "js")
    }

    private fun guessMimeType(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            "html", "htm" -> "text/html"
            "js" -> "application/javascript"
            "css" -> "text/css"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a", "aac" -> "audio/mp4"
            "ttf" -> "font/ttf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "json" -> "application/json"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }

    /**
     * ایجاد یک WebViewClient هوشمند برای رمزگشایی و مدیریت لینک‌های ناوبری (بعدی/قبلی).
     */
    fun createWebViewClient(
        context: Context,
        onNavigateItem: ((ContentItem) -> Unit)? = null,
        onPageFinished: ((view: WebView, url: String) -> Unit)? = null,
    ): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url ?: return false
                val last = runCatching { URLDecoder.decode(url.lastPathSegment.orEmpty(), "UTF-8") }.getOrDefault(url.lastPathSegment.orEmpty())
                if (last.endsWith(".html", ignoreCase = true) || last.endsWith(".htm", ignoreCase = true)) {
                    ContentCatalog.load(context)
                    val item = ContentCatalog.findByPathOrName(last)
                    if (item != null && onNavigateItem != null) {
                        onNavigateItem(item)
                        return true
                    }
                }
                return false
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val intercepted = interceptRequest(context, request)
                if (intercepted != null) return intercepted
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                onPageFinished?.invoke(view, url)
            }
        }
    }
}
