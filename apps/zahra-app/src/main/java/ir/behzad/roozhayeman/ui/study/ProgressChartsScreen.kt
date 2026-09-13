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
import ir.behzad.platform.feature.study.StudyPack
import ir.behzad.roozhayeman.LocalAppContainer

/**
 * «نمودار پیشرفت دروس» (v1.14) — تفکیک‌شده به‌ازای هر کتاب:
 * فقط آمار واقعیِ خودکارِ ثبت‌شده و سینک‌شونده، غیرقابل ویرایش:
 *  - تدریس: وضعیت اولین دوره‌ی هر درس — مشاهده‌شده از کل + باقیمانده، نشست‌ها،
 *    «اتمام دوره‌ی اول در چند نشست» و تاریخ/ساعت شمسیِ شروع/آخرین نشست/اتمام؛
 *  - فلش‌کارت‌ها: کل/یادگرفته/باقیمانده/مرورشده + تسلط؛
 *  - آزمون‌ها: تعداد، آخرین/بهترین/میانگین + موضوعات ضعیف، با تاریخ/ساعت شمسی.
 */
@Composable
fun ProgressChartsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val ctx = LocalContext.current
    val books = remember { BookModuleRegistry.modules }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("نمودار پیشرفت دروس", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------- جمع‌بندی کلی ----------
            val allPacks = books.flatMap { it.packs }
            val startedAll = allPacks.count { TeachStats.raw(ctx, it.packId).length() > 0 }
            val doneAll = allPacks.count { TeachStats.isDone(ctx, it.packId) }
            SectionCard(
                title = "جمع‌بندی",
                body = "${toPersianDigits(startedAll.toString())} درس شروع شده · دوره‌ی اولِ ${toPersianDigits(doneAll.toString())} درس کامل شده · " +
                    "همه‌ی اعداد خودکار ثبت و سینک می‌شوند و قابل ویرایش نیستند (تاریخ‌ها شمسی).",
            ) { }

            // ---------- به‌تفکیک هر کتاب ----------
            books.forEach { module ->
                val packs = module.packs
                val teachActive = packs.filter { TeachStats.raw(ctx, it.packId).length() > 0 }
                val doneHere = packs.count { TeachStats.isDone(ctx, it.packId) }
                val cardRows = packs.mapNotNull { p ->
                    val states = runCatching { container.studyProgress.cards(p.packId) }.getOrDefault(emptyMap())
                    if (p.flashcards.isEmpty() || states.isEmpty()) null else p to states
                }
                val quizRows = packs.mapNotNull { p ->
                    val at = runCatching { container.studyProgress.attempts(p.packId) }.getOrDefault(emptyList())
                    if (at.isEmpty()) null else p to at
                }
                if (teachActive.isEmpty() && cardRows.isEmpty() && quizRows.isEmpty()) return@forEach

                Text("📘 ${module.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (teachActive.isNotEmpty()) {
                    Text(
                        "دوره‌ی اول کامل شده: ${toPersianDigits(doneHere.toString())} از ${toPersianDigits(teachActive.size.toString())} درس شروع‌شده",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                teachActive.forEach { pack -> TeachRow(pack) }

                cardRows.forEach { (pack, states) ->
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

                quizRows.forEach { (pack, at) ->
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
                "این صفحه فقط از آمارِ خودکار پر می‌شود — چیزی اینجا قابل ویرایش نیست. با هر نوشتن، داده‌ها در حسابِ زهرا هم ذخیره می‌شوند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** ردیف تدریس یک درس — وضعیت اولین دوره با محاسبه‌ی مشاهده/باقیمانده و نشست‌ها. */
@Composable
private fun TeachRow(pack: StudyPack) {
    val snap = TeachStats.snap(LocalContext.current, pack.packId)
    val totalSec = (snap.audioDurSec + snap.videoDurSec).coerceAtLeast(1)
    val watched = snap.watchedSec.coerceAtMost(totalSec)
    val remainSec = (totalSec - watched).coerceAtLeast(0)
    val pct = (watched * 100) / totalSec
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (snap.done) "✅" else "⏳",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.width(6.dp))
                Text(pack.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${toPersianDigits(pct.toString())}٪", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { watched.toFloat() / totalSec },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "مشاهده: ${teachMmss(watched * 1000L)} از ${teachMmss(totalSec * 1000L)} · باقیمانده: ${teachMmss(remainSec * 1000L)} · نشست‌ها: ${toPersianDigits(snap.sessions.toString())}" +
                    if (snap.done) " · اتمام دوره‌ی اول: ${toPersianDigits(snap.sessionsToDone.toString())} نشست"
                    else "",
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
