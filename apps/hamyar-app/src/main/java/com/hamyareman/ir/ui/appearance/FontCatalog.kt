package com.hamyareman.ir.ui.appearance

import com.hamyareman.ir.ui.hub.HubCatalog
import com.hamyareman.ir.ui.navigation.Tabs

data class FontSlot(
    val id: String,
    val title: String,
    val group: String,
    val fallbackId: String,
    val defaultFont: String,
    val sample: String = "نمونه متن همیار من",
)

data class SlotChoice(
    val font: String,
    val size: Int = 0,
)

/**
 * طبقه‌بندی استاندارد فونت کل اپ:
 * نقش سراسری → نوار پایین → هاب (کارت اصلی جدا از زیرکارت) → هر صفحه.
 */
object FontCatalog {

    const val ROLE_GREETING = "role.greeting"
    const val ROLE_CLOCK = "role.clock"
    const val ROLE_HEADING = "role.heading"
    const val ROLE_TILE = "role.tile"
    const val ROLE_BODY = "role.body"
    const val ROLE_NAV = "role.nav"

    val all: List<FontSlot> by lazy { buildAll() }
    private val byId: Map<String, FontSlot> by lazy { all.associateBy { it.id } }

    fun slot(id: String): FontSlot? = byId[id]

    fun normalize(route: String?): String {
        if (route.isNullOrBlank()) return "home"
        var r = route.substringBefore("?")
        r = r.substringBefore("/{")
        val known = setOf(
            "study-book", "study-teach", "study-lesson", "study-lesson-pdf",
            "video-teach", "lesson", "exercise", "recipedetail", "tool",
        )
        val first = r.substringBefore("/")
        return if (first in known) first else r
    }

    fun forRoute(route: String?): List<FontSlot> {
        val r = normalize(route)
        val nav = all.filter { it.group == "نوار پایین" }
        val page = when (r) {
            "home" -> all.filter { it.id.startsWith("page.home.") || it.id.startsWith("role.") }
            "study" -> all.filter { it.id.startsWith("hub.school.") }
            "academy" -> all.filter { it.id.startsWith("hub.academy.") }
            "more" -> all.filter { it.id.startsWith("hub.more.") }
            "health" -> all.filter { it.id.startsWith("hub.health.") }
            "awareness" -> all.filter { it.id.startsWith("hub.awareness.") }
            "chat" -> all.filter { it.id.startsWith("page.chat.") }
            else -> all.filter { it.id.startsWith("page.$r.") }
        }
        val extras = if (page.isEmpty()) pageParts(r, pageTitle(r)) else page
        return nav + extras
    }

    fun pageTitle(route: String?): String = PAGE_TITLES[normalize(route)] ?: (route ?: "")

