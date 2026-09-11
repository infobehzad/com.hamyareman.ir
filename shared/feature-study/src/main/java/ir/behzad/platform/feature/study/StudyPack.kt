package ir.behzad.platform.feature.study

import org.json.JSONArray
import org.json.JSONObject

/**
 * «پک مطالعه» — کل محتوای تفکیک‌شده‌ی یک درس در یک JSON آفلاین:
 * بخش‌های مهم/نکات/نکات امتحانی، فلش‌کارت‌ها، سوالات نمونه با جواب و توضیح، و حل کتاب.
 * منبع داده: assets اپ (آفلاین کامل) — به‌روزرسانی محتوایی بعدها از سرور هم می‌آید.
 */
data class StudyPack(
    val packId: String,
    val bookCode: String,
    val lessonId: String,
    val title: String,
    val bookTitle: String,
    val pdfFileName: String,
    val sections: List<Section>,
    val flashcards: List<Flashcard>,
    val questions: List<Question>,
    val solutions: List<Solution>,
) {
    data class Section(val id: String, val title: String, val kind: String, val body: String)
    data class Flashcard(val id: String, val front: String, val back: String, val topic: String, val hint: String)
    data class Question(
        val id: String,
        val type: String,            // mcq | numeric | short
        val text: String,
        val options: List<String>,   // فقط mcq
        val answer: String,
        val explanation: String,
        val topic: String,
        val difficulty: Int,         // 1..3
        val refSectionId: String,
    )
    data class Solution(val id: String, val title: String, val body: String)

    fun sectionById(id: String): Section? = sections.firstOrNull { it.id == id }

    companion object {
        fun fromJson(raw: String): StudyPack? = runCatching {
            val o = JSONObject(raw)
            fun strArray(obj: JSONObject, key: String): List<String> {
                val a = obj.optJSONArray(key) ?: return emptyList()
                return (0 until a.length()).map { a.getString(it) }
            }
            val sections = (o.optJSONArray("sections") ?: JSONArray()).let { a ->
                (0 until a.length()).map {
                    val s = a.getJSONObject(it)
                    Section(s.optString("id"), s.optString("title"), s.optString("kind", "note"), s.optString("body"))
                }
            }
            val cards = (o.optJSONArray("flashcards") ?: JSONArray()).let { a ->
                (0 until a.length()).map {
                    val c = a.getJSONObject(it)
                    Flashcard(c.optString("id"), c.optString("front"), c.optString("back"), c.optString("topic"), c.optString("hint"))
                }
            }
            val questions = (o.optJSONArray("questions") ?: JSONArray()).let { a ->
                (0 until a.length()).map {
                    val q = a.getJSONObject(it)
                    Question(
                        id = q.optString("id"), type = q.optString("type", "mcq"), text = q.optString("text"),
                        options = strArray(q, "options"), answer = q.optString("answer"),
                        explanation = q.optString("explanation"), topic = q.optString("topic"),
                        difficulty = q.optInt("difficulty", 1), refSectionId = q.optString("refSectionId"),
                    )
                }
            }
            val solutions = (o.optJSONArray("solutions") ?: JSONArray()).let { a ->
                (0 until a.length()).map {
                    val s = a.getJSONObject(it)
                    Solution(s.optString("id"), s.optString("title"), s.optString("body"))
                }
            }
            StudyPack(
                packId = o.optString("packId"), bookCode = o.optString("bookCode"),
                lessonId = o.optString("lessonId"), title = o.optString("title"),
                bookTitle = o.optString("bookTitle"), pdfFileName = o.optString("pdfFileName"),
                sections = sections, flashcards = cards, questions = questions, solutions = solutions,
            )
        }.getOrNull()
    }
}
