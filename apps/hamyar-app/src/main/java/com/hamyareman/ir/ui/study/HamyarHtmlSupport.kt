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
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * پشتیبانی جامع برای نمایش HTML در WebView:
 *  - اصلاح نشانی‌های موسیقی پس‌زمینه (Background-music.html) تا از سرور ایرانی آروان خوانده شود.
 *  - اینترسپت درخواست‌های زیرمنبع (Background-music.html، عکس‌ها، صوت، فونت) در shouldInterceptRequest.
 *  - تنظیم دسترسی‌های WebView (پخش خودکار موسیقی بدون نیاز به gesture، فعال‌سازی جاوااسکریپت، زوم و ...).
 */
object HamyarHtmlSupport {

    const val BG_MUSIC_FILE = "Background-music.html"

    /** نشانی عمومی سرور ایرانی برای موسیقی پس‌زمینه. */
    fun bgMusicUrl(): String = ServerResolver.internal(BG_MUSIC_FILE)

    /**
     * پیش‌پردازش متن HTML قبل از ارسال به WebView:
     * هرگونه ارجاع به Background-music.html (در کادر iframe، تگ script، embed، صوت یا لینک)
     * به نشانی سرور ایرانی آروان تبدیل می‌شود.
     */
    fun preprocessHtml(html: String): String {
        if (html.isBlank()) return html
        val irBgMusicUrl = bgMusicUrl()

        // ۱) جایگزینی src یا href به Background-music.html (با هر پیشوند یا مسیر نسبی)
        val bgRegex = Regex("""(?i)(src|href)\s*=\s*(["'])(?:(?:\./|\.\./|/|https?://[^"']*/)?)(?:html\s*ها/)?(?:موسیقی\s*پس\s*زمینه/)?Background[-_]music\.html(?:\?[^"']*)?\2""")
        var processed = bgRegex.replace(html) { matchResult ->
            val attr = matchResult.groupValues[1]
            val quote = matchResult.groupValues[2]
            """$attr=$quote$irBgMusicUrl$quote"""
        }

        // ۲) جایگزینی ارجاعات متنی مستقیم در جاوااسکریپت یا تگ‌های دیگر
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
     * اعمال تنظیمات استاندارد روی WebView برای پشتیبانی از موسیقی، زوم و اسکریپت‌ها.
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
     * رهگیری درخواست‌های وب‌ویو برای لود کردن Background-music.html و سایر رسانه‌ها از سرور ایرانی.
     */
    fun interceptRequest(request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url ?: return null
        val urlStr = uri.toString()
        val path = uri.path.orEmpty()
        val lastSegment = uri.lastPathSegment.orEmpty()

        val isBgMusic = lastSegment.equals(BG_MUSIC_FILE, ignoreCase = true) ||
            urlStr.contains("Background-music.html", ignoreCase = true) ||
            urlStr.contains("background-music.html", ignoreCase = true) ||
            urlStr.contains("Background_music.html", ignoreCase = true)

        if (isBgMusic) {
            val responseStream = fetchRemoteStream(bgMusicUrl(), ServerResolver.external(BG_MUSIC_FILE))
            if (responseStream != null) {
                return WebResourceResponse(
                    "text/html",
                    "utf-8",
                    200,
                    "OK",
                    mapOf(
                        "Access-Control-Allow-Origin" to "*",
                        "Content-Type" to "text/html; charset=utf-8",
                    ),
                    responseStream,
                )
            }
        }

        // اگر از دامنه ساختگی local.hamyar منبع دیگری درخواست شده بود (صوت، تصویر، فونت و...)
        if (uri.host == "local.hamyar" || urlStr.startsWith("https://local.hamyar/") || urlStr.startsWith("http://local.hamyar/")) {
            val cleanPath = path.trimStart('/')
            if (cleanPath.isNotEmpty()) {
                val candidateKey = ContentCatalog.keyFor(cleanPath) ?: cleanPath
                val stream = fetchRemoteStream(ServerResolver.internal(candidateKey), ServerResolver.external(cleanPath))
                if (stream != null) {
                    val mime = guessMimeType(cleanPath)
                    return WebResourceResponse(
                        mime,
                        "utf-8",
                        200,
                        "OK",
                        mapOf("Access-Control-Allow-Origin" to "*"),
                        stream,
                    )
                }
            }
        }

        return null
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
            "svg" -> "image/svg+xml"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a", "aac" -> "audio/mp4"
            "json" -> "application/json"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }

    private fun fetchRemoteStream(vararg urls: String): InputStream? {
        for (u in urls.distinct()) {
            if (u.isBlank()) continue
            val stream = runCatching {
                val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 40_000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                }
                conn.connect()
                if (conn.responseCode in 200..299) {
                    conn.inputStream.readBytes().let { ByteArrayInputStream(it) }
                } else {
                    conn.disconnect()
                    null
                }
            }.getOrNull()
            if (stream != null) return stream
        }
        return null
    }

    /**
     * ایجاد یک WebViewClient کامل مجهز به اینترسپت هوشمند سرور ایرانی.
     */
    fun createWebViewClient(
        onPageFinished: ((view: WebView, url: String) -> Unit)? = null,
    ): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val intercepted = interceptRequest(request)
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
