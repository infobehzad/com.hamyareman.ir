package ir.behzad.roozhayeman.ui.study

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.behzad.platform.core.common.JalaliDate
import ir.behzad.platform.core.common.LocalStore
import ir.behzad.platform.core.common.toPersianDigits
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.feature.study.BookModuleRegistry
import ir.behzad.roozhayeman.LocalAppContainer
import ir.behzad.roozhayeman.ui.hub.HubBody
import ir.behzad.roozhayeman.ui.hub.HubHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs

private val TEACH_SPEEDS = listOf(0.75f, 0.9f, 1f, 1.25f)

/** یک فایل صوتی قابل‌پخش در صفحه‌ی تدریس. */
private data class TeachTrack(val label: String, val fileId: String, val cacheKey: String)

private fun teachMmss(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000
    return toPersianDigits(String.format(Locale.US, "%d:%02d", s / 60, s % 60))
}

private fun teachSpeedLabel(v: Float): String = when (v) {
    0.75f -> "۰٫۷۵×"
    0.9f -> "۰٫۹×"
    1f -> "۱×"
    1.25f -> "۱٫۲۵×"
    else -> "${v}×"
}

/**
 * صفحه‌ی «تدریس» هر درس — طبق بازخورد واقعی:
 *  ۱) پلیر صوت «بالای صفحه»: یک پلیر برای همه‌ی صوت‌های درس (درس/مقدمه)؛
 *     فقط یکی در لحظه پخش می‌شود، موقعیت و سرعت هر ترک «حافظه‌دار» است
 *     (بعد از بستن اپ از همان‌جا ادامه می‌دهد)؛
 *  ۲) زیر پلیر، «خود کتاب» باز می‌شود: PDF درس از سرور دانلود و صفحه‌به‌صفحه
 *     رندر می‌شود؛ صوت WHILE خواندن کتاب پخش می‌ماند؛
 *  ۳) اگر PDF درس هنوز روی سرور نبود، متن سکشن‌ها به‌عنوان جایگزین می‌آید.
 */
@Composable
fun LessonTeachScreen(
    packId: String,
    onBack: () -> Unit,
    onStudy: (String) -> Unit,
    onPdf: (String) -> Unit,
) {
    val pack = remember(packId) { BookModuleRegistry.pack(packId) }
    if (pack == null) {
        AppTopBar(title = "تدریس درس", onBack = onBack)
        HubBody { Text("این درس پیدا نشد.") }
        return
    }

    AppTopBar(title = "تدریس — ${pack.title}", onBack = onBack)
    // پلیر همیشه «بالای صفحه» ثابت می‌ماند و کتاب (PDF) زیرش اسکرول می‌شود.
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val tracks = buildList {
            if (pack.audioFileId.isNotBlank()) add(TeachTrack("صوت درس", pack.audioFileId, "${packId}_AUDIO.mp3"))
            if (pack.audio2FileId.isNotBlank()) add(TeachTrack(pack.audio2Title.ifBlank { "مقدمه" }, pack.audio2FileId, "${packId}_INTRO.mp3"))
        }
        if (tracks.isNotEmpty()) TeachAudioBar(packId = packId, tracks = tracks)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onStudy(packId) }, modifier = Modifier.weight(1f)) { Text("🎯 فلش‌کارت و آزمون") }
            OutlinedButton(onClick = { onPdf(packId) }, modifier = Modifier.weight(1f)) { Text("📕 فقط PDF") }
        }

        val container = LocalAppContainer.current
        val today = remember { JalaliDate.todayIso() }
        val mastery = remember(packId) { runCatching { container.studyProgress.masteryPct(pack) }.getOrDefault(0) }
        val due = remember(packId) { runCatching { container.studyProgress.dueCards(pack, today).size }.getOrDefault(0) }
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("پیشرفت درس", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(progress = { mastery / 100f }, modifier = Modifier.weight(1f).height(8.dp))
                Spacer(Modifier.width(6.dp))
                Text("${toPersianDigits(mastery.toString())}٪ · مرور امروز: ${toPersianDigits(due.toString())}", style = MaterialTheme.typography.labelSmall)
            }
        }

        TeachPdfPages(modifier = Modifier.weight(1f), fileId = pack.pdfFileName, pack = pack)
    }
}

/**
 * پلیر صوتِ بالای صفحه — تک‌پلیر، حافظه‌دار:
 *  - انتخاب ترک (درس/مقدمه) با چیپ؛ سوییچ = توقف قبلی و پخش جدید (فقط یکی)؛
 *  - موقعیت هر ترک جداگانه ذخیره و هنگام prepare بازیابی می‌شود؛
 *  - سرعت پخش هم به‌خاطر سپرده می‌شود؛
 *  - اگر فایل کش نشده باشد اول با دکمه/خودکار از سرور دانلود می‌شود (با درصد).
 */
