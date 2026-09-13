package ir.behzad.roozhayeman.ui.study

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import ir.behzad.platform.feature.study.BookModule
import ir.behzad.platform.feature.study.BookModuleRegistry
import ir.behzad.platform.feature.study.StudyPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * «مدیریت دانلود کتاب‌ها» (v1.14) — کارتی به‌تفکیک کتاب:
 *  - وضعیت هر فایل: دانلود شده ✓ / در حال دانلود (٪) / دانلود نشده ⬇ /
 *    «فایل روی سرور نیست» (HTTP 404 — ماندگار) / «اتصال اینترنت را بررسی کن» (گذرا)؛
 *  - «دانلود یکجای این کتاب» با نوار پیشرفت و نمایش حجم دانلودشده؛
 *  - PDF در همان کش صفحه‌ی تدریس (media/pdf-cache) و صوت در گاوصندوق رمزشده
 *    (MediaVault) ذخیره می‌شود — پس هرچه اینجا دانلود شود، آفلاین هم کار می‌کند.
 */

private enum class FileKind(val label: String) { PDF("📕 PDF"), AUDIO("🎧 صوت") }

private fun keyOf(packId: String, kind: FileKind) = "$packId:${kind.name}"

private fun pdfCacheFile(ctx: android.content.Context, fileId: String): File =
    File(File(ctx.filesDir, "media/pdf-cache").apply { mkdirs() }, fileId)

private fun pdfCached(ctx: android.content.Context, fileId: String): Boolean =
    pdfCacheFile(ctx, fileId).let { it.exists() && it.length() > 1024 }

private fun fmtSize(bytes: Long): String =
    if (bytes >= 1024L * 1024L) {
        "${toPersianDigits(String.format(Locale.US, "%.1f", bytes / (1024.0 * 1024.0)))} مگابایت"
    } else {
        "${toPersianDigits((bytes / 1024L).toString())} کیلوبایت"
    }

/** فایل روی سرور وجود ندارد (HTTP 404). */
private class NotFoundOnServer : Exception("404")

/** دانلود PDF به کش مشترک تدریس — ۴۰۴ تفکیک می‌شود. */
private fun downloadPdfBlocking(ctx: android.content.Context, fileId: String, onProgress: (Int) -> Unit) {
    val target = pdfCacheFile(ctx, fileId)
    val tmp = File(target.parentFile, "$fileId.part")
    val conn = (URL(StudyMedia.viewUrl(fileId)).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true
    }
    conn.connect()
    if (conn.responseCode == 404) throw NotFoundOnServer()
    if (conn.responseCode !in 200..299) throw java.io.IOException("HTTP ${conn.responseCode}")
    val total = conn.contentLengthLong
    conn.inputStream.use { input ->
        java.io.FileOutputStream(tmp).use { out ->
            val buf = ByteArray(64 * 1024)
            var read: Int
            var done = 0L
            while (input.read(buf).also { read = it } > 0) {
                out.write(buf, 0, read); done += read
                if (total > 0) onProgress(((done * 100) / total).toInt())
            }
        }
    }
    val ok = tmp.length() > 1024 && runCatching {
        tmp.inputStream().use { val h = ByteArray(5); it.read(h); String(h) == "%PDF-" }
    }.getOrDefault(false)
    if (!ok) { tmp.delete(); throw java.io.IOException("ناقص") }
    if (!tmp.renameTo(target)) { tmp.copyTo(target, overwrite = true); tmp.delete() }
}

