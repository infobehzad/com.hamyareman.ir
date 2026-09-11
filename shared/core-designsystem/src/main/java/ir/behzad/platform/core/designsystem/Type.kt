package ir.behzad.platform.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * تایپوگرافی پارامتری: فونت از ظاهر/تنظیمات می‌آید (فونت سیستم یا فونت دانلودی).
 * `PlatformTypography` برای سازگاری با کدهای قبلی سرِ جایش می‌ماند.
 */
fun platformTypography(font: FontFamily = FontFamily.Default): Typography = Typography(
    displayLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 58.sp),
    headlineMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
)

val PlatformTypography: Typography = platformTypography()
