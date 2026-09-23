package com.hamyareman.ir.ui.home

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.R

/**
 * فونت‌های داشبورد از `res/font` — نقش‌ها طبق انتخاب کاربر:
 *  - greeting / aviny: خوش‌آمد
 *  - clock / estedad: ساعت و تاریخ
 *  - title / titr: عنوان کارت‌ها (برنامه کلاسی و …)
 *  - tile / parastoo: کاشی‌های میانبر
 *  - label / shekari: برچسب‌ها
 *  - content / badkhat: سخن بزرگان، نام درس داخل برنامه، متن ذخیره‌شونده
 *
 * اندازه: شکاری همه‌جا +۱۰، بقیه فونت‌ها همه‌جا +۲ (نسبت به اندازهٔ پایهٔ قبلی).
 */
object DashboardFonts {
    const val LABEL_BUMP = 10
    const val OTHER_BUMP = 2

    val greeting = FontFamily(Font(R.font.aviny, FontWeight.Normal))
    val clock = FontFamily(Font(R.font.estedad_bold, FontWeight.Bold))
    val title = FontFamily(Font(R.font.titr, FontWeight.Normal))
    val tile = FontFamily(Font(R.font.parastoo_bold, FontWeight.Bold))
    val label = FontFamily(Font(R.font.shekari, FontWeight.Normal))
    val content = FontFamily(Font(R.font.badkhat_bold, FontWeight.Bold))

    val quote = label
    val aria = title
    val section = title
    val lalezar = title
    val hilda = content
    val wisdom = content
    val badkhat = content

    fun isLabel(family: FontFamily): Boolean = family === label || family === quote

    fun bump(family: FontFamily, baseSp: Int): TextUnit =
        (baseSp + if (isLabel(family)) LABEL_BUMP else OTHER_BUMP).sp

    fun bump(family: FontFamily, base: TextUnit): TextUnit {
        val v = if (base.isSp) base.value else 14f
        return (v + if (isLabel(family)) LABEL_BUMP else OTHER_BUMP).sp
    }
}
