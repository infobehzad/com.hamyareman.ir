package com.hamyareman.ir.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.R
import com.hamyareman.ir.ui.appearance.EmbeddedFonts
import com.hamyareman.ir.ui.appearance.FontCatalog
import com.hamyareman.ir.ui.appearance.FontTheme
import com.hamyareman.ir.ui.appearance.SlotChoice

/**
 * تایپوگرافی واحد کل اپ همیار من.
 *
 * نقش فونت (قابل‌تغییر از ظاهر و فونت):
 *  - greeting / آوینی: خوش‌آمد
 *  - clock / استعداد: ساعت و تاریخ
 *  - heading / تیتر: عنوان کارت‌ها و بخش‌ها
 *  - tile / پرستو: کاشی میانبر
 *  - body / بدخط: برچسب، بدنه، دکمه، جزوه، دست‌نویس
 *
 * اندازه پایه (پس از +۲): h1=۲۴  h2=۱۸  text=۱۶  caption=۱۴  button=۱۶
 * لغزندهٔ هر نقش: −۲۰ تا +۲۰ sp روی همان پایه.
 */
object AppTypography {
    const val BUMP = 2

    var greeting by mutableStateOf(FontFamily(Font(R.font.aviny, FontWeight.Normal)))
        private set
    var clock by mutableStateOf(FontFamily(Font(R.font.estedad_bold, FontWeight.Bold)))
        private set
    var heading by mutableStateOf(FontFamily(Font(R.font.titr, FontWeight.Normal)))
        private set
    var tile by mutableStateOf(FontFamily(Font(R.font.parastoo_bold, FontWeight.Bold)))
        private set
    var body by mutableStateOf(FontFamily(Font(R.font.badkhat_bold, FontWeight.Bold)))
        private set

    var greetingDelta by mutableIntStateOf(0)
        private set
    var clockDelta by mutableIntStateOf(0)
        private set
    var headingDelta by mutableIntStateOf(0)
        private set
    var tileDelta by mutableIntStateOf(0)
        private set
    var bodyDelta by mutableIntStateOf(0)
        private set

    val h1: TextStyle
        get() = TextStyle(
            fontFamily = heading,
            fontWeight = FontWeight.Normal,
            fontSize = (24 + headingDelta).sp,
            lineHeight = (32 + headingDelta).coerceAtLeast(20).sp,
        )
    val h2: TextStyle
        get() = TextStyle(
            fontFamily = heading,
            fontWeight = FontWeight.Normal,
            fontSize = (18 + headingDelta).sp,
            lineHeight = (26 + headingDelta).coerceAtLeast(18).sp,
        )
    val text: TextStyle
        get() = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Bold,
            fontSize = (16 + bodyDelta).sp,
            lineHeight = (24 + bodyDelta).coerceAtLeast(18).sp,
        )
    val caption: TextStyle
        get() = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Bold,
            fontSize = (14 + bodyDelta).sp,
            lineHeight = (20 + bodyDelta).coerceAtLeast(16).sp,
        )
    val button: TextStyle
        get() = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Bold,
            fontSize = (16 + bodyDelta).sp,
            lineHeight = (22 + bodyDelta).coerceAtLeast(16).sp,
        )

    fun apply(theme: FontTheme) {
        greeting = EmbeddedFonts.familyFresh(theme.greetingFont)
        clock = EmbeddedFonts.familyFresh(theme.clockFont)
        heading = EmbeddedFonts.familyFresh(theme.headingFont)
        tile = EmbeddedFonts.familyFresh(theme.tileFont)
        body = EmbeddedFonts.familyFresh(theme.bodyFont)
        greetingDelta = theme.greetingSize.coerceIn(-20, 20)
        clockDelta = theme.clockSize.coerceIn(-20, 20)
        headingDelta = theme.headingSize.coerceIn(-20, 20)
        tileDelta = theme.tileSize.coerceIn(-20, 20)
        bodyDelta = theme.bodySize.coerceIn(-20, 20)
    }

    fun applyRoute(route: String, slots: Map<String, SlotChoice>) {
        val r = FontCatalog.normalize(route)
        fun pick(id: String): SlotChoice {
            var cur: String? = id
            val seen = HashSet<String>()
            while (cur != null && seen.add(cur)) {
                slots[cur]?.let { return it }
                val slot = FontCatalog.slot(cur) ?: break
                if (slot.fallbackId == cur) return SlotChoice(slot.defaultFont, 0)
                cur = slot.fallbackId
            }
            return SlotChoice("badkhat_bold", 0)
        }
        val greet = pick(if (r == "home") "page.home.greeting" else FontCatalog.ROLE_GREETING)
        val clk = pick(if (r == "home") "page.home.clock" else FontCatalog.ROLE_CLOCK)
        val head = pick(if (r == "home") "page.home.heading" else "page.$r.title")
        val tiles = pick(if (r == "home") "page.home.tile" else "page.$r.card")
        val bodies = pick(if (r == "home") "page.home.body" else "page.$r.body")
        apply(
            FontTheme(
                greetingFont = greet.font,
                greetingSize = greet.size,
                clockFont = clk.font,
                clockSize = clk.size,
                headingFont = head.font,
                headingSize = head.size,
                tileFont = tiles.font,
                tileSize = tiles.size,
                bodyFont = bodies.font,
                bodySize = bodies.size,
            ),
        )
    }

    fun bump(baseSp: Int): TextUnit = (baseSp + BUMP).sp

    fun bump(base: TextUnit): TextUnit {
        val v = if (base.isSp) base.value else 14f
        return (v + BUMP).sp
    }

    fun bump(family: FontFamily, baseSp: Int): TextUnit =
        (baseSp + BUMP + deltaOf(family)).sp

    fun bump(family: FontFamily, base: TextUnit): TextUnit {
        val v = if (base.isSp) base.value else 14f
        return (v + BUMP + deltaOf(family)).sp
    }

    private fun deltaOf(family: FontFamily): Int = when {
        family === greeting -> greetingDelta
        family === clock -> clockDelta
        family === heading -> headingDelta
        family === tile -> tileDelta
        else -> bodyDelta
    }
}
