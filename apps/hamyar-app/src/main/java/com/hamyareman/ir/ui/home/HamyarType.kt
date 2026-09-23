package com.hamyareman.ir.ui.home

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * تایپوگرافی مرکزی اپ — مرجع اندازه‌ها صفحهٔ داشبورد است.
 * برای همهٔ پایه‌ها (چهارم تا دوازدهم) یکی است؛ از این استایل‌ها استفاده کن
 * به‌جای عدد sp پراکنده.
 *
 *  h1      ۲۴sp  عنوان اصلی (خوش‌آمد داشبورد پس از +۲)
 *  h2      ۱۸sp  عنوان کارت / بخش
 *  body    ۱۶sp  متن بدنه
 *  caption ۱۴sp  برچسب / توضیح
 *  button  ۱۶sp  دکمه
 *
 * فونت بدنه و برچسب و دکمه = بدخط (جایگزین شکاری).
 * عنوان h1/h2 = تیتر.
 */
object HamyarType {
    val h1 = TextStyle(
        fontFamily = DashboardFonts.title,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    )
    val h2 = TextStyle(
        fontFamily = DashboardFonts.title,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    )
    val body = TextStyle(
        fontFamily = DashboardFonts.content,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    val caption = TextStyle(
        fontFamily = DashboardFonts.content,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val button = TextStyle(
        fontFamily = DashboardFonts.content,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
}
