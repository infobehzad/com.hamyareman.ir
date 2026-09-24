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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.R
import com.hamyareman.ir.ui.appearance.EmbeddedFonts
import com.hamyareman.ir.ui.appearance.FontTheme
import com.hamyareman.ir.ui.appearance.SlotChoice

/**
 * تایپوگرافی واحد و لازم‌الاجرای کل اپ.
 *
 * A  آکاردئون: عنوان + توضیح
 * B  کارت (و زیرکارت آکاردئون): عنوان + توضیح
 * C  صفحهٔ بازشده از زیرکارت: عنوان بالا، عناوین دیگر، متن، جدول، دکمه
 * E  هر عنوان دیگر: متن عنوان + توضیح زیرش
 * D  داشبورد D1…D12
 *
 * فلوتر فونت همین نقش‌ها را زنده عوض می‌کند تا ترکیب نهایی ذخیره شود.
 */
object AppTypography {
    const val BUMP = 2

    data class Spec(
        val id: String,
        val title: String,
        val group: String,
        val defaultFont: String,
        val baseSp: Int,
        val sample: String,
    )

    class Role(val spec: Spec) {
        var fontKey by mutableStateOf(spec.defaultFont)
            internal set
        var sizeSp by mutableIntStateOf(spec.baseSp + BUMP)
            internal set
        var weightKey by mutableStateOf(EmbeddedFonts.defaultWeightOf(spec.defaultFont))
            internal set
        var family by mutableStateOf(EmbeddedFonts.familyFresh(spec.defaultFont, EmbeddedFonts.defaultWeightOf(spec.defaultFont)))
            internal set

        val delta: Int get() = sizeSp - spec.baseSp - BUMP
        val weight: FontWeight get() = EmbeddedFonts.fontWeight(weightKey)
        val size: TextUnit get() = sizeSp.sp
        val style: TextStyle get() = TextStyle(fontFamily = family, fontSize = size, fontWeight = weight)

        fun apply(font: String, sizeDelta: Int) = apply(font, sizeDelta, weight = null, absolute = false)

        fun apply(font: String?, size: Int?, weight: String? = null, absolute: Boolean = true) {
            if (font != null) {
                fontKey = if (EmbeddedFonts.isKnown(font)) EmbeddedFonts.canonicalKey(font) else spec.defaultFont
                if (weight == null) weightKey = EmbeddedFonts.weightFromKey(font, weightKey)
            }
            if (weight != null) weightKey = EmbeddedFonts.normalizeWeight(weight)
            if (size != null) {
                sizeSp = if (absolute) size.coerceIn(8, 40) else (spec.baseSp + BUMP + size).coerceIn(8, 40)
            }
            family = EmbeddedFonts.familyFresh(fontKey, weightKey)
        }
    }

    private fun role(id: String, title: String, group: String, font: String, base: Int, sample: String) =
        Role(Spec(id, title, group, font, base, sample))

    /** A — کارت آکاردئونی */
    val accordionTitle = role("a.title", "عنوان آکاردئون", "A آکاردئون", "titr", 16, "کتاب‌ها")
    val accordionSub = role("a.sub", "توضیح آکاردئون", "A آکاردئون", "badkhat_bold", 12, "هر کتاب با درس‌ها")

    /** B — کارت و زیرکارت */
    val cardTitle = role("b.title", "عنوان کارت", "B کارت", "parastoo_bold", 14, "برنامه‌ی هفتگی من")
    val cardSub = role("b.sub", "توضیح کارت", "B کارت", "badkhat_bold", 12, "جدول زمانی شخصی")

    /** C — صفحات بازشده از زیرکارت */
    val pageTitle = role("c.title", "عنوان اصلی بالای صفحه", "C صفحه", "titr", 22, "برنامه هفتگی")
    val pageHeading = role("c.heading", "عناوین دیگر صفحه", "C صفحه", "titr", 18, "شنبه")
    val pageBody = role("c.body", "متن صفحه", "C صفحه", "badkhat_bold", 16, "متن بدنه")
    val pageTable = role("c.table", "جدول", "C صفحه", "badkhat_bold", 13, "درس / زنگ")
    val pageButton = role("c.button", "دکمه", "C صفحه", "badkhat_bold", 16, "ذخیره")

