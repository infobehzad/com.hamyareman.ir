package com.hamyareman.ir.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.study.ClassPlanStore
import java.time.LocalDate

private val LessonColors = listOf(
    Color(0xFF0F766E),
    Color(0xFF4338CA),
    Color(0xFFB45309),
    Color(0xFFBE185D),
    Color(0xFF0369A1),
)

@Composable
fun ClassPlanCard(
    onOpenPlan: () -> Unit,
    onOpenPrep: () -> Unit,
    onOpenAlarm: () -> Unit,
) {
    val ctx = LocalContext.current
    val reminders = LocalAppContainer.current.reminders
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val today = remember(tick) { LocalDate.now(JalaliDate.TEHRAN) }
    val tomorrow = today.plusDays(1)
    val snap = remember(tick) { ClassPlanStore.load(ctx) }
    LaunchedEffect(tick, snap.cycleWeeks, snap.anchorIso, snap.fixedEvening, snap.morningHour, snap.noonHour) {
        ClassPlanStore.syncAlarms(ctx, reminders, snap, today)
    }
    val j = JalaliDate.toJalali(today.toString())
    val dayName = JalaliDate.weekDayFa(today.toString())
    val dateFa = j?.let { toPersianDigits("${it.day} ${JalaliDate.monthName(it.month)}") } ?: ""
    val shift = ClassPlanStore.shiftOf(snap, today)
    val holiday = ClassPlanStore.isSchoolHoliday(snap, today)
    val showDate = generateSequence(tomorrow) { it.plusDays(1) }
        .take(8)
        .firstOrNull { !ClassPlanStore.isSchoolHoliday(snap, it) }
        ?: tomorrow
    val tomorrowLessons = ClassPlanStore.lessonsFor(snap, showDate).ifEmpty { listOf("—", "—", "—") }
    val boxes = (tomorrowLessons + listOf("—", "—", "—")).take(3)
    val isoN = showDate.toString()
    val isoT = today.toString()
    var bag by remember(tick, isoN) { mutableStateOf(ClassPlanStore.prepBag(ctx, isoN)) }
    var hw by remember(tick, isoN) { mutableStateOf(ClassPlanStore.prepHw(ctx, isoN)) }
    val alarmOn = ClassPlanStore.alarmIsSet(reminders, snap, today)
    val alarmPrefs = remember(tick) { com.hamyareman.ir.ui.study.SchoolAlarmStore.load(ctx) }
    val (ah, am) = if (shift == com.hamyareman.ir.ui.study.Shift.MORNING) alarmPrefs.wakeMH to alarmPrefs.wakeMM else alarmPrefs.wakeNH to alarmPrefs.wakeNM
    val alarmLabel = toPersianDigits("%d:%02d".format(ah, am))
    val sleep = toPersianDigits(
        if (shift == com.hamyareman.ir.ui.study.Shift.MORNING) "%d:%02d".format(alarmPrefs.sleepMH, alarmPrefs.sleepMM)
        else "%d:%02d".format(alarmPrefs.sleepNH, alarmPrefs.sleepNM),
    )
    val virtual = ClassPlanStore.isVirtual(ctx, isoN)
    var exam by remember(tick, isoN) { mutableStateOf(ClassPlanStore.examOf(ctx, isoN)) }
    var examOpen by remember { mutableStateOf(false) }
    var report by remember(tick, isoN, exam) { mutableStateOf(ClassPlanStore.reportOf(ctx, isoN)) }
    val examOptions = ClassPlanStore.lessonsFor(snap, showDate).filter { it.isNotBlank() && it != "—" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "برنامه کلاسی مدرسه",
                fontFamily = DashboardFonts.aria,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenPlan),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayName, fontFamily = DashboardFonts.quote, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(dateFa, fontFamily = DashboardFonts.quote, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(shift.label, fontFamily = DashboardFonts.quote, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                if (holiday) {
                    Box(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFEF3C7)).padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            ClassPlanStore.holidayRoutine(today),
                            fontFamily = DashboardFonts.quote,
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    boxes.forEachIndexed { i, name ->
                        Box(
                            Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(14.dp))
                                .background(LessonColors[i % LessonColors.size]),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                name,
                                color = Color.White,
                                fontFamily = DashboardFonts.lalezar,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                }
            }
            if (virtual) {
                Text(
                    "فردا کلاس مجازی است — کیف مدرسه لازم نیست.",
                    color = Color(0xFFB91C1C),
                    fontFamily = DashboardFonts.quote,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PrepTick(
                        label = "کیف مدرسه آماده است",
                        checked = bag && !virtual,
                        enabled = !virtual,
                        modifier = Modifier.weight(1f),
                    ) {
                        bag = it
                        ClassPlanStore.setPrepBag(ctx, isoN, it)
                    }
                    PrepTick(
                        label = "تکالیف انجام شده",
                        checked = hw,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        hw = it
                        ClassPlanStore.setPrepHw(ctx, isoN, it)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PrepTick(
                        label = "آلارم برای ساعت $alarmLabel تنظیم شده",
                        checked = alarmOn,
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onOpenAlarm, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = "تنظیم آلارم")
                    }
                    PrepTick(
                        label = "ساعت خوابت $sleep باشد",
                        checked = true,
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Box(Modifier.fillMaxWidth()) {
                PrepTick(
                    label = if (exam.isBlank()) "فردا امتحان داری؟" else "فردا امتحان $exam",
                    checked = exam.isNotBlank(),
                    enabled = true,
                    modifier = Modifier.fillMaxWidth().clickable { examOpen = true },
                ) { examOpen = true }
                DropdownMenu(expanded = examOpen, onDismissRequest = { examOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("بدون امتحان", fontFamily = DashboardFonts.quote) },
                        onClick = {
                            exam = ""
                            ClassPlanStore.setExam(ctx, isoN, "")
                            examOpen = false
                        },
                    )
                    examOptions.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text(sub, fontFamily = DashboardFonts.lalezar) },
                            onClick = {
                                exam = sub
                                ClassPlanStore.setExam(ctx, isoN, sub)
                                examOpen = false
                            },
                        )
                    }
                }
            }
            if (exam.isNotBlank()) {
                OutlinedTextField(
                    value = report,
                    onValueChange = {
                        report = it
                        ClassPlanStore.saveExamReport(ctx, isoN, exam, it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("گزارش نتیجه امتحان", fontFamily = DashboardFonts.lalezar) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = DashboardFonts.lalezar, fontSize = 16.sp),
                )
            }
            Text(
                "آماده‌سازی کامل‌تر",
                fontFamily = DashboardFonts.quote,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onOpenPrep),
            )
        }
    }
}

@Composable
private fun PrepTick(
    label: String,
    checked: Boolean,
    enabled: Boolean = false,
    modifier: Modifier = Modifier,
    onChecked: ((Boolean) -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Checkbox(
            checked = checked,
            onCheckedChange = { if (enabled && onChecked != null) onChecked(it) },
            enabled = enabled,
        )
        Text(label, fontFamily = DashboardFonts.quote, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
