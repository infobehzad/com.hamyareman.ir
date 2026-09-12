package ir.behzad.platform.feature.study

/**
 * درس‌های ۲ به بعد همه‌ی کتاب‌ها (v1.7) — ثبت بر اساس فهرست واقعی باکت:
 * هر درس PDF کتابش را دارد (نام‌ها یک‌به‌یک با فایل‌های سرور چک شده) و
 * صفحه‌ی «تدریس» همان لحظه کار می‌کند (کتاب + صوت به‌محض آپلودشدن).
 *
 * محتوای تعاملی (فلش‌کارت/آزمون/حل) نوبت‌به‌نوبت از PDF بازسازی و به پکِ همان
 * درس اضافه می‌شود — تا آن موقع تب‌ها پیام صادقانه‌ی «به‌زودی» می‌دهند.
 * صوت‌ها با قرارداد شناخته‌شده‌ی `<packId-خط‌دار>-AUDIO.mp3` خوانده می‌شوند؛
 * به‌محض آپلود در باکت، بدون آپدیت اپ روشن می‌شوند.
 */
object ExtraLessons {

    /** lessonId های باقی‌مانده‌ی هر کتاب (درس ۱ هر کتاب از قبل در رجیستری است). */
    private val remaining: Map<String, List<String>> = mapOf(
        "C901" to (2..11).map { "L%02d".format(it) },
        "C902" to listOf(
            "E01-L02", "E02-L01", "E02-L02", "E02-L03", "E03-L01", "E03-L02",
            "E04-L01", "E05-L01", "E05-L02", "E05-L03", "E05-L04",
        ),
        "C903" to listOf(
            "E01-L02", "E01-L03", "E01-L04",
            "E02-L01", "E02-L02", "E02-L03", "E02-L04",
            "E03-L01", "E03-L02", "E03-L03",
            "E04-L01", "E04-L02", "E04-L03", "E04-L04", "E04-L05",
            "E05-L01", "E05-L02", "E05-L03", "E05-L04", "E05-L05",
            "E06-L01", "E06-L02", "E06-L03", "E06-L04", "E06-L05",
            "E07-L01", "E07-L02",
            "E08-L01", "E08-L02", "E08-L03",
        ),
        "C904" to (2..8).map { "L%02d".format(it) },
        "C905" to listOf(
            "E01-L02", "E01-L03", "E01-L04",
            "E02-L01", "E02-L02", "E02-L03",
            "E03-L01", "E03-L02", "E03-L03", "E03-L04", "E03-L05",
            "E04-L01", "E04-L02", "E04-L03", "E04-L04",
            "E05-L01", "E05-L02", "E05-L03",
            "E06-L01", "E06-L02", "E06-L03",
            "E07-L01", "E07-L02", "E07-L03",
            "E08-L01", "E08-L02", "E08-L03",
        ),
        "C906" to (2..15).map { "E%02d-L01".format(it) },
        "C907" to listOf(
            "E01-L02",
            "E02-L01", "E02-L02",
            "E03-L01", "E03-L02",
            "E04-L01", "E04-L02",
            "E05-L01", "E05-L02",
            "E06-L01", "E06-L02",
            "E07-L01", "E07-L02",
            "E08-L01", "E08-L02",
            "E09-L01", "E09-L02",
            "E10-L01", "E10-L02",
            "E11-L01", "E11-L02",
            "E12-L01", "E12-L02",
        ),
        "C908" to listOf(
            "E01-L02", "E01-L03", "E01-L04",
            "E02-L01", "E02-L02", "E02-L03",
            "E03-L01", "E03-L02", "E03-L03",
            "E04-L01", "E04-L02", "E04-L03", "E04-L04",
            "E05-L01", "E05-L02", "E05-L03",
            "E06-L01", "E06-L02", "E06-L03",
            "E07-L01", "E07-L02", "E07-L03",
            "E08-L01", "E08-L02", "E08-L03", "E08-L04", "E08-L05",
            "E09-L01", "E09-L02", "E09-L03",
        ),
        "C909" to (2..10).map { "L%02d".format(it) },
        "C910" to (2..9).map { "L%02d".format(it) },
        "C911" to (2..6).map { "L%02d".format(it) },
        "C917" to listOf("E01-L02", "E01-L03", "E01-L04", "E01-L05", "E02-L01", "E02-L02", "E02-L03", "E02-L04", "E02-L05", "E02-L06"),
        "C941" to (2..12).map { "L%02d".format(it) },
    )

    /** درس‌هایِ هنوز ثبت‌نشده‌ی یک ماژول (PDF واقعی، بدون محتوای تعاملی تا بازسازی). */
    fun extrasFor(module: BookModule): List<StudyPack> {
        val ids = remaining[module.bookCode] ?: return emptyList()
        val unit = if (module.bookCode == "C906") "فصل" else "درس"
        return ids.mapIndexed { i, lessonId ->
            val packId = "${module.bookCode}_$lessonId"
            val n = i + 2 // درس ۱ هر کتاب از قبل ثبت است
            StudyPack(
                packId = packId,
                bookCode = module.bookCode,
                lessonId = lessonId,
                title = "$unit ${ir.behzad.platform.core.common.toPersianDigits(n.toString())}",
                bookTitle = module.title,
                pdfFileName = "${packId}_BOOK.pdf",
                sections = emptyList(),
                flashcards = emptyList(),
                questions = emptyList(),
                solutions = emptyList(),
                audioFileId = "${packId.replace('_', '-')}-AUDIO.mp3",
            )
        }
    }
}