@Composable
private fun TeachAudioBar(packId: String, tracks: List<TeachTrack>) {
    val context = LocalContext.current
    val store = remember { LocalStore(context, "hamyar_teach") }
    val scope = rememberCoroutineScope()

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var activeIdx by remember { mutableIntStateOf(store.getString("teach_${packId}_track", "0").toIntOrNull()?.coerceIn(0, tracks.size - 1) ?: 0) }
    var prepared by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableFloatStateOf(store.getString("teach_${packId}_speed", "1").toFloatOrNull() ?: 1f) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(-1) }
    var msg by remember { mutableStateOf<String?>(null) }
    var loadedKey by remember { mutableStateOf<String?>(null) }

    val track = tracks[activeIdx]
    val cacheDir = remember { File(context.cacheDir, "lesson-media") }

    fun posKey(t: TeachTrack) = "teach_${packId}_${t.cacheKey}_pos"
    fun savedPos(t: TeachTrack) = store.getString(posKey(t), "0").toLongOrNull() ?: 0L
    fun savePos(t: TeachTrack, p: Long) { if (p > 0) store.putString(posKey(t), p.toString()) }
    fun cached(t: TeachTrack) = File(cacheDir, t.cacheKey).let { it.exists() && it.length() > 0 }

    fun applySpeed(p: MediaPlayer?, v: Float) {
        p ?: return
        runCatching { p.playbackParams = p.playbackParams.setSpeed(v) }
    }

    fun prepareAndPlay(t: TeachTrack, autoplay: Boolean) {
        player?.release()
        val p = MediaPlayer()
        player = p
        prepared = false
        playing = false
        pos = savedPos(t)
        dur = 0
        loadedKey = t.cacheKey
        runCatching {
            p.setDataSource(File(cacheDir, t.cacheKey).absolutePath)
            p.setOnPreparedListener { mp ->
                prepared = true
                dur = mp.duration.toLong()
                val resume = savedPos(t)
                if (resume > 0 && resume < mp.duration) mp.seekTo(resume.toInt())
                applySpeed(mp, speed)
                if (autoplay) { mp.start(); playing = true }
            }
            p.setOnCompletionListener { savePos(t, 0); playing = false }
            p.prepareAsync()
        }.onFailure { msg = "پخش این فایل ممکن نشد."; playing = false }
    }

    fun loadTrack(t: TeachTrack, autoplay: Boolean) {
        msg = null
        if (cached(t)) {
            prepareAndPlay(t, autoplay)
        } else {
            downloading = true; progress = -1
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        cacheDir.mkdirs()
                        val conn = URL(StudyMedia.viewUrl(t.fileId)).openConnection() as HttpURLConnection
                        conn.connectTimeout = 15000; conn.readTimeout = 30000
                        conn.instanceFollowRedirects = true
                        conn.connect()
                        if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                        val total = conn.contentLength
                        conn.inputStream.use { input ->
                            val tmp = File(cacheDir, t.cacheKey + ".part")
                            tmp.outputStream().use { out ->
                                val buf = ByteArray(32 * 1024)
                                var read: Int; var done = 0
                                while (input.read(buf).also { read = it } > 0) {
                                    out.write(buf, 0, read); done += read
                                    if (total > 0) progress = done * 100 / total
                                }
                            }
                            tmp.renameTo(File(cacheDir, t.cacheKey))
                        }
                    }
                    downloading = false; progress = -1
                    prepareAndPlay(t, autoplay)
                } catch (e: Exception) {
                    downloading = false; progress = -1
                    msg = "دریافت از سرور ناموفق بود؛ اینترنت را چک کن و دوباره روی ترک بزن."
                }
            }
        }
    }

    // ذخیره‌ی موقعیت هنگام خروج از صفحه.
    DisposableEffect(packId) {
        onDispose {
            loadedKey?.let { key -> tracks.firstOrNull { it.cacheKey == key }?.let { savePos(it, pos) } }
            player?.release()
            player = null
        }
    }

    // تیک موقعیت + ذخیره‌ی دوره‌ای (~هر ۴ ثانیه).
    LaunchedEffect(playing, prepared) {
        while (playing && prepared) {
            player?.let { p ->
                pos = p.currentPosition.toLong()
                if (pos % 4000L < 600L) savePos(track, pos)
            }
            delay(500)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tracks.forEachIndexed { i, t ->
                    FilterChip(
                        selected = i == activeIdx,
                        onClick = {
                            if (i != activeIdx) {
                                loadedKey?.let { key -> tracks.firstOrNull { it.cacheKey == key }?.let { savePos(it, pos) } }
                                activeIdx = i
                                store.putString("teach_${packId}_track", i.toString())
                                loadTrack(t, autoplay = playing)
                            }
                        },
                        label = { Text((if (cached(t)) "✓ " else "") + t.label) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val p = player
                    if (p != null && prepared && loadedKey == track.cacheKey) {
                        if (playing) { p.pause(); playing = false; savePos(track, pos) }
                        else { p.start(); playing = true }
                    } else {
                        loadTrack(track, autoplay = true)
                    }
                }) { Text(if (playing) "⏸ توقف" else "▶ پخش") }
                TextButton(onClick = {
                    savePos(track, 0)
                    player?.release(); player = null
                    prepared = false; playing = false; pos = 0
                }) { Text("⏹") }
                if (prepared && loadedKey == track.cacheKey) {
                    Text("${teachMmss(pos)} / ${teachMmss(dur)}", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (prepared && loadedKey == track.cacheKey && dur > 0) {
                Slider(
                    value = (pos.toFloat() / dur).coerceIn(0f, 1f),
                    onValueChange = { frac ->
                        val target = (frac * dur).toInt()
                        player?.seekTo(target)
                        pos = target.toLong()
                        savePos(track, pos)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TEACH_SPEEDS.forEach { v ->
                    FilterChip(
                        selected = speed == v,
                        onClick = { speed = v; applySpeed(player, v); store.putString("teach_${packId}_speed", v.toString()) },
                        label = { Text(teachSpeedLabel(v)) },
                    )
                }
            }
            if (downloading) {
                Spacer(Modifier.height(4.dp))
                if (progress >= 0) {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("دریافت… ${toPersianDigits(progress.toString())}٪", style = MaterialTheme.typography.bodySmall)
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("در حال دریافت از سرور…", style = MaterialTheme.typography.bodySmall)
                }
            }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private sealed class TeachPdfState {
    data object Idle : TeachPdfState()
    data class Downloading(val pct: Int) : TeachPdfState()
    data class Ready(val pageCount: Int) : TeachPdfState()
    data class Error(val message: String) : TeachPdfState()
}

/** کش LRU صفحه‌های PDF (~۸ صفحه). */
private class TeachPageCache(private val maxPages: Int = 8) {
    private val map = LinkedHashMap<Int, Bitmap>(16, 0.75f, true)
    operator fun get(index: Int): Bitmap? = synchronized(this) { map[index] }
    operator fun set(index: Int, value: Bitmap) {
        synchronized(this) {
            map[index] = value
            while (map.size > maxPages) map.remove(map.keys.first())
        }
    }
}

/** کتابِ درس — دانلود PDF از باکت و رندر صفحه‌به‌صفحه (PdfRenderer). */
@Composable
private fun TeachPdfPages(modifier: Modifier = Modifier, fileId: String, pack: ir.behzad.platform.feature.study.StudyPack) {
    val ctx = LocalContext.current
    var state by remember(fileId) { mutableStateOf<TeachPdfState>(TeachPdfState.Idle) }
    val pageCache = remember(fileId) { TeachPageCache() }
    var renderer by remember(fileId) { mutableStateOf<PdfRenderer?>(null) }
    val renderLock = remember(fileId) { Any() }

    LaunchedEffect(fileId) {
        if (fileId.isBlank()) {
            state = TeachPdfState.Error("این درس فایل PDF ندارد.")
            return@LaunchedEffect
        }
        try {
            val cacheDir = File(ctx.filesDir, "media/pdf-cache").apply { mkdirs() }
            val target = File(cacheDir, fileId)
            if (!target.exists() || target.length() < 1024) {
                state = TeachPdfState.Downloading(0)
                val conn = (URL(StudyMedia.viewUrl(fileId)).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true
                }
                if (conn.responseCode !in 200..299) {
                    state = TeachPdfState.Error("PDF این درس هنوز روی سرور نیست.")
                    return@LaunchedEffect
                }
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    java.io.FileOutputStream(target).use { out ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int; var done = 0L
                        while (input.read(buf).also { read = it } > 0) {
                            out.write(buf, 0, read); done += read
                            if (total > 0) {
                                val pct = ((done * 100) / total).toInt()
                                if (state is TeachPdfState.Downloading) state = TeachPdfState.Downloading(pct)
                            }
                        }
                    }
                }
            }
            val fd = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
            val r = PdfRenderer(fd)
            synchronized(renderLock) { renderer = r }
            state = TeachPdfState.Ready(r.pageCount)
        } catch (e: Exception) {
            state = TeachPdfState.Error("بازکردن PDF ناموفق بود؛ احتمالاً هنوز روی سرور آپلود نشده.")
        }
    }

    when (val st = state) {
        is TeachPdfState.Error -> {
            // PDF نیست → متن سکشن‌ها جایگزین (fallback خودکار)
            Column(
                modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("📕 کتاب درس", style = MaterialTheme.typography.titleMedium)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(st.message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "تا آپلودشدن PDF، متن درس را همین‌جا می‌بینی:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                pack.sections.forEach { sec ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(sec.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(sec.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        is TeachPdfState.Downloading -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("در حال آماده‌سازی کتاب… (${toPersianDigits(st.pct.toString())}٪)", style = MaterialTheme.typography.bodySmall)
            }
        }
        is TeachPdfState.Ready -> Column(modifier = modifier) {
            Text("📕 کتاب درس — ${toPersianDigits(st.pageCount.toString())} صفحه", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                                b
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
                                Image(bitmap = bmp!!.asImageBitmap(), contentDescription = "صفحه ${index + 1}", modifier = Modifier.fillMaxWidth())
                            }
                            Text(
                                "صفحه ${toPersianDigits((index + 1).toString())} از ${toPersianDigits(st.pageCount.toString())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
        TeachPdfState.Idle -> Card(modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}