    private fun buildAll(): List<FontSlot> {
        val out = mutableListOf<FontSlot>()
        fun add(
            id: String,
            title: String,
            group: String,
            fallback: String,
            font: String,
            sample: String = "نمونه متن همیار من",
        ) {
            out += FontSlot(id, title, group, fallback, font, sample)
        }

        add(ROLE_GREETING, "خوش‌آمد (سراسری)", "نقش سراسری", ROLE_GREETING, "aviny", "صبح‌ت بخیر")
        add(ROLE_CLOCK, "ساعت (سراسری)", "نقش سراسری", ROLE_CLOCK, "estedad_bold", "۱۴:۳۰")
        add(ROLE_HEADING, "عنوان (سراسری)", "نقش سراسری", ROLE_HEADING, "titr", "عنوان کارت")
        add(ROLE_TILE, "کاشی (سراسری)", "نقش سراسری", ROLE_TILE, "parastoo_bold", "میانبر")
        add(ROLE_BODY, "متن (سراسری)", "نقش سراسری", ROLE_BODY, "badkhat_bold", "متن بدنه")
        add(ROLE_NAV, "نوار پایین (سراسری)", "نوار پایین", ROLE_NAV, "badkhat_bold", "داشبورد")
        add("nav.bottom", "نوار پایین — همهٔ تب‌ها", "نوار پایین", ROLE_NAV, "badkhat_bold", "مدرسه")
        Tabs.forEach { tab ->
            add("nav.bottom.${tab.route}", "تب ${tab.label}", "نوار پایین", "nav.bottom", "badkhat_bold", tab.label)
        }

        add("page.home.greeting", "خوش‌آمد داشبورد", "داشبورد", ROLE_GREETING, "aviny", "صبح‌ت بخیر")
        add("page.home.clock", "ساعت داشبورد", "داشبورد", ROLE_CLOCK, "estedad_bold", "۱۴:۳۰")
        add("page.home.heading", "عنوان داشبورد", "داشبورد", ROLE_HEADING, "titr", "امروز")
        add("page.home.tile", "کاشی داشبورد", "داشبورد", ROLE_TILE, "parastoo_bold", "مدرسه")
        add("page.home.body", "متن داشبورد", "داشبورد", ROLE_BODY, "badkhat_bold")
        add("page.home.quote", "سخن بزرگان", "داشبورد", ROLE_BODY, "badkhat_bold")

        add("hub.school.header", "هدر مدرسه", "مدرسه — کارت اصلی", ROLE_HEADING, "titr", "مدرسه")
        add("hub.school.group.books", "آکاردئون کتاب‌ها", "مدرسه — کارت اصلی", ROLE_HEADING, "titr", "کتاب‌ها")
        add("hub.school.item.book", "زیرکارت کتاب درسی", "مدرسه — زیرکارت", ROLE_TILE, "parastoo_bold")
        HubCatalog.schoolExtra().forEach { g ->
            add("hub.school.group.${g.id}", "آکاردئون ${g.title}", "مدرسه — کارت اصلی", ROLE_HEADING, "titr", g.title)
            g.items.forEach { it ->
                val key = it.route.substringBefore("?").substringBefore("/")
                add("hub.school.item.$key", "زیرکارت ${it.title}", "مدرسه — زیرکارت", ROLE_TILE, "parastoo_bold", it.title)
            }
        }

        add("hub.academy.header", "هدر آموزشگاه", "آموزشگاه — کارت اصلی", ROLE_HEADING, "titr", "آموزشگاه")
        HubCatalog.academy().forEach { g ->
            add("hub.academy.group.${g.id}", "آکاردئون ${g.title}", "آموزشگاه — کارت اصلی", ROLE_HEADING, "titr", g.title)
            g.items.forEach { it ->
                val key = it.route.substringBefore("?").substringBefore("/")
                add("hub.academy.item.$key", "زیرکارت ${it.title}", "آموزشگاه — زیرکارت", ROLE_TILE, "parastoo_bold", it.title)
            }
        }

        add("hub.health.header", "هدر سلامتی", "سلامتی — کارت اصلی", ROLE_HEADING, "titr", "سلامتی")
        listOf(
            "health-progress" to "پیشرفت سلامتی",
            "wellness" to "حرکات ورزشی",
            "water" to "آب بنوش",
            "sleep-log" to "خواب من",
            "cycle" to "چرخه ی ماهانه",
            "meds" to "یادآور دارو",
            "routine" to "روتین روز",
        ).forEach { (id, title) ->
            add("hub.health.item.$id", "کارت $title", "سلامتی — زیرکارت", ROLE_TILE, "parastoo_bold", title)
        }

        add("hub.more.header", "هدر بیشتر", "بیشتر — کارت اصلی", ROLE_HEADING, "titr", "بیشتر")
        listOf(
            "safespace" to "فضای امن من",
            "helplines" to "شماره‌های کمک",
            "user-profile" to "پروفایل من",
            "appearance" to "ظاهر و فونت",
            "settings" to "تنظیمات",
            "quiet" to "زمان درس",
        ).forEach { (id, title) ->
            add("hub.more.item.$id", "کارت $title", "بیشتر — زیرکارت", ROLE_TILE, "parastoo_bold", title)
        }

        add("hub.awareness.header", "هدر آگاهی", "آگاهی — کارت اصلی", ROLE_HEADING, "titr", "آگاهی")
        listOf(
            "journal" to "دفترچه‌ی من",
            "breath" to "تمرین نفس",
            "mindfulness" to "ذهن‌آگاهی",
            "calm" to "آرامش سریع",
            "reading-corner" to "مطالعه‌ی غیردرسی",
            "wellness" to "صداهای آرامش‌بخش",
        ).forEach { (id, title) ->
            add("hub.awareness.item.$id", "کارت $title", "آگاهی — زیرکارت", ROLE_TILE, "parastoo_bold", title)
        }

        PAGE_TITLES.forEach { (id, title) ->
            if (id == "home" || id == "study" || id == "academy" || id == "health" || id == "more" || id == "awareness") return@forEach
            out += pageParts(id, title)
        }
        return out.distinctBy { it.id }
    }

