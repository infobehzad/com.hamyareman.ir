package ir.behzad.roozhayeman.ui.study

import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
 * صفحه‌ی یک کتاب — فهرست درختی رسمی کتاب (v1.10):
 *  - بخش/فصل‌ها جمع‌شونده‌اند؛ پیش‌فرض جمع‌شده و وضعیت باز/بسته‌شان حافظه دارد؛
 *  - دروس زیر هر فصل ردیف ثابت‌اند: لمس → صفحه‌ی تدریس همان درس؛
 *  - کنار هر درس، درصد تسلط و وضعیت قفلِ «مطالعه و آزمون» دیده می‌شود؛
 *  - ردیف‌های بدون درس (ستایش/نیایش/واژه‌نامه/جلسه‌ها/…) فقط راهنمای فهرست‌اند.
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
                        "هر درس را لمس کن تا تدریسش باز شود — صوت همان‌جا پخش می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onCharts) { Text("📈 نمودار پیشرفت") }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val tocStore = remember(bookCode) { LocalStore(ctx, "hamyar_toc") }
        Column(Modifier.fillMaxWidth()) {
            BookToc.forBook(bookCode).forEach { node ->
                TocNodeRow(
                    bookCode = bookCode,
                    node = node,
                    depth = 0,
                    store = tocStore,
                    onTeach = onTeach,
                )
            }
        }
    }
}

@Composable
private fun TocNodeRow(
    bookCode: String,
    node: TocNode,
    depth: Int,
    store: LocalStore,
    onTeach: (String) -> Unit,
) {
    val isSection = node.packId == null && node.children.isNotEmpty()
    val key = "open_${bookCode}_${node.id}"
    var sectionOpen by remember(node.id) { mutableStateOf(isSection && store.getString(key, "0") == "1") }
    when {
        // فصل/بخش: جمع‌شونده با حافظه — پیش‌فرض جمع‌شده.
        isSection -> SectionRow(node, depth, sectionOpen) {
            sectionOpen = !sectionOpen
            store.putString(key, if (sectionOpen) "1" else "0")
        }
        // درس: ردیف ثابت → صفحه‌ی تدریس؛ زیرردیف‌های ثابتش (جلسه/…) همیشه دیده می‌شوند.
        node.packId != null -> LessonRow(node, depth, onTeach)
        // ردیف ثابت بدون درس (ستایش/نیایش/واژه‌نامه/…).
        else -> StaticRow(node, depth)
    }
    if (node.children.isNotEmpty() && (node.packId != null || sectionOpen)) {
        node.children.forEach { child -> TocNodeRow(bookCode, child, depth + 1, store, onTeach) }
    }
}

@Composable
private fun SectionRow(node: TocNode, depth: Int, open: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = (depth * 16).dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (open) "بستن" else "بازکردن",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            node.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LessonRow(node: TocNode, depth: Int, onTeach: (String) -> Unit) {
    val packId = node.packId ?: return
    val pack = remember(packId) { BookModuleRegistry.pack(packId) }
    val container = LocalAppContainer.current
    val ctx = LocalContext.current
    val done = remember(packId) {
        if (pack == null) false else {
            val expected = teachTracksOf(pack).size + (if (StudyMedia.videoIds(packId).isNotEmpty()) 1 else 0)
            TeachStats.expectMedia(ctx, packId, expected.coerceAtLeast(1))
            TeachStats.isDone(ctx, packId)
        }
    }
    val mastery = remember(packId) {
        if (pack == null) 0 else runCatching {
            container.studyProgress.masteryPct(pack)
        }.getOrDefault(0)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTeach(packId) }
            .padding(start = (depth * 16 + 8).dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.PlayCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            node.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (pack != null) {
            Text(
                "${toPersianDigits(mastery.toString())}٪",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            if (done) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
            contentDescription = if (done) "مطالعه باز است" else "مطالعه بعد از تدریس کامل",
            tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun StaticRow(node: TocNode, depth: Int) {
    Text(
        node.title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16 + 8).dp, top = 6.dp, bottom = 6.dp),
    )
}
