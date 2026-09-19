package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.notifications.Reminder
import com.hamyareman.ir.platform.core.notifications.ReminderScheduler
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.ui.home.IranOfficialHolidays
import com.hamyareman.ir.ui.profile.GradeGate
import org.json.JSONArray
import java.time.LocalDate

/**
 * برنامهٔ کلاسی مدرسه + آماده‌سازی فردا + شیفت چرخشی.
 * خصوصی روی دستگاه؛ Sync نمی‌شود.
 */
object ClassPlanStore {

    private const val PREF = "hamyar_class_plan"
    val WEEKDAYS = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه") // ۱..۵
    const val SPORT = "ورزش"

    data class SlotDay(val subjects: List<String>)
    data class Snapshot(
        val locked: Boolean,
        val days: Map<Int, List<String>>, // dayIndex 1..5
        val cycleWeeks: Int,
        val anchorIso: String,
        val fixedEvening: Boolean,
        val lunarOffset: Int,
        val morningHour: Int,
        val morningMinute: Int,
        val wakeLeadMin: Int,
        val noonHour: Int,
        val noonMinute: Int,
        val sleepMorning: String,
        val sleepEvening: String,
        val weekPattern: List<String>,
        val virtualMorningHour: Int,
        val virtualMorningMinute: Int,
        val virtualNoonHour: Int,
        val virtualNoonMinute: Int,
    )

    fun store(ctx: Context) = LocalStore(ctx, PREF)

    fun shortBookName(title: String): String =
        title.replace(" پایه نهم", "").substringBefore(" — ").trim()

    fun subjectOptions(ctx: Context): List<String> {
        val books = GradeGate.filter(BookModuleRegistry.modules) { it.bookCode }
            .map { shortBookName(it.title) }
        return books + SPORT
    }

    fun load(ctx: Context): Snapshot {
        val s = store(ctx)
        val today = LocalDate.now(JalaliDate.TEHRAN)
        val days = (1..5).associateWith { d ->
            runCatching {
                val arr = JSONArray(s.getString("day_$d", "[]"))
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            }.getOrDefault(emptyList())
        }
        return Snapshot(
            locked = s.getBool("locked", false),
            days = days,
            cycleWeeks = s.getInt("cycle_weeks", 2).let { if (it in listOf(1, 2, 4)) it else 2 },
            anchorIso = s.getString("anchor").ifBlank { SchoolShift.startOfPersianWeek(today).toString() },
            fixedEvening = s.getBool("fixed_evening", false),
            lunarOffset = s.getInt("lunar_offset", 0).coerceIn(-2, 2),
            morningHour = s.getInt("m_hour", 7),
            morningMinute = s.getInt("m_min", 30),
            wakeLeadMin = s.getInt("wake_lead", 60),
            noonHour = s.getInt("n_hour", 12),
            noonMinute = s.getInt("n_min", 0),
            sleepMorning = s.getString("sleep_am", "21:30").ifBlank { "21:30" },
            sleepEvening = s.getString("sleep_pm", "23:00").ifBlank { "23:00" },
            weekPattern = s.getString("week_pattern").split(",").map { it.trim() }.filter { it == "morning" || it == "evening" },
            virtualMorningHour = s.getInt("virt_m_h", 8),
            virtualMorningMinute = s.getInt("virt_m_m", 0),
            virtualNoonHour = s.getInt("virt_n_h", 14),
            virtualNoonMinute = s.getInt("virt_n_m", 0),
        )
    }

    fun saveDays(ctx: Context, days: Map<Int, List<String>>, locked: Boolean) {
        val s = store(ctx)
        days.forEach { (d, list) ->
            val arr = JSONArray(); list.forEach { arr.put(it) }
            s.putString("day_$d", arr.toString())
        }
        s.putBool("locked", locked)
    }

    fun saveShift(ctx: Context, cycleWeeks: Int, anchorIso: String, fixedEvening: Boolean, pattern: List<String> = emptyList()) {
        val s = store(ctx)
        s.putInt("cycle_weeks", cycleWeeks)
        s.putString("anchor", anchorIso)
        s.putBool("fixed_evening", fixedEvening)
        if (pattern.isNotEmpty()) s.putString("week_pattern", pattern.joinToString(","))
    }

