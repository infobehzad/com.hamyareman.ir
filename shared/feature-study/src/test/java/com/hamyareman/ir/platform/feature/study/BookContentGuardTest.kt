package com.hamyareman.ir.platform.feature.study

import com.hamyareman.ir.platform.feature.study.books.EdafaiC915
import com.hamyareman.ir.platform.feature.study.books.TafakkorC941
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * نگهبانِ محتوای کتاب‌های درسی: فهرست، عنوان‌ها، درس‌ها و پک‌های محتوا باید
 * سه‌تایی هم‌خوان باشند تا نه کارتِ مدرسه «۰ درس» نشان دهد و نه لینکی به بن‌بست برود.
 *
 * چرا لازم است: هر کتاب در سه جای جدا توصیف می‌شود (`BookToc` برای درختِ فهرست،
 * `LessonTitles` برای عنوانِ نمایشی، و ماژولِ کتاب برای محتوای تعاملی). اگر کسی
 * یکی را عوض کند و دیگری جا بماند، کاربر در اپ نتیجه‌اش را می‌بیند — این تست آن را
 * پیش از انتشار می‌گیرد.
 */
class BookContentGuardTest {

    private fun lessonPackIds(nodes: List<BookToc.TocNode>): List<String> =
        nodes.flatMap { n -> (n.packId?.let { listOf(it) } ?: emptyList()) + lessonPackIds(n.children) }

    /** بررسیِ کاملِ یک کتاب: فهرست ↔ عنوان ↔ محتوا. */
    private fun assertBook(
        bookCode: String,
        packs: List<StudyPack>,
        chapterCount: Int,
        lessonCount: Int,
    ) {
        val toc = BookToc.forBook(bookCode)
        assertTrue("فهرستِ $bookCode در BookToc نیست", toc.isNotEmpty())
        assertEquals("شمارِ فصل‌های $bookCode", chapterCount, toc.count { it.packId == null })

        val lessons = lessonPackIds(toc)
        assertEquals("شمارِ درس‌های $bookCode", lessonCount, lessons.size)

        val byPackId = packs.associateBy { it.packId }
        assertEquals("دروسِ فهرست و پک‌های محتوا یکی نیستند ($bookCode)", lessons.sorted(), byPackId.keys.sorted())

        for (id in lessons) {
            assertTrue("عنوانی برای $id در LessonTitles نیست", !LessonTitles.titles[id].isNullOrBlank())
            val pack = byPackId.getValue(id)
            assertEquals("کتابِ پک اشتباه است ($id)", bookCode, pack.bookCode)
            assertEquals("شناسهٔ پک با درس نمی‌خواند ($id)", "${bookCode}_${pack.lessonId}", id)
            assertTrue("$id درس‌نامه ندارد", pack.sections.size >= 3)
            assertTrue("$id فلش‌کارت کافی ندارد", pack.flashcards.size >= 4)
            assertTrue("$id پرسش کافی ندارد", pack.questions.size >= 4)
            assertTrue("خلاصهٔ درس خالی است ($id)", pack.summary.length > 40)
            assertTrue("نکاتِ امتحانی خالی است ($id)", pack.examTips.length > 20)
            assertEquals("شناسهٔ فایلِ صوتیِ $id با قاعده نمی‌خواند", "${bookCode}_${pack.lessonId}_AUDIO.mp3", pack.audioFileId)

            val sectionIds = pack.sections.map { it.id }.toSet()
            assertEquals("شناسهٔ بخشِ تکراری در $id", pack.sections.size, sectionIds.size)
            for (s in pack.sections) {
                assertTrue("متنِ کوتاه در بخشِ ${s.id} از $id", s.body.length > 60)
                assertTrue("نوعِ بخشِ نامعلوم در $id (${s.kind})", s.kind in setOf("concept", "important", "note", "exam"))
            }
            assertEquals("شناسهٔ فلش‌کارتِ تکراری در $id", pack.flashcards.size, pack.flashcards.map { it.id }.toSet().size)
            assertEquals("شناسهٔ سؤالِ تکراری در $id", pack.questions.size, pack.questions.map { it.id }.toSet().size)
            for (q in pack.questions) {
                assertTrue("مرجعِ بخشِ نامعتبر در ${q.id} از $id", q.refSectionId in sectionIds)
                assertTrue("سطحِ سختی بیرون از ۱..۳ (${q.id})", q.difficulty in 1..3)
                if (q.type == "mcq") {
                    assertTrue("گزینه‌های ناقص در ${q.id}", q.options.size >= 3)
                    assertTrue("پاسخِ درست بینِ گزینه‌ها نیست (${q.id})", q.answer in q.options)
                } else {
                    assertTrue("پاسخِ سؤالِ تشریحی خالی است (${q.id})", q.answer.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `C915 آمادگی دفاعی — ۳ فصل، ۱۱ درس`() {
        assertBook("C915", EdafaiC915.packs, chapterCount = 3, lessonCount = 11)
    }

    @Test
    fun `C941 از من تا خدا — ۱۲ درس`() {
        // این کتاب فصل‌بندیِ ردیف‌دار ندارد؛ کلِ فهرست درس‌های پشت‌سرهم است.
        assertBook("C941", TafakkorC941.packs, chapterCount = 0, lessonCount = 12)
    }

    @Test
    fun `همهٔ ماژول‌های کتاب سالم و خودسازگارند`() {
        // بررسیِ سبک برای همهٔ ماژول‌ها (کتاب‌های قدیمی‌تر ممکن است عنوانِ همهٔ
        // درس‌هایشان در `LessonTitles` نباشد؛ آن‌ها اینجا فقط از نظرِ «سالم‌بودنِ
        // خودِ پک» بررسی می‌شوند. بررسیِ کاملِ فهرست ↔ عنوان ↔ محتوا در دو تستِ
        // اختصاصیِ C915 و C941 انجام می‌شود.)
        val modules = BookModuleRegistry.modules
        assertTrue("ماژول‌های کتاب خالی است", modules.size >= 10)
        for (module in modules) {
            assertTrue("پکِ تکراری در ${module.bookCode}", module.packs.size == module.packs.map { it.packId }.toSet().size)
            for (pack in module.packs) {
                assertTrue("پکِ ${pack.packId} به کتابِ ${module.bookCode} تعلق ندارد", pack.packId.startsWith("${module.bookCode}_"))
                assertEquals("کتابِ پک اشتباه است (${pack.packId})", module.bookCode, pack.bookCode)
                assertTrue("پکِ ${pack.packId} درس‌نامه ندارد", pack.sections.isNotEmpty())
                assertTrue("پکِ ${pack.packId} فلش‌کارت ندارد", pack.flashcards.isNotEmpty())
                assertTrue("پکِ ${pack.packId} پرسش ندارد", pack.questions.isNotEmpty())
                assertEquals(
                    "شناسهٔ سؤالِ تکراری در ${pack.packId}",
                    pack.questions.size,
                    pack.questions.map { it.id }.toSet().size,
                )
            }
        }
    }
}
