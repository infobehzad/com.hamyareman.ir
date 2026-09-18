package com.hamyareman.ir.ui.home

import android.content.Context
import com.hamyareman.ir.platform.core.common.LocalStore
import kotlin.random.Random

data class WisdomLine(val text: String, val author: String) {
    fun oneLine(): String = "$text — $author"
}

object WisdomQuotes {
    private const val PREF = "hamyar_quotes"
    private const val KEY_IDX = "idx"
    private const val KEY_UNTIL = "until"

    fun load(context: Context): List<WisdomLine> {
        val raw = runCatching {
            context.assets.open("quotes/sokhanan.txt").bufferedReader(Charsets.UTF_8).readText()
        }.getOrDefault("")
        return raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { line ->
            val parts = line.split(" — ", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            WisdomLine(parts[0].trim(), parts[1].trim())
        }.toList()
    }

    fun current(context: Context, all: List<WisdomLine>): WisdomLine {
        if (all.isEmpty()) return WisdomLine("همیار من کنارت است.", "")
        val store = LocalStore(context, PREF)
        val now = System.currentTimeMillis()
        var idx = store.getInt(KEY_IDX, -1)
        var until = store.getLong(KEY_UNTIL, 0L)
        if (idx !in all.indices || now >= until) {
            idx = Random.nextInt(all.size)
            val hours = 3.0 + Random.nextDouble() // ۳ تا ۴ ساعت
            until = now + (hours * 3_600_000L).toLong()
            store.putInt(KEY_IDX, idx)
            store.putLong(KEY_UNTIL, until)
        }
        return all[idx]
    }

    fun remainingMs(context: Context): Long {
        val until = LocalStore(context, PREF).getLong(KEY_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(1_000L)
    }
}
