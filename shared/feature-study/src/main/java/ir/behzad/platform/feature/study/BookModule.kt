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
        ir.behzad.platform.feature.study.books.QuranC901.module,
        ir.behzad.platform.feature.study.books.EslamiC902.module,
        ir.behzad.platform.feature.study.books.FarsiC903.module,
        ir.behzad.platform.feature.study.books.NegarC904.module,
        ir.behzad.platform.feature.study.books.ScienceC906.module,
        ir.behzad.platform.feature.study.books.EjtemaiC907.module,
        ir.behzad.platform.feature.study.books.HonarC908.module,
        ir.behzad.platform.feature.study.books.ArabicC909.module,
        ir.behzad.platform.feature.study.books.EnglishC910.module,
        ir.behzad.platform.feature.study.books.EnglishWbC911.module,
        ir.behzad.platform.feature.study.books.KarC917.module,
        ir.behzad.platform.feature.study.books.TafakkorC941.module,
    )

    fun pack(packId: String): StudyPack? =
        modules.asSequence().flatMap { it.packs }.firstOrNull { it.packId == packId }

    fun books(): List<BookModule> = modules
}
