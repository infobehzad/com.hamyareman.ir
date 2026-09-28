@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hamyareman.ir.ui.content

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.profile.StudentProfileState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun fixNum(n: Int): String = toPersianDigits(n.toString()).padStart(2, '۰')
private fun fixPct(n: Int): String = toPersianDigits(n.toString()).padStart(3, '۰')

private fun mbFixed(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 0.1) {
        val kb = bytes / 1024.0
        "${toPersianDigits(kb.roundToInt().toString())} کیلوبایت"
    } else {
        "${toPersianDigits(String.format(java.util.Locale.US, "%.1f", mb))} مگابایت"
    }
}

private val numStyle: TextStyle
    @Composable get() = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")

/**
 * صفحه مدیریت دانلود محتوا و صفحات HTML تعاملی:
 * طراحی و ساختار کاملاً مشابه مدیریت دانلود کتاب‌ها (آکاردئون دسته‌ها، حجم، چیپ وضعیت، دانلود گروهی و تک‌به‌تک).
 */
@Composable
fun ContentDownloadsScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    remember { ContentCatalog.load(ctx) }

    val store = remember { LocalStore(ctx, "hamyar_content_dl") }
    var openCat by remember { mutableStateOf(store.getString("open_cat", "amozesh")) }
    fun toggleCat(catId: String) {
        openCat = if (openCat == catId) "" else catId
        store.putString("open_cat", openCat)
    }

    val gender = StudentProfileState.gender
    val categories = remember { ContentCatalog.categories() }
    val busy = remember { mutableStateMapOf<String, Int>() }       // itemId -> percent
    val netErr = remember { mutableStateMapOf<String, Boolean>() }   // itemId -> error
    var tick by remember { mutableIntStateOf(0) }
    var deleteConfirmItem by remember { mutableStateOf<ContentItem?>(null) }
    var deleteConfirmCategory by remember { mutableStateOf<ContentCat?>(null) }

    fun refresh() {
        tick++
    }

    suspend fun dl(item: ContentItem) {
        if (busy.containsKey(item.id)) return
        netErr.remove(item.id)
        busy[item.id] = 0
        val ok = ContentDownloadStore.downloadItem(ctx, item) { pct ->
            busy[item.id] = pct
        }
        busy.remove(item.id)
        if (!ok) {
            netErr[item.id] = true
        }
        refresh()
    }

    fun downloadAll(items: List<ContentItem>) {
        val missing = items.filter { !ContentDownloadStore.isCached(ctx, it.id) }
        scope.launch {
            missing.forEach { item ->
                dl(item)
            }
        }
    }

    if (deleteConfirmItem != null) {
        val item = deleteConfirmItem!!
        AlertDialog(
            onDismissRequest = { deleteConfirmItem = null },
            title = { Text("حذف فایل دانلود شده") },
            text = { Text("آیا از حذف صفحه «${item.title}» از حافظه گوشی مطمئن هستی؟") },
            confirmButton = {
                Button(onClick = {
                    ContentDownloadStore.deleteItem(ctx, item.id)
                    deleteConfirmItem = null
                    refresh()
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmItem = null }) { Text("انصراف") }
            },
        )
    }

    if (deleteConfirmCategory != null) {
        val cat = deleteConfirmCategory!!
        AlertDialog(
            onDismissRequest = { deleteConfirmCategory = null },
            title = { Text("حذف دانلودهای این دسته") },
            text = { Text("آیا می‌خواهی همه فایل‌های دانلود شده دسته «${cat.title}» حذف شوند؟") },
            confirmButton = {
                Button(onClick = {
                    val items = ContentCatalog.itemsOf(cat.id, gender)
                    items.forEach { ContentDownloadStore.deleteItem(ctx, it.id) }
                    deleteConfirmCategory = null
                    refresh()
                }) { Text("حذف همه") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmCategory = null }) { Text("انصراف") }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("مدیریت دانلود محتوا", onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                val totalBytes = remember(tick) { ContentDownloadStore.totalSize(ctx) }
                val totalItems = remember(gender) { categories.sumOf { ContentCatalog.itemsOf(it.id, gender).size } }
                val downloadedCount = remember(tick, gender) {
                    categories.sumOf { cat ->
                        ContentCatalog.itemsOf(cat.id, gender).count { ContentDownloadStore.isCached(ctx, it.id) }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📱 وضعیت حافظه محتوا و HTMLها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "مجموع صفحات دانلود شده: ${fixNum(downloadedCount)} از ${fixNum(totalItems)} صفحه (${mbFixed(totalBytes)})",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "فایل‌های دانلود شده به صورت رمزگشایی‌شده روی گوشی ذخیره می‌شوند و بدون اینترنت با سرعت بالا باز می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            items(categories, key = { it.id }) { cat ->
                val items = remember(cat.id, gender) { ContentCatalog.itemsOf(cat.id, gender) }
                val doneCount = remember(cat.id, tick) { items.count { ContentDownloadStore.isCached(ctx, it.id) } }
                val missingCount = items.size - doneCount
                val isOpen = openCat == cat.id
                val anyBusy = items.any { busy.containsKey(it.id) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { toggleCat(cat.id) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(cat.emoji, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cat.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${fixNum(doneCount)} از ${fixNum(items.size)} صفحه دانلود شده",
                                    style = numStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        if (isOpen) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { downloadAll(items) },
                                    enabled = !anyBusy && missingCount > 0,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("دانلود همه (${toPersianDigits(missingCount.toString())})", style = numStyle)
                                }
                                if (doneCount > 0) {
                                    OutlinedButton(
                                        onClick = { deleteConfirmCategory = cat },
                                        enabled = !anyBusy,
                                    ) {
                                        Text("حذف")
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items.forEach { item ->
                                    val isDone = ContentDownloadStore.isCached(ctx, item.id)
                                    val pct = busy[item.id]
                                    val isNetErr = netErr[item.id] == true
                                    val isItemBusy = pct != null

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isItemBusy) {
                                                if (isDone) {
                                                    onOpenItem(item.id)
                                                } else {
                                                    scope.launch { dl(item) }
                                                }
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                        )

                                        val (chipIcon, chipTint) = when {
                                            isItemBusy -> Icons.Outlined.Schedule to MaterialTheme.colorScheme.primary
                                            isDone -> Icons.Outlined.CheckCircle to MaterialTheme.colorScheme.tertiary
                                            isNetErr -> Icons.Outlined.ErrorOutline to MaterialTheme.colorScheme.error
                                            else -> Icons.Outlined.Download to MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        val chipLabel = when {
                                            isItemBusy -> "در حال دانلود ${fixPct(pct ?: 0)}٪"
                                            isDone -> "دانلود شده"
                                            isNetErr -> "خطای شبکه"
                                            else -> "دانلود نشده"
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable(enabled = !isItemBusy) {
                                                    if (isDone) {
                                                        deleteConfirmItem = item
                                                    } else {
                                                        scope.launch { dl(item) }
                                                    }
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Icon(chipIcon, contentDescription = null, tint = chipTint, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(chipLabel, style = numStyle, color = chipTint)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
