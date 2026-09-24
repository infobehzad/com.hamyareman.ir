package com.hamyareman.ir.ui.tools

import android.content.Context
import com.hamyareman.ir.ui.profile.AppEdition
import com.hamyareman.ir.ui.study.StudyMedia
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * ابزارها و آزمایشگاه‌ها روی باکت Appwrite می‌نشینند تا از APK استخراج نشوند.
 *
 * شناسهٔ فایل (لاتین، حداکثر ۳۶ نویسه):
 *  - ابزار مشترک: `tool-{id}.html`  (خط تیره به‌جای _)
 *    مثال: tool-dj120d.html ، tool-casio991.html ، tool-ti-nspire.html
 *  - آزمایشگاه: `lab-{پایه}[-رشته]-{kind}.html`
 *    پایه: 04..12   رشتهٔ دهم به بعد: r ریاضی‌فیزیک / t تجربی / e انسانی
 *    kind: chemistry | physics | biology
 *    مثال نهم: lab-09-chemistry.html
 *    مثال دهم تجربی: lab-10t-biology.html
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

    fun urlOf(toolId: String): String = StudyMedia.viewUrl(fileId(toolId))

    private fun cacheFile(ctx: Context, toolId: String): File {
        val dir = File(ctx.filesDir, "hamyar-tools").apply { mkdirs() }
        return File(dir, fileId(toolId))
    }

    fun cachedPath(ctx: Context, toolId: String): String? {
        val f = cacheFile(ctx, toolId)
        return if (f.exists() && f.length() > 64) f.absolutePath else null
    }

    /**
     * فایل را از باکت می‌گیرد. اگر قبلاً ذخیره شده باشد همان را برمی‌گرداند.
     * @return مسیر محلی یا null
     */
    fun ensure(ctx: Context, toolId: String): String? {
        cachedPath(ctx, toolId)?.let { return it }
        val dest = cacheFile(ctx, toolId)
        val part = File(dest.absolutePath + ".part")
        val ok = runCatching {
            val conn = (URL(urlOf(toolId)).openConnection() as HttpURLConnection).apply {
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
            conn.inputStream.use { input ->
                part.outputStream().use { output -> input.copyTo(output) }
            }
            conn.disconnect()
            part.renameTo(dest)
            dest.length() > 64
        }.getOrDefault(false)
        if (!ok) {
            part.delete()
            dest.delete()
            return null
        }
        return dest.absolutePath
    }
}
