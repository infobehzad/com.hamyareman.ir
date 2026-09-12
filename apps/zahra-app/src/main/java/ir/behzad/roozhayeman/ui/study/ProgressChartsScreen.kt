package ir.behzad.roozhayeman.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.behzad.platform.core.common.JalaliDate
import ir.behzad.platform.core.common.ScreenTimeReport
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.core.designsystem.PrimaryButton
import ir.behzad.platform.core.designsystem.SectionCard
import ir.behzad.roozhayeman.LocalAppContainer
import ir.behzad.roozhayeman.ui.art.readGallery
import ir.behzad.roozhayeman.ui.exercise.exerciseMinutesOn
import ir.behzad.roozhayeman.ui.exercise.exerciseSessionsOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** آمار یک روز از داده‌ی واقعیِ روی دستگاه (نه عدد ساختگی). */
private data class DayStat(
    val dayIso: String,
    val water: Int,
    val exerciseMinutes: Int,
    val exerciseSessions: Int,
    val artCount: Int,
    val quizAttempts: Int,
    val quizCorrect: Int,
    val quizTotal: Int,
)

// مصوب: آب/ورزش/نقاشی به «پیشرفت سلامتی» منتقل شد؛ این نمودار فقط دروس است.
private enum class Metric(val label: String, val unit: String) {
    QUIZ("آزمون", "درصد"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressChartsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val store = container.store
    var metric by remember { mutableStateOf(Metric.QUIZ) }

    val gallery = remember { readGallery(store) }
    val attempts = remember { readQuizAttempts(store) }

    val days = remember(gallery, attempts) {
        val today = LocalDate.now()
        (6 downTo 0).map { offset ->
            val iso = today.minusDays(offset.toLong()).toString()
            val dayAttempts = attempts.filter { it.dayIso == iso }
            DayStat(
                dayIso = iso,
                water = store.getInt("consumed_$iso"),
                exerciseMinutes = exerciseMinutesOn(store, iso),
                exerciseSessions = exerciseSessionsOn(store, iso),
                artCount = gallery.count { it.dateIso == iso },
                quizAttempts = dayAttempts.size,
                quizCorrect = dayAttempts.sumOf { it.score },
                quizTotal = dayAttempts.sumOf { it.total },
            )
        }
    }

    val values = days.map { stat ->
        when (metric) {
            Metric.QUIZ -> if (stat.quizTotal == 0) 0 else (stat.quizCorrect * 100) / stat.quizTotal
        }
    }
    // گزارش زمان صفحه یک فراخوانی سیستمی است؛ روی IO می‌رود تا UI گیر نکند.
    var screenTime by remember { mutableStateOf<ScreenTimeReport?>(null) }
    LaunchedEffect(Unit) {
        screenTime = withContext(Dispatchers.IO) { container.screenTime.report() }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("نمودار پیشرفت مدرسه", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("۷ روز گذشته — فقط تشویق، بدون مقایسه با کسی", style = MaterialTheme.typography.titleMedium)

            BarChart(values = values, labels = days.map { JalaliDate.toJalali(it.dayIso)?.day?.toString()?.let(JalaliDate::toPersianDigits) ?: "" })

            val summary = when (metric) {
                Metric.QUIZ -> {
                    val total = days.sumOf { it.quizTotal }
                    val correct = days.sumOf { it.quizCorrect }
                    if (total == 0) "این هفته آزمونی ندادی — هر وقت خواستی شروع کن"
                    else "$correct از $total درست (${(correct * 100) / total}٪) در ${days.sumOf { it.quizAttempts }} آزمون"
                }
            }
            SectionCard(title = metric.label, body = "$summary · واحد: ${metric.unit}") { }

            val report = screenTime
            SectionCard(
                title = "زمان صفحه (این هفته)",
                body = when {
                    report == null -> "در حال خواندن از سیستم…"
                    report.permissionGranted ->
                        "مجموع ${report.weekTotalMinutes} دقیقه · میانگین روزانه ${report.dailyAverageMinutes} دقیقه"

                    else -> "برای دیدن عدد واقعی، دسترسی «Usage Access» لازم است. بدون آن هیچ عددی نمی‌سازیم."
                },
            ) { }
            if (report != null && !report.permissionGranted) {
                PrimaryButton("رفتن به تنظیمات دسترسی") { container.screenTime.openUsageAccessSettings() }
            }

            SectionCard(
                title = "استریک‌ها",
                body = "مرور فاصله‌دار: ${dueItems(store).size} سؤال در انتظار · آمار آب/ورزش/نقاشی در «پیشرفت سلامتی»",
            ) { }
            Text(
                "اگر یک روز رد شد، مهم نیست؛ فردا از نو. این نمودار برای مچ‌گرفتن نیست.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** نمودار میله‌ای ساده با Compose — بدون وابستگی نموداری. */
@Composable
private fun BarChart(values: List<Int>, labels: List<String>) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .height(160.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        values.forEachIndexed { index, value ->
            val barHeight = ((110f * value) / max).coerceAtLeast(if (value > 0) 6f else 2f)
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(JalaliDate.toPersianDigits(value.toString()), style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(barHeight.dp)
                        .background(
                            color = if (value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                        ),
                )
                Text(
                    labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
