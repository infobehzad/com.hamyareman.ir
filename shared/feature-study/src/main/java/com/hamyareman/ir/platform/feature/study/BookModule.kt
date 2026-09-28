package com.hamyareman.ir.platform.feature.study

/**
 * ماژول هر کتاب درسی شامل کد، عنوان، موضوع و پک‌های درسی.
 */
data class BookModule(
    val bookCode: String,
    val title: String,
    val subject: String,
    val packs: List<StudyPack> = emptyList(),
)

/**
 * رجیستری مرکزی ماژول کتاب‌ها برای پایه‌های نهم و ششم.
 */
object BookModuleRegistry {

    private val bookDefinitions: List<Pair<String, Pair<String, String>>> = listOf(
        // Grade 9
        "C901" to ("آموزش قرآن" to "قرآن"),
        "C902" to ("پیام‌های آسمان" to "پیام‌ها"),
        "C903" to ("فارسی" to "فارسی"),
        "C904" to ("نگارش" to "نگارش"),
        "C905" to ("ریاضی" to "ریاضی"),
        "C906" to ("علوم تجربی" to "علوم"),
        "C907" to ("مطالعات اجتماعی" to "مطالعات"),
        "C908" to ("فرهنگ و هنر" to "هنر"),
        "C909" to ("عربی، زبان قرآن" to "عربی"),
        "C910" to ("انگلیسی" to "زبان"),
        "C911" to ("کتاب کار انگلیسی" to "زبان"),
        "C915" to ("آمادگی دفاعی" to "دفاعی"),
        "C917" to ("کار و فناوری" to "مهارت"),
        "C941" to ("تفکر و سبک زندگی" to "تفکر"),
        "C902D" to ("هدیه‌های آسمان (دو‌زبانه)" to "هدیه‌ها"),

        // Grade 6
        "C601" to ("فارسی" to "فارسی"),
        "C602" to ("نگارش فارسی" to "نگارش"),
        "C603" to ("ریاضی" to "ریاضی"),
        "C604" to ("علوم تجربی" to "علوم"),
        "C605" to ("هدیه‌های آسمان" to "هدیه‌ها"),
        "C606" to ("آموزش قرآن" to "قرآن"),
        "C607" to ("مطالعات اجتماعی" to "مطالعات"),
        "C608" to ("کار و فناوری" to "مهارت"),
        "C612" to ("تفکّر و پژوهش" to "تفکر"),
        "C613" to ("آموزش خط تحریری" to "هنر"),
    )

    val modules: List<BookModule> = bookDefinitions.map { (code, meta) ->
        val mod = BookModule(
            bookCode = code,
            title = meta.first,
            subject = meta.second,
        )
        mod.copy(packs = ExtraLessons.extrasFor(mod))
    }

    fun pack(packId: String): StudyPack? =
        modules.asSequence().flatMap { it.packs }.firstOrNull { it.packId == packId }

    fun books(): List<BookModule> = modules
}
