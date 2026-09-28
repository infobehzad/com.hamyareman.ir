package com.hamyareman.ir.ui.content

import android.content.Context
import com.hamyareman.ir.ui.net.NetState
import com.hamyareman.ir.ui.net.ResilientHttp
import com.hamyareman.ir.ui.study.HtmlCodec
import com.hamyareman.ir.ui.study.ServerResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * مدیریت ذخیره‌سازی محلی و دانلود فایل‌های HTML (آموزشگاه، یوگا، ورزش، تنفس، ابزارها و آزمایشگاه‌ها).
 * فایل‌ها پس از دانلود، بازگشایی (HMK1 unwrapped) و در حافظهٔ دستگاه ذخیره می‌شوند تا
 * کاملاً آفلاین و با بیشترین سرعت در WebView اجرا گردند.
 */
object ContentDownloadStore {

    private const val DIR_NAME = "media/html-cache"

    fun cacheDir(ctx: Context): File =
        File(ctx.filesDir, DIR_NAME).apply { mkdirs() }

    fun cacheFile(ctx: Context, itemId: String): File {
        val safeId = itemId.trim().replace('/', '_').replace('\\', '_')
        val name = if (safeId.endsWith(".html", ignoreCase = true)) safeId else "$safeId.html"
        return File(cacheDir(ctx), name)
    }

    fun isCached(ctx: Context, itemId: String): Boolean {
        val f = cacheFile(ctx, itemId)
        return f.exists() && f.length() > 32
    }

    fun getCachedHtml(ctx: Context, itemId: String): String? {
        val f = cacheFile(ctx, itemId)
        if (!f.exists() || f.length() <= 32) return null
        return runCatching {
            val bytes = f.readBytes()
            val plain = if (HtmlCodec.isWrapped(bytes)) {
                HtmlCodec.unwrap(ctx, bytes)
            } else {
                bytes
            }
            String(plain, Charsets.UTF_8)
        }.getOrNull()
    }

    fun saveHtml(ctx: Context, itemId: String, plainHtml: String) {
        runCatching {
            val f = cacheFile(ctx, itemId)
            f.writeText(plainHtml, Charsets.UTF_8)
        }
    }

    fun deleteItem(ctx: Context, itemId: String): Boolean {
        val f = cacheFile(ctx, itemId)
        return f.delete()
    }

    fun totalSize(ctx: Context): Long {
        val dir = cacheDir(ctx)
        if (!dir.exists()) return 0L
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun cachedCountForCategory(ctx: Context, catId: String, gender: String): Int {
        val items = ContentCatalog.itemsOf(catId, gender)
        return items.count { isCached(ctx, it.id) }
    }

    /**
     * دانلود و بازگشایی یک آیتم کاتالوگ با تلاش متوالی از سرور ایرانی (پوشه‌بندی) و سرور خارجی (روت باکت).
     */
    suspend fun downloadItem(
        ctx: Context,
        item: ContentItem,
        onProgress: (Int) -> Unit = {},
    ): Boolean = withContext(Dispatchers.IO) {
        val target = cacheFile(ctx, item.id)
        val tmp = File(target.parentFile, "${target.name}.part")

        // ترتیب سرورها: اول سرور ایرانی (با پوشه‌بندی)، در صورت خطا سرور خارجی (روت باکت)
        val candidateUrls = listOf(
            ServerResolver.internal(item.key),
            ServerResolver.pick(item.aw, item.key),
            ServerResolver.external(item.aw),
            "${ServerResolver.ARVAN_PUBLIC}/${item.key}",
        ).distinct()

        var downloadedBytes: ByteArray? = null
        for (url in candidateUrls) {
            var attempt = 0
            while (attempt <= 3) {
                try {
                    val conn = ResilientHttp.open(
                        url,
                        connectMs = 12000,
                        readMs = 25000,
                        attempts = 2,
                    )
                    if (conn.responseCode in 200..299) {
                        val total = conn.contentLengthLong
                        val bytes = java.io.ByteArrayOutputStream()
                        conn.inputStream.use { input ->
                            val buf = ByteArray(32 * 1024)
                            var read: Int
                            var written = 0L
                            while (input.read(buf).also { read = it } > 0) {
                                bytes.write(buf, 0, read)
                                written += read
                                if (total > 0) {
                                    onProgress(((written * 100) / total).toInt().coerceIn(0, 99))
                                }
                            }
                        }
                        conn.disconnect()
                        downloadedBytes = bytes.toByteArray()
                        break
                    }
                    conn.disconnect()
                    attempt++
                } catch (e: Exception) {
                    attempt++
                    if (!NetState.isOnline(ctx)) NetState.awaitOnlineBlocking(ctx)
                }
            }
            if (downloadedBytes != null && downloadedBytes.isNotEmpty()) break
        }

        if (downloadedBytes == null || downloadedBytes.isEmpty()) {
            return@withContext false
        }

        // رمزگشایی با HMK1 (در صورت کدگذاری بودن فایل روی سرور)
        val plainBytes = runCatching {
            if (HtmlCodec.isWrapped(downloadedBytes)) {
                HtmlCodec.unwrap(ctx, downloadedBytes)
            } else {
                downloadedBytes
            }
        }.getOrElse { downloadedBytes }

        runCatching {
            tmp.writeBytes(plainBytes)
            if (tmp.renameTo(target) || (tmp.copyTo(target, overwrite = true).also { tmp.delete() } != null)) {
                onProgress(100)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }
}
