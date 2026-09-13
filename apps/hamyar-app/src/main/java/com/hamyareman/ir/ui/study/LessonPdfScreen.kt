package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.LocalAppContainer
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.LinkedHashMap

/**
 * نمایشگر PDF کتاب درس (پرامپت ۰۵ + تصمیم مصوب): فایل از باکت Appwrite
 * دانلود و روی گوشی کش می‌شود (filesDir — بدون کش سرور)، با PdfRenderer
 * بومی اندروید صفحه‌به‌صفحه رندر می‌شود (lazy + LRU).
 */
private const val PDF_ENDPOINT = "https://fra.cloud.appwrite.io/v1"
private const val PDF_PROJECT = "6a9d59e3002751cc3ea8"
private const val PDF_BUCKET = "6aa1eaae00303400117b"

private sealed class PdfState {
    data object Idle : PdfState()
    data class Downloading(val progressPct: Int) : PdfState()
    data class Ready(val pageCount: Int) : PdfState()
    data class Error(val message: String) : PdfState()
}

/** کش LRU صفحه‌ها تا حافظه کنترل شود (همزمان حداکثر ~۸ صفحه). */
private class PageCache(private val maxPages: Int = 8) {
    private val map = LinkedHashMap<Int, Bitmap>(16, 0.75f, true)

    operator fun get(index: Int): Bitmap? = synchronized(this) { map[index] }

    operator fun set(index: Int, value: Bitmap) {
        synchronized(this) {
            map[index] = value
            while (map.size > maxPages) map.remove(map.keys.first())
        }
    }
}

@Composable
fun LessonPdfScreen(packId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val ctx = LocalContext.current
    val pack = remember(packId) { container.studyPacks.pack(packId) }
    val fileId = pack?.pdfFileName.orEmpty()

    var state by remember(fileId) { mutableStateOf<PdfState>(PdfState.Idle) }
    val pageCache = remember(fileId) { PageCache() }
    var renderer by remember(fileId) { mutableStateOf<PdfRenderer?>(null) }
    val renderLock = remember(fileId) { Any() }

    LaunchedEffect(fileId) {
        if (fileId.isBlank()) {
            state = PdfState.Error("این پک فایل PDF ندارد.")
            return@LaunchedEffect
        }
        try {
            val cacheDir = File(ctx.filesDir, "media/pdf-cache").apply { mkdirs() }
            val target = File(cacheDir, fileId)
            if (!target.exists() || target.length() < 1024) {
                state = PdfState.Downloading(0)
                val url = URL("$PDF_ENDPOINT/storage/buckets/$PDF_BUCKET/files/$fileId/view?project=$PDF_PROJECT")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                }
                if (conn.responseCode !in 200..299) {
                    state = PdfState.Error("دانلود ناموفق بود (کد ${conn.responseCode}). اینترنت یا باکت را بررسی کنید.")
                    return@LaunchedEffect
                }
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    java.io.FileOutputStream(target).use { out ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        var done = 0L
                        while (input.read(buf).also { read = it } > 0) {
                            out.write(buf, 0, read)
                            done += read
                            if (total > 0) {
                                val pct = ((done * 100) / total).toInt()
                                if (state is PdfState.Downloading && (state as PdfState.Downloading).progressPct != pct) {
                                    state = PdfState.Downloading(pct)
                                }
                            }
                        }
                    }
                }
            }
            state = PdfState.Downloading(100)
            val fd = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
            val r = PdfRenderer(fd)
            synchronized(renderLock) { renderer = r }
            state = PdfState.Ready(r.pageCount)
        } catch (e: Exception) {
            state = PdfState.Error(e.message ?: "خطای ناشناخته در باز کردن PDF")
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "📕 " + (pack?.title ?: "کتاب درس"), onBack = onBack)
        // v1.9: پلیر صوت در همه‌ی صفحات جزوه‌ها هم هست (صوت همان درس، همان‌جا پخش می‌شود).
        val tracks = remember(packId) { pack?.let { teachTracksOf(it) } ?: emptyList() }
        if (tracks.isNotEmpty()) {
            TeachAudioBar(packId = packId, screenTitle = pack?.title ?: "", tracks = tracks)
        }
        when (val st = state) {
            is PdfState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(st.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            is PdfState.Downloading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("در حال آماده‌سازی کتاب… (${st.progressPct}٪)", style = MaterialTheme.typography.bodySmall)
                }
            }
            is PdfState.Ready -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(st.pageCount) { index ->
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            var bmp by remember(fileId, index) { mutableStateOf<Bitmap?>(pageCache[index]) }
                            LaunchedEffect(fileId, index) {
                                if (bmp == null) {
                                    val r = renderer ?: return@LaunchedEffect
                                    val rendered: Bitmap? = try {
                                        synchronized(renderLock) {
                                            r.openPage(index).use { page ->
                                                val targetW = 1080
                                                val scale = targetW.toFloat() / page.width.toFloat()
                                                val w = targetW
                                                val h = (page.height * scale).toInt().coerceAtLeast(1)
                                                val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                                b.eraseColor(Color.WHITE)
                                                page.render(b, null, android.graphics.Matrix().apply { setScale(scale, scale) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                // v1.9: تصحیح چرخش PDFهای ۱۸۰° آپلودشده.
                                                val deg = com.hamyareman.ir.platform.feature.study.PdfRotations.degrees[fileId] ?: 0
                                                if (deg % 360 != 0) {
                                                    val m = android.graphics.Matrix().apply { postRotate(deg.toFloat()) }
                                                    Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
                                                } else {
                                                    b
                                                }
                                            }
                                        }
                                    } catch (e: Exception) { null }
                                    if (rendered != null) {
                                        synchronized(pageCache) { pageCache[index] = rendered }
                                        bmp = rendered
                                    }
                                }
                            }
                            if (bmp == null) {
                                Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.padding(16.dp))
                                }
                            } else {
                                Image(
                                    bitmap = bmp!!.asImageBitmap(),
                                    contentDescription = "صفحه ${index + 1}",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Text(
                                "صفحه ${index + 1} از ${st.pageCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }
            PdfState.Idle -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
