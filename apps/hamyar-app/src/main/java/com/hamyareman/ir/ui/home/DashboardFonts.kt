package com.hamyareman.ir.ui.home

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.R

/**
 * فونت‌های داشبورد از `res/font` — نقش‌ها:
 *  - greeting / aviny: خوش‌آمد
 *  - clock / estedad: ساعت و تاریخ
 *  - title / titr: عنوان کارت‌ها
 *  - tile / parastoo: کاشی‌های میانبر
 *  - label / content / badkhat: برچسب، سخن بزرگان، نام درس، متن ذخیره‌شونده
 *    (شکاری حذف شد؛ همه‌ی نقشِ برچسب الان بدخط است)
 *
 * اندازه: همه +۲ نسبت به پایهٔ داشبورد. مقیاس نام‌دار در [HamyarType].
 */
object DashboardFonts {
    const val OTHER_BUMP = 2

    val greeting = FontFamily(Font(R.font.aviny, FontWeight.Normal))
    val clock = FontFamily(Font(R.font.estedad_bold, FontWeight.Bold))
    val title = FontFamily(Font(R.font.titr, FontWeight.Normal))
    val tile = FontFamily(Font(R.font.parastoo_bold, FontWeight.Bold))
    val content = FontFamily(Font(R.font.badkhat_bold, FontWeight.Bold))
    val label = content

    val quote = content
    val aria = title
    val section = title
    val lalezar = title
    val hilda = content
    val wisdom = content
    val badkhat = content

    fun bump(family: FontFamily, baseSp: Int): TextUnit = (baseSp + OTHER_BUMP).sp

    fun bump(family: FontFamily, base: TextUnit): TextUnit {
        val v = if (base.isSp) base.value else 14f
        return (v + OTHER_BUMP).sp
    }
}
