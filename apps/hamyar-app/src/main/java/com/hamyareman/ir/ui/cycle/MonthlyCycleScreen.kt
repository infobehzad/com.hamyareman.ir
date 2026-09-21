package com.hamyareman.ir.ui.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.ui.profile.StudentProfileState
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * «چرخه ی ماهانه» — تقویمِ شمسی برای ثبت و دیدنِ دورانِ پریود، راهنمای همان روز،
 * تمرین‌های کم‌کردنِ درد و سینکِ سرور.
 *
 * **فقط برای دخترها:** اگر جنسیتِ پروفایل «پسر» باشد، هم کارتش در «سلامتی» پنهان
 * است و هم اگر کسی مستقیم به این مسیر بیاید چیزی نمی‌بیند.
 */
@Composable
fun MonthlyCycleScreen(
    onBack: () -> Unit,
    onMood: () -> Unit,
    onMind: () -> Unit,
    onMoves: () -> Unit,
) {
    if (StudentProfileState.gender == "boy") {
        // محافظِ دوم (کارت هم در هاب سلامتی پنهان است): هیچ داده‌ای نمایش داده نمی‌شود.
        Column(Modifier.fillMaxSize()) {
            AppTopBar("سلامتی", onBack)
            Text(
                "این بخش برای دخترهاست.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(MonthlyCycle.load(ctx)) }
    var syncNote by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    // شناسهٔ کاربر suspend است؛ پس در افکت خوانده می‌شود و سینکِ بی‌صدای ورود هم
    // همین‌جا انجام می‌گیرد (تا در اولین فرصت، داده‌ی سرور بیاید).
    var uid by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        uid = runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isNotBlank()) {
            MonthlyCycle.sync(ctx, container.tables, uid)
            state = MonthlyCycle.load(ctx)
        }
    }

    val today = remember { LocalDate.now(JalaliDate.TEHRAN).toString() }
    // تبدیلِ شمسیِ «امروز» قطعی است؛ نگهبانِ null فقط برای امنیتِ نوع است.
    val todayJalali = remember { JalaliDate.toJalali(today) ?: JalaliDate.Jalali(1400, 1, 1) }
    var viewYear by remember { mutableStateOf(todayJalali.year) }
    var viewMonth by remember { mutableStateOf(todayJalali.month) }
    var picked by remember { mutableStateOf(today) }

    fun update(new: MonthlyCycle.State) {
        state = new
        MonthlyCycle.saveLocal(ctx, new)
    }

    fun syncNow() {
        if (syncing) return
        syncing = true
        syncNote = null
        scope.launch {
            MonthlyCycle.sync(ctx, container.tables, uid)
            state = MonthlyCycle.load(ctx)
            syncNote = if (uid.isBlank()) {
                "برای سینک، اول وارد حساب شو."
            } else {
                "همگام شد. آخرین تغییرِ محلی: " + (
                    MonthlyCycle.localAt(ctx).takeIf { it > 0 }?.let { JalaliDate.clockFa(it) } ?: "—"
                    )
            }
            syncing = false
        }
    }

    val (phaseTitle, phaseBody) = MonthlyCycle.todayCard(state, today)
    val daysToNext = MonthlyCycle.daysToNext(state, today)
    val nextStart = MonthlyCycle.nextStart(state)

    Column(Modifier.fillMaxSize()) {
        AppTopBar("چرخه ی ماهانه", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ---- امروز ----
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("امروز — $phaseTitle", style = MaterialTheme.typography.titleMedium)
                    Text(phaseBody, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { update(MonthlyCycle.toggleDay(state, today)) }) {
                            Text(if (today in state.periodDays) "امروز پریودم — برداشته شود" else "امروز پریودم")
                        }
                    }
                    TextButton(onClick = { update(MonthlyCycle.markStart(state, today)) }) {
                        Text("دوره از امروز شروع شد (۵ روزِ بعد هم ثبت می‌شود)")
                    }
                }
            }

            // ---- تقویم شمسی ----
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            if (viewMonth == 1) { viewMonth = 12; viewYear -= 1 } else viewMonth -= 1
                        }) { Text("‹ ماه قبل") }
                        Text(
                            JalaliDate.monthName(viewMonth) + " " + toPersianDigits(viewYear.toString()),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            if (viewMonth == 12) { viewMonth = 1; viewYear += 1 } else viewMonth += 1
                        }) { Text("ماه بعد ›") }
                    }

                    Row(Modifier.fillMaxWidth()) {
                        listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { d ->
                            Text(
                                d,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    val firstIso = JalaliDate.toGregorianIso(JalaliDate.Jalali(viewYear, viewMonth, 1))
                    val firstDow = firstIso?.let {
                        runCatching { (LocalDate.parse(it).dayOfWeek.value + 1) % 7 }.getOrDefault(0)
                    } ?: 0
                    val daysCount = JalaliDate.daysInMonth(viewYear, viewMonth)
                    val cells = firstDow + daysCount
                    val rows = (cells + 6) / 7

                    for (r in 0 until rows) {
                        Row(Modifier.fillMaxWidth()) {
                            for (c in 0 until 7) {
                                val dayIndex = r * 7 + c - firstDow + 1
                                if (dayIndex < 1 || dayIndex > daysCount) {
                                    Box(Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val iso = JalaliDate.toGregorianIso(JalaliDate.Jalali(viewYear, viewMonth, dayIndex))
                                    DayCell(
                                        day = dayIndex,
                                        iso = iso.orEmpty(),
                                        state = state,
                                        today = today,
                                        selected = iso == picked,
                                        modifier = Modifier.weight(1f),
                                        onClick = { picked = iso.orEmpty() },
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        LegendDot(MaterialTheme.colorScheme.primary, "ثبت‌شده")
                        LegendDot(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), "پیش‌بینی")
                        LegendDot(MaterialTheme.colorScheme.outline, "امروز")
                    }

                    Text(
                        "روزِ انتخابی: " + (JalaliDate.toJalali(picked)?.faLong ?: "—"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { update(MonthlyCycle.toggleDay(state, picked)) }) {
                            Text(if (picked in state.periodDays) "برداشتنِ علامت" else "این روز پریود بود")
                        }
                        OutlinedButton(onClick = { update(MonthlyCycle.markStart(state, picked)) }) {
                            Text("شروع دوره از این روز")
                        }
                    }
                }
            }

            // ---- خلاصه ----
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("وضعیتِ چرخه", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "آخرین شروع: " + (
                            state.lastStart.takeIf { it.length == 10 }
                                ?.let { JalaliDate.toJalali(it)?.faLong } ?: "هنوز ثبت نشده"
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "شروعِ بعدی (تخمینی): " + (
                            nextStart?.let { JalaliDate.toJalali(it)?.faLong } ?: "—"
                            ) + if (daysToNext != null) {
                            "  •  " + when {
                                daysToNext > 0 -> toPersianDigits(daysToNext.toString()) + " روز مانده"
                                daysToNext == 0 -> "همین امروز"
                                else -> toPersianDigits((-daysToNext).toString()) + " روز گذشته"
                            }
                        } else "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("طولِ چرخه: " + toPersianDigits(state.cycleLength.toString()) + " روز", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { update(state.copy(cycleLength = (state.cycleLength - 1).coerceAtLeast(21))) }) { Text("−") }
                        TextButton(onClick = { update(state.copy(cycleLength = (state.cycleLength + 1).coerceAtMost(35))) }) { Text("+") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("طولِ پریود: " + toPersianDigits(state.periodLength.toString()) + " روز", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { update(state.copy(periodLength = (state.periodLength - 1).coerceAtLeast(2))) }) { Text("−") }
                        TextButton(onClick = { update(state.copy(periodLength = (state.periodLength + 1).coerceAtMost(10))) }) { Text("+") }
                    }
                    Text(
                        "این عددها تخمینی‌اند و مالِ خودت؛ اگر بی‌نظمی دیدی، برای بررسی با پزشک حرف بزن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---- تمرین‌های کم‌کردن درد ----
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("کم‌کردنِ دردِ روزهای اول", style = MaterialTheme.typography.titleMedium)
                    MonthlyCycle.painExercises.forEach { ex ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(ex.emoji, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ex.title + "  ·  " + ex.duration,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    ex.how,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onMoves) { Text("حرکاتِ ملایم و یوگا") }
                        OutlinedButton(onClick = onMind) { Text("ذهن‌آگاهی") }
                    }
                    Text(
                        "اگر درد طوری است که نمی‌توانی مدرسه بروی یا با مسکنِ معمولی بهتر نمی‌شود، " +
                            "به مامان/بابا بگو و با پزشک مشورت کن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---- حال و ذهن ----
            SectionCard("حالِ امروزم چطوره؟", "با یک ایموجی ثبتش کن — اختیاریه.") { onMood() }
            SectionCard("ذهن‌آگاهی", "تمرین‌های کوتاه حضور") { onMind() }

            // ---- سینک ----
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ذخیره و سینک", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "این تقویم روی گوشیِ خودت ذخیره می‌شود و با حسابِ خودت در سرور هم همگام می‌شود " +
                            "(سطرِ خصوصیِ خودت؛ هیچ‌کس دیگری نمی‌بیند).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "آخرین تغییر محلی: " + (
                            MonthlyCycle.localAt(ctx).takeIf { it > 0 }?.let { JalaliDate.clockFa(it) } ?: "—"
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    syncNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !syncing, onClick = { syncNow() }) {
                            Text(if (syncing) "در حال سینک…" else "همگام‌سازی")
                        }
                        OutlinedButton(
                            enabled = !syncing,
                            onClick = {
                                scope.launch {
                                    val r = MonthlyCycle.pushNow(ctx, container.tables, uid)
                                    syncNote = if (r is AppResult.Err) r.error.userMessage
                                    else "روی سرور ذخیره شد."
                                }
                            },
                        ) { Text("ذخیره در سرور") }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

/** یک خانهٔ تقویم: رنگِ پریود، حاشیهٔ پیش‌بینی، قابِ روزِ انتخابی. */
@Composable
private fun DayCell(
    day: Int,
    iso: String,
    state: MonthlyCycle.State,
    today: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isPeriod = iso.isNotBlank() && iso in state.periodDays
    val next = MonthlyCycle.nextStart(state)
    val predicted = next != null && iso.isNotBlank() && runCatching {
        val d = LocalDate.parse(iso).toEpochDay() - LocalDate.parse(next).toEpochDay()
        d in 0 until state.periodLength.toLong()
    }.getOrDefault(false)

    val bg = when {
        isPeriod -> MaterialTheme.colorScheme.primary
        predicted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    val fg = if (isPeriod) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(10.dp))
                else Modifier,
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                toPersianDigits(day.toString()),
                style = MaterialTheme.typography.bodyMedium,
                color = fg,
                fontWeight = if (iso == today) FontWeight.Bold else FontWeight.Normal,
            )
            if (iso == today) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