@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { LocalStore(ctx, "hamyar_downloads") }
    val books = remember { BookModuleRegistry.modules }
    val scope = rememberCoroutineScope()
    // key → درصد (حضور کلید = در حال دانلود)
    val busy = remember { mutableStateMapOf<String, Int>() }
    // key → true (خطای شبکه‌ی گذرا)
    val netErr = remember { mutableStateMapOf<String, Boolean>() }
    var tick by remember { mutableIntStateOf(0) }

    /** دانلود یک فایل؛ خطاها تفکیک و ثبت می‌شوند. */
    suspend fun dlFile(pack: StudyPack, kind: FileKind, fileId: String, cacheKey: String) {
        val key = keyOf(pack.packId, kind)
        if (busy.containsKey(key)) return
        netErr.remove(key)
        busy[key] = 0
        try {
            withContext(Dispatchers.IO) {
                when (kind) {
                    FileKind.PDF -> downloadPdfBlocking(ctx, fileId) { busy[key] = it }
                    FileKind.AUDIO -> MediaVault.downloadEncrypted(ctx, StudyMedia.viewUrl(fileId), cacheKey) { busy[key] = it }
                }
            }
            tick++
        } catch (e: NotFoundOnServer) {
            // ماندگار: این فایل روی سرور نیست — بعدها که آپلود شد خودکار درست می‌شود.
            store.putString("dl404_$fileId", "1")
        } catch (e: Exception) {
            netErr[key] = true
        } finally {
            busy.remove(key)
        }
    }

    fun fileIds(pack: StudyPack, kind: FileKind): Pair<String?, String?> = when (kind) {
        FileKind.PDF -> pack.pdfFileName to null
        FileKind.AUDIO -> teachTracksOf(pack).firstOrNull()?.let { it.fileId to it.cacheKey } ?: (null to null)
    }

    fun isDone(pack: StudyPack, kind: FileKind): Boolean {
        val (fid, ck) = fileIds(pack, kind)
        return when (kind) {
            FileKind.PDF -> fid != null && pdfCached(ctx, fid)
            FileKind.AUDIO -> ck != null && MediaVault.isCached(ctx, ck)
        }
    }

    fun missing(pack: StudyPack): List<Pair<FileKind, Pair<String?, String?>>> =
        listOf(FileKind.PDF, FileKind.AUDIO)
            .filter { !isDone(pack, it) }
            .map { it to fileIds(pack, it) }

    fun downloadPack(pack: StudyPack) {
        scope.launch {
            for ((kind, ids) in missing(pack)) {
                if (!isActive) break
                val (fid, ck) = ids
                if (fid == null) continue
                dlFile(pack, kind, fid, ck ?: "")
            }
        }
    }

    fun downloadBook(module: BookModule) {
        scope.launch {
            for (p in module.packs) {
                if (!isActive) break
                for ((kind, ids) in missing(p)) {
                    if (!isActive) break
                    val (fid, ck) = ids
                    if (fid == null) continue
                    dlFile(p, kind, fid, ck ?: "")
                }
            }
        }
    }

    AppTopBar("مدیریت دانلود کتاب‌ها", onBack)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item {
            Text(
                "صوت و PDF همه‌ی درس‌ها — هر چیزی که اینجا دانلود شود، بدون اینترنت هم پخش و باز می‌شود (صوت رمزشده روی گوشی ذخیره می‌شود).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        items(books.size) { i ->
            BookDownloadCard(
                module = books[i],
                store = store,
                busy = busy,
                netErr = netErr,
                tick = tick,
                onDownloadBook = { downloadBook(books[i]) },
                onDownloadPack = { p -> downloadPack(p) },
            )
        }
    }
}

