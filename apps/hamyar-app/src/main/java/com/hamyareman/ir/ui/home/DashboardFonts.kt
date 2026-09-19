package com.hamyareman.ir.ui.home

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.hamyareman.ir.R

/** فونت‌های My-Font برای کارت خوش‌آمد و سخنان. */
object DashboardFonts {
    val greeting = FontFamily(Font(R.font.delbar, FontWeight.Normal))
    val quote = FontFamily(Font(R.font.aria_semibold, FontWeight.SemiBold))
    val aria = quote
    val lalezar = FontFamily(Font(R.font.lalezar, FontWeight.Normal))
    /** فونت هیلدا — متن‌های دست‌نویس‌گونه (گزارش امتحان، دفتر نکات). */
    val hilda = FontFamily(Font(R.font.hilda, FontWeight.Normal))
    /** فونت عنوانِ بخش‌ها (مثل «خواب — دعوت به خواب آرام»، تب‌ها، «کلاس مجازی»). */
    val section = quote
}
