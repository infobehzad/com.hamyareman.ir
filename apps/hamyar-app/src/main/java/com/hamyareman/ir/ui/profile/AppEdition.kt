package com.hamyareman.ir.ui.profile

/**
 * هویت این نسخهٔ اپ — هر پایه یک کپی از ریپو است.
 * برای پایهٔ دیگر: همین سه مقدار را عوض کن، آیکون را با عدد فارسی آن پایه بگذار،
 * و فایل‌های Books/assets همان پایه را آپلود کن. بقیهٔ کد دست نخورد.
 */
object AppEdition {
    val grade: GradeLevel = GradeLevel.G9
    const val faNumeral: String = "۹"
    const val booksFolder: String = "Base-09"
    val gradeFa: String get() = grade.fa
}
