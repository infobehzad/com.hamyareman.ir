package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.home.DashboardFonts
import java.time.LocalDate

@Composable
fun TomorrowPrepScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val reminders = LocalAppContainer.current.reminders
    val today = LocalDate.now(JalaliDate.TEHRAN)
    val tomorrow = today.plusDays(1)
    val snap = remember { ClassPlanStore.load(ctx) }
    remember {
        ClassPlanStore.syncAlarms(ctx, reminders, snap, today)
        true
    }
    val isoN = tomorrow.toString()
    val isoT = today.toString()
    var bag by remember { mutableStateOf(ClassPlanStore.prepBag(ctx, isoN)) }
    var hw by remember { mutableStateOf(ClassPlanStore.prepHw(ctx, isoN)) }
    var exam by remember { mutableStateOf(ClassPlanStore.examOf(ctx, isoN)) }
    var report by remember { mutableStateOf(ClassPlanStore.reportOf(ctx, isoT)) }
    var examOpen by remember { mutableStateOf(false) }
    val shift = ClassPlanStore.shiftOf(snap, today)
    val (ah, am) = ClassPlanStore.wakeHourMinute(snap, shift)
    val sleep = ClassPlanStore.sleepText(snap, shift)
    val alarmOn = ClassPlanStore.alarmIsSet(reminders, snap, today)
    val lessons = ClassPlanStore.lessonsFor(snap, tomorrow)
    val tomorrowFa = JalaliDate.formatFaLong(isoN)

    Column(Modifier.fillMaxSize()) {
        AppTopBar("آماده‌سازی فردا", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("فردا $tomorrowFa · ${ClassPlanStore.captionOf(snap, tomorrow)}", fontFamily = DashboardFonts.quote)
            if (lessons.isNotEmpty()) {
                Text("درس‌های فردا: ${lessons.joinToString("، ")}", fontFamily = DashboardFonts.quote)
            } else {
                Text("برای فردا درسی در برنامهٔ هفتگی نیست.", fontFamily = DashboardFonts.quote, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = bag, onCheckedChange = {
                    bag = it; ClassPlanStore.setPrepBag(ctx, isoN, it)
                })
                Text("کیف مدرسه آماده است", fontFamily = DashboardFonts.quote)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hw, onCheckedChange = {
                    hw = it; ClassPlanStore.setPrepHw(ctx, isoN, it)
                })
                Text("تکالیف انجام شده", fontFamily = DashboardFonts.quote)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = alarmOn, onCheckedChange = {}, enabled = false)
                Text("آلارم برای ساعت ${toPersianDigits("%d:%02d".format(ah, am))} تنظیم شده", fontFamily = DashboardFonts.quote)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = true, onCheckedChange = null, enabled = false)
                Text("ساعت خوابت ${toPersianDigits(sleep)} باشد", fontFamily = DashboardFonts.quote)
            }

            Text("امتحان فردا (اختیاری)", fontFamily = DashboardFonts.greeting, fontSize = 18.sp)
            Text("اگر امتحان داری، از درس‌های همان روز انتخاب کن.", fontFamily = DashboardFonts.quote, style = MaterialTheme.typography.bodySmall)
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { examOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(exam.ifBlank { "بدون امتحان / انتخاب درس" }, fontFamily = DashboardFonts.quote)
                }
                DropdownMenu(expanded = examOpen, onDismissRequest = { examOpen = false }) {
                    DropdownMenuItem(text = { Text("بدون امتحان") }, onClick = {
                        exam = ""; ClassPlanStore.setExam(ctx, isoN, ""); examOpen = false
                    })
                    lessons.forEach { sub ->
                        DropdownMenuItem(text = { Text(sub, fontFamily = DashboardFonts.quote) }, onClick = {
                            exam = sub; ClassPlanStore.setExam(ctx, isoN, sub); examOpen = false
                        })
                    }
                }
            }

            Text("گزارش عملکرد امروز (بعد از رسیدن به خانه)", fontFamily = DashboardFonts.greeting, fontSize = 18.sp)
            OutlinedTextField(
                value = report,
                onValueChange = { report = it; ClassPlanStore.setReport(ctx, isoT, it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("چه کارهایی انجام شد؟") },
            )
        }
    }
}
