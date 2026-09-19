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
    var days by remember {
        mutableStateOf(
            (1..5).associateWith { d ->
                val v = snap.days[d].orEmpty()
                (if (v.isEmpty()) listOf("", "", "") else v).toMutableList()
            },
        )
    }
    var locked by remember { mutableStateOf(snap.locked) }
    var confirmEdit by remember { mutableStateOf(false) }

    fun persist(lock: Boolean, map: Map<Int, List<String>> = days) {
        val clean = map.mapValues { e ->
            val v = e.value.toMutableList()
            while (v.size < 3) v.add("")
            v.toList()
        }
        ClassPlanStore.saveDays(ctx, clean, lock)
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
            val slots = (days[di] ?: emptyList()).let { s ->
                if (s.size >= 3) s else (s + List(3 - s.size) { "" })
            }
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
                            while (next.size <= si) next.add("")
                            next[si] = picked
                            val map = days.toMutableMap().also { it[di] = next }
                            days = map
                            persist(lock = false, map = map)
                        },
                    )
                }
                if (!locked) {
                    TextButton(onClick = {
                        days = days.toMutableMap().also { it[di] = (slots + "").toMutableList() }
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
    var alarm by remember { mutableStateOf(SchoolAlarmStore.load(ctx)) }
    var settingsOpen by remember { mutableStateOf(false) }
    var confirmShift by remember { mutableStateOf(false) }
    var virtGear by remember { mutableStateOf(false) }
    var virtFrom by remember { mutableStateOf(today.toString()) }
    var virtTo by remember { mutableStateOf(today.toString()) }
    val current = ClassPlanStore.shiftOf(snap, today)

    fun flushAlarm(next: SchoolAlarmStore.Prefs = alarm) {
        SchoolAlarmStore.save(ctx, next)
        alarm = SchoolAlarmStore.load(ctx)
        snap = ClassPlanStore.load(ctx)
        ClassPlanStore.syncAlarms(ctx, reminders, snap, today)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(ClassPlanStore.captionOf(snap, today), fontFamily = DashboardFonts.greeting, fontSize = 20.sp, modifier = Modifier.clickable { confirmShift = true })
        Text("برای ویرایش شیفت، روی عنوان بالا بزن.", fontFamily = DashboardFonts.quote, style = MaterialTheme.typography.bodySmall)
        Text("شیفت هفتهٔ جاری: ${current.label}", fontFamily = DashboardFonts.quote, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("آلارم‌های صدادار", fontFamily = DashboardFonts.quote, fontWeight = FontWeight.Bold)
            TextButton(onClick = { settingsOpen = true }) { Text("چرخ‌دنده تنظیمات", fontFamily = DashboardFonts.quote) }
        }
        Text("شیفت صبح — سه کادر ساعت", fontFamily = DashboardFonts.quote, fontWeight = FontWeight.Bold)
        TimePick("آلارم بیداری", alarm.wakeMH, alarm.wakeMM) { h, m -> flushAlarm(alarm.copy(wakeMH = h, wakeMM = m)) }
        TimePick("حضور در سرویس", alarm.busMH, alarm.busMM) { h, m -> flushAlarm(alarm.copy(busMH = h, busMM = m)) }
        TimePick("حضور در مدرسه", alarm.schoolMH, alarm.schoolMM) { h, m -> flushAlarm(alarm.copy(schoolMH = h, schoolMM = m)) }
        Text("شیفت ظهر — سه کادر ساعت", fontFamily = DashboardFonts.quote, fontWeight = FontWeight.Bold)
        TimePick("آماده شدن ظهر", alarm.wakeNH, alarm.wakeNM) { h, m -> flushAlarm(alarm.copy(wakeNH = h, wakeNM = m)) }
        TimePick("حضور در سرویس", alarm.busNH, alarm.busNM) { h, m -> flushAlarm(alarm.copy(busNH = h, busNM = m)) }
        TimePick("حضور در مدرسه", alarm.schoolNH, alarm.schoolNM) { h, m -> flushAlarm(alarm.copy(schoolNH = h, schoolNM = m)) }
        Text("خواب — دعوت به خواب آرام", fontFamily = DashboardFonts.quote, fontWeight = FontWeight.Bold)
        TimePick("خواب شیفت صبح", alarm.sleepMH, alarm.sleepMM) { h, m -> flushAlarm(alarm.copy(sleepMH = h, sleepMM = m)) }
        TimePick("خواب شیفت ظهر", alarm.sleepNH, alarm.sleepNM) { h, m -> flushAlarm(alarm.copy(sleepNH = h, sleepNM = m)) }
        Text(
            "آلارم شیفت مخالف خاموش می‌شود. اگر دعوت خواب لمس نشود، یک‌بار دیگر بعد از ۵ دقیقه تکرار می‌شود.",
            fontFamily = DashboardFonts.quote,
            style = MaterialTheme.typography.bodySmall,
        )
        Text("کلاس مجازی", fontFamily = DashboardFonts.greeting, fontSize = 18.sp)
        Text("یک روز یا بازه را مجازی کن. روی داشبورد قرمز می‌شود و تیک کیف غیرفعال.", fontFamily = DashboardFonts.quote, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = virtFrom, onValueChange = { virtFrom = it }, label = { Text("از تاریخ (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = virtTo, onValueChange = { virtTo = it }, label = { Text("تا تاریخ") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            ClassPlanStore.setVirtualRange(ctx, virtFrom, virtTo, true)
            snap = ClassPlanStore.load(ctx)
        }, modifier = Modifier.fillMaxWidth()) { Text("تأیید کلاس مجازی", fontFamily = DashboardFonts.quote) }
        OutlinedButton(onClick = {
            ClassPlanStore.setVirtualRange(ctx, virtFrom, virtTo, false)
            snap = ClassPlanStore.load(ctx)
        }, modifier = Modifier.fillMaxWidth()) { Text("حذف این بازه از مجازی", fontFamily = DashboardFonts.quote) }
        TextButton(onClick = { virtGear = true }) { Text("چرخ‌دنده ساعت کلاس مجازی", fontFamily = DashboardFonts.quote) }
        val vdays = ClassPlanStore.virtualDays(ctx)
        if (vdays.isNotEmpty()) {
            Text("روزهای مجازی: ${vdays.sorted().take(12).joinToString("، ")}", fontFamily = DashboardFonts.quote, fontSize = 12.sp)
        }
    }
    if (settingsOpen) {
        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text("تنظیمات آلارم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("آهنگ: پیش‌فرض سیستم", fontFamily = DashboardFonts.quote)
                    Text("بلندی: ${toPersianDigits(alarm.volume.toString())}٪", fontFamily = DashboardFonts.quote)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(40, 60, 80, 100).forEach { v ->
                            FilterChip(selected = alarm.volume == v, onClick = { flushAlarm(alarm.copy(volume = v)) }, label = { Text(toPersianDigits(v.toString())) })
                        }
                    }
                    Text("تعداد تکرار", fontFamily = DashboardFonts.quote)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { n ->
                            FilterChip(selected = alarm.repeat == n, onClick = { flushAlarm(alarm.copy(repeat = n)) }, label = { Text(toPersianDigits(n.toString())) })
                        }
                    }
                    FilterChip(
                        selected = alarm.crescendo,
                        onClick = { flushAlarm(alarm.copy(crescendo = !alarm.crescendo)) },
                        label = { Text("صدای افزایشی", fontFamily = DashboardFonts.quote) },
                    )
                }
            },
            confirmButton = { TextButton(onClick = { settingsOpen = false }) { Text("بستن") } },
        )
    }
}

@Composable
private fun TimePick(label: String, hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            android.app.TimePickerDialog(ctx, { _, h, m -> onChange(h, m) }, hour, minute, true).show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "$label  ${toPersianDigits("%d:%02d".format(hour, minute))}",
            fontFamily = DashboardFonts.quote,
        )
    }
}
