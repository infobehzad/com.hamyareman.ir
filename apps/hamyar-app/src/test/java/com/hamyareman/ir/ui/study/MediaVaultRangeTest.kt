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
        val inHtml = Regex("data-seek-ms=\"(\\d+)\"")
            .findAll(asset.readText())
            .map { it.groupValues[1].toLong() }
            .toList()
        // ۱۰ لینکِ فهرست؛ دو «data-seek-ms» باقی‌مانده داخلِ اسکریپتِ خودِ فایل است.
        assertTrue("HTML times: $inHtml vs map: $times", inHtml.containsAll(times))
        assertEquals(57_000L, times.first())
        assertEquals(1_495_000L, times.last())
    }
    /**
     * نگهبانِ همگامی: `data-seek-ms`های HTML تدریس باید دقیقاً همان فهرست‌های
     * زمان‌بندیِ پوشهٔ «Books/Base-09/ریاضی/04- صوت تدریس» (فایل‌های
     * `ryazif01….txt`) باشند — وگرنه لمسِ فهرست، پلیر را به دقیقه‌ثانیه‌ی غلط
     * می‌برد. (اگر پوشه‌ی Books در دسترس نبود، تست سکوت می‌کند.)
     */
    @Test
    fun `html seek times match the Books timing lists`() {
        val names = listOf("ryazif01d01", "ryazif01d02", "ryazif01d03", "ryazif01d04", "ryazif01review")
        var checked = 0
        for (n in names) {
            val txt = File("../../Books/Base-09/ریاضی/04- صوت تدریس/$n.txt")
            val html = File("src/main/assets/math/c905/$n.html")
            if (!txt.exists() || !html.exists()) continue
            val want = parseTimingList(txt.readText())
            val got = Regex("data-seek-ms=\"(\\d+)\"")
                .findAll(html.readText())
                .map { it.groupValues[1].toLong() }
                .toList()
            assertEquals("$n — فهرستِ زمان‌بندیِ Books با HTML یکی نیست", want, got)
            checked++
        }
        assertTrue("هیچ فهرستِ زمان‌بندی پیدا نشد", checked >= 1)
    }

    /** «۱. 0:57 — عنوان» ⇒ 57000 (میلی‌ثانیه) — با ارقامِ فارسی یا لاتین. */
    private fun parseTimingList(text: String): List<Long> = text.lineSequence().mapNotNull { line ->
        val s = line.map { c -> if (c in '\u06F0'..'\u06F9') '0' + (c - '\u06F0') else c }.joinToString("")
        Regex("""(\d{1,3}):(\d{2})""").find(s)
            ?.let { m -> m.groupValues[1].toLong() * 60_000 + m.groupValues[2].toLong() * 1000 }
    }.toList()
}
