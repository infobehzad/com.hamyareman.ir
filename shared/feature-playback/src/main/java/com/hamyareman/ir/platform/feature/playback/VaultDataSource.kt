package com.hamyareman.ir.platform.feature.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * پخشِ مستقیم از «گاوصندوق رسانه» با URI ساده‌ی `vault://<cacheKey>`.
 *
 * تا پیش از این، فایلِ دانلودشده از راه یک سرور HTTPِ دست‌ساز روی ۱۲۷.۰.۰.۱
 * پخش می‌شد (سوکت + پورت + توکن + پاسخ‌دستی). هر خطای کوچک در آن مسیر باعث
 * می‌شد پلیر **بی‌صدا** شکست بخورد. این کلاس آن لایه را کلاً حذف می‌کند:
 * فایلِ رمزشده در همان پروسه رمزگشایی و مستقیم به ExoPlayer داده می‌شود.
 *
 * لایه‌ی اپ (که `MediaVault` را می‌شناسد) فقط یک تابع به [VaultSourceHooks] می‌دهد؛
 * بنابراین ماژولِ پخش به ماژولِ اپ وابسته نمی‌شود.
 */
object VaultSourceHooks {
    /** cacheKey → بایت‌های رمزگشایی‌شده؛ اگر null برگردد یعنی فایل در دسترس نیست. */
    @Volatile
    var decrypt: ((String) -> ByteArray?)? = null
}

internal fun vaultKeyOf(uri: Uri): String =
    (uri.schemeSpecificPart ?: uri.toString()).removePrefix("//")

/** کارخانه‌ی منبعی که `vault://` را خودش و بقیه را به [default] می‌سپارد. */
@UnstableApi
fun vaultAwareDataSourceFactory(default: DataSource.Factory): DataSource.Factory =
    DataSource.Factory {
        object : DataSource {
            private var active: DataSource? = null

            override fun addTransferListener(transferListener: TransferListener) = Unit

            override fun open(dataSpec: DataSpec): Long {
                active = if (dataSpec.uri.scheme == "vault") {
                    VaultDataSource()
                } else {
                    default.createDataSource()
                }
                return active!!.open(dataSpec)
            }

            override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int =
                active!!.read(buffer, offset, readLength)

            override fun getUri(): Uri? = active?.uri

            override fun close() {
                runCatching { active?.close() }
                active = null
            }
        }
    }

/** یک منبعِ ساده روی حافظه — بدون سوکت و بدون HTTP. */
@UnstableApi
private class VaultDataSource : DataSource {

    private var data: ByteArray? = null
    private var uri: Uri? = null
    private var pos = 0
    private var end = 0

    override fun addTransferListener(transferListener: TransferListener) = Unit

    override fun open(dataSpec: DataSpec): Long {
        val key = vaultKeyOf(dataSpec.uri)
        val bytes = VaultSourceHooks.decrypt?.invoke(key)
            ?: throw java.io.IOException("فایلِ گاوصندوق در دسترس نیست: $key")
        data = bytes
        uri = dataSpec.uri
        val start = dataSpec.position.toInt().coerceIn(0, bytes.size)
        val length = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            bytes.size - start
        } else {
            dataSpec.length.toInt().coerceAtMost(bytes.size - start)
        }.coerceAtLeast(0)
        pos = start
        end = start + length
        return length.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        val d = data ?: return C.RESULT_END_OF_INPUT
        if (pos >= end) return C.RESULT_END_OF_INPUT
        val n = readLength.coerceAtMost(end - pos)
        System.arraycopy(d, pos, buffer, offset, n)
        pos += n
        return n
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        data = null
        uri = null
        pos = 0
        end = 0
    }
}

/** سازنده‌ی MediaSourceِ آگاه به گاوصندوق — برای پلیرهایی که خودشان ExoPlayer می‌سازند. */
@UnstableApi
fun vaultAwareMediaSourceFactory(context: android.content.Context): DefaultMediaSourceFactory =
    DefaultMediaSourceFactory(vaultAwareDataSourceFactory(DefaultDataSource.Factory(context)))
