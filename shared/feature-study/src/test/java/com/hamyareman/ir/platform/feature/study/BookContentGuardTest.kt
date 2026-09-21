package com.hamyareman.ir.platform.feature.study

import com.hamyareman.ir.platform.feature.study.books.EdafaiC915
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * نگهبانِ محتوای «کتاب‌های تازه‌افزوده‌شده»: فهرست، عنوان‌ها، درس‌ها و پک‌های محتوا
 * باید دوتا-دوتا بخوانند تا نه کارتِ مدرسه درسِ خالی نشان دهد و نه لینکی به بن‌بست برود.
 *
 * چرا این تست لازم است: کتابِ «آمادگی دفاعی» (C915) با فهرستِ رسمی و جلد اضافه شد و
 * ممکن است بعداً کسی عنوان یا شمارهٔ درسی را در یکی از سه فایل (BookToc، LessonTitles،
 * ماژولِ محتوا) عوض کند و آن یکی جا بماند — آن‌وقت کارتِ درس یا «بدونِ‌درس» می‌شود یا
 * وارد صفحهٔ «این درس پیدا نشد» می‌رود.
 */
class BookContentGuardTest {

    private val c915: List<BookToc.TocNode> = BookToc.forBook("C915")

    private fun lessonPackIds(nodes: List<BookToc.TocNode>): List<String> =
        nodes.flatMap { n -> (n.packId?.let { listOf(it) } ?: emptyList()) + lessonPackIds(n.children) }

    @Test
    fun `C915 toc has three chapters and eleven lessons`() {
        // ردیف‌های «فصل» در این کتاب، درس نیستند (packId ندارند).
        assertEquals(3, c915.count { it.packId == null })
        val lessons = lessonPackIds(c915)
        assertEquals(11, lessons.size)
        assertEquals("C915_E01-L01", lessons.first())
        assertEquals("C915_E03-L04", lessons.last())
    }

    @Test
    fun `every C915 toc lesson has both a title and a content pack`() {
        val lessons = lessonPackIds(c915)
        val packs = EdafaiC915.packs.associateBy { it.packId }
        for (id in lessons) {
            assertTrue("عنوانی برای $id در LessonTitles نیست", !LessonTitles.titles[id].isNullOrBlank())
            val pack = packs[id]
            assertTrue("پکِ محتوایی برای $id وجود ندارد", pack != null)
            requireNotNull(pack)
            assertEquals("کتابِ پک اشتباه است ($id)", "C915", pack.bookCode)
            assertTrue("پکِ $id بخش/فلش‌کارت/سؤال ندارد", pack.sections.size >= 3 && pack.flashcards.size >= 4 && pack.questions.size >= 4)
        }
        assertEquals(lessons.sorted(), packs.keys.sorted())
    }

    @Test
    fun `C915 packs are internally consistent`() {
        for (pack in EdafaiC915.packs) {
            assertTrue("عنوانِ خالی در ${pack.packId}", pack.title.isNotBlank())
            assertTrue("نامِ کتابِ خالی در ${pack.packId}", pack.bookTitle.isNotBlank())
            assertTrue("نامِ فایلِ PDF خالی در ${pack.packId}", pack.pdfFileName.isNotBlank())
            assertEquals("شناسهٔ فایلِ صوتی با قاعدهٔ نام‌گذاری نمی‌خواند (${pack.packId})",
                "C915_${pack.lessonId}_AUDIO.mp3", pack.audioFileId)

            val sectionIds = pack.sections.map { it.id }.toSet()
            assertEquals("شناسهٔ بخشِ تکراری در ${pack.packId}", pack.sections.size, sectionIds.size)
            for (s in pack.sections) {
                assertTrue("متنِ خالی در بخشِ ${s.id} از ${pack.packId}", s.body.length > 60)
                assertTrue("نوعِ بخش نامعلوم است (${s.kind})", s.kind in setOf("concept", "important", "note", "exam"))
            }

            val questionIds = pack.questions.map { it.id }.toSet()
            assertEquals("شناسهٔ سؤالِ تکراری در ${pack.packId}", pack.questions.size, questionIds.size)
            for (q in pack.questions) {
                assertTrue("مرجعِ بخشِ نامعتبر در سؤالِ ${q.id} از ${pack.packId}",
                    q.refSectionId in sectionIds)
                assertTrue("سطحِ سختی بیرون از ۱..۳ است (${q.id})", q.difficulty in 1..3)
                if (q.type == "mcq") {
                    assertTrue("گزینه‌های سؤالِ چندگزینه‌ای ناقص است (${q.id})", q.options.size >= 3)
                    assertTrue("پاسخِ درست بینِ گزینه‌ها نیست (${q.id})", q.answer in q.options)
                } else {
                    assertTrue("پاسخِ سؤالِ تشریحی خالی است (${q.id})", q.answer.isNotBlank())
                }
            }
            assertTrue("خلاصهٔ درس خالی است (${pack.packId})", pack.summary.length > 40)
            assertTrue("نکاتِ امتحانی خالی است (${pack.packId})", pack.examTips.length > 20)
        }
    }
}
