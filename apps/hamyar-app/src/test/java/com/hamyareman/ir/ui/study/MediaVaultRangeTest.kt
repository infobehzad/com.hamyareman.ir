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
        val inHtml = Regex("""data-seek-ms="(\d+)""")
            .findAll(asset.readText())
            .map { it.groupValues[1].toLong() }
            .toList()
        assertEquals("HTML times vs map", times, inHtml)
        assertEquals(57_000L, times.first())
        assertEquals(1_495_000L, times.last())
    }

    /**
     * نگهبانِ همگامی: `data-seek-ms`های HTML تدریس باید دقیقاً همان فهرست‌های
     * زمان‌بندیِ پوشهٔ صوتِ تدریس در Books باشند. این بررسی «داده‌محور» است: هر
     * فایلِ `ryazif….txt` که HTML هم‌نامش در assets باشد مقایسه می‌شود، پس با
     * افزودنِ درس‌های تازه (فصلِ ۲ و ۳) خودبه‌خود پوشش داده می‌شوند.
     * HTMLهایی که هنوز `data-seek-ms` ندارند (فهرستِ زمانشان نرسیده) رد می‌شوند
     * تا بیلدِ کاربر بی‌دلیل قرمز نشود.
     */
    @Test
    fun `html seek times match the Books timing lists`() {
        val dir = File("../../Books/Base-09/ریاضی/04- صوت تدریس")
        if (!dir.isDirectory) return
        var checked = 0
        var pending = 0
        for (txt in dir.listFiles().orEmpty().sortedBy { it.name }) {
            val name = txt.nameWithoutExtension
            if (!txt.name.endsWith(".txt") || !name.startsWith("ryazif")) continue
            val html = File("src/main/assets/math/c905/$name.html")
            if (!html.exists()) continue
            val got = Regex("""data-seek-ms="(\d+)""")
                .findAll(html.readText())
                .map { it.groupValues[1].toLong() }
                .toList()
            if (got.isEmpty()) { pending++; continue }
            val want = parseTimingList(txt.readText())
            assertEquals("$name — فهرستِ زمان‌بندیِ Books با HTML یکی نیست", want, got)
            checked++
        }
        assertTrue("هیچ HTML همگام‌شده‌ای پیدا نشد", checked >= 5)
    }

    /**
     * «۱. ۰:۵۷ — عنوان» ⇒ 57000 میلی‌ثانیه. بردبار: ارقامِ فارسی/عربی، فاصلهٔ
     * اختیاری کنارِ دونقطه («۱۴ :۴۵») و ثانیهٔ تک‌رقمی («۲:۸» = ۲ دقیقه و ۸ ثانیه).
     */
    private fun parseTimingList(text: String): List<Long> = text.lineSequence().mapNotNull { line ->
        val s = line.map { c ->
            when (c) {
                in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                else -> c
            }
        }.joinToString("")
        Regex("""(\d{1,3})\s*:\s*(\d{1,2})(?!\d)""").find(s)?.let { m ->
            val sec = m.groupValues[2].toLong()
            if (sec < 60) m.groupValues[1].toLong() * 60_000 + sec * 1000 else null
        }
    }.toList()
}
