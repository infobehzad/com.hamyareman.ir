package com.hamyareman.ir.ui.study

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.LocalAppContainer
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * برنامه‌ی مدرسه و شیفت — واقعی و قابل ویرایش.
 *
 * چرخه‌ی دوهفته‌ای صبح/عصر از یک «لنگر» تاریخ حساب می‌شود (که خودش قابل تنظیم است)،
 * درس‌های هر روز/شیفت را زهرا می‌نویسد، و چک‌لیست‌های «امروز» و «آماده‌سازی فردا»
 * روی دستگاه ذخیره می‌شوند. هیچ‌کدام Sync نمی‌شوند (برنامه‌ی مدرسه داده‌ی خصوصی است).
 */
internal enum class Shift(val label: String) { MORNING("صبح"), EVENING("عصر") }

/**
 * ریاضیِ خالصِ تقویم مدرسه — جدا از SharedPreferences تا قابل تست باشد.
 */
internal object SchoolShift {

    /** شماره‌ی روز هفته به سبک ایرانی: شنبه = ۱ … جمعه = ۷. */
    fun dayIndex(date: LocalDate): Int = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> 1
        DayOfWeek.SUNDAY -> 2
        DayOfWeek.MONDAY -> 3
        DayOfWeek.TUESDAY -> 4
        DayOfWeek.WEDNESDAY -> 5
        DayOfWeek.THURSDAY -> 6
        DayOfWeek.FRIDAY -> 7
    }

    /**
     * شیفت یک تاریخ با چرخه‌ی دوهفته‌ای از «لنگر» (اولین روز یک هفته‌ی صبح).
     * هفته‌های زوج = صبح، فرد = عصر؛ قبل از لنگر هم درست است.
     */
    fun shiftOn(anchorIso: String, date: LocalDate): Shift {
        val anchor = runCatching { LocalDate.parse(anchorIso) }.getOrDefault(date)
        val days = ChronoUnit.DAYS.between(anchor, date)
        val weekIndex = Math.floorDiv(days, 7L)
        return if (Math.floorMod(weekIndex, 2L) == 0L) Shift.MORNING else Shift.EVENING
    }

    /** شنبه‌ی همان هفته‌ای که `date` در آن است. */
    fun startOfPersianWeek(date: LocalDate): LocalDate =
        date.minusDays((dayIndex(date) - 1).toLong())
}

private const val PLAN_KEY = "school_plan"
private const val ANCHOR_KEY = "shift_anchor"
private val PREP_ITEMS = listOf("کیف مدرسه آماده شد", "تکلیف‌ها انجام شد", "وسایل ورزش فردا", "ساعت خواب فردا تنظیم شد")

private fun planKey(dayIndex: Int, shift: Shift) = "${dayIndex}_${shift.name}"

private fun readPlan(store: LocalStore): Map<String, List<String>> = runCatching {
    val o = JSONObject(store.getString(PLAN_KEY, "{}"))
    buildMap {
        o.keys().forEach { key ->
            val array = o.optJSONArray(key) ?: return@forEach
            put(key, buildList { for (i in 0 until array.length()) add(array.optString(i)) })
        }
    }
}.getOrDefault(emptyMap())

private fun writePlan(store: LocalStore, plan: Map<String, List<String>>) {
    val o = JSONObject()
    plan.forEach { (key, subjects) -> o.put(key, JSONArray(subjects)) }
    store.putString(PLAN_KEY, o.toString())
}

/**
 * شیفت یک تاریخ مشخص، از چرخه‌ی دوهفته‌ای نسبت به «لنگر».
 *
 * لنگر اولین روزِ یک هفته‌ی «صبح» است. هفته‌های زوج = صبح، فرد = عصر.
 * برای تاریخ‌های قبل از لنگر هم درست کار می‌کند (`floorDiv` سمت منفی را هم گرد می‌کند).
 */
/** لنگر چرخه‌ی شیفت؛ اگر نبود، امروز لنگر می‌شود. */
private fun anchorOf(store: LocalStore, date: LocalDate): String =
    store.getString(ANCHOR_KEY).takeUnless { it.isBlank() } ?: date.toString().also {
        store.putString(ANCHOR_KEY, it)
    }

