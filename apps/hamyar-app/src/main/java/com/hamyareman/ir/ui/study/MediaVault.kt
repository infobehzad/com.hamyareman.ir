package com.hamyareman.ir.ui.study

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * «گاوصندوق رسانه» — فایل‌های صوتی/تصویریِ دانلودشده هرگز به‌صورت خام روی گوشی
 * نمی‌مانند؛ با AES رمز می‌شوند (کلید داده با Keystore پوشانده می‌شود) و پخشِ
 * محلی فقط از راه [LocalMediaServer] (فقط ۱۲۷.۰.۰.۱ داخل خود اپ) انجام می‌شود.
 * نتیجه: فایل از بیرون اپ (مدیر فایل/کپی/اشتراک) قابل پخش یا خواندن نیست.
 */
object MediaVault {

    private const val DIR = "media-vault"
    private const val MAGIC = "HMV1"
    private const val PREFS = "hamyar_vault_keys"
    private const val WRAPPED_KEY = "wrapped_data_key"
    private const val EPOCH_KEY = "vault_epoch"
    /** نسخه‌ی محتوای گاوصندوق — v2: پاک‌سازی placeholderهای کش‌شده‌ی نسخه‌های قدیمی اپ. */
    private const val CACHE_EPOCH = 2

    private var dataKey: SecretKey? = null

