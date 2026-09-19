package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.notifications.Reminder
import com.hamyareman.ir.platform.core.notifications.ReminderScheduler
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.ui.home.IranOfficialHolidays
import com.hamyareman.ir.ui.profile.GradeGate
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

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
        /** ساعت خروج از مدرسه — شیفت صبح (HH:MM). */
        val exitMorning: String = "13:30",
        /** ساعت خروج از مدرسه — شیفت ظهر (HH:MM). */
        val exitNoon: String = "17:30",
    )

    /** ساعت خروجِ شیفتِ داده‌شده به صورت «HH:MM». */
    fun exitOf(snap: Snapshot, shift: Shift): String =
        if (shift == Shift.MORNING) snap.exitMorning.ifBlank { "13:30" } else snap.exitNoon.ifBlank { "17:30" }

    /** دقیقه‌های گذشته از نیمه‌شب برای یک زمانِ «HH:MM». */
    fun minutesOf(hhmm: String): Int {
        val p = hhmm.split(":")
        val h = p.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val m = p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    /**
     * آیا «رفرشِ بعد از مدرسه» برای امروز انجام شده؟ (تیک‌های فردا آزاد می‌شوند)
     */
    fun refreshedToday(ctx: Context, today: LocalDate = LocalDate.now(JalaliDate.TEHRAN)): Boolean =
        store(ctx).getBool("refreshed_$today", false)

    /**
     * رفرشِ اطلاع‌رسانی‌های فردا در ساعتِ خروج (یا پایانِ کلاس مجازی):
     * تیک‌های قفل‌شده آزاد می‌شوند تا برای روز بعد آماده شوند.
     */
    fun maybeRefreshAtExit(ctx: Context, now: LocalDateTime = LocalDateTime.now(JalaliDate.TEHRAN)) {
        val today = now.toLocalDate()
        val s = store(ctx)
        if (s.getBool("refreshed_$today", false)) return
        val snap = load(ctx)
        val shift = shiftOf(snap, today)
        val exitMin = minutesOf(exitOf(snap, shift))
        // روزهای مجازی: پایانِ کلاس مجازی جای ساعت خروج را می‌گیرد.
        val endMin = virtualSessions(ctx).firstOrNull { it.dayIndex == SchoolShift.dayIndex(today) }
            ?.let { it.endH * 60 + it.endM }
        val gate = endMin ?: exitMin
        if (now.hour * 60 + now.minute < gate) return
        s.keysWithPrefix("lock_").forEach { s.remove(it) }
        s.putBool("refreshed_$today", true)
    }

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
            exitMorning = s.getString("exit_am").ifBlank { "13:30" },
            exitNoon = s.getString("exit_pm").ifBlank { "17:30" },
        )
    }

    /** حذفِ یک خانه (درس) از یک روزِ برنامهٔ هفتگی. */
    fun removeSlot(ctx: Context, dayIndex: Int, slotIndex: Int) {
        val s = store(ctx)
        val arr = runCatching { JSONArray(s.getString("day_$dayIndex", "[]")) }.getOrDefault(JSONArray())
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            if (i == slotIndex) continue
            out.put(arr.optString(i))
        }
        s.putString("day_$dayIndex", out.toString())
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

    fun saveExitTimes(ctx: Context, morning: String, noon: String) {
        val s = store(ctx)
        s.putString("exit_am", morning)
        s.putString("exit_pm", noon)
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

    /**
     * تیکِ «کیف/تکالیف» بعد از یک‌بار زدن **قفل** می‌شود تا ساعتِ خروج؛
     * آن‌وقت [maybeRefreshAtExit] قفل‌ها را برای روز بعد باز می‌کند.
     */
    fun bagLocked(ctx: Context, iso: String) = store(ctx).getBool("lock_bag_$iso", false)
    fun hwLocked(ctx: Context, iso: String) = store(ctx).getBool("lock_hw_$iso", false)

    fun setPrepBag(ctx: Context, iso: String, v: Boolean) {
        store(ctx).putBool("bag_$iso", v)
        if (v) store(ctx).putBool("lock_bag_$iso", true) else store(ctx).remove("lock_bag_$iso")
    }

    fun setPrepHw(ctx: Context, iso: String, v: Boolean) {
        store(ctx).putBool("hw_$iso", v)
        if (v) store(ctx).putBool("lock_hw_$iso", true) else store(ctx).remove("lock_hw_$iso")
    }

    // --- آمادگیِ امتحان (فقط وقتی امتحان وجود دارد) ---
    private val EXAM_PREP = listOf("مرور خلاصهٔ درس", "حل تمرین‌های کلیدی", "فلش‌کارت‌ها", "یک نمونه‌سؤال")

    fun examPrepOptions(): List<String> = EXAM_PREP

    fun examPrepDone(ctx: Context, iso: String, option: String): Boolean =
        store(ctx).getBool("examprep_${iso}_$option", false)

    fun setExamPrepDone(ctx: Context, iso: String, option: String, v: Boolean) {
        store(ctx).putBool("examprep_${iso}_$option", v)
    }
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

    // ------------------------------------------------------------ کلاس مجازی

    /** یک جلسه‌ی کلاس مجازی در یک روز هفته (شنبه=۱ … پنجشنبه=۵). */
    data class VirtualSession(
        val dayIndex: Int,
        val startH: Int,
        val startM: Int,
        val endH: Int,
        val endM: Int,
        val subject: String = "",
    ) {
        val timeFa: String
            get() = toPersianDigits("%d:%02d".format(startH, startM)) +
                " تا " + toPersianDigits("%d:%02d".format(endH, endM))
    }

    fun virtualSessions(ctx: Context): List<VirtualSession> {
        val arr = runCatching { JSONArray(store(ctx).getString("virtual_sessions", "[]")) }
            .getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            VirtualSession(
                dayIndex = o.optInt("dayIndex", 1).coerceIn(1, 5),
                startH = o.optInt("startH", 8).coerceIn(0, 23),
                startM = o.optInt("startM", 0).coerceIn(0, 59),
                endH = o.optInt("endH", 9).coerceIn(0, 23),
                endM = o.optInt("endM", 0).coerceIn(0, 59),
                subject = o.optString("subject"),
            )
        }.sortedBy { it.dayIndex }
    }

    fun saveVirtualSessions(ctx: Context, list: List<VirtualSession>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject()
                    .put("dayIndex", s.dayIndex)
                    .put("startH", s.startH).put("startM", s.startM)
                    .put("endH", s.endH).put("endM", s.endM)
                    .put("subject", s.subject),
            )
        }
        store(ctx).putString("virtual_sessions", arr.toString())
    }

    /** یک بازه‌ی تاریخ مجازی (برای نمایش در آکاردیون و حذف تکی). */
    data class VirtualRange(val id: String, val fromIso: String, val toIso: String)

    fun virtualRanges(ctx: Context): List<VirtualRange> {
        val arr = runCatching { JSONArray(store(ctx).getString("virtual_ranges", "[]")) }
            .getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            VirtualRange(
                id = o.optString("id"),
                fromIso = o.optString("from"),
                toIso = o.optString("to"),
            )
        }.sortedBy { it.fromIso }
    }

    /** ثبت بازه + اعمال روی روزها؛ شناسه برمی‌گردد تا در آکاردیون لیست شود. */
    fun addVirtualRange(ctx: Context, fromIso: String, toIso: String): VirtualRange? {
        val from = runCatching { LocalDate.parse(fromIso) }.getOrNull() ?: return null
        val to = runCatching { LocalDate.parse(toIso) }.getOrNull() ?: return null
        val (a, b) = if (from.isAfter(to)) to to from else from to to
        val range = VirtualRange(id = "vr_${System.currentTimeMillis()}", a.toString(), b.toString())
        val list = virtualRanges(ctx).toMutableList()
        list += range
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().put("id", r.id).put("from", r.fromIso).put("to", r.toIso))
        }
        store(ctx).putString("virtual_ranges", arr.toString())
        setVirtualRange(ctx, a.toString(), b.toString(), true)
        return range
    }

    fun removeVirtualRange(ctx: Context, id: String) {
        val list = virtualRanges(ctx)
        val gone = list.firstOrNull { it.id == id } ?: return
        val arr = JSONArray()
        list.filterNot { it.id == id }.forEach { r ->
            arr.put(JSONObject().put("id", r.id).put("from", r.fromIso).put("to", r.toIso))
        }
        store(ctx).putString("virtual_ranges", arr.toString())
        setVirtualRange(ctx, gone.fromIso, gone.toIso, false)
    }

    // ------------------------------------------------- واژه‌ی «امروز/فردا»

    /**
     * واژه‌ی روز مقصد: اگر همان روزِ جاری باشد «امروز»، اگر فردا باشد «فردا»،
     * وگرنه نام روز هفته.
     */
    fun dayWordFor(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "امروز"
        today.plusDays(1) -> "فردا"
        else -> JalaliDate.weekDayFa(date.toString())
    }

    /**
     * روزِ مقصدِ آماده‌سازی با لحاظ‌کردن عبور از نیمه‌شب:
     * بعد از ساعت ۱۲ شب، «فردا»ی دیشب همان «امروز» است.
     * (تا ساعت ۴ صبح هنوز همان روزِ پیش‌رو مدنظر است.)
     */
    fun prepTargetDate(now: LocalDateTime = LocalDateTime.now(JalaliDate.TEHRAN)): LocalDate {
        val today = now.toLocalDate()
        return if (now.hour < 4) today else today.plusDays(1)
    }

    /** برچسب کاملِ روز مقصد همراه با شیفت: «امروز صبح» / «فردا ظهر». */
    fun dayLabelFor(date: LocalDate, today: LocalDate, shift: Shift): String =
        "${dayWordFor(date, today)} ${if (shift == Shift.MORNING) "صبح" else "ظهر"}"

    /** نخستین روزِ غیرتعطیل از [from] به بعد (برای آماده‌سازیِ روز بعد). */
    fun firstSchoolDay(snap: Snapshot, from: LocalDate): LocalDate =
        generateSequence(from) { it.plusDays(1) }
            .take(14)
            .firstOrNull { !isSchoolHoliday(snap, it) }
            ?: from


    // ------------------------------------------------------- سینک با سرور

    /** وضعیتِ یک کلید را به صورت JSON بیرون می‌دهد (برای `app_state`). */
    fun exportState(ctx: Context, key: String): String {
        val snap = load(ctx)
        return when (key) {
            StateSync.KEY_WEEK -> JSONObject().apply {
                put("locked", snap.locked)
                val days = JSONObject()
                snap.days.forEach { (d, list) -> days.put(d.toString(), JSONArray().apply { list.forEach { put(it) } }) }
                put("days", days)
            }.toString()

            StateSync.KEY_SHIFT -> JSONObject().apply {
                put("cycleWeeks", snap.cycleWeeks)
                put("anchorIso", snap.anchorIso)
                put("fixedEvening", snap.fixedEvening)
                put("weekPattern", JSONArray().apply { snap.weekPattern.forEach { put(it) } }.toString())
                put("morningHour", snap.morningHour)
                put("morningMinute", snap.morningMinute)
                put("noonHour", snap.noonHour)
                put("noonMinute", snap.noonMinute)
                put("wakeLeadMin", snap.wakeLeadMin)
                put("sleepMorning", snap.sleepMorning)
                put("sleepEvening", snap.sleepEvening)
                put("exitMorning", snap.exitMorning)
                put("exitNoon", snap.exitNoon)
            }.toString()

            StateSync.KEY_VIRTUAL -> JSONObject().apply {
                put("sessions", JSONArray().apply {
                    virtualSessions(ctx).forEach { s ->
                        put(
                            JSONObject()
                                .put("dayIndex", s.dayIndex)
                                .put("startH", s.startH).put("startM", s.startM)
                                .put("endH", s.endH).put("endM", s.endM)
                                .put("subject", s.subject),
                        )
                    }
                })
                put("ranges", JSONArray().apply {
                    virtualRanges(ctx).forEach { r ->
                        put(JSONObject().put("id", r.id).put("from", r.fromIso).put("to", r.toIso))
                    }
                })
                put("days", JSONArray().apply { virtualDays(ctx).sorted().forEach { put(it) } })
            }.toString()

            StateSync.KEY_CHECKS -> JSONObject().apply {
                val s = store(ctx)
                val bag = JSONObject(); val hw = JSONObject(); val exam = JSONObject(); val rep = JSONObject()
                s.keysWithPrefix("bag_").forEach { k -> bag.put(k.removePrefix("bag_"), s.getBool(k)) }
                s.keysWithPrefix("hw_").forEach { k -> hw.put(k.removePrefix("hw_"), s.getBool(k)) }
                s.keysWithPrefix("exam_").forEach { k -> exam.put(k.removePrefix("exam_"), s.getString(k)) }
                s.keysWithPrefix("rep_").forEach { k -> rep.put(k.removePrefix("rep_"), s.getString(k)) }
                put("bag", bag); put("hw", hw); put("exam", exam); put("report", rep)
            }.toString()

            else -> "{}"
        }
    }

    /** اِعمالِ وضعیتِ رسیده از سرور روی دستگاه (آخرین نوشته برنده است). */
    fun importState(ctx: Context, key: String, payload: String) {
        val o = runCatching { JSONObject(payload) }.getOrNull() ?: return
        when (key) {
            StateSync.KEY_WEEK -> {
                val daysObj = o.optJSONObject("days") ?: return
                val days = (1..5).associate { d ->
                    d to runCatching {
                        val arr = daysObj.optJSONArray(d.toString()) ?: JSONArray()
                        (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
                    }.getOrDefault(emptyList())
                }
                if (days.values.any { it.isNotEmpty() }) {
                    saveDays(ctx, days, o.optBoolean("locked", false))
                }
            }

            StateSync.KEY_SHIFT -> {
                val cycle = o.optInt("cycleWeeks", 2).let { if (it in listOf(1, 2, 4)) it else 2 }
                val anchor = o.optString("anchorIso")
                val pattern = runCatching {
                    val arr = JSONArray(o.optString("weekPattern", "[]"))
                    (0 until arr.length()).map { arr.optString(it) }.filter { it == "morning" || it == "evening" }
                }.getOrDefault(emptyList())
                if (anchor.isNotBlank()) {
                    saveShift(ctx, cycle, anchor, o.optBoolean("fixedEvening", false), pattern)
                }
                saveTimes(
                    ctx,
                    o.optInt("morningHour", 7), o.optInt("morningMinute", 30), o.optInt("wakeLeadMin", 60),
                    o.optInt("noonHour", 12), o.optInt("noonMinute", 0),
                    o.optString("sleepMorning").ifBlank { "21:30" },
                    o.optString("sleepEvening").ifBlank { "23:00" },
                )
                saveExitTimes(
                    ctx,
                    o.optString("exitMorning").ifBlank { "13:30" },
                    o.optString("exitNoon").ifBlank { "17:30" },
                )
            }

            StateSync.KEY_VIRTUAL -> {
                runCatching {
                    val arr = o.optJSONArray("sessions") ?: JSONArray()
                    val sessions = (0 until arr.length()).mapNotNull { i ->
                        val so = arr.optJSONObject(i) ?: return@mapNotNull null
                        VirtualSession(
                            dayIndex = so.optInt("dayIndex", 1),
                            startH = so.optInt("startH", 8), startM = so.optInt("startM", 0),
                            endH = so.optInt("endH", 9), endM = so.optInt("endM", 0),
                            subject = so.optString("subject"),
                        )
                    }
                    if (sessions.isNotEmpty()) saveVirtualSessions(ctx, sessions)
                }
                runCatching {
                    val days = o.optJSONArray("days") ?: JSONArray()
                    val set = (0 until days.length()).map { days.optString(it) }.filter { it.isNotBlank() }.toSet()
                    set.forEach { setVirtual(ctx, it, true) }
                }
            }

            StateSync.KEY_CHECKS -> {
                val s = store(ctx)
                fun obj(name: String) = o.optJSONObject(name)
                obj("bag")?.keys()?.forEach { k -> s.putBool("bag_$k", obj("bag")!!.optBoolean(k)) }
                obj("hw")?.keys()?.forEach { k -> s.putBool("hw_$k", obj("hw")!!.optBoolean(k)) }
                obj("exam")?.keys()?.forEach { k -> s.putString("exam_$k", obj("exam")!!.optString(k)) }
                obj("report")?.keys()?.forEach { k -> s.putString("rep_$k", obj("report")!!.optString(k)) }
            }
        }
    }
}
