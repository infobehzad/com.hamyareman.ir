package com.hamyareman.ir.platform.feature.study

/**
 * درس‌های کتاب‌ها بر اساس راهنمای رسمی کتاب‌های درسی مخزن:
 * هر درس PDF کتابش را دارد (نام فایل انگلیسی خلاصه در باکت) و
 * در سرور ایرانی با پوشه‌بندی کامل و در سرور خارجی به صورت مستقیم بارگذاری می‌شود.
 */
object ExtraLessons {

    fun extrasFor(module: BookModule): List<StudyPack> {
        val nodes = BookToc.allPacksForBook(module.bookCode)
        return nodes.map { node ->
            val pid = node.packId ?: "${module.bookCode}_${node.id}"
            val pdfName = node.pdfFileName ?: BookToc.pdfFileName(pid) ?: "${pid}.pdf"
            StudyPack(
                packId = pid,
                bookCode = module.bookCode,
                lessonId = pid.removePrefix("${module.bookCode}_"),
                title = node.title,
                bookTitle = module.title,
                pdfFileName = pdfName,
                sections = emptyList(),
                flashcards = emptyList(),
                questions = emptyList(),
                solutions = emptyList(),
                audioFileId = "${pid}_AUDIO.mp3",
                teachText = "",
                teachSpeech = "",
                summary = "",
                examTips = "",
                exercises = emptyList(),
                pdfOnly = false,
            )
        }
    }
}