@Composable
private fun BookDownloadCard(
    module: BookModule,
    store: LocalStore,
    busy: Map<String, Int>,
    netErr: Map<String, Boolean>,
    tick: Int,
    onDownloadBook: () -> Unit,
    onDownloadPack: (StudyPack) -> Unit,
) {
    val ctx = LocalContext.current
    val cover = remember(module.bookCode) {
        runCatching { BitmapFactory.decodeStream(ctx.assets.open("book-covers/${module.bookCode}.jpg")) }.getOrNull()
    }
    val anyBusy = remember(tick, busy.size) {
        module.packs.any { p ->
            busy.containsKey(keyOf(p.packId, FileKind.PDF)) || busy.containsKey(keyOf(p.packId, FileKind.AUDIO))
        }
    }
    val bytesOnDevice = remember(module.bookCode, tick) {
        module.packs.sumOf { p -> pdfCacheFile(ctx, p.pdfFileName).takeIf { it.exists() }?.length() ?: 0L } +
            module.packs.sumOf { p ->
                teachTracksOf(p).sumOf { t ->
                    if (MediaVault.isCached(ctx, t.cacheKey)) MediaVault.vaultFile(ctx, t.cacheKey).length() else 0L
                }
            }
    }
    val totalFiles = remember(module.bookCode) {
        module.packs.sumOf { 1 + teachTracksOf(it).size }
    }
    val doneFiles = remember(module.bookCode, tick) {
        module.packs.sumOf { p ->
            (if (pdfCached(ctx, p.pdfFileName)) 1 else 0) + teachTracksOf(p).count { MediaVault.isCached(ctx, it.cacheKey) }
        }
    }
    val doneInBatch = remember(module.bookCode, tick) {
        module.packs.count { p ->
            pdfCached(ctx, p.pdfFileName) && teachTracksOf(p).all { MediaVault.isCached(ctx, it.cacheKey) }
        }
    }
    val curBusy = busy.entries.firstOrNull { e ->
        module.packs.any { p -> e.key == keyOf(p.packId, FileKind.PDF) || e.key == keyOf(p.packId, FileKind.AUDIO) }
    }
    val overallPct = if (anyBusy) {
        ((doneInBatch * 100 + (curBusy?.value ?: 0).coerceAtLeast(0)).toFloat() / (totalFiles.coerceAtLeast(1) * 100f)).coerceIn(0f, 1f)
    } else {
        doneFiles.toFloat() / totalFiles.coerceAtLeast(1)
    }

    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cover != null) {
                    Image(
                        bitmap = cover.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.width(40.dp).height(54.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(module.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "دانلودشده: ${toPersianDigits(doneFiles.toString())} از ${toPersianDigits(totalFiles.toString())} فایل · ${fmtSize(bytesOnDevice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { overallPct },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onDownloadBook,
                enabled = !anyBusy && doneFiles < totalFiles,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (doneFiles >= totalFiles) "همه‌ی این کتاب دانلود شده ✓" else "دانلود یکجای این کتاب")
            }
            Spacer(Modifier.height(6.dp))
            module.packs.forEach { p ->
                LessonDlRow(p, store, busy, netErr, onDownloadPack)
            }
        }
    }
}

@Composable
private fun LessonDlRow(
    pack: StudyPack,
    store: LocalStore,
    busy: Map<String, Int>,
    netErr: Map<String, Boolean>,
    onDownload: (StudyPack) -> Unit,
) {
    val ctx = LocalContext.current
    val tracks = remember(pack.packId) { teachTracksOf(pack) }
    val audio = tracks.firstOrNull()

    @Composable
    fun fileChip(kind: FileKind) {
        val key = keyOf(pack.packId, kind)
        val fileId = when (kind) {
            FileKind.PDF -> pack.pdfFileName
            FileKind.AUDIO -> audio?.fileId ?: ""
        }
        val cacheKey = when (kind) {
            FileKind.PDF -> ""
            FileKind.AUDIO -> audio?.cacheKey ?: ""
        }
        val done = when (kind) {
            FileKind.PDF -> pdfCached(ctx, fileId)
            FileKind.AUDIO -> cacheKey.isNotEmpty() && MediaVault.isCached(ctx, cacheKey)
        }
        val is404 = store.getString("dl404_$fileId", "0") == "1"
        val pct = busy[key]
        val (icon, label, color) = when {
            pct != null -> FileState(Icons.Outlined.Schedule, "در حال دانلود ${toPersianDigits(pct.toString())}٪", MaterialTheme.colorScheme.primary)
            done -> FileState(Icons.Outlined.CheckCircle, "دانلود شده", MaterialTheme.colorScheme.tertiary)
            is404 -> FileState(Icons.Outlined.CloudOff, "فایل روی سرور نیست", MaterialTheme.colorScheme.outline)
            netErr[key] == true -> FileState(Icons.Outlined.ErrorOutline, "اتصال اینترنت را بررسی کن", MaterialTheme.colorScheme.error)
            else -> FileState(Icons.Outlined.Download, "دانلود نشده", MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                "${kind.label} · $label",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
            )
        }
    }

    val anyBusy = busy.containsKey(keyOf(pack.packId, FileKind.PDF)) || busy.containsKey(keyOf(pack.packId, FileKind.AUDIO))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !anyBusy) { onDownload(pack) }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            pack.title,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        fileChip(FileKind.PDF)
        Spacer(Modifier.width(10.dp))
        if (audio != null) fileChip(FileKind.AUDIO)
    }
}

private data class FileState(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val color: androidx.compose.ui.graphics.Color)
