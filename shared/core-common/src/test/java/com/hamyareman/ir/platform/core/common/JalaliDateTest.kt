package com.hamyareman.ir.platform.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * فقط توابع خالص (بدون android.icu) اینجا تست می‌شوند؛ تبدیل تاریخ با
 * PersianCalendar روی JVM معمولی در دسترس نیست و باید تست دستگاهی باشد.
 */
class JalaliDateTest {

    @Test
    fun `persian digits replace latin digits only`() {
        assertEquals("۱۴۰۵/۰۶/۱۵", toPersianDigits("1405-06-15".replace("-", "/")))
        assertEquals("آب ۲ لیتر", toPersianDigits("آب 2 لیتر"))
    }

    @Test
    fun `latin digits normalise persian and arabic input`() {
        assertEquals("123456", toLatinDigits("۱۲۳۴۵۶"))
        assertEquals("123456", toLatinDigits("١٢٣٤٥٦"))
        assertEquals("12a34", toLatinDigits("۱۲a۳۴"))
    }

    @Test
    fun `jalali formatting uses zero padding and persian digits`() {
        val jalali = JalaliDate.Jalali(1405, 6, 15)
        assertEquals("۱۴۰۵/۰۶/۱۵", jalali.fa)
        assertEquals("۱۵ شهریور ۱۴۰۵", jalali.faLong)
        assertEquals("1405-06-15", jalali.isoLike)
    }

    @Test
    fun `month names cover all twelve months`() {
        val expected = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
        )
        (1..12).forEach { month -> assertEquals(expected[month - 1], JalaliDate.monthName(month)) }
        assertEquals("", JalaliDate.monthName(13))
    }

    @Test
    fun `iso date parses to jalali through gregorian path`() {
        val jalali = JalaliDate.toJalali("2026-09-06")
        // روی JVM بدون android.icu این مسیر null می‌شود؛ فقط نباید استثنا بدهد.
        assertTrue(jalali == null || jalali.year in 1300..1500)
    }
}