    fun saveVirtualHours(ctx: Context, mh: Int, mm: Int, nh: Int, nm: Int) {
        val s = store(ctx)
        s.putInt("virt_m_h", mh); s.putInt("virt_m_m", mm)
        s.putInt("virt_n_h", nh); s.putInt("virt_n_m", nm)
    }

    fun saveTimes(
        ctx: Context,
        morningHour: Int, morningMinute: Int, wakeLeadMin: Int,
        noonHour: Int, noonMinute: Int,
        sleepMorning: String, sleepEvening: String,
    ) {
        val s = store(ctx)
        s.putInt("m_hour", morningHour); s.putInt("m_min", morningMinute)
        s.putInt("wake_lead", wakeLeadMin)
        s.putInt("n_hour", noonHour); s.putInt("n_min", noonMinute)
        s.putString("sleep_am", sleepMorning); s.putString("sleep_pm", sleepEvening)
    }

    fun saveLunarOffset(ctx: Context, offset: Int) {
        store(ctx).putInt("lunar_offset", offset.coerceIn(-2, 2))
    }

    fun shiftOf(snap: Snapshot, date: LocalDate): Shift {
        if (snap.weekPattern.size == snap.cycleWeeks && snap.weekPattern.isNotEmpty()) {
            val pos = Math.floorMod(SchoolShift.weekIndex(snap.anchorIso, date), snap.cycleWeeks.toLong()).toInt()
            return if (snap.weekPattern.getOrNull(pos) == "evening") Shift.EVENING else Shift.MORNING
        }
        if (snap.cycleWeeks == 1) return if (snap.fixedEvening) Shift.EVENING else Shift.MORNING
        return SchoolShift.shiftOn(snap.anchorIso, date, snap.cycleWeeks)
    }

    fun captionOf(snap: Snapshot, date: LocalDate): String {
        val shift = shiftOf(snap, date)
        val cap = if (snap.cycleWeeks == 1) "هفته جاری" else SchoolShift.cycleCaption(snap.anchorIso, date, snap.cycleWeeks)
        return "$cap · ${shift.label}"
    }

    /** شیفت هفتهٔ جاری را عوض کن و لنگر را طوری بگذار که محاسبه درست دربیاید. */
    fun setCurrentWeekShift(ctx: Context, want: Shift) {
        val snap = load(ctx)
        val today = LocalDate.now(JalaliDate.TEHRAN)
        val weekStart = SchoolShift.startOfPersianWeek(today)
        if (snap.cycleWeeks == 1) {
            saveShift(ctx, 1, weekStart.toString(), want == Shift.EVENING)
            return
        }
        // لنگر = شنبه‌ای که هفتهٔ صبحِ چرخه است.
        val anchor = if (want == Shift.MORNING) weekStart else {
            val back = if (snap.cycleWeeks == 4) 14L else 7L
            weekStart.minusDays(back)
        }
        saveShift(ctx, snap.cycleWeeks, anchor.toString(), false)
    }

    fun lessonsFor(snap: Snapshot, date: LocalDate): List<String> {
        val idx = SchoolShift.dayIndex(date)
        if (idx !in 1..5) return emptyList()
        return snap.days[idx].orEmpty()
    }

    fun isSchoolHoliday(snap: Snapshot, date: LocalDate): Boolean {
        val idx = SchoolShift.dayIndex(date)
        if (idx >= 6) return true // پنجشنبه و جمعه
        val j = JalaliDate.toJalali(date.toString()) ?: return false
        if (IranOfficialHolidays.occasion(j) != null) return true
        return lunarOccasion(j, snap.lunarOffset) != null
    }

    fun occasionLabel(snap: Snapshot, date: LocalDate): String? {
        val j = JalaliDate.toJalali(date.toString()) ?: return null
        IranOfficialHolidays.occasion(j)?.let { return it }
        lunarOccasion(j, snap.lunarOffset)?.let { return it }
        val idx = SchoolShift.dayIndex(date)
        return when (idx) {
            6 -> "پنجشنبه"
            7 -> "جمعه"
            else -> null
        }
    }

    fun holidayRoutine(date: LocalDate): String {
        return when (SchoolShift.dayIndex(date)) {
            6 -> "امروز پنجشنبه است؛ مرور سبک درس‌ها و کمی استراحت."
            7 -> "امروز جمعه است؛ خانواده، بازی و خواب کافی."
            else -> "امروز تعطیل رسمی است؛ روتین آرام: کتاب آزاد و پیاده‌روی کوتاه."
        }
    }