    private fun vaultDir(ctx: Context): File {
        val dir = File(ctx.filesDir, DIR).apply { mkdirs() }
        // نسخه‌ی کش: با بالا رفتن CACHE_EPOCH، همه‌ی فایل‌های دانلودشده با نسخه‌ی
        // قبلی (مثلاً placeholderهای قدیمی) یک‌بار پاک می‌شوند و دوباره دانلود واقعی می‌شود.
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(EPOCH_KEY, 0) != CACHE_EPOCH) {
            dir.listFiles()?.forEach { it.delete() }
            prefs.edit().putInt(EPOCH_KEY, CACHE_EPOCH).apply()
        }
        return dir
    }

    fun vaultFile(ctx: Context, cacheKey: String) = File(vaultDir(ctx), "$cacheKey.enc")

    fun isCached(ctx: Context, cacheKey: String): Boolean =
        vaultFile(ctx, cacheKey).let { it.exists() && it.length() > 32 }

    /**
     * آیا این فایلِ دانلودشده **تأیید شده** است؟ (دانلود کامل و قابلِ رمزگشایی)
     * فقط فایل‌های تأییدشده برای پخشِ آفلاین استفاده می‌شوند تا یک فایلِ نیمه‌کاره
     * پخش را نشکند.
     */
    fun isVerified(ctx: Context, cacheKey: String): Boolean =
        isCached(ctx, cacheKey) && ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet("verified", emptySet()).orEmpty().contains(cacheKey)

    fun markVerified(ctx: Context, cacheKey: String) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet("verified", emptySet()).orEmpty().toMutableSet()
        set += cacheKey
        prefs.edit().putStringSet("verified", set).apply()
    }

    /** نگاهِ کوتاه به ابتدای فایلِ رمزگشایی‌شده — برای اطمینان از سالم‌بودنِ دانلود. */
    fun peek(ctx: Context, cacheKey: String, bytes: Int = 4096): ByteArray? {
        val all = vaultFile(ctx, cacheKey)
        if (!all.exists() || all.length() <= 20) return null
        return runCatching {
            // فقط همان چند کیلوبایتِ اول خوانده می‌شود (نه کل فایل) — در CTR
            // رمزگشاییِprefix معتبر است و حافظه هم مصرف نمی‌شود.
            val want = (20 + bytes).coerceAtMost(all.length().toInt())
            val raw = ByteArray(want)
            java.io.FileInputStream(all).use { fin ->
                var off = 0
                while (off < want) {
                    val n = fin.read(raw, off, want - off)
                    if (n <= 0) break
                    off += n
                }
            }
            if (!raw.startsWith(MAGIC.toByteArray(Charsets.US_ASCII))) return@runCatching null
            val iv = raw.copyOfRange(4, 20)
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(ctx), IvParameterSpec(iv))
            cipher.doFinal(raw, 20, raw.size - 20)
        }.getOrNull()
    }

    fun delete(ctx: Context, cacheKey: String) {
        vaultFile(ctx, cacheKey).delete()
        File(vaultDir(ctx), "$cacheKey.enc.part").delete()
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet("verified", emptySet()).orEmpty().toMutableSet()
        if (set.remove(cacheKey)) prefs.edit().putStringSet("verified", set).apply()
    }

    fun cachedBytes(ctx: Context): Long =
        vaultDir(ctx).listFiles()?.sumOf { it.length() } ?: 0L

    // ------------------------------------------------------------ کلیدها

    @Synchronized
    private fun key(ctx: Context): SecretKey {
        dataKey?.let { return it }
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = runCatching { unwrapWithKeystore(prefs.getString(WRAPPED_KEY, null)) }.getOrNull()
        if (raw != null) {
            dataKey = SecretKeySpec(raw, "AES")
            return dataKey!!
        }
        // ساخت کلید داده‌ی تازه + پوشاندن با Keystore (یا ذخیره‌ی مستقیم در بدترین حالت).
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrapped = runCatching { wrapWithKeystore(bytes) }.getOrNull()
        prefs.edit().putString(WRAPPED_KEY, wrapped ?: Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
        dataKey = SecretKeySpec(bytes, "AES")
        return dataKey!!
    }

    private fun masterKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("hamyar_media_master", null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                "hamyar_media_master",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private fun wrapWithKeystore(plain: ByteArray): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, masterKey())
        val ct = c.doFinal(plain)
        val out = ByteArray(12 + ct.size)
        c.iv.copyInto(out)
        ct.copyInto(out, 12)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun unwrapWithKeystore(wrapped: String?): ByteArray? {
        wrapped ?: return null
        val all = Base64.decode(wrapped, Base64.NO_WRAP)
        if (all.size <= 12) return null
        val iv = all.copyOfRange(0, 12)
        val ct = all.copyOfRange(12, all.size)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, iv))
        return c.doFinal(ct)
    }

    // ------------------------------------------------------ دانلود رمزشده

    /**
     * دانلود از [url] و نوشتنِ رمزشده (AES/CTR با IV تازه) در گاوصندوق.
     * روی دیسک حتی یک بایتِ خام از رسانه نوشته نمی‌شود.
     */
    fun downloadEncrypted(ctx: Context, url: String, cacheKey: String, onProgress: (Long, Long) -> Unit) {
        val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
        }
        conn.connect()
        if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
        // باکت پاسخِ chunked می‌دهد: نه GET و نه HEAD هدرِ Content-Length ندارند،
        // پس اندازه‌ی کل از «Content-Range» یک درخواستِ ۱بایتی خوانده می‌شود تا
        // درصدِ دانلود واقعاً نمایش داده شود.
        var total = conn.contentLengthLong
        if (total <= 0) total = probeSize(url)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, key(ctx), IvParameterSpec(iv))
        val target = vaultFile(ctx, cacheKey)
        val part = File(vaultDir(ctx), "$cacheKey.enc.part")
        var done = 0L
        try {
            java.io.FileOutputStream(part).use { out ->
                out.write(MAGIC.toByteArray(Charsets.US_ASCII))
                out.write(iv)
                conn.inputStream.use { input ->
                    val buf = ByteArray(32 * 1024)
                    var read: Int
                    while (input.read(buf).also { read = it } > 0) {
                        val enc = cipher.update(buf, 0, read)
                        if (enc != null) out.write(enc)
                        done += read
                        onProgress(done, total)
                    }
                    val tail = cipher.doFinal()
                    if (tail != null) out.write(tail)
                }
            }
            if (total > 0 && done < total) error("دانلود کامل نشد ($done از $total بایت).")
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            // تأییدِ سلامت: ابتدای فایل باید واقعاً قابلِ رمزگشایی باشد؛ «تأییدشده»
            // فقط بعد از موفقیتِ بررسی علامت می‌خورد (قبلاً قبل از بررسی علامت می‌خورد).
            val head = peek(ctx, cacheKey)
            if (head == null || head.size < 64) {
                target.delete()
                error("فایل ناقص دانلود شد؛ دوباره تلاش کن.")
            }
            markVerified(ctx, cacheKey)
        } finally {
            part.delete()
        }
    }

    /**
     * اندازه‌ی کلِ فایل از هدرِ `Content-Range`.
     *
     * باکتِ Appwrite پاسخِ chunked می‌دهد (بدونِ `Content-Length`) و rangeِ
     * تک‌بایتی `bytes=0-0` را با **416** رد می‌کند، ولی `bytes=0-1023` را با
     * `206` و هدرِ `Content-Range: bytes 0-1023/<total>` جواب می‌دهد؛ پس کاوش
     * با ۱ کیلوبایت انجام می‌شود (و اگر پاسخِ ۴۱۶ هم «ستاره/اندازه» برگرداند،
     * همان خوانده می‌شود).
     */
    private fun probeSize(url: String): Long = runCatching {
        val c = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            setRequestProperty("Range", "bytes=0-1023")
        }
        c.connect()
        val range = c.getHeaderField("Content-Range")
        val len = c.contentLengthLong
        runCatching { c.inputStream.close() }
        c.disconnect()
        val total = totalFromContentRange(range)
        if (total > 0) total else if (c.responseCode == 200) len else -1L
    }.getOrDefault(-1L)

    /**
     * بازکردنِ جریانِ خوانشِ جسته‌گریخته روی فایلِ گاوصندوق (برای پخش).
     * برخلاف [decryptToMemory] کل فایل را در حافظه نمی‌ریخت و رمزگشایی نمی‌کند؛
     * هر بلاک درست لحظه‌ی درخواستِ پلیر رمزگشایی می‌شود — بنابراین بازکردن آنی است
     * و STATE_BUFFERINGِ ساختگیِ «پخش محلی شروع نشد» از بین می‌رود.
     */
    fun openPlainStream(ctx: Context, cacheKey: String): com.hamyareman.ir.platform.feature.playback.VaultStream? {
        val f = vaultFile(ctx, cacheKey)
        if (!isVerified(ctx, cacheKey) || !f.exists() || f.length() <= 20) return null
        return runCatching {
            val raf = java.io.RandomAccessFile(f, "r")
            val header = ByteArray(20)
            raf.seek(0)
            raf.readFully(header)
            if (String(header, 0, 4, Charsets.US_ASCII) != MAGIC) {
                raf.close()
                return null
            }
            val raw = key(ctx).encoded ?: return null
            com.hamyareman.ir.platform.feature.playback.CtrPlainReader(raw, header.copyOfRange(4, 20), 20L, f.length() - 20, raf)
        }.getOrNull()
    }

    /** رمزگشایی کامل به حافظه — فقط داخل اپ و برای پخش (خروجی هرگز ذخیره نمی‌شود). */
    fun decryptToMemory(ctx: Context, cacheKey: String): ByteArray {
        val f = vaultFile(ctx, cacheKey)
        val all = f.readBytes()
        if (all.size <= 20 || !all.startsWith(MAGIC.toByteArray(Charsets.US_ASCII))) {
            error("فایل گاوصندوق معتبر نیست.")
        }
        val iv = all.copyOfRange(4, 20)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(ctx), IvParameterSpec(iv))
        return cipher.doFinal(all, 20, all.size - 20)
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) if (this[i] != prefix[i]) return false
        return true
    }

    /**
     * آدرسِ پخشِ محلیِ رمزگشایی‌شده‌ی [cacheKey].
     * این آدرس دیگر روی شبکه نمی‌رود: با طرحِ `vault://` مستقیم در همان پروسه و
     * بدون سوکت/پورت/توکن خوانده می‌شود (نگاه کنید به VaultDataSource).
     */
    fun localUrl(ctx: Context, cacheKey: String): String = "vault://$cacheKey"
}