    /** E — همهٔ عناوین */
    val title = role("e.title", "متن عنوان", "E عنوان", "titr", 18, "مدرسه")
    val titleSub = role("e.sub", "توضیح زیر عنوان", "E عنوان", "badkhat_bold", 14, "کلاسِ درس همیشه باز است")

    /** داشبورد */
    val d1Greeting = role("d1", "D1 خوش‌آمدگویی", "D داشبورد", "aviny", 22, "صبح‌ت بخیر")
    val d2GreetingSub = role("d2", "D2 متن زیر خوش‌آمد", "D داشبورد", "badkhat_bold", 14, "همیار من کنارت است")
    val d3Date = role("d3", "D3 تاریخ", "D داشبورد", "estedad_bold", 16, "چهارشنبه ۲ مهر")
    val d4Clock = role("d4", "D4 ساعت", "D داشبورد", "estedad_bold", 24, "۲:۳۰ بعد از ظهر")
    val d5Gregorian = role("d5", "D5 تاریخ میلادی", "D داشبورد", "estedad_bold", 16, "2026/Sep/24")
    val d6Subscription = role("d6", "D6 کادر اشتراک", "D داشبورد", "badkhat_bold", 12, "اشتراک تا تاریخ")
    val d7Quote = role("d7", "D7 کادر سخنان", "D داشبورد", "badkhat_bold", 16, "سخن بزرگان")
    val d8Tile = role("d8", "D8 نه کاشی داشبورد", "D داشبورد", "parastoo_bold", 14, "مدرسه")
    val d9Section = role("d9", "D9 عناوین بخش (برنامه کلاسی / امروز / امتحان داری؟)", "D داشبورد", "titr", 16, "برنامه کلاسی مدرسه")
    val d10ClassBox = role("d10", "D10 سه کادر برنامه کلاسی", "D داشبورد", "badkhat_bold", 13, "ریاضی")
    val d11Check = role("d11", "D11 متن تیک‌ها", "D داشبورد", "badkhat_bold", 12, "کیف مدرسه آماده است")
    val d12ClassDate = role("d12", "D12 تاریخ و روز کنار کادرها", "D داشبورد", "badkhat_bold", 12, "چهارشنبه")

    /** نوار پایین جدا از بقیهٔ نقش‌ها. */
    val navBar = role("nav.bar", "نوار پایین", "نوار پایین", "titr", 11, "خانه")

    /** فاصلهٔ افقی کارت سخنان از دو طرف (۰ تا ۳۲ dp). */
    var quoteSideDp by mutableIntStateOf(1)
        internal set

    val slots: List<Role> = listOf(
        accordionTitle, accordionSub,
        cardTitle, cardSub,
        pageTitle, pageHeading, pageBody, pageTable, pageButton,
        title, titleSub,
        d1Greeting, d2GreetingSub, d3Date, d4Clock, d5Gregorian, d6Subscription, d7Quote,
        d8Tile, d9Section, d10ClassBox, d11Check, d12ClassDate,
        navBar,
    )

    private val byId = slots.associateBy { it.spec.id }

    fun slot(id: String): Role? = byId[id]

    fun slotsFor(route: String?): List<Role> {
        val r = (route ?: "home").substringBefore("?").substringBefore("/{")
        val core = listOf(
            accordionTitle, accordionSub, cardTitle, cardSub,
            pageTitle, pageHeading, pageBody, pageTable, pageButton,
            title, titleSub,
        )
        val dash = listOf(
            d1Greeting, d2GreetingSub, d3Date, d4Clock, d5Gregorian, d6Subscription, d7Quote,
            d8Tile, d9Section, d10ClassBox, d11Check, d12ClassDate,
        )
        return if (r == "home" || r.isBlank()) core + dash + listOf(navBar) else core + listOf(navBar)
    }

