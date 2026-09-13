package ir.behzad.roozhayeman.ui.study

import android.graphics.BitmapFactory
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.behzad.platform.core.common.LocalStore
import ir.behzad.platform.core.common.toPersianDigits
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.feature.study.BookModuleRegistry
import ir.behzad.platform.feature.study.BookToc
import ir.behzad.platform.feature.study.BookToc.TocNode
import ir.behzad.roozhayeman.LocalAppContainer
import ir.behzad.roozhayeman.ui.hub.HubBody

/**
 * صفحه‌ی یک کتاب (v1.13) — فهرست درختیِ رسمی با قالب کارتیِ نسخه‌ی قبلی:
 *  - فصل/بخش = کارتِ جمع‌شونده (پیش‌فرض جمع‌شده، وضعیت با حافظه — مثل جمع‌شونده‌های دیگر اپ)؛
 *  - درس = کارت کامل با آمار، نوار تسلط و دو دکمه‌ی «تدریس» و «مطالعه و آزمون»
 *    (مطالعه با همان شرط اتمام اولین دوره‌ی تدریس باز می‌شود + دیالوگِ قفل)؛
 *  - ردیف‌های بدون درس (ستایش/نیایش/واژه‌نامه/جلسه‌ها/…) کارتِ ساده‌ی ثابت‌اند.
 */
@Composable
fun BookDetailScreen(
    bookCode: String,
    onBack: () -> Unit,
    onTeach: (String) -> Unit,
    onStudy: (String) -> Unit,
    onCharts: () -> Unit,
) {
    val module = remember(bookCode) { BookModuleRegistry.modules.firstOrNull { it.bookCode == bookCode } }
    val container = LocalAppContainer.current
    val today = remember { ir.behzad.platform.core.common.JalaliDate.todayIso() }

    AppTopBar(title = module?.title ?: "کتاب", onBack = onBack)
    HubBody {
        if (module == null) {
            Text("این کتاب هنوز محتوایی ندارد.")
            return@HubBody
        }

        val ctx = LocalContext.current
        val cover = remember(bookCode) {
            runCatching { BitmapFactory.decodeStream(ctx.assets.open("book-covers/$bookCode.jpg")) }.getOrNull()
        }
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (cover != null) {
                    Image(
                        bitmap = cover.asImageBitmap(),
                        contentDescription = "کاور ${module.title}",
                        modifier = Modifier.width(96.dp).height(128.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(module.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "فصل‌ها را باز کن و هر درس را لمس کن — اول تدریس، بعد تمرین.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onCharts) { Text("📈 نمودار پیشرفت دروس") }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val tocStore = remember(bookCode) { LocalStore(ctx, "hamyar_toc") }
        Column(Modifier.fillMaxWidth()) {
            BookToc.forBook(bookCode).forEach { node ->
                TocRow(bookCode, node, 0, tocStore, onTeach, onStudy)
            }
        }
    }
}

@Composable
private fun TocRow(
    bookCode: String,
    node: TocNode,
    depth: Int,
    store: LocalStore,
    onTeach: (String) -> Unit,
    onStudy: (String) -> Unit,
) {
    val isSection = node.packId == null && node.children.isNotEmpty()
    val key = "open_${bookCode}_${node.id}"
    var open by remember(node.id) { mutableStateOf(isSection && store.getString(key, "0") == "1") }
    when {
        isSection -> SectionCardCollapsible(node, depth, open) {
            open = !open
            store.putString(key, if (open) "1" else "0")
        }
        node.packId != null -> LessonCard(node, depth, onTeach, onStudy)
        else -> StaticCard(node, depth)
    }
    if (node.children.isNotEmpty() && (node.packId != null || open)) {
        node.children.forEach { child -> TocRow(bookCode, child, depth + 1, store, onTeach, onStudy) }
    }
}

/** کارت فصل/بخش — جمع‌شونده با حافظه (مثل جمع‌شونده‌های برنامه‌ی هفتگی و آزمون‌ها). */
@Composable
private fun SectionCardCollapsible(node: TocNode, depth: Int, open: Boolean, onToggle: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(start = (depth * 10).dp, top = 4.dp, bottom = 4.dp)
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (open) "بستن فصل" else "بازکردن فصل",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                node.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** کارت درس — مثل نسخه‌ی قبلی: آمار + تسلط + دو دکمه‌ی تدریس/مطالعه با شرط اتمام. */
@Composable
private fun LessonCard(node: TocNode, depth: Int, onTeach: (String) -> Unit, onStudy: (String) -> Unit) {
    val packId = node.packId ?: return
    val pack = remember(packId) { BookModuleRegistry.pack(packId) }
    val container = LocalAppContainer.current
    val ctx = LocalContext.current
    val today = remember { ir.behzad.platform.core.common.JalaliDate.todayIso() }

    val teachDone = remember(packId) {
        if (pack == null) false else {
            val expected = teachTracksOf(pack).size + (if (StudyMedia.videoIds(packId).isNotEmpty()) 1 else 0)
            TeachStats.expectMedia(ctx, packId, expected.coerceAtLeast(1))
            TeachStats.isDone(ctx, packId)
        }
    }
    val mastery = remember(packId) {
        if (pack == null) 0 else runCatching { container.studyProgress.masteryPct(pack) }.getOrDefault(0)
    }
    val due = remember(packId) {
        if (pack == null) 0 else runCatching { container.studyProgress.dueCards(pack, today).size }.getOrDefault(0)
    }

    Card(
        Modifier
            .fillMaxWidth()
            .padding(start = (depth * 10).dp, top = 4.dp, bottom = 4.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(node.title, style = MaterialTheme.typography.titleMedium)
            if (pack != null) {
                Text(
                    "${pack.sections.size} سکشن · ${pack.flashcards.size} کارت · ${pack.questions.size} سؤال",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تسلط ${toPersianDigits(mastery.toString())}٪", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { mastery / 100f },
                        modifier = Modifier.weight(1f).height(8.dp),
                    )
                }
                if (due > 0) {
                    Text("🔔 ${toPersianDigits(due.toString())} کارت امروز باید مرور شود", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            // زیرردیف‌های ثابتِ درس (جلسه‌های قرآن / Talking about انگلیسی).
            node.children.forEach { child -> StaticCard(child, 0) }
            Spacer(Modifier.height(8.dp))
            val showLockDialog = remember(packId) { mutableStateOf(false) }
            if (showLockDialog.value) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showLockDialog.value = false },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { showLockDialog.value = false }) { Text("متوجه شدم") }
                    },
                    title = { Text("🔒 اول تدریس، بعد تمرین") },
                    text = { Text("برای باز شدن «مطالعه و آزمون»، اول دوره‌ی اول تدریس این درس را تا انتها ببین. همین‌که صوت/ویدیو تمام شود، خودکار فعال می‌شود.") },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onTeach(packId) }, modifier = Modifier.weight(1f)) { Text("📖 تدریس") }
                OutlinedButton(
                    onClick = {
                        if (teachDone) onStudy(packId) else showLockDialog.value = true
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(if (teachDone) "🎯 مطالعه و آزمون" else "🔒 مطالعه و آزمون") }
            }
        }
    }
}

/** کارت ساده‌ی ردیف‌های بدون درس (ستایش/نیایش/واژه‌نامه/جلسه/…). */
@Composable
private fun StaticCard(node: TocNode, depth: Int) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(start = (depth * 10).dp, top = 3.dp, bottom = 3.dp),
    ) {
        Text(
            node.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
