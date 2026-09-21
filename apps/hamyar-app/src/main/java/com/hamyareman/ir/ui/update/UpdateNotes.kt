package com.hamyareman.ir.ui.update

import android.content.Context
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.net.NetState
import com.hamyareman.ir.ui.net.ResilientHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * «متنِ تغییراتِ نسخه» از یک فایلِ متنیِ ریپوی انتشار خوانده می‌شود، نه از کدِ اپ.
 *
 * فایل: `update-notes.json` در مخزنِ عمومیِ انتشار
 * (`Aydinnza/hamyar-releases`). شکلش یک آبجکتِ ساده است: کلید = شمارهٔ نسخه،
 * مقدار = لیستِ خطوطِ تغییرات:
 *
 * ```json
 * { "1.74": ["دانلودِ درون‌برنامه‌ای با نوار پیشرفت", "…"] }
 * ```
 *
 * پس بدونِ ساختنِ نسخهٔ تازه می‌شود متنِ صفحهٔ آپدیت را ویرایش کرد؛ اپ هر بار
 * نسخهٔ تازه را می‌بیند، همین فایل را می‌خواند (و آخرین نسخهٔ خوانده‌شده را برای
 * حالتِ آفلاین کش می‌کند تا اگر اینترنت نبود، متنِ قبلی نمایش داده شود).
 */
object UpdateNotes {

    /** نشانیِ خامِ فایلِ متنِ تغییرات در مخزنِ عمومیِ انتشار. */
    const val RAW_URL =
        "https://raw.githubusercontent.com/Aydinnza/hamyar-releases/main/update-notes.json"

    private const val PREF = "hamyar_update_notes"
    private const val KEY_JSON = "json"
    private const val KEY_AT = "fetched_at"

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    /** کلِ جدولِ تغییراتِ کش‌شده (نسخه → خطوط). */
    fun table(ctx: Context): Map<String, List<String>> = runCatching {
        val root = JSONObject(store(ctx).getString(KEY_JSON, "{}"))
        buildMap {
            root.keys().forEach { version ->
                val arr = root.optJSONArray(version) ?: return@forEach
                put(version, buildList {
                    for (i in 0 until arr.length()) {
                        val line = arr.optString(i).trim()
                        if (line.isNotEmpty()) add(line)
                    }
                })
            }
        }
    }.getOrDefault(emptyMap())

    /**
     * خطوطِ تغییراتِ یک نسخه. اول با برچسبِ همان نسخه، بعد با کلیدهای عمومی
     * (`"all"` برای همه و `"latest"` برای آخرین نسخه) جایگزین می‌شود.
     */
    fun notes(ctx: Context, version: String): List<String> {
        val table = table(ctx)
        val keys = listOf(version.trim(), version.trim().removePrefix("v"), "latest", "all")
        keys.forEach { k -> table[k]?.takeIf { it.isNotEmpty() }?.let { return it } }
        return emptyList()
    }

    fun fetchedAt(ctx: Context): Long = store(ctx).getLong(KEY_AT, 0L)

    /** خواندنِ تازه از مخزن؛ خطای شبکه بی‌صدا نادیده گرفته می‌شود (کش می‌ماند). */
    suspend fun refresh(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        if (!NetState.isOnline(ctx)) return@withContext false
        runCatching {
            val conn = ResilientHttp.open(RAW_URL, attempts = 3)
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            // فقط اگر JSONِ معتبر بود کش می‌شود (فایلِ نیمه‌کاره جای متنِ درست را نگیرد).
            JSONObject(body)
            store(ctx).putString(KEY_JSON, body)
            store(ctx).putLong(KEY_AT, System.currentTimeMillis())
            true
        }.getOrDefault(false)
    }
}
