package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.BookToc
import com.hamyareman.ir.ui.profile.StudentProfileState

/**
 * ورود به درس: فهرست و درس۱ هر فصل همیشه باز؛ بعدی‌ها اشتراک یا اتمام تدریس قبلی.
 */
object LessonAccess {

    enum class Gate { Open, NeedSub, NeedPrev }

    fun isPremium(): Boolean {
        val s = StudentProfileState.subscription.trim().lowercase()
        return s.isNotBlank() && s != "free"
    }

    fun isToc(packId: String): Boolean =
        packId.endsWith("_TOC") || packId.substringAfter('_', "") == "TOC"

    fun isAlwaysOpen(packId: String): Boolean {
        if (isToc(packId)) return true
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
        if (!isPremium()) return Gate.NeedSub
        val ids = orderedPackIds(bookCode)
        val i = ids.indexOf(packId)
        var j = i - 1
        while (j >= 0 && (isToc(ids[j]) || BookModuleRegistry.pack(ids[j])?.pdfOnly == true)) j--
        if (j < 0) return Gate.Open
        val prev = ids[j]
        val prevPack = BookModuleRegistry.pack(prev)
        if (prevPack != null && teachTracksOf(prevPack).isNotEmpty()) {
            TeachStats.expectMedia(ctx, prev, expectedTeachMedia(prevPack))
            return if (TeachStats.isDone(ctx, prev)) Gate.Open else Gate.NeedPrev
        }
        return Gate.Open
    }
}
