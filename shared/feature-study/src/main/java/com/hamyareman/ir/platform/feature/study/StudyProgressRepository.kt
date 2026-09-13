package com.hamyareman.ir.platform.feature.study

import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.sync.SyncEngine
import org.json.JSONArray
import org.json.JSONObject

/**
 * حافظه‌ی پیشرفت مطالعه: وضعیت SM-2 هر فلش‌کارت + تاریخچه‌ی آزمون‌های هر پک.
 * الگو مانند پرامپت ۰۱: نوشتن همیشه محلی، ارسال با صف [SyncEngine] (آفلاین‌پسند).
 */
class StudyProgressRepository(
    private val store: LocalStore,
    private val sync: SyncEngine,
    private val userIdProvider: () -> String,
) {

    data class Attempt(
        val dateKey: String,      // yyyy-MM-dd
        val scorePct: Int,
        val total: Int,
        val wrongIds: List<String>,
        val weakTopics: List<String>,
        /** لحظه‌ی دقیق آزمون (epoch) — v1.13: نمایش با تاریخ/ساعت شمسی. */
        val atMs: Long = 0L,
    ) {
        fun toJson(): String = JSONObject()
            .put("dateKey", dateKey).put("scorePct", scorePct).put("total", total)
            .put("wrongIds", JSONArray(wrongIds)).put("weakTopics", JSONArray(weakTopics))
            .put("atMs", atMs).toString()

        companion object {
            fun fromJson(raw: String): Attempt = runCatching {
                val o = JSONObject(raw)
                fun arr(o: JSONObject, key: String): List<String> {
                    val a = o.optJSONArray(key) ?: return emptyList()
                    return (0 until a.length()).map { a.getString(it) }
                }
                Attempt(
                    dateKey = o.optString("dateKey"),
                    scorePct = o.optInt("scorePct"),
                    total = o.optInt("total"),
                    wrongIds = arr(o, "wrongIds"),
                    weakTopics = arr(o, "weakTopics"),
                    atMs = o.optLong("atMs"),
                )
            }.getOrDefault(Attempt("", 0, 0, emptyList(), emptyList()))
        }
    }

    /** آزمون دوره‌ای: هر ۷ روز یک‌بار مرورِ اشتباه‌های قبلی پیشنهاد می‌شود. */
    val PERIODIC_DAYS = 7

    // ---------- فلش‌کارت‌ها ----------

    fun cards(packId: String): Map<String, Sm2.CardState> {
        val raw = store.getString("study:$packId:cards")
        if (raw.isBlank()) return emptyMap()
        val o = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
        val out = mutableMapOf<String, Sm2.CardState>()
        o.keys().forEach { k -> out[k] = Sm2.CardState.fromJson(o.getJSONObject(k).toString()) }
        return out
    }

    fun stateOf(packId: String, cardId: String): Sm2.CardState = cards(packId)[cardId] ?: Sm2.CardState()

    fun reviewCard(packId: String, cardId: String, quality: Int, todayKey: String): Sm2.CardState {
        val all = cards(packId).toMutableMap()
        val next = Sm2.review(stateOf(packId, cardId), quality, todayKey)
        all[cardId] = next
        val o = JSONObject()
        all.forEach { (k, v) -> o.put(k, JSONObject(v.toJson())) }
        store.putString("study:$packId:cards", o.toString())
        enqueue(packId)
        return next
    }

    /** کارت‌های سررسید امروز (کارت‌های نو هم اولین‌بار سررسیدند). */
    fun dueCards(pack: StudyPack, todayKey: String): List<StudyPack.Flashcard> {
        val states = cards(pack.packId)
        return pack.flashcards.filter { Sm2.isDue(states[it.id] ?: Sm2.CardState(), todayKey) }
    }

    /** درصد تسلط کل پک (کارت‌های نو = صفر). */
    fun masteryPct(pack: StudyPack): Int {
        if (pack.flashcards.isEmpty()) return 0
        val states = cards(pack.packId)
        val sum = pack.flashcards.sumOf { Sm2.mastery(states[it.id] ?: Sm2.CardState()) }
        return (sum / pack.flashcards.size).coerceIn(0, 100)
    }

    // ---------- آزمون‌ها ----------

    fun attempts(packId: String): List<Attempt> {
        val raw = store.getString("study:$packId:attempts")
        if (raw.isBlank()) return emptyList()
        val a = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (0 until a.length()).map { Attempt.fromJson(a.getJSONObject(it).toString()) }
    }

    fun recordAttempt(packId: String, attempt: Attempt) {
        val all = attempts(packId) + attempt
        val arr = JSONArray()
        all.takeLast(50).forEach { arr.put(JSONObject(it.toJson())) }
        store.putString("study:$packId:attempts", arr.toString())
        enqueue(packId)
    }

    /** آزمون دوره‌ای سررسید شده؟ (۷ روز از آخرین آزمون گذشته باشد) */
    fun periodicQuizDue(packId: String, todayKey: String): Boolean {
        val last = attempts(packId).maxByOrNull { it.dateKey } ?: return false
        return Sm2.addDays(last.dateKey, PERIODIC_DAYS) <= todayKey
    }

    /** سوال‌های «دوره‌ای»: اشتباه‌های آزمون‌های قبلی (بازپرسی تا اطمینان). */
    fun periodicWrongIds(packId: String): List<String> =
        attempts(packId).flatMap { it.wrongIds }.distinct()

    // ---------- سینک ----------

    private fun enqueue(packId: String) {
        val uid = userIdProvider().ifBlank { "anon" }
        val rowId = "sp-${uid}-${packId}".replace(Regex("[^A-Za-z0-9_.\\-]"), "_")
        val payload = mapOf(
            "userId" to uid,
            "packId" to packId,
            "srsState" to store.getString("study:$packId:cards"),
            "attempts" to store.getString("study:$packId:attempts"),
            "updatedAtIso" to java.time.Instant.now().toString(),
        )
        sync.enqueue(com.hamyareman.ir.platform.core.common.TableIds.STUDY_PROGRESS, rowId, payload)
    }
}