/**
 * تجزیه‌ی هدرِ `Content-Range` برای گرفتنِ اندازه‌ی کل:
 * `bytes 0-1023/23947850` ⇒ 23947850 و `bytes */12345` ⇒ 12345؛
 * در نبودِ عدد (`*` یا هدرِ غایب) ⇒ 1-.
 */
internal fun totalFromContentRange(header: String?): Long {
    val total = header?.substringAfterLast('/', "")?.trim()
    if (total.isNullOrEmpty() || total == "*") return -1L
    return total.toLongOrNull()?.takeIf { it > 0 } ?: -1L
}

/**
 * سرور حلقه‌ی محلی (۱۲۷.۰.۰.۱) — تنها دروازه‌ی پخش فایل‌های رمزشده.
 * به درخواست‌های Range جواب می‌دهد (سازگار با MediaPlayer و ExoPlayer).
 */
object LocalMediaServer {

    private var port: Int = -1
    private var secret: String = ""
    private var lastKey: String? = null
    private var lastBytes: ByteArray? = null
    private var appCtx: Context? = null

    @Synchronized
    fun ensureStarted(ctx: Context) {
        if (port > 0) return
        appCtx = ctx.applicationContext
        secret = (0..3).joinToString("") { "${System.nanoTime()}" } + ctx.packageName
        val sock = ServerSocket(0, 8, java.net.InetAddress.getByName("127.0.0.1"))
        port = sock.localPort
        val server = sock
        Thread {
            while (true) {
                runCatching {
                    val conn = server.accept()
                    Thread { handle(appCtx ?: return@Thread, conn) }.start()
                }.onFailure { runCatching { Thread.sleep(80) } }
            }
        }.apply { isDaemon = true; name = "hamyar-media-server" }.start()
    }

    fun urlFor(ctx: Context, cacheKey: String): String {
        ensureStarted(ctx)
        val token = token(cacheKey)
        return "http://127.0.0.1:$port/v/$cacheKey?t=$token"
    }

    private fun token(cacheKey: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest("$cacheKey#$secret".toByteArray())
        return d.joinToString("") { "%02x".format(it) }.take(24)
    }

    private fun contentType(key: String): String = when {
        key.endsWith(".mp3") -> "audio/mpeg"
        key.endsWith(".m4a") -> "audio/mp4"
        key.endsWith(".mp4") -> "video/mp4"
        key.endsWith(".ogg") -> "audio/ogg"
        else -> "application/octet-stream"
    }

    private fun handle(ctx: Context, conn: Socket) {
        /** پاسخِ خطا — به‌جای بسته‌شدنِ بی‌صدا، پلیر خطای واقعی ببیند و فال‌بک کند. */
        fun fail(code: Int) {
            runCatching {
                conn.getOutputStream().apply {
                    write("HTTP/1.1 $code ERROR\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    flush()
                }
            }
        }
        runCatching {
            val reader = conn.getInputStream().bufferedReader()
            val requestLine = reader.readLine() ?: return
            var rangeStart: Long = -1
            var rangeEnd: Long = -1
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                if (line.startsWith("Range:", ignoreCase = true)) {
                    val m = Regex("bytes=(\\d*)-(\\d*)").find(line.substring(6).trim())
                    if (m != null) {
                        rangeStart = m.groupValues[1].toLongOrNull() ?: -1
                        rangeEnd = m.groupValues[2].toLongOrNull() ?: -1
                    }
                }
            }
            // GET /v/<key>?t=<token>
            val parts = requestLine.split(" ")
            if (parts.size < 2) { fail(400); return }
            val path = parts[1]
            val seg = Regex("/v/([^?]+)\\?t=([0-9a-f]+)").find(path) ?: run { fail(400); return }
            val (cacheKey, token) = seg.destructured
            if (token(cacheKey) != token) { fail(403); return }

            val bytes = synchronized(this) {
                if (cacheKey == lastKey && lastBytes != null) lastBytes!!
                else {
                    val b = try {
                        MediaVault.decryptToMemory(ctx.applicationContext, cacheKey)
                    } catch (e: Exception) {
                        conn.getOutputStream().write(
                            ("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").toByteArray(),
                        )
                        conn.getOutputStream().flush()
                        return
                    }
                    lastKey = cacheKey; lastBytes = b
                    b
                }
            }

            val out = conn.getOutputStream()
            val isHead = parts[0].equals("HEAD", true)
            if (rangeStart >= 0) {
                val start = rangeStart
                val end = if (rangeEnd >= 0) rangeEnd.coerceAtMost(bytes.size - 1L) else bytes.size - 1L
                val len = if (end >= start) (end - start + 1).toInt() else 0
                out.write(
                    (
                        "HTTP/1.1 206 Partial Content\r\n" +
                            "Content-Type: ${contentType(cacheKey)}\r\n" +
                            "Content-Length: $len\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            "Content-Range: bytes $start-$end/${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                        ).toByteArray(),
                )
                if (!isHead && len > 0) out.write(bytes, start.toInt(), len)
            } else {
                out.write(
                    (
                        "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: ${contentType(cacheKey)}\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            "Connection: close\r\n\r\n"
                        ).toByteArray(),
                )
                if (!isHead) out.write(bytes)
            }
            out.flush()
        }
        runCatching { conn.close() }
    }
}
