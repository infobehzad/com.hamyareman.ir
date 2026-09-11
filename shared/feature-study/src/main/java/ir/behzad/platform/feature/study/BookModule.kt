package ir.behzad.platform.feature.study

/**
 * ماژول هر کتاب — معماری مصوب: هر کتاب یک فایل Kotlin با کل محتوای درس‌هایش
 * (سکشن/فلش‌کارت/سوال/حل داخل کد). فصل‌های جدید به همان فایل اضافه می‌شوند.
 */
data class BookModule(
    val bookCode: String,
    val title: String,
    val subject: String,
    val packs: List<StudyPack>,
)

/**
 * رجیستری مرکزی ماژول کتاب‌ها — [StudyPackRepository] اول اینجا را می‌گردد.
 * کتاب جدید = ماژول جدید + افزودن به این فهرست.
 */
object BookModuleRegistry {
    val modules: List<BookModule> = listOf(
        ir.behzad.platform.feature.study.books.MathC905.module,
    )

    fun pack(packId: String): StudyPack? =
        modules.asSequence().flatMap { it.packs }.firstOrNull { it.packId == packId }

    fun books(): List<BookModule> = modules
}
