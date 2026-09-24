package com.hamyareman.ir.ui.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.ui.AppTypography

/** انتخاب فونت/سایز هر اسلات + حل زنجیرهٔ fallback. */
object TypeSlots {
    var rev by mutableIntStateOf(0)
        private set
    var currentRoute by mutableStateOf("home")

    private var choices: Map<String, SlotChoice> = emptyMap()
    private val familyCache = mutableMapOf<String, FontFamily>()

    fun load(map: Map<String, SlotChoice>) {
        choices = map
        familyCache.clear()
        rev++
        AppTypography.applyRoute(currentRoute, map)
    }

    fun choice(id: String): SlotChoice? = choices[id]

    fun resolved(id: String): SlotChoice {
        val _r = rev
        var cur: String? = id
        val seen = HashSet<String>()
        while (cur != null && seen.add(cur)) {
            choices[cur]?.let { return it }
            val slot = FontCatalog.slot(cur) ?: break
            if (slot.fallbackId == cur) {
                return SlotChoice(slot.defaultFont, 0)
            }
            cur = slot.fallbackId
        }
        val slot = FontCatalog.slot(id)
        return SlotChoice(slot?.defaultFont ?: "badkhat_bold", 0)
    }

    fun family(id: String): FontFamily {
        val _r = rev
        familyCache[id]?.let { return it }
        val key = resolved(id).font
        val fam = EmbeddedFonts.familyFresh(key)
        familyCache[id] = fam
        return fam
    }

    fun size(id: String, baseSp: Int): TextUnit {
        val extra = resolved(id).size
        return (baseSp + AppTypography.BUMP + extra).sp
    }

    fun setRoute(route: String?) {
        currentRoute = FontCatalog.normalize(route)
        familyCache.clear()
        rev++
        AppTypography.applyRoute(currentRoute, choices)
    }
}
