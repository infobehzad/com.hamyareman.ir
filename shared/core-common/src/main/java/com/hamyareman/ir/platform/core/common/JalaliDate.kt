package com.hamyareman.ir.platform.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * تاریخ جلالی (شمسی).
 *
 * تبدیل با الگوریتم جلالی مستقل از ICU انجام می‌شود؛ چون Android ICU کلاس
 * `PersianCalendar` ندارد و استفاده از آن در زمان کامپایل ممکن نیست.
 *
 * قرارداد پروژه:
 *  - کلیدهای ذخیره‌سازی و محاسبات فاصله‌ی روزها همچنان ISO میلادی هستند ([todayIso]) تا
 *    داده‌های قدیمی نشکنند؛
 *  - هر چیزی که به کاربر نمایش داده می‌شود جلالی و با رقم فارسی است ([formatFa]).
 */
object JalaliDate {

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    data class Jalali(val year: Int, val month: Int, val day: Int) {
        /** «۱۴۰۵/۰۶/۱۵» با رقم فارسی. */
        val fa: String get() = toPersianDigits("%04d/%02d/%02d".format(year, month, day))

        /** «۱۵ شهریور ۱۴۰۵» */
        val faLong: String get() = toPersianDigits("$day ${monthName(month)} $year")

        val isoLike: String get() = "%04d-%02d-%02d".format(year, month, day)
    }

    private val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private val weekDayNames = listOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه",
    )

    fun monthName(month: Int): String = monthNames.getOrElse(month - 1) { "" }

    /** نام روز هفته‌ی یک تاریخ ISO میلادی. */
    fun weekDayFa(isoDate: String): String =
        runCatching { weekDayNames[LocalDate.parse(isoDate).dayOfWeek.value % 7] }.getOrDefault("")

    fun todayIso(): String = LocalDate.now().format(iso)

    fun toJalali(isoDate: String): Jalali? = runCatching {
        val date = LocalDate.parse(isoDate)
        toJalali(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }.getOrNull()

    fun todayJalali(): Jalali = toJalali(System.currentTimeMillis())

    fun toJalali(epochMillis: Long): Jalali {
        val date = LocalDate.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        val converted = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
        return Jalali(converted[0], converted[1], converted[2])
    }

    /** جلالی → میلادی (برای محاسبه‌ی فاصله‌ی روزها و ذخیره‌سازی ISO). */
    fun toGregorianIso(jalali: Jalali): String = runCatching {
        val converted = jalaliToGregorian(jalali.year, jalali.month, jalali.day)
        LocalDate.of(converted[0], converted[1], converted[2]).format(iso)
    }.getOrDefault(todayIso())

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
        val monthDays = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var jy = if (gy > 1600) 979 else 0
        val baseYear = if (gy > 1600) gy - 1600 else gy - 621
        val adjustedYear = if (gm > 2) gy + 1 else gy
        var days = 365 * baseYear + (adjustedYear + 3) / 4 - (adjustedYear + 99) / 100 +
            (adjustedYear + 399) / 400 - 80 + gd + monthDays[gm - 1]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
        return intArrayOf(jy, jm, jd)
    }

    private fun jalaliToGregorian(jyInput: Int, jm: Int, jd: Int): IntArray {
        var jy = jyInput + 1597
        var days = -355668 + 365 * jy + (jy / 33) * 8 + ((jy % 33) + 3) / 4 + jd +
            if (jm < 7) (jm - 1) * 31 else (jm - 1) * 30 + 6
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
        val monthLengths = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var remaining = gd
        var gm = 1
        while (remaining > monthLengths[gm - 1]) {
            remaining -= monthLengths[gm - 1]
            gm++
        }
        return intArrayOf(gy, gm, remaining)
    }

    /** نمایش فارسی یک تاریخ ISO میلادی → «۱۴۰۵/۰۶/۱۵». */
    fun formatFa(isoDate: String): String = toJalali(isoDate)?.fa ?: isoDate.replace("-", "/")

    /** نمایش بلند فارسی → «۱۵ شهریور ۱۴۰۵». */
    /** «۱۴۰۵/۰۶/۱۵» از روی زمان میلی‌ثانیه‌ای (برای مهر پیام‌ها و رویدادها). */
    fun formatFa(epochMillis: Long): String = toJalali(epochMillis).fa

    /** «۱۵ شهریور ۱۴۰۵» از روی زمان میلی‌ثانیه‌ای. */
    fun formatFaLong(epochMillis: Long): String = toJalali(epochMillis).faLong

    /** ساعت با رقم فارسی: «۱۴:۳۵». */
    fun clockFa(epochMillis: Long): String {
        val time = java.time.LocalDateTime.ofInstant(
            Instant.ofEpochMilli(epochMillis),
            ZoneId.systemDefault(),
        )
        return toPersianDigits("%02d:%02d".format(time.hour, time.minute))
    }

    /** «۱۵ شهریور ۱۴۰۵، ۱۴:۳۵» — مهر کامل برای پیام‌ها و گزارش‌ها. */
    fun stampFa(epochMillis: Long): String = "${formatFaLong(epochMillis)}، ${clockFa(epochMillis)}"

    fun formatFaLong(isoDate: String): String = toJalali(isoDate)?.faLong ?: isoDate

    fun daysBetween(fromIso: String, toIso: String): Long = runCatching {
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(fromIso), LocalDate.parse(toIso))
    }.getOrDefault(0)

    /** ۰۱۲۳۴۵۶۷۸۹ → ۰۱۲۳۴۵۶۷۸۹ (رقم فارسی). */
    fun toPersianDigits(input: String): String = buildString(input.length) {
        input.forEach { ch ->
            append(if (ch in '0'..'9') ('۰' + (ch - '0')) else ch)
        }
    }
}
