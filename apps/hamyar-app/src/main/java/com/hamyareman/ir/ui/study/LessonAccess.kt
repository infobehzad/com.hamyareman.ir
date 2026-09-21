package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.BookToc
import com.hamyareman.ir.ui.profile.StudentProfileState

/**
 * ورود به درس: درس اول هر فصل و کل فصل ۱ ریاضی رایگان؛ بقیه نیاز به اشتراک.
 * قفل سیک/سرعت/سربرگ اعمال نمی‌شود.
 */
object LessonAccess {

    enum class Gate { Open, NeedSub }

    fun isPremium(): Boolean {
        val s = StudentProfileState.subscription.trim().lowercase()
        return s.isNotBlank() && s != "free"
    }

    fun isToc(packId: String): Boolean =
        packId.endsWith("_TOC") || packId.substringAfter('_', "") == "TOC"

    fun isAlwaysOpen(packId: String): Boolean {
        if (isToc(packId)) return true
        val pack = BookModuleRegistry.pack(packId)
        if (pack?.pdfOnly == true) return true
        // کل فصل ۱ ریاضی نهم (درس‌ها + جمع‌بندی)
        if (packId.startsWith("C905_E01")) return true
        val lesson = packId.substringAfter('_', packId)
        return lesson == "L01" || lesson.endsWith("-L01") || lesson.endsWith("_L01")
    }

    fun orderedPackIds(bookCode: String): List<String> {
        fun walk(n: BookToc.TocNode): List<String> {
            val self = n.packId?.let { listOf(it) } ?: emptyList()
            return self + n.children.flatMap { walk(it) }
        }
        val fromToc = BookToc.forBook(bookCode).flatMap { walk(it) }
        if (fromToc.isNotEmpty()) return fromToc
        return BookModuleRegistry.modules.firstOrNull { it.bookCode == bookCode }?.packs?.map { it.packId }.orEmpty()
    }

    fun gate(ctx: Context, bookCode: String, packId: String): Gate {
        if (isAlwaysOpen(packId)) return Gate.Open
        if (isPremium()) return Gate.Open
        return Gate.NeedSub
    }
}