    private fun pageParts(id: String, title: String): List<FontSlot> = listOf(
        FontSlot("page.$id.title", "عنوان — $title", title, ROLE_HEADING, "titr", title),
        FontSlot("page.$id.body", "متن — $title", title, ROLE_BODY, "badkhat_bold"),
        FontSlot("page.$id.caption", "برچسب — $title", title, ROLE_BODY, "badkhat_bold"),
        FontSlot("page.$id.button", "دکمه — $title", title, ROLE_BODY, "badkhat_bold"),
        FontSlot("page.$id.card", "کارت — $title", title, ROLE_TILE, "parastoo_bold"),
    )

    private val PAGE_TITLES = mapOf(
        "home" to "داشبورد",
        "study" to "مدرسه",
        "academy" to "آموزشگاه",
        "health" to "سلامتی",
        "chat" to "همراه من",
        "more" to "بیشتر",
        "awareness" to "آگاهی",
        "study-home" to "مطالعه",
        "cycle" to "چرخه ی ماهانه",
        "mood" to "حال‌واحوصل",
        "mindfulness" to "ذهن‌آگاهی",
        "calm" to "آرامش",
        "calm-hub" to "هاب آرامش",
        "free-reading" to "مطالعه آزاد",
        "journal" to "دفترچه",
        "breath" to "نفس",
        "routine" to "روتین",
        "safespace" to "فضای امن",
        "writing" to "نوشتن",
        "helplines" to "شماره‌های کمک",
        "library" to "کتابخانه",
        "audiobook" to "کتاب صوتی",
        "school" to "شیفت مدرسه",
        "leave" to "مرخصی",
        "class-plan" to "برنامه کلاس",
        "class-plan-shift" to "شیفت و زنگ",
        "virtual-class" to "کلاس مجازی",
        "subscription" to "اشتراک",
        "tomorrow-prep" to "آماده‌سازی کامل",
        "sleep-night" to "خواب شب",
        "weekly-schedule" to "برنامه هفتگی",
        "meds" to "دارو",
        "sleep-log" to "خواب من",
        "reading-corner" to "قفسه مطالعه",
        "appearance" to "ظاهر و فونت",
        "quiz" to "آزمون",
        "quizreview" to "بازخورد آزمون",
        "study-book" to "کتاب درسی",
        "study-teach" to "تدریس",
        "study-lesson" to "مطالعه درس",
        "study-lesson-pdf" to "PDF درس",
        "pdf" to "جزوه",
        "charts" to "نمودار پیشرفت",
        "video-teach" to "ویدیوی تدریس",
        "study-downloads" to "دانلودها",
        "health-progress" to "پیشرفت سلامتی",
        "art" to "هنر روزانه",
        "gallery" to "گالری هنر",
        "learning" to "درس‌های من",
        "lesson" to "درس تعاملی",
        "placement" to "تعیین سطح",
        "roadmap" to "نقشه راه",
        "ailearning" to "یادگیری AI",
        "aiassessment" to "ارزیابی AI",
        "recipes" to "آشپزی",
        "recipedetail" to "دستور پخت",
        "exercise" to "ورزش",
        "water" to "آب",
        "settings" to "تنظیمات",
        "user-profile" to "پروفایل",
        "privacy" to "حریم خصوصی",
        "chatsettings" to "تنظیمات گفتگو",
        "badges" to "بج",
        "lock" to "قفل",
        "reminders" to "یادآورها",
        "sync" to "همگام‌سازی",
        "wellness" to "حرکات و یوگا",
        "sketch-gallery" to "گالری نقاشی",
        "cycle-training" to "تمرین دوره",
        "toolkit-general" to "جعبه‌ابزار عمومی",
        "lab-chemistry" to "آزمایشگاه شیمی",
        "lab-physics" to "آزمایشگاه فیزیک",
        "lab-biology" to "آزمایشگاه زیست",
        "toolkit-math" to "جعبه‌ابزار ریاضی",
        "tool" to "ابزار",
        "call" to "تماس",
        "album" to "آلبوم",
    )
}
