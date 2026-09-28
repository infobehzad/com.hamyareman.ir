package com.hamyareman.ir.ui.tools

import android.content.Context
import com.hamyareman.ir.ui.profile.AppEdition
import com.hamyareman.ir.ui.content.ContentCatalog
import com.hamyareman.ir.ui.study.HtmlCodec
import com.hamyareman.ir.ui.study.ServerPrefs
import com.hamyareman.ir.ui.study.ServerResolver
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * ابزارها و آزمایشگاه‌ها با نشانی دوگانه:
 *  - خارجی: باکت Appwrite
 *  - ایرانی: سراسری آروان (کلید از catalog.json)
 * انتخاب با «تنظیمات سرور» (ServerResolver). فایل دانلودی با HtmlCodec باز می‌شود؛
 * کش محلی همیشه متنِ ساده (بدون HMK1) می‌نویسد.
 *
 * شناسهٔ فایل (لاتین، حداکثر ۳۶ نویسه):
 *  - ابزار مشترک: `tool-{id}.html`  (خط تیره به‌جای _)
 *  - آزمایشگاه: `lab-{پایه}[-رشته]-{kind}.html`   مثال نهم: lab-09-chemistry.html
 */
object ToolRemote {

    private val labs = setOf("chemistry", "physics", "biology")

    fun gradeToken(): String {
        val n = AppEdition.grade.num
        val folder = AppEdition.booksFolder
        val track = when {
            folder.contains("ریاضی") -> "r"
            folder.contains("تجربی") -> "t"
            folder.contains("انسانی") -> "e"
            else -> ""
        }
        return if (n >= 10 && track.isNotEmpty()) "$n$track" else "%02d".format(n)
    }

    fun fileId(toolId: String): String {
        val id = toolId.trim().lowercase().replace('_', '-')
        return if (toolId in labs) "lab-${gradeToken()}-$id.html" else "tool-$id.html"
    }

    fun urlOf(toolId: String): String {
        val id = fileId(toolId)
        return ServerResolver.pick(id, ContentCatalog.keyFor(id))
    }

    private fun cacheFile(ctx: Context, toolId: String): File {
        val dir = File(ctx.filesDir, "hamyar-tools").apply { mkdirs() }
        return File(dir, fileId(toolId))
    }

    fun cachedPath(ctx: Context, toolId: String): String? {
        val f = cacheFile(ctx, toolId)
        if (!f.exists() || f.length() <= 64) return null
        // نسخهٔ کش‌شدهٔ رمزشده (باقی‌مانده از نصب‌های قدیمی) را همین‌جا باز کن
        val bytes = runCatching { f.readBytes() }.getOrNull() ?: return null
        if (HtmlCodec.isWrapped(bytes)) {
            val plain = runCatching { HtmlCodec.unwrap(ctx, bytes) }.getOrNull() ?: return null
            runCatching { f.writeBytes(plain) }
        }
        return f.absolutePath
    }

    private fun download(url: String, part: File): Boolean = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        conn.connect()
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            return@runCatching false
        }
        conn.inputStream.use { input -> part.outputStream().use { output -> input.copyTo(output) } }
        conn.disconnect()
        part.length() > 64
    }.getOrDefault(false)

    /**
     * فایل را از سرورِ انتخاب‌شده می‌گیرد، رمز را باز می‌کند و در کش می‌نویسد.
     * در حالت «سریع‌ترین» اگر داخلی خطا داد، خارجی را امتحان می‌کند.
     * @return مسیر محلی (متن ساده) یا null
     */
    fun ensure(ctx: Context, toolId: String): String? {
        ContentCatalog.load(ctx)
        cachedPath(ctx, toolId)?.let { return it }

        val id = fileId(toolId)
        val dest = cacheFile(ctx, toolId)

        // ۱. بررسی حافظه کش مشترک ContentDownloadStore
        val cachedHtml = com.hamyareman.ir.ui.content.ContentDownloadStore.getCachedHtml(ctx, id)
        if (cachedHtml != null && cachedHtml.length > 64) {
            runCatching {
                dest.writeText(cachedHtml, Charsets.UTF_8)
                return dest.absolutePath
            }
        }

        // ۲. تلاش برای دانلود از سرور ایرانی (پوشه‌بندی) و سرور خارجی
        val key = ContentCatalog.keyFor(id)
        val candidates = listOf(
            if (key != null) ServerResolver.internal(key) else "",
            ServerResolver.pick(id, key),
            ServerResolver.external(id),
            if (key != null) "${ServerResolver.ARVAN_PUBLIC}/$key" else "",
        ).filter { it.isNotBlank() }.distinct()

        val part = File(dest.absolutePath + ".part")
        var ok = false
        for (url in candidates) {
            if (download(url, part)) {
                ok = true
                break
            }
        }

        if (!ok) {
            part.delete()
            dest.delete()
            return null
        }
        // باز کردن HMK1 ← کش همیشه متن ساده است
        val plain = runCatching {
            val raw = part.readBytes()
            if (HtmlCodec.isWrapped(raw)) HtmlCodec.unwrap(ctx, raw) else raw
        }.getOrElse {
            part.delete()
            dest.delete()
            return null
        }
        runCatching {
            dest.writeBytes(plain)
            part.delete()
            // ذخیره همزمان در کش کاتالوگ محتوا
            com.hamyareman.ir.ui.content.ContentDownloadStore.saveHtml(ctx, id, String(plain, Charsets.UTF_8))
        }.getOrElse {
            part.delete()
            return null
        }
        return dest.absolutePath
    }
}