private fun shiftOn(store: LocalStore, date: LocalDate): Shift =
    SchoolShift.shiftOn(anchorOf(store, date), date)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolScheduleScreen(onBack: () -> Unit) {
    val store = LocalAppContainer.current.store
    val today = remember { LocalDate.now() }
    val tomorrow = remember { today.plusDays(1) }

    var plan by remember { mutableStateOf(readPlan(store)) }
    var editDay by remember { mutableIntStateOf(SchoolShift.dayIndex(today)) }
    var editShift by remember { mutableStateOf(shiftOn(store, today)) }
    var subjectInput by remember { mutableStateOf("") }
    var doneTick by remember { mutableIntStateOf(0) }

    val shift = remember(doneTick) { shiftOn(store, today) }
    val todaySubjects = plan[planKey(SchoolShift.dayIndex(today), shift)].orEmpty()
    val tomorrowShift = remember(doneTick) { shiftOn(store, tomorrow) }
    val tomorrowSubjects = remember(plan, tomorrowShift) {
        plan[planKey(SchoolShift.dayIndex(tomorrow), tomorrowShift)].orEmpty()
    }

    val todayDone = remember(doneTick) { store.getStringSet("school_done_${today}") }
    val prepDone = remember(doneTick) { store.getStringSet("prep_done_${tomorrow}") }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("شیفت مدرسه", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionCard(
                title = "شیفت جاری: ${shift.label}",
                body = "${JalaliDate.formatFaLong(today.toString())} · ${JalaliDate.weekDayFa(today.toString())} — " +
                    "چرخه‌ی دوهفته‌ای صبح/عصر. اگر اشتباه است، لنگر را در پایین عوض کن.",
            ) { }

            Text("درس‌های امروز", style = MaterialTheme.typography.titleMedium)
            if (todaySubjects.isEmpty()) {
                Text("برای امروز درسی ثبت نکردی — از بخش «برنامه‌ی هفته» پایین اضافه کن.", style = MaterialTheme.typography.bodySmall)
            }
            todaySubjects.forEach { subject ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = subject in todayDone,
                        onCheckedChange = { checked ->
                            val next = if (checked) todayDone + subject else todayDone - subject
                            store.putStringSet("school_done_${today}", next)
                            doneTick++
                        },
                    )
                    Text(subject, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("آماده‌سازی فردا", style = MaterialTheme.typography.titleMedium)
            if (tomorrowSubjects.isNotEmpty()) {
                SectionCard(
                    title = "درس‌های فردا: ${tomorrowSubjects.joinToString("، ")}",
                    body = "${JalaliDate.weekDayFa(tomorrow.toString())} · شیفت ${tomorrowShift.label} — وسایل هر درس را امشب بگذار توی کیف.",
                ) { }
            }
            PREP_ITEMS.forEach { item ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = item in prepDone,
                        onCheckedChange = { checked ->
                            val next = if (checked) prepDone + item else prepDone - item
                            store.putStringSet("prep_done_${tomorrow}", next)
                            doneTick++
                        },
                    )
                    Text(item, style = MaterialTheme.typography.bodyLarge)
                }
            }
            SectionCard(
                title = "ساعت خواب",
                body = if (shift == Shift.MORNING) "شیفت صبح: امشب زودتر بخواب (حداقل ۸ ساعت)." else "شیفت عصر: دیشب خواب کافی مهم بود؛ امشب جبران کن.",
            ) { }

            Spacer(Modifier.height(4.dp))
            Text("برنامه‌ی هفته", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (1..7).forEach { dayIndex ->
                    val iso = SchoolShift.startOfPersianWeek(today).plusDays((dayIndex - 1).toLong())
                    FilterChip(
                        selected = editDay == dayIndex,
                        onClick = { editDay = dayIndex },
                        label = { Text(JalaliDate.weekDayFa(iso.toString())) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Shift.entries.forEach { item ->
                    FilterChip(
                        selected = editShift == item,
                        onClick = { editShift = item },
                        label = { Text(item.label) },
                    )
                }
            }

            val editing = plan[planKey(editDay, editShift)].orEmpty()
            OutlinedTextField(
                value = subjectInput,
                onValueChange = { subjectInput = it },
                label = { Text("اسم درس (مثلاً ریاضی)") },
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("افزودن درس") {
                val name = subjectInput.trim()
                if (name.isNotEmpty() && name !in editing) {
                    plan = plan + (planKey(editDay, editShift) to (editing + name))
                    writePlan(store, plan)
                    subjectInput = ""
                }
            }
            editing.forEach { subject ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(subject, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = {
                        val key = planKey(editDay, editShift)
                        plan = plan + (key to (plan[key].orEmpty() - subject))
                        writePlan(store, plan)
                    }) { Text("حذف") }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("لنگر چرخه‌ی شیفت", style = MaterialTheme.typography.titleSmall)
            Text(
                "اگر شیفت نمایش‌داده‌شده با واقعیت نمی‌خورد، این تاریخ را به اولین روزِ هفته‌ی «صبح» تغییر بده.",
                style = MaterialTheme.typography.bodySmall,
            )
            PrimaryButton("این هفته را صبح فرض کن (لنگر = امروز)") {
                store.putString(ANCHOR_KEY, today.toString())
                doneTick++
            }
        }
    }
}
