package ir.behzad.roozhayeman.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.behzad.platform.core.common.JalaliDate
import ir.behzad.platform.core.common.toPersianDigits
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.core.designsystem.SectionCard
import ir.behzad.platform.feature.study.BookModuleRegistry
import ir.behzad.roozhayeman.LocalAppContainer

/**
 * «نمودار پیشرفت دروس» (v1.13) — جایگزین نمودار ۷روزه‌ی مدرسه:
 * فقط آمار واقعی و محاسباتیِ درس‌ها، همه‌ی خودکار ثبت‌شده و سینک‌شونده، غیرقابل ویرایش:
 *  ۱) تدریس — وضعیت اولین دوره‌ی هر درس: زمانِ مشاهده‌شده از کل + باقیمانده،
 *     تعداد نشست‌ها و اینکه اتمام دوره‌ی اول در چند نشست بوده (با تاریخ/ساعت شمسی)؛
 *  ۲) فلش‌کارت‌ها — کل/یادگرفته/باقیمانده/مرورشده و درصد تسلط؛
 *  ۳) آزمون‌ها — تعداد، آخرین/بهترین نمره، موضوعات ضعیف (با تاریخ/ساعت شمسی).
 * منبع آمار: TeachStats + StudyProgressRepository (هر دو در هر نوشتن به سرور هم می‌روند).
 */
@Composable
fun ProgressChartsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val ctx = LocalContext.current
    val today = remember { JalaliDate.todayIso() }

    // همه‌ی محاسبات از داده‌ی واقعیِ ثبت‌شده — هیچ ورودی دستی وجود ندارد.
    val rows = remember {
        BookModuleRegistry.modules
            .asSequence()
            .flatMap { m -> m.packs.asSequence().map { m.title to it } }
            .toList()
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("نمودار پیشرفت دروس", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------- خلاصه‌ی کلی ----------
            val teachActive = rows.mapNotNull { (_, p) ->
                if (TeachStats.raw(ctx, p.packId).length() > 0) p else null
            }
            val doneCount = teachActive.count { TeachStats.isDone(ctx, it.packId) }
            SectionCard(
                title = "جمع‌بندی",
                body = "${toPersianDigits(teachActive.size.toString())} درس شروع شده · " +
                    "دوره‌ی اولِ ${toPersianDigits(doneCount.toString())} درس کامل شده · " +
                    "همه‌ی اعداد خودکار ثبت و سینک می‌شوند و قابل ویرایش نیستند.",
            ) { }

            // ---------- تدریس ----------
            if (teachActive.isNotEmpty()) {
                Text("تدریس — اولین دوره‌ی هر درس", style = MaterialTheme.typography.titleMedium)
                teachActive.forEach { pack ->
                    val snap = TeachStats.snap(ctx, pack.packId)
                    val totalSec = (snap.audioDurSec + snap.videoDurSec).coerceAtLeast(1)
                    val watched = snap.watchedSec.coerceAtMost(totalSec)
                    val remainSec = (totalSec - watched).coerceAtLeast(0)
                    val pct = (watched * 100) / totalSec
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(pack.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${teachMmss(watched * 1000L)} از ${teachMmss(totalSec * 1000L)} (${toPersianDigits(pct.toString())}٪)",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Spacer(Modifier.width(8.dp))
                                LinearProgressIndicator(
                                    progress = { watched.toFloat() / totalSec },
                                    modifier = Modifier.weight(1f).height(8.dp),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "باقیمانده: ${teachMmss(remainSec * 1000L)} · نشست‌ها: ${toPersianDigits(snap.sessions.toString())}" +
                                    if (snap.done) " · اتمام دوره‌ی اول: ${toPersianDigits(snap.sessionsToDone.toString())} نشست ✓"
                                    else " · دوره‌ی اول هنوز کامل نشده",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                buildString {
                                    append("شروع: ")
                                    append(if (snap.startedAtMs > 0) JalaliDate.stampFa(snap.startedAtMs) else "—")
                                    append(" · آخرین نشست: ")
                                    append(if (snap.lastSessionAtMs > 0) JalaliDate.stampFa(snap.lastSessionAtMs) else "—")
                                    if (snap.done && snap.completedAtMs > 0) {
                                        append(" · اتمام: ")
                                        append(JalaliDate.stampFa(snap.completedAtMs))
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ---------- فلش‌کارت‌ها ----------
            val cardPacks = rows.mapNotNull { (book, p) ->
                if (p.flashcards.isEmpty()) return@mapNotNull null
                val states = runCatching { container.studyProgress.cards(p.packId) }.getOrDefault(emptyMap())
                if (states.isEmpty()) return@mapNotNull null
                Triple(book, p, states)
            }
            if (cardPacks.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("فلش‌کارت‌ها", style = MaterialTheme.typography.titleMedium)
                cardPacks.forEach { (_, pack, states) ->
                    val total = pack.flashcards.size
                    val reviewed = states.values.sumOf { it.reps }
                    val learned = states.values.count { it.reps > 0 }
                    val remain = (total - learned).coerceAtLeast(0)
                    val mastery = runCatching { container.studyProgress.masteryPct(pack) }.getOrDefault(0)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(pack.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "یادگرفته ${toPersianDigits(learned.toString())} از ${toPersianDigits(total.toString())} · باقیمانده ${toPersianDigits(remain.toString())} · مرورشده ${toPersianDigits(reviewed.toString())} بار",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Spacer(Modifier.width(8.dp))
                                LinearProgressIndicator(
                                    progress = { mastery / 100f },
                                    modifier = Modifier.weight(1f).height(8.dp),
                                )
                            }
                            Text(
                                "تسلط: ${toPersianDigits(mastery.toString())}٪",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ---------- آزمون‌ها ----------
            val quizPacks = rows.mapNotNull { (_, p) ->
                val at = runCatching { container.studyProgress.attempts(p.packId) }.getOrDefault(emptyList())
                if (at.isEmpty()) null else p to at
            }
            if (quizPacks.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("تحلیل آزمون‌ها", style = MaterialTheme.typography.titleMedium)
                quizPacks.forEach { (pack, at) ->
                    val last = at.last()
                    val best = at.maxOf { it.scorePct }
                    val avg = at.sumOf { it.scorePct } / at.size.coerceAtLeast(1)
                    val weak = at.flatMap { it.weakTopics }.distinct().take(4)
                    val lastWhen = if (last.atMs > 0) JalaliDate.stampFa(last.atMs) else JalaliDate.formatFa(last.dateKey)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(pack.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${toPersianDigits(at.size.toString())} آزمون · آخرین: ${toPersianDigits(last.scorePct.toString())}٪" +
                                    " · بهترین: ${toPersianDigits(best.toString())}٪ · میانگین: ${toPersianDigits(avg.toString())}٪",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "آخرین آزمون: $lastWhen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (weak.isNotEmpty()) {
                                Text(
                                    "موضوعات نیازمند مرور: ${weak.joinToString("، ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            Text(
                "این صفحه فقط از آمارِ خودکار پر می‌شود — چیزی اینجا قابل ویرایش نیست. با هر نوشتن، داده‌ها در حسابِ زهرا هم ذخیره می‌شوند (تاریخ‌ها شمسی‌اند).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
