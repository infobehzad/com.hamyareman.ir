package com.hamyareman.ir.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.StudyPack
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.art.readGallery
import com.hamyareman.ir.ui.exercise.exerciseMinutesOn
import com.hamyareman.ir.ui.exercise.exerciseSessionsOn
import java.time.LocalDate
import java.util.Locale

/** وقتی مطالعه قفل است — هیچ ورودی دیگری به فلش‌کارت/آزمون راه ندارد. */
@Composable
fun LockedStudyScreen(onBack: () -> Unit) {
    AppTopBar(title = "قفل است 🔒", onBack = onBack)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("اول تدریس، بعد تمرین 🌱", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text(
            "«مطالعه و آزمون» بعد از اتمام دوره‌ی اول تدریسِ همان درس باز می‌شود.\nصوت یا ویدیوی تدریس را تا انتها ببین؛ خودکار باز می‌شود.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("متوجه شدم", onClick = onBack)
    }
}

private fun mmss(sec: Int): String {
    val s = sec.coerceAtLeast(0)
    return toPersianDigits(String.format(Locale.US, "%d:%02d", s / 60, s % 60))
}

private fun fa(n: Int) = toPersianDigits(n.toString())

/**
 * «پیشرفت سلامتی» — طبق بازخورد مصوب، همه‌ی سنجه‌های سلامتی و آمارگیری یادگیری
 * این‌جا جمع می‌شود؛ کاملاً محاسباتی/رویدادی، ثبت خودکار و «غیرقابل ویرایش»:
 *  ۱) آب / ورزش / نقاشی (۷ روز اخیر) — از ثبت‌های واقعی خود اپ؛
 *  ۲) تدریس‌ها: نشست‌ها، پیشرفت و زمانِ دوره‌ی اول (دیده از کل / باقی‌مانده)،
 *     تعداد پرش‌های بیش از ۳ ثانیه در پلیر، اتمام دوره (حتی در چند نشست)؛
 *  ۳) فلش‌کارت‌ها: یادگرفته / باقی‌مانده / مرور امروز؛
 *  ۴) آزمون‌ها: تعداد، میانگین و بهترین درصد.
 */
@Composable
fun HealthProgressScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val ctx = LocalContext.current
    val store = container.store
    val today = remember { JalaliDate.todayIso() }

    // ارسال صف سینک ابری آمار (اگر چیزی مانده باشد) — بی‌سروصدا.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { TeachCloud.push(container.sync) }
    }
    AppTopBar(title = "پیشرفت سلامتی 📊", onBack = onBack)
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ---------------------------------------------- ۱) آب / ورزش / نقاشی
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("سلامتی روزانه — ۷ روز اخیر", style = MaterialTheme.typography.titleMedium)
                val gallery = remember { readGallery(store) }
                val todayDate = remember { LocalDate.now() }
                (6 downTo 0).forEach { offset ->
                    val iso = todayDate.minusDays(offset.toLong()).toString()
                    val water = store.getInt("consumed_$iso")
                    val exMin = exerciseMinutesOn(store, iso)
                    val art = gallery.count { it.dateIso == iso }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            JalaliDate.weekDayFa(iso),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(64.dp),
                        )
                        StatBar("💧", water, 8, Modifier.weight(1f))
                        StatBar("🏃‍♀️", exMin, 45, Modifier.weight(1f))
                        StatBar("🎨", art, 5, Modifier.weight(1f))
                    }
                }
                Text(
                    "هدف: ۸ لیوان آب · ۴۵ دقیقه ورزش · تمرین هنری روزانه",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---------------------------------------------- ۲) تدریس‌ها
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تدریس‌ها — دوره‌ی اول", style = MaterialTheme.typography.titleMedium)
                val rows = remember {
                    BookModuleRegistry.modules.filter { com.hamyareman.ir.ui.profile.GradeGate.canSeeBook(it.bookCode) }.flatMap { m -> m.packs.map { it to TeachStats.snap(ctx, it.packId) } }
                        .filter { it.second.sessions > 0 || it.second.watchedSec > 0 }
                }
                if (rows.isEmpty()) {
                    Text(
                        "هنوز تدریسی شروع نشده. از «مدرسه» یک درس را باز کن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                rows.forEach { (pack, snap) ->
                    val expectedMedia = teachTracksOf(pack).size + (if (StudyMedia.videoIds(pack.packId).isNotEmpty()) 1 else 0)
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pack.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(
                                if (snap.done) "✓ دوره‌ی اول تمام شد"
                                else "در جریان (${fa(snap.doneMedia)}/${fa(expectedMedia)} رسانه کامل)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (snap.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { snap.passPct / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        )
                        Text(
                            buildString {
                                append("نشست‌ها: ${fa(snap.sessions)}")
                                if (snap.lastSessionAtMs > 0) append(" (آخرین: ${JalaliDate.formatFa(snap.lastSessionAtMs)})")
                                append(" · دیده‌شده: ${mmss(snap.watchedSec)} از ${mmss(snap.totalSec)}")
                                append(" · باقی: ${mmss(snap.remainSec)}")
                                if (snap.jumps > 0) append(" · پرش >۳ث: ${fa(snap.jumps)}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ---------------------------------------------- ۳) فلش‌کارت‌ها
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("فلش‌کارت‌ها — مرور فاصله‌دار", style = MaterialTheme.typography.titleMedium)
                val cardRows = remember(today) {
                    BookModuleRegistry.modules.filter { com.hamyareman.ir.ui.profile.GradeGate.canSeeBook(it.bookCode) }.flatMap { m -> m.packs }.mapNotNull { pack ->
                        val states = runCatching { container.studyProgress.cards(pack.packId) }.getOrDefault(emptyMap())
                        if (states.isEmpty()) null else Triple(pack, states, pack.flashcards.size)
                    }
                }
                if (cardRows.isEmpty()) {
                    Text(
                        "هنوز مروری ثبت نشده.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                cardRows.forEach { (pack, states, total) ->
                    val learned = states.values.count { it.reps > 0 }
                    val due = runCatching { container.studyProgress.dueCards(pack, today).size }.getOrDefault(0)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pack.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(
                            "یادگرفته ${fa(learned)} از ${fa(total)} · باقی ${fa((total - learned).coerceAtLeast(0))}" +
                                if (due > 0) " · امروز ${fa(due)}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ---------------------------------------------- ۴) آزمون‌ها
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("آزمون‌ها — تحلیل نتایج", style = MaterialTheme.typography.titleMedium)
                val attemptRows = remember {
                    BookModuleRegistry.modules.filter { com.hamyareman.ir.ui.profile.GradeGate.canSeeBook(it.bookCode) }.flatMap { it.packs }.mapNotNull { pack ->
                        val at = runCatching { container.studyProgress.attempts(pack.packId) }.getOrDefault(emptyList())
                        if (at.isEmpty()) null else pack to at
                    }
                }
                if (attemptRows.isEmpty()) {
                    Text(
                        "هنوز آزمونی داده نشده. آزمون‌ها بعد از اتمام تدریس فعال می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                attemptRows.forEach { (pack, at) ->
                    val avg = at.map { it.scorePct }.average().toInt()
                    val best = at.maxOf { it.scorePct }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pack.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(
                            "${fa(at.size)} آزمون · میانگین ${fa(avg)}٪ · بهترین ${fa(best)}٪",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Text(
            "🔒 این آمار خودکار و از روی رویدادهای واقعی ثبت می‌شود و قابل ویرایش نیست.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatBar(emoji: String, value: Int, target: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(emoji, style = MaterialTheme.typography.labelSmall)
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (if (target <= 0) 0f else value.toFloat() / target).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(fa(value), style = MaterialTheme.typography.labelSmall)
    }
}
