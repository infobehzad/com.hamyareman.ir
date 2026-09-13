package com.hamyareman.ir.ui.screentime

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
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.ScreenTimeReport
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.LocalAppContainer

@Composable
fun ScreenTimeScreen(onBack: () -> Unit, onFocus: () -> Unit) {
    val container = LocalAppContainer.current
    val tracker = container.screenTime
    var minutes by remember { mutableFloatStateOf(tracker.goalMinutes().toFloat()) }
    var report by remember { mutableStateOf(tracker.report()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AppTopBar("زمان صفحه", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "سقف را خودت انتخاب می‌کنی، نه بابا و نه اپ. این عدد هرگز به سرور نمی‌رود.",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (!report.permissionGranted) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("دسترسی به آمار مصرف لازم است", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "اندروید این مجوز را فقط دستی می‌دهد: در صفحه‌ای که باز می‌شود، " +
                                "«روزهای من» را پیدا کن و «اجازه‌ی دسترسی به مصرف» را روشن کن. " +
                                "تا آن موقع هیچ عددی نشان نمی‌دهیم — حتی تقریبی.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton("بازکردن تنظیمات دسترسی") {
                            tracker.openUsageAccessSettings()
                            report = tracker.report()
                        }
                    }
                }
            } else {
                SummaryCard(report)
                TopAppsCard(report)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                toPersianDigits("${minutes.toInt()} دقیقه در روز"),
                style = MaterialTheme.typography.titleLarge,
            )
            Slider(
                value = minutes,
                onValueChange = { minutes = it },
                valueRange = 30f..240f,
                steps = 13,
            )
            PrimaryButton("ذخیره‌ی هدف") {
                tracker.setGoalMinutes(minutes.toInt())
                report = tracker.report()
            }
            PrimaryButton("حالت تمرکز") { onFocus() }
            PrimaryButton("به‌روزرسانی عدد امروز") { report = tracker.report() }
        }
    }
}

@Composable
private fun SummaryCard(report: ScreenTimeReport) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("امروز", style = MaterialTheme.typography.titleSmall)
            Text(
                toPersianDigits("${report.today.totalMinutes} دقیقه"),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                toPersianDigits("میانگین هفت روز گذشته: ${report.dailyAverageMinutes} دقیقه"),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Text("هفت روز گذشته", style = MaterialTheme.typography.labelMedium)
            report.week.reversed().forEach { day ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(JalaliDate.formatFa(day.dateIso), style = MaterialTheme.typography.bodySmall)
                    Text(toPersianDigits("${day.totalMinutes} دقیقه"), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TopAppsCard(report: ScreenTimeReport) {
    if (report.today.byApp.isEmpty()) {
        Card(Modifier.fillMaxWidth()) {
            Text(
                "امروز هنوز داده‌ای ثبت نشده. اگر تازه مجوز را داده‌ای، چند دقیقه صبر کن و دوباره به‌روزرسانی بزن.",
                Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }
    val max = report.today.byApp.maxOf { it.minutes }.coerceAtLeast(1L)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("پرمصرف‌ترین اپ‌های امروز", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            report.today.byApp.take(6).forEach { app ->
                Text(app.label, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { (app.minutes.toFloat() / max).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    toPersianDigits("${app.minutes} دقیقه"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun FocusModeScreen(onBack: () -> Unit) {
    val tracker = LocalAppContainer.current.screenTime
    var on by remember { mutableStateOf(tracker.isFocusOn()) }
    var minutes by remember { mutableStateOf(tracker.focusMinutes()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AppTopBar("حالت تمرکز", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (on) {
                    "حالت تمرکز روشن است. هر وقت خواستی خاموشش کن."
                } else {
                    "خاموش است. فقط خودت روشنش می‌کنی؛ هیچ‌کس از بیرون فعالش نمی‌کند."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        toPersianDigits("${minutes} دقیقه"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        if (on) "از وقتی که روشنش کردی" else "جلسه‌ی فعالی نیست",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            PrimaryButton(
                if (on) "خاموش کردن" else "روشن کردن",
            ) {
                if (on) tracker.stopFocus() else tracker.startFocus()
                on = tracker.isFocusOn()
                minutes = tracker.focusMinutes()
            }
            PrimaryButton("به‌روزرسانی زمان") { minutes = tracker.focusMinutes() }
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    "صادقانه: این حالت گوشی را قفل نمی‌کند و نمی‌تواند بکند (بدون دسترسی مدیر دستگاه). " +
                        "فقط یک تایمر و یک یادآوری ملایم است که کمکت می‌کند خودت تصمیم بگیری.",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
