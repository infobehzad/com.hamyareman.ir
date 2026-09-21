package com.hamyareman.ir.platform.feature.study.books

import com.hamyareman.ir.platform.feature.study.BookModule

/**
 * کتاب «آمادگی دفاعی پایه نهم» (کد ۹۱۵) — فصل‌ها و درس‌ها از فهرست رسمی کتاب.
 *
 * محتوای تعاملیِ هر درس به‌مرور اضافه می‌شود؛ الان هر درس یک پکِ اسکلت است
 * (PDFِ رسمی و صوتِ تدریس از باکت، و عنوان‌ها از `LessonTitles`/`BookToc`).
 */
object EdafaiC915 {
    val packs: List<com.hamyareman.ir.platform.feature.study.StudyPack> = emptyList()

    val module = BookModule(
        bookCode = "C915",
        title = "آمادگی دفاعی پایه نهم",
        subject = "",
        packs = packs,
    )
}
