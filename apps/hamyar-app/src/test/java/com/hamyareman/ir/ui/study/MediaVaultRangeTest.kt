package com.hamyareman.ir.ui.study

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اندازه‌ی کلِ فایل‌های باکت از `Content-Range` خوانده می‌شود (پاسخ chunked است و
 * `Content-Length` ندارد) — این تجزیه باید دقیق باشد وگرنه درصدِ دانلود نشان
 * داده نمی‌شود.
 */
class MediaVaultRangeTest {

    @Test
    fun `total from a normal 206 content-range`() {
        assertEquals(23_947_850L, totalFromContentRange("bytes 0-1023/23947850"))
        assertEquals(27_903_834L, totalFromContentRange("bytes 0-1023/27903834"))
    }

    @Test
    fun `total from a 416 star-range response`() {
        assertEquals(12_345L, totalFromContentRange("bytes */12345"))
    }

    @Test
    fun `unknown total stays -1`() {
        assertEquals(-1L, totalFromContentRange(null))
        assertEquals(-1L, totalFromContentRange(""))
        assertEquals(-1L, totalFromContentRange("bytes 0-1023/*"))
        assertEquals(-1L, totalFromContentRange("bytes 0-1023/abc"))
    }

    /**
     * زمان‌بندیِ HTML درس‌ها باید با `TeachSeekMap` یکی باشد — وگرنه لمسِ فهرست،
     * پلیر را به دقیقه‌ثانیه‌ی غلط می‌برد. (اگر فایلِ asset در دسترس نبود، رد می‌شود.)
     */
    @Test
    fun `lesson-1 html seek times match the seek map`() {
        val asset = File("src/main/assets/math/c905/ryazif01d01.html")
        if (!asset.exists()) return
        val times = TeachSeekMap.times("C905_E01-L01")
        assertEquals(10, times.size)
        val inHtml = Regex("""data-seek-ms="(\d+)"""")
            .findAll(asset.readText())
            .map { it.groupValues[1].toLong() }
            .toList()
        // ۱۰ لینکِ فهرست؛ دو «data-seek-ms» باقی‌مانده داخلِ اسکریپتِ خودِ فایل است.
        assertTrue("HTML times: $inHtml vs map: $times", inHtml.containsAll(times))
        assertEquals(65_000L, times.first())
        assertEquals(1_430_000L, times.last())
    }
}
