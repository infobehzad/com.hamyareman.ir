package com.hamyareman.ir.ui.study

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * نقشه‌ی رسانه‌های درس‌ها — صوت/ویدیوی هر پک از باکت عمومی Appwrite.
 * ریاضی نهم: نام جدید روی دیسک (`ryazif01d01.mp3` / `C905f01d01.pdf`)؛
 * اگر در باکت نبود، نام قدیمی `*_AUDIO.mp3` / `*_BOOK.pdf` امتحان می‌شود.
 */
object StudyMedia {
    const val BUCKET = "6aa1eaae00303400117b"
    private const val PROJECT = "6a9d59e3002751cc3ea8"
    private const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"

    fun videoIds(packId: String): List<String> =
        listOf("${packId.replace("_", "-")}-V01.mp4")

    fun viewUrl(fileId: String): String =
        "$ENDPOINT/storage/buckets/$BUCKET/files/$fileId/view?project=$PROJECT"

    fun candidateIds(fileId: String): List<String> {
        if (fileId.isBlank()) return emptyList()
        val out = linkedSetOf(fileId)
        Regex("""^ryazif(\d{2})d(\d{2})\.mp3$""").find(fileId)?.let { m ->
            out += "C905_E%02d-L%02d_AUDIO.mp3".format(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        Regex("""^C905f(\d{2})d(\d{2})\.pdf$""").find(fileId)?.let { m ->
            out += "C905_E%02d-L%02d_BOOK.pdf".format(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        return out.toList()
    }

    private val resolved = ConcurrentHashMap<String, String>()

    fun resolveFileId(fileId: String): String {
        if (fileId.isBlank()) return fileId
        resolved[fileId]?.let { return it }
        for (id in candidateIds(fileId)) {
            if (existsOnServer(id)) {
                resolved[fileId] = id
                return id
            }
        }
        return fileId
    }

    fun existsOnServer(fileId: String): Boolean {
        return runCatching {
            val conn = (URL(viewUrl(fileId)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                requestMethod = "HEAD"
            }
            conn.connect()
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        }.getOrDefault(false)
    }
}
