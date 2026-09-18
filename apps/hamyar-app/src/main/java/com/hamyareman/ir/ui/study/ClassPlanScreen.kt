package com.hamyareman.ir.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.home.DashboardFonts
import com.hamyareman.ir.ui.home.IranOfficialHolidays
import java.time.LocalDate

@Composable
fun ClassPlanScreen(onBack: () -> Unit, initialTab: Int = 0) {
    var tab by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("برنامه کلاسی مدرسه", onBack)
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
            listOf("هفتگی", "تقویم", "شیفت مدرسه").forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = {
                    Text(label, fontFamily = DashboardFonts.quote)
                })
            }
        }
        when (tab) {
            0 -> WeeklyTimetableSection()
            1 -> ShamsiCalendarSection()
            else -> ShiftSection()
        }
    }
}

@Composable
private fun WeeklyTimetableSection() {
    val ctx = LocalContext.current
    val options = remember { ClassPlanStore.subjectOptions(ctx) }
    var snap by remember { mutableStateOf(ClassPlanStore.load(ctx)) }
    var days by remember { mutableStateOf(snap.days.mapValues { (_, v) -> v.ifEmpty { listOf("", "", "") }.toMutableList() }) }
    var locked by remember { mutableStateOf(snap.locked) }
    var confirmEdit by remember { mutableStateOf(false) }

    fun persist(lock: Boolean) {
        ClassPlanStore.saveDays(ctx, days.mapValues { it.value.filter { s -> s.isNotBlank() }.ifEmpty { it.value } }, lock)
        locked = lock
        snap = ClassPlanStore.load(ctx)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "شنبه تا چهارشنبه، سه درس در روز. از فهرست کتاب‌ها یا ورزش انتخاب کن. پس از تکمیل، فهرست‌ها غیرفعال می‌شوند.",
            fontFamily = DashboardFonts.quote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ClassPlanStore.WEEKDAYS.forEachIndexed { i, name ->
            val di = i + 1
            val slots = days[di] ?: listOf("", "", "")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(name, fontFamily = DashboardFonts.greeting, fontSize = 18.sp)
                slots.forEachIndexed { si, value ->
                    SubjectDropdown(
                        value = value,
                        options = options,
                        enabled = !locked,
                        onPick = { picked ->
                            val next = slots.toMutableList()
                            next[si] = picked
                            days = days + (di to next)
                        },
                    )
                }
                if (!locked) {
                    TextButton(onClick = {
                        days = days + (di to (slots + ""))
                    }) { Text("افزودن خانه", fontFamily = DashboardFonts.quote) }
                }
            }
        }
        if (locked) {
            OutlinedButton(onClick = { confirmEdit = true }, modifier = Modifier.fillMaxWidth()) {
                Text("ویرایش برنامه هفتگی", fontFamily = DashboardFonts.quote)
            }
        } else {
            OutlinedButton(
                onClick = {
                    persist(lock = (1..5).all { d -> (days[d]?.count { it.isNotBlank() } ?: 0) >= 3 })
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ذخیره برنامه", fontFamily = DashboardFonts.quote) }
        }
    }
    if (confirmEdit) {
        AlertDialog(
            onDismissRequest = { confirmEdit = false },
            title = { Text("ویرایش برنامه؟") },
            text = { Text("برنامهٔ هفتگی قفل است. مطمئنی می‌خواهی تغییرش بدهی؟") },
            confirmButton = {
                TextButton(onClick = { confirmEdit = false; locked = false }) { Text("بله، ویرایش") }
            },
            dismissButton = { TextButton(onClick = { confirmEdit = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun SubjectDropdown(
    value: String,
    options: List<String>,
    enabled: Boolean,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { if (enabled) open = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(value.ifBlank { "انتخاب درس" }, fontFamily = DashboardFonts.quote)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontFamily = DashboardFonts.quote) },
                    onClick = { onPick(opt); open = false },
                )
            }
        }
    }
}

@Composable
private fun ShamsiCalendarSection() {
    val ctx = LocalContext.current
    var snap by remember { mutableStateOf(ClassPlanStore.load(ctx)) }
    val todayJ = JalaliDate.todayJalali()
    var year by remember { mutableIntStateOf(todayJ.year) }
    var month by remember { mutableIntStateOf(todayJ.month) }
    val weekHdr = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    fun firstDow(y: Int, m: Int): Int {
        val iso = JalaliDate.toGregorianIso(JalaliDate.Jalali(y, m, 1)) ?: return 0
        return (SchoolShift.dayIndex(LocalDate.parse(iso)) - 1).coerceIn(0, 6)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("تعطیلات قمری را در صورت اختلاف اعلام، تا ۲ روز جابه‌جا کن.", fontFamily = DashboardFonts.quote, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (-2..2).forEach { off ->
                FilterChip(
                    selected = snap.lunarOffset == off,
                    onClick = {
                        ClassPlanStore.saveLunarOffset(ctx, off)
                        snap = ClassPlanStore.load(ctx)
                    },
                    label = { Text(if (off == 0) "۰" else toPersianDigits((if (off > 0) "+$off" else "$off")), fontFamily = DashboardFonts.quote) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                if (month == 1) { month = 12; year-- } else month--
            }) { Text("ماه قبل") }
            Text(
                "${JalaliDate.monthName(month)} ${toPersianDigits(year.toString())}",
                fontFamily = DashboardFonts.greeting,
                fontSize = 20.sp,
            )
            TextButton(onClick = {
                if (month == 12) { month = 1; year++ } else month++
            }) { Text("ماه بعد") }
        }
        Row(Modifier.fillMaxWidth()) {
            weekHdr.forEach { h ->
                Text(h, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = DashboardFonts.quote, fontWeight = FontWeight.Bold)
            }
        }
        val dim = JalaliDate.daysInMonth(year, month)
        val pad = firstDow(year, month)
        val cells = List(pad) { 0 } + (1..dim).toList()
        cells.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day > 0) {
                            val iso = JalaliDate.toGregorianIso(JalaliDate.Jalali(year, month, day))
                            val date = iso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                            val occ = date?.let { ClassPlanStore.occasionLabel(snap, it) }
                            val holiday = date?.let { ClassPlanStore.isSchoolHoliday(snap, it) } == true
                            val isToday = year == todayJ.year && month == todayJ.month && day == todayJ.day
                            val bg = when {
                                isToday -> MaterialTheme.colorScheme.primary
                                holiday -> Color(0xFFFECACA)
                                else -> Color.Transparent
                            }
                            val fg = when {
                                isToday -> Color.White
                                holiday -> Color(0xFF9F1239)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Column(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(bg),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(toPersianDigits(day.toString()), color = fg, fontFamily = DashboardFonts.quote, fontSize = 13.sp)
                                if (!occ.isNullOrBlank() && occ != "پنجشنبه" && occ != "جمعه") {
                                    Text("•", color = fg, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        val monthOccasions = (1..dim).mapNotNull { d ->
            val iso = JalaliDate.toGregorianIso(JalaliDate.Jalali(year, month, d)) ?: return@mapNotNull null
            val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return@mapNotNull null
            val lab = ClassPlanStore.occasionLabel(snap, date) ?: return@mapNotNull null
            if (lab == "پنجشنبه" || lab == "جمعه") null
            else toPersianDigits("$d ${JalaliDate.monthName(month)}") + " — $lab"
        }
        if (monthOccasions.isNotEmpty()) {
            Text("مناسبات این ماه", fontFamily = DashboardFonts.greeting, fontSize = 16.sp)
            monthOccasions.forEach { Text(it, fontFamily = DashboardFonts.quote, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ShiftSection() {
    val ctx = LocalContext.current
    val reminders = LocalAppContainer.current.reminders
    var snap by remember { mutableStateOf(ClassPlanStore.load(ctx)) }
    val today = LocalDate.now(JalaliDate.TEHRAN)
    var cycle by remember { mutableIntStateOf(snap.cycleWeeks) }
    var mH by remember { mutableIntStateOf(snap.morningHour) }
    var mM by remember { mutableIntStateOf(snap.morningMinute) }
    var lead by remember { mutableIntStateOf(snap.wakeLeadMin) }
    var nH by remember { mutableIntStateOf(snap.noonHour) }
    var nM by remember { mutableIntStateOf(snap.noonMinute) }
    var sleepAm by remember { mutableStateOf(snap.sleepMorning) }
    var sleepPm by remember { mutableStateOf(snap.sleepEvening) }
    val current = ClassPlanStore.shiftOf(snap, today)

    fun flushTimes() {
        ClassPlanStore.saveTimes(ctx, mH, mM, lead, nH, nM, sleepAm, sleepPm)
        snap = ClassPlanStore.load(ctx)
        ClassPlanStore.syncAlarms(ctx, reminders, snap, today)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(ClassPlanStore.captionOf(snap, today), fontFamily = DashboardFonts.greeting, fontSize = 20.sp)
        Text("بازهٔ چرخش شیفت", fontFamily = DashboardFonts.quote, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "یک هفته", 2 to "دو هفته", 4 to "یک ماه").forEach { (n, lab) ->
                FilterChip(
                    selected = cycle == n,
                    onClick = {
                        cycle = n
                        ClassPlanStore.saveShift(ctx, n, snap.anchorIso, snap.fixedEvening)
                        snap = ClassPlanStore.load(ctx)
                    },
                    label = { Text(lab, fontFamily = DashboardFonts.quote) },
                )
            }
        }
        Text("شیفت هفتهٔ جاری", fontFamily = DashboardFonts.quote, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shift.entries.forEach { sh ->
                FilterChip(
                    selected = current == sh,
                    onClick = {
                        ClassPlanStore.setCurrentWeekShift(ctx, sh)
                        snap = ClassPlanStore.load(ctx)
                        ClassPlanStore.syncAlarms(ctx, reminders, snap, today)
                    },
                    label = { Text(sh.label, fontFamily = DashboardFonts.quote) },
                )
            }
        }
        Text("زنگ صبح (ساعت:دقیقه) و فاصلهٔ بیداری قبل از زنگ", fontFamily = DashboardFonts.quote, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TinyIntField("ساعت صبح", mH, 0, 23) { mH = it; flushTimes() }
            TinyIntField("دقیقه", mM, 0, 59) { mM = it; flushTimes() }
            TinyIntField("دقیقه قبل", lead, 15, 180) { lead = it; flushTimes() }
        }
        Text("آلارم شیفت بعدازظهر (حوالی ۱۲)", fontFamily = DashboardFonts.quote, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TinyIntField("ساعت ظهر", nH, 10, 15) { nH = it; flushTimes() }
            TinyIntField("دقیقه", nM, 0, 59) { nM = it; flushTimes() }
        }
        OutlinedTextField(value = sleepAm, onValueChange = { sleepAm = it; flushTimes() }, label = { Text("ساعت خواب شیفت صبح") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sleepPm, onValueChange = { sleepPm = it; flushTimes() }, label = { Text("ساعت خواب شیفت بعدازظهر") }, modifier = Modifier.fillMaxWidth())
        val (ah, am) = ClassPlanStore.wakeHourMinute(snap, ClassPlanStore.shiftOf(snap, today))
        Text(
            "آلارم فعال: ${toPersianDigits("%d:%02d".format(ah, am))} — شیفت مخالف حذف می‌شود.",
            fontFamily = DashboardFonts.quote,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TinyIntField(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { t -> t.toIntOrNull()?.let { onChange(it.coerceIn(min, max)) } },
        label = { Text(label, fontSize = 11.sp) },
        modifier = Modifier.width(110.dp),
    )
}
