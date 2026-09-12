package ir.behzad.roozhayeman.ui.study

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.feature.study.BookModuleRegistry
import ir.behzad.roozhayeman.LocalAppContainer
import ir.behzad.roozhayeman.ui.hub.HubBody
import ir.behzad.roozhayeman.ui.hub.HubHeader

/**
 * صفحه‌ی یک کتاب — فهرست درس‌ها/فصل‌های همان کتاب (از BookModuleRegistry).
 * هر درس دو در دارد: «تدریس» (متن + صوت + ویدیو) و «مطالعه» (فلش‌کارت/آزمون/حل).
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
                        "${module.packs.size} درس/فصل آماده است — اول تدریس را ببین، بعد تمرین کن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onCharts) { Text("📈 نمودار پیشرفت") }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        module.packs.forEach { pack ->
            val mastery = remember(pack.packId) { runCatching { container.studyProgress.masteryPct(pack) }.getOrDefault(0) }
            val due = remember(pack.packId) { runCatching { container.studyProgress.dueCards(pack, today).size }.getOrDefault(0) }
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(pack.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${pack.sections.size} سکشن · ${pack.flashcards.size} کارت · ${pack.questions.size} سؤال",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("تسلط ${ir.behzad.platform.core.common.toPersianDigits(mastery.toString())}٪", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { mastery / 100f },
                            modifier = Modifier.weight(1f).height(8.dp),
                        )
                    }
                    if (due > 0) {
                        Text("🔔 $due کارت امروز باید مرور شود", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    val expectedMedia = teachTracksOf(pack).size + (if (StudyMedia.videoIds(pack.packId).isNotEmpty()) 1 else 0)
                    val teachDone = run {
                        TeachStats.expectMedia(ctx, pack.packId, expectedMedia.coerceAtLeast(1))
                        TeachStats.isDone(ctx, pack.packId)
                    }
                    val showLockDialog = remember(pack.packId) { androidx.compose.runtime.mutableStateOf(false) }
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
                        Button(onClick = { onTeach(pack.packId) }, modifier = Modifier.weight(1f)) { Text("📖 تدریس") }
                        OutlinedButton(
                            onClick = {
                                if (teachDone) onStudy(pack.packId) else showLockDialog.value = true
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(if (teachDone) "🎯 مطالعه و آزمون" else "🔒 مطالعه و آزمون") }
                    }
                }
            }
        }
    }
}
