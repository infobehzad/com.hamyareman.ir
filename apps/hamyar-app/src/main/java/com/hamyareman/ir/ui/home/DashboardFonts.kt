package com.hamyareman.ir.ui.home

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.hamyareman.ir.R

/**
 * فونت‌ها از پوشهٔ `res/font` — یکدست برای هر نقش:
 *  - greeting / delbar: خوش‌آمد دست‌نویس
 *  - aria: عنوان بخش‌ها و کارت‌ها
 *  - lalezar: نام درس در خانه‌های برنامه
 *  - hilda / wisdom: سخن بزرگان و دفتر نکات
 *    (فونت درخواستی «بدخط بولد» در ریپوی خصوصی My-Font از این محیط قابل دانلود نبود؛
 *     هیلدا نزدیک‌ترین دست‌نویس موجود است. با گذاشتن `badkhat_bold.ttf` در res/font عوض می‌شود.)
 */
object DashboardFonts {
    val greeting = FontFamily(Font(R.font.delbar, FontWeight.Normal))
    val quote = FontFamily(Font(R.font.aria_semibold, FontWeight.SemiBold))
    val aria = quote
    val lalezar = FontFamily(Font(R.font.lalezar, FontWeight.Normal))
    val hilda = FontFamily(Font(R.font.hilda, FontWeight.Normal))
    val wisdom = hilda
    val section = aria
}
