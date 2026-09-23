package com.hamyareman.ir.ui.home

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.hamyareman.ir.R

/**
 * فونت‌های داشبورد از `res/font` — نقش‌ها طبق انتخاب کاربر:
 *  - greeting / aviny: خوش‌آمد
 *  - clock / estedad: ساعت و تاریخ
 *  - title / titr: عنوان کارت‌ها (برنامه کلاسی و …)
 *  - tile / parastoo: کاشی‌های میانبر
 *  - label / shekari: برچسب‌ها
 *  - content / badkhat: سخن بزرگان، نام درس داخل برنامه، متن ذخیره‌شونده
 */
object DashboardFonts {
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
}