    fun applySlot(id: String, font: String?, size: Int?, weight: String? = null) {
        if (id == "d7.pad") {
            quoteSideDp = (size ?: quoteSideDp).coerceIn(0, 32)
            return
        }
        val role = byId[id] ?: return
        role.apply(font = font, size = size, weight = weight, absolute = true)
        syncAliases()
    }

    fun applyAll(map: Map<String, SlotChoice>) {
        map["d7.pad"]?.let { quoteSideDp = it.size.coerceIn(0, 32) }
        map.forEach { (id, c) ->
            if (id != "d7.pad") {
                byId[id]?.apply(font = c.font, size = c.size, weight = c.weight, absolute = c.absolute)
            }
        }
        syncAliases()
    }

    fun snapshot(): Map<String, SlotChoice> = buildMap {
        slots.forEach {
            put(it.spec.id, SlotChoice(it.fontKey, it.sizeSp, it.weightKey, absolute = true))
        }
        put("d7.pad", SlotChoice("badkhat_bold", quoteSideDp, EmbeddedFonts.W_REGULAR, absolute = true))
    }

    // ---- نام‌های قدیمی برای کد باقی‌مانده؛ به نقش‌های جدید نگاشت می‌شوند ----
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

    val h1: TextStyle get() = pageTitle.style.copy(lineHeight = (32 + pageTitle.delta).coerceAtLeast(20).sp)
    val h2: TextStyle get() = pageHeading.style.copy(lineHeight = (26 + pageHeading.delta).coerceAtLeast(18).sp)
    val text: TextStyle get() = pageBody.style.copy(lineHeight = (24 + pageBody.delta).coerceAtLeast(18).sp)
    val caption: TextStyle get() = titleSub.style.copy(lineHeight = (20 + titleSub.delta).coerceAtLeast(16).sp)
    val button: TextStyle get() = pageButton.style.copy(lineHeight = (22 + pageButton.delta).coerceAtLeast(16).sp)

    private fun syncAliases() {
        greeting = d1Greeting.family
        greetingDelta = d1Greeting.delta
        clock = d4Clock.family
        clockDelta = d4Clock.delta
        heading = title.family
        headingDelta = title.delta
        tile = cardTitle.family
        tileDelta = cardTitle.delta
        body = pageBody.family
        bodyDelta = pageBody.delta
    }

    fun apply(theme: FontTheme) {
        d1Greeting.apply(theme.greetingFont, theme.greetingSize, absolute = false)
        d4Clock.apply(theme.clockFont, theme.clockSize, absolute = false)
        title.apply(theme.headingFont, theme.headingSize, absolute = false)
        pageHeading.apply(theme.headingFont, theme.headingSize, absolute = false)
        pageTitle.apply(theme.headingFont, theme.headingSize, absolute = false)
        cardTitle.apply(theme.tileFont, theme.tileSize, absolute = false)
        d8Tile.apply(theme.tileFont, theme.tileSize, absolute = false)
        pageBody.apply(theme.bodyFont, theme.bodySize, absolute = false)
        accordionSub.apply(theme.bodyFont, theme.bodySize, absolute = false)
        cardSub.apply(theme.bodyFont, theme.bodySize, absolute = false)
        titleSub.apply(theme.bodyFont, theme.bodySize, absolute = false)
        syncAliases()
    }

    fun applyRoute(route: String, slots: Map<String, SlotChoice>) {
        applyAll(slots)
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
        family === d1Greeting.family || family === greeting -> d1Greeting.delta
        family === d4Clock.family || family === clock -> d4Clock.delta
        family === title.family || family === heading -> title.delta
        family === cardTitle.family || family === tile -> cardTitle.delta
        else -> pageBody.delta
    }

    val quoteSide get() = quoteSideDp.dp
}
