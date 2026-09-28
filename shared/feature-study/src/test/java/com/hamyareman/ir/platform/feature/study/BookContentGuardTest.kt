package com.hamyareman.ir.platform.feature.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * نگهبانِ محتوای کتاب‌های درسی: فهرست، عنوان‌ها، درس‌ها و پک‌های محتوا باید
 * سه‌تایی هم‌خوان باشند تا نه کارتِ مدرسه «۰ درس» نشان دهد و نه لینکی به بن‌بست برود.
 */
class BookContentGuardTest {

    @Test
    fun `رجیستریِ کتاب‌ها کامل و بدونِ تکرار است`() {
        val modules = BookModuleRegistry.modules
        assertTrue("تعدادِ ماژول‌های کتاب باید شامل نهم و ششم باشد", modules.size >= 20)
        assertEquals("کدِ کتابِ تکراری در رجیستری", modules.size, modules.map { it.bookCode }.toSet().size)
        for (m in modules) {
            assertTrue("عنوانِ کتابِ ${m.bookCode} خالی است", m.title.isNotBlank())
            assertTrue("کتابِ ${m.bookCode} هیچ پکی ندارد", m.packs.isNotEmpty())
            assertEquals("شناسهٔ پکِ تکراری در ${m.bookCode}", m.packs.size, m.packs.map { it.packId }.toSet().size)
        }
    }
}
