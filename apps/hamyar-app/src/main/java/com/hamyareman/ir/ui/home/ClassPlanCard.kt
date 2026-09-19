package com.hamyareman.ir.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    val isoT = today.toString()
    val isoN = tomorrow.toString()
    val bag = ClassPlanStore.prepBag(ctx, isoN)
    val hw = ClassPlanStore.prepHw(ctx, isoN)
    val alarmOn = ClassPlanStore.alarmIsSet(reminders, snap, today)
    val (ah, am) = ClassPlanStore.wakeHourMinute(snap, shift)
    val alarmLabel = toPersianDigits("%d:%02d".format(ah, am))
    val sleep = toPersianDigits(ClassPlanStore.sleepText(snap, shift))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "برنامه کلاسی مدرسه",
                fontFamily = DashboardFonts.greeting,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenPlan),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                                fontFamily = DashboardFonts.quote,
                                fontSize = 12.sp,
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
            Column(Modifier.fillMaxWidth().clickable(onClick = onOpenPrep)) {
                PrepTick("کیف مدرسه آماده است", bag)
                PrepTick("تکالیف انجام شده", hw)
                PrepTick("آلارم برای ساعت $alarmLabel تنظیم شده", alarmOn)
                PrepTick("ساعت خوابت $sleep باشد", true)
            }
        }
    }
}

@Composable
private fun PrepTick(label: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = {}, enabled = false)
        Text(label, fontFamily = DashboardFonts.quote, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
