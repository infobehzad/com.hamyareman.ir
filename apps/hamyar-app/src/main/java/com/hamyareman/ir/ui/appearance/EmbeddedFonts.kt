package com.hamyareman.ir.ui.appearance

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.hamyareman.ir.R

/** فونت‌های امبدشده در APK — ریپو همیار + ۹ وزن وزیرمتن. */
object EmbeddedFonts {

    data class Face(
        val key: String,
        val label: String,
        val group: String,
        val resId: Int,
        val weight: FontWeight = FontWeight.Normal,
    )

    val catalog: List<Face> = listOf(
        Face("aviny", "آوینی — خوش‌آمد", "ریپو همیار", R.font.aviny),
        Face("estedad_bold", "استعداد — ساعت", "ریپو همیار", R.font.estedad_bold, FontWeight.Bold),
        Face("titr", "تیتر — عنوان", "ریپو همیار", R.font.titr),
        Face("parastoo_bold", "پرستو — کاشی", "ریپو همیار", R.font.parastoo_bold, FontWeight.Bold),
        Face("shekari", "شکاری", "ریپو همیار", R.font.shekari),
        Face("badkhat_bold", "بدخط — متن", "ریپو همیار", R.font.badkhat_bold, FontWeight.Bold),
        Face("vazirmatn_thin", "وزیر نازک", "وزیرمتن", R.font.vazirmatn_thin, FontWeight.Thin),
        Face("vazirmatn_extralight", "وزیر خیلی‌نازک", "وزیرمتن", R.font.vazirmatn_extralight, FontWeight.ExtraLight),
        Face("vazirmatn_light", "وزیر لایت", "وزیرمتن", R.font.vazirmatn_light, FontWeight.Light),
        Face("vazirmatn_regular", "وزیر معمولی", "وزیرمتن", R.font.vazirmatn_regular),
        Face("vazirmatn_medium", "وزیر متوسط", "وزیرمتن", R.font.vazirmatn_medium, FontWeight.Medium),
        Face("vazirmatn_semibold", "وزیر نیمه‌ضخیم", "وزیرمتن", R.font.vazirmatn_semibold, FontWeight.SemiBold),
        Face("vazirmatn_bold", "وزیر ضخیم", "وزیرمتن", R.font.vazirmatn_bold, FontWeight.Bold),
        Face("vazirmatn_extrabold", "وزیر خیلی‌ضخیم", "وزیرمتن", R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
        Face("vazirmatn_black", "وزیر سیاه", "وزیرمتن", R.font.vazirmatn_black, FontWeight.Black),
    )

    private val byKey: Map<String, Face> = catalog.associateBy { it.key }

    private val cache = mutableMapOf<String, FontFamily>()

    fun face(key: String): Face = byKey[key] ?: byKey.getValue("badkhat_bold")

    fun labelOf(key: String): String = face(key).label

    /** نمونهٔ کش‌شده برای پیش‌نمایش فهرست. */
    @Synchronized
    fun family(key: String): FontFamily {
        cache[key]?.let { return it }
        val f = face(key)
        val fam = runCatching { FontFamily(Font(f.resId, f.weight)) }.getOrDefault(FontFamily.Default)
        cache[key] = fam
        return fam
    }

    /** نمونهٔ تازه برای هر نقش تا هویت FontFamily در bump جدا بماند. */
    fun familyFresh(key: String): FontFamily {
        val f = face(key)
        return runCatching { FontFamily(Font(f.resId, f.weight)) }.getOrDefault(FontFamily.Default)
    }

    fun isKnown(key: String): Boolean = byKey.containsKey(key)
}