    private fun lunarOccasion(j: JalaliDate.Jalali, offset: Int): String? {
        if (offset == 0) {
            return IranOfficialHolidays.lunarOn(j)
        }
        val iso = JalaliDate.toGregorianIso(j) ?: return null
        val shifted = LocalDate.parse(iso).minusDays(offset.toLong())
        val sj = JalaliDate.toJalali(shifted.toString()) ?: return null
        return IranOfficialHolidays.lunarOn(sj)
    }

    fun prepBag(ctx: Context, iso: String) = store(ctx).getBool("bag_$iso", false)
    fun prepHw(ctx: Context, iso: String) = store(ctx).getBool("hw_$iso", false)
    fun setPrepBag(ctx: Context, iso: String, v: Boolean) { store(ctx).putBool("bag_$iso", v) }
    fun setPrepHw(ctx: Context, iso: String, v: Boolean) { store(ctx).putBool("hw_$iso", v) }
    fun examOf(ctx: Context, iso: String) = store(ctx).getString("exam_$iso")
    fun setExam(ctx: Context, iso: String, subject: String) { store(ctx).putString("exam_$iso", subject) }
    fun reportOf(ctx: Context, iso: String) = store(ctx).getString("rep_$iso")
    fun setReport(ctx: Context, iso: String, text: String) { store(ctx).putString("rep_$iso", text) }

    fun virtualDays(ctx: Context): Set<String> {
        val arr = runCatching { JSONArray(store(ctx).getString("virtual_days", "[]")) }.getOrDefault(JSONArray())
        return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toSet()
    }

    fun isVirtual(ctx: Context, iso: String): Boolean = iso in virtualDays(ctx)

    fun setVirtual(ctx: Context, iso: String, on: Boolean) {
        val set = virtualDays(ctx).toMutableSet()
        if (on) set += iso else set -= iso
        val arr = JSONArray(); set.sorted().forEach { arr.put(it) }
        store(ctx).putString("virtual_days", arr.toString())
    }

    fun setVirtualRange(ctx: Context, fromIso: String, toIso: String, on: Boolean) {
        val from = runCatching { LocalDate.parse(fromIso) }.getOrNull() ?: return
        val to = runCatching { LocalDate.parse(toIso) }.getOrNull() ?: return
        var d = from
        while (!d.isAfter(to)) {
            setVirtual(ctx, d.toString(), on)
            d = d.plusDays(1)
        }
    }

    fun packIdForSubject(subject: String): String? {
        if (subject.isBlank() || subject == SPORT) return null
        return BookModuleRegistry.modules.firstOrNull { shortBookName(it.title) == subject }
            ?.packs?.firstOrNull()?.packId
    }

    fun saveExamReport(ctx: Context, iso: String, subject: String, text: String) {
        setReport(ctx, iso, text)
        val packId = packIdForSubject(subject) ?: return
        if (text.isBlank()) return
        TeachStats.noteSchoolExam(ctx, packId, iso, text)
        StudyActivity.add(ctx, packId, "school_exam", "گزارش امتحان مدرسه ($iso): $text")
    }

    fun wakeHourMinute(snap: Snapshot, shift: Shift): Pair<Int, Int> {
        return if (shift == Shift.MORNING) {
            var m = snap.morningHour * 60 + snap.morningMinute - snap.wakeLeadMin
            if (m < 0) m = 0
            (m / 60) to (m % 60)
        } else {
            snap.noonHour to snap.noonMinute
        }
    }

    fun sleepText(snap: Snapshot, shift: Shift): String =
        if (shift == Shift.MORNING) snap.sleepMorning else snap.sleepEvening

    const val ALARM_MORNING = "school_wake_morning"
    const val ALARM_NOON = "school_wake_noon"

    fun syncAlarms(ctx: Context, reminders: ReminderScheduler, snap: Snapshot, date: LocalDate) {
        SchoolAlarmStore.sync(ctx, reminders, snap, date)
    }

    fun alarmIsSet(reminders: ReminderScheduler, snap: Snapshot, date: LocalDate): Boolean {
        val shift = shiftOf(snap, date)
        val id = if (shift == Shift.MORNING) ALARM_MORNING else ALARM_NOON
        val r = reminders.find(id) ?: return false
        val (h, m) = wakeHourMinute(snap, shift)
        return r.enabled && r.hour == h && r.minute == m
    }
}
