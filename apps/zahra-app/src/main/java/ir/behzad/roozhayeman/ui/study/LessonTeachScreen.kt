package ir.behzad.roozhayeman.ui.study

import android.graphics.Bitmap
import android.graphics.Color
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ir.behzad.platform.core.common.LocalStore
import ir.behzad.platform.core.common.toPersianDigits
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.feature.playback.PlaybackController
import ir.behzad.platform.feature.study.BookModuleRegistry
import ir.behzad.platform.feature.study.StudyPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs

internal val TEACH_SPEEDS = listOf(0.75f, 0.9f, 1f, 1.25f)

/** یک فایل صوتی قابل‌پخش در صفحه‌ی تدریس/خلاصه‌ها. */
internal data class TeachTrack(val label: String, val fileId: String, val cacheKey: String)

internal fun teachTracksOf(pack: StudyPack): List<TeachTrack> = buildList {
    if (pack.audioFileId.isNotBlank()) add(TeachTrack("صوت درس", pack.audioFileId, "${pack.packId}_AUDIO.mp3"))
    if (pack.audio2FileId.isNotBlank()) add(TeachTrack(pack.audio2Title.ifBlank { "مقدمه" }, pack.audio2FileId, "${pack.packId}_INTRO.mp3"))
}

internal fun teachMmss(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000
    return toPersianDigits(String.format(Locale.US, "%d:%02d", s / 60, s % 60))
}

internal fun teachSpeedLabel(v: Float): String = when (v) {
    0.75f -> "۰٫۷۵×"
    0.9f -> "۰٫۹×"
    1f -> "۱×"
    1.25f -> "۱٫۲۵×"
    else -> "${v}×"
}

/**
 * صفحه‌ی «تدریس» هر درس — طبق بازخورد مصوب:
 *  ۱) پلیر صوت «بالای صفحه»: تک‌پلیر (فقط یکی پخش می‌شود)، حافظه‌دار (موقعیت و
 *     سرعت هر ترک جداگانه)، با اعلان/کنترل قفل‌صفحه (سرویس رسانه)، پخش از سرور
 *     یا فایل رمزشده‌ی روی گوشی (دانلود/حذف)؛
 *  ۲) زیر پلیر «خود کتاب» باز می‌شود (PDF صفحه‌به‌صفحه) و صوت روی آن پخش می‌ماند؛
 *  ۳) بدون دکمه‌ی فلش‌کارت/آزمون/جزوه — مطالعه فقط بعد از اتمام اولین دوره‌ی
 *     تدریس از صفحه‌ی کتاب باز می‌شود؛
 *  ۴) همه‌ی رویدادها (نشست، ثانیه‌ی شنیدن، پرش، اتمام دوره) در TeachStats ثبت می‌شود.
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
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { Text("این درس پیدا نشد.") }
        return
    }

    AppTopBar(title = "تدریس — ${pack.title}", onBack = onBack)
    // پلیر همیشه «بالای صفحه» ثابت می‌ماند و کتاب (PDF) زیرش اسکرول می‌شود.
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val tracks = teachTracksOf(pack)
        if (tracks.isNotEmpty()) TeachAudioBar(packId = packId, screenTitle = pack.title, tracks = tracks)

        // اگر درس ویدیو دارد، زیر پلیر صوت پخش می‌شود (با شمارش تماشا/پرش و کش رمزشده).
        if (StudyMedia.videoIds(packId).isNotEmpty()) {
            LessonVideoSection(packId = packId, packTitle = pack.title)
        }

        TeachPdfPages(modifier = Modifier.weight(1f), fileId = pack.pdfFileName, pack = pack)
    }
}

/**
 * پلیر صوتِ بالای صفحه — تک‌پلیر رسانه‌ای با اعلان:
 *  - موتور پخش: [PlaybackController] (media3 + سرویس MediaSession → اعلان قفل‌صفحه)؛
 *  - فقط یک ترک در لحظه؛ سوییچ چیپ = توقف قبلی و ادامه‌ی جدید؛
 *  - موقعیت هر ترک جداگانه ذخیره و هنگام پخش محلی بازیابی می‌شود؛ سرعت هم حافظه‌دار؛
 *  - «پخش از سرور» مستقیم استریم می‌کند؛ «دانلود» فایل را رمزشده نگه می‌دارد
 *    (MediaVault) و پخش بعدی محلی از روی گاوصندوق انجام می‌شود؛ «حذف» پاکش می‌کند؛
 *  - ثانیه‌ی شنیدن/پرش‌های >۳ثانیه/اتمام دوره → TeachStats (غیرقابل ویرایش).
 */
@Composable
internal fun TeachAudioBar(packId: String, screenTitle: String, tracks: List<TeachTrack>) {
    val context = LocalContext.current
    val store = remember { LocalStore(context, "hamyar_teach") }
    val scope = rememberCoroutineScope()
    val playback = remember(packId) { PlaybackController(context) }
    val state by playback.state.collectAsState()

    var activeIdx by remember {
        mutableIntStateOf(store.getString("teach_${packId}_track", "0").toIntOrNull()?.coerceIn(0, tracks.size - 1) ?: 0)
    }
    var speed by remember { mutableFloatStateOf(store.getString("teach_${packId}_speed", "1").toFloatOrNull() ?: 1f) }
    var posMs by remember { mutableLongStateOf(0L) }
    var downloading by remember { mutableStateOf(false) }
    var progressPct by remember { mutableIntStateOf(-1) }
    var msg by remember { mutableStateOf<String?>(null) }
    var cacheTick by remember { mutableIntStateOf(0) }
    var loadedKey by remember { mutableStateOf<String?>(null) }
    var listenAccumMs by remember { mutableLongStateOf(0L) }
    var lastSaveMs by remember { mutableLongStateOf(0L) }
    var dragMs by remember { mutableLongStateOf(-1L) }
    var quiet by remember { mutableStateOf(false) }

    val track = tracks[activeIdx]
    LaunchedEffect(packId, tracks.size) {
        // شرط اتمام دوره = کامل‌شدن همه‌ی رسانه‌های درس: صوت‌ها + ویدیو (اگر دارد).
        val expected = tracks.size + (if (StudyMedia.videoIds(packId).isNotEmpty()) 1 else 0)
        TeachStats.expectMedia(context, packId, expected.coerceAtLeast(1))
    }

    fun posKey(t: TeachTrack) = "teach_${packId}_${t.cacheKey}_pos"
    fun savedPos(t: TeachTrack) = store.getString(posKey(t), "0").toLongOrNull() ?: 0L
    fun savePos(t: TeachTrack, p: Long) { if (p > 0) store.putString(posKey(t), p.toString()) else store.remove(posKey(t)) }
    fun cached(t: TeachTrack) = cacheTick >= 0 && MediaVault.isCached(context, t.cacheKey)

    /** «زمان درس» — سکوتِ اجباری پلیر دروس (کلید سراسری از «بیشتر»). */
    fun quietOn(): Boolean = store.getString("quiet_mode", "0") == "1"

    LaunchedEffect(packId) { TeachStats.enter(context, packId) }

    var forceServer by remember { mutableStateOf(false) }

    fun startTrack(t: TeachTrack, autoplay: Boolean, fromServer: Boolean = false) {
        msg = null
        if (quietOn()) {
            msg = "🔇 «زمان درس» روشن است — تا خاموشش کنی، پخش صدا فعال نمی‌شود."
            return
        }
        scope.launch {
            val ok = playback.connect()
            if (!ok) {
                msg = "اتصال به سرویس پخش ممکن نشد؛ یک‌بار دیگر امتحان کن."
                return@launch
            }
            // صف همه‌ی ترک‌های درس — اعلان سیستمی دکمه‌ی قبلی/بعدی می‌دهد.
            val items = tracks.map { tr ->
                val useLocal = !forceServer && MediaVault.isCached(context, tr.cacheKey) && !(fromServer && tr.cacheKey == t.cacheKey)
                MediaItem.Builder()
                    .setMediaId(tr.cacheKey)
                    .setUri(if (useLocal) MediaVault.localUrl(context, tr.cacheKey) else StudyMedia.viewUrl(tr.fileId))
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(screenTitle).setArtist(tr.label).build())
                    .build()
            }
            playback.setMediaItems(items, tracks.indexOf(t).coerceAtLeast(0), if (cached(t) && !fromServer) savedPos(t) else 0L)
            playback.setSpeed(speed)
            if (autoplay) playback.play()
        }
    }

    // نظرسنجی موقعیت + آمار شنیدن + ذخیره‌ی دوره‌ای موقعیت (~۴ ثانیه).
    LaunchedEffect(state.playing, loadedKey) {
        while (true) {
            delay(500)
            // «زمان درس» فعال شد؟ هر صدایی از پلیر دروس فوراً متوقف می‌شود.
            if (quietOn()) {
                quiet = true
                if (state.playing) {
                    runCatching { playback.pause() }
                    savePos(track, playback.positionMs)
                    msg = "🔇 «زمان درس» فعال است — پخش متوقف شد."
                }
                continue
            }
            quiet = false
            if (!state.playing) continue
            posMs = playback.positionMs
            listenAccumMs += 500
            if (listenAccumMs >= 5000) {
                val addSec = (listenAccumMs / 1000).toInt()
                TeachStats.addListen(context, packId, addSec, (state.durationMs / 1000).toInt())
                listenAccumMs = 0
            }
            if (posMs - lastSaveMs >= 4000 || posMs < lastSaveMs) {
                loadedKey?.let { key -> tracks.firstOrNull { it.cacheKey == key }?.let { savePos(it, posMs) } }
                lastSaveMs = posMs
            }
            // اگر از اعلان (قبلی/بعدی) ترک عوض شد، UI همگام شود.
            val cur = playback.currentMediaId()
            if (cur != null && cur != loadedKey) {
                val idx = tracks.indexOfFirst { it.cacheKey == cur }
                if (idx >= 0 && idx != activeIdx) {
                    activeIdx = idx
                    store.putString("teach_${packId}_track", idx.toString())
                }
                loadedKey = cur
                lastSaveMs = 0L
                posMs = 0L
            }
        }
    }

    // تشخیص پایان ترک = پایان اولین دوره‌ی تدریس (حتی در چند نشست، وقتی تا انتها برسد).
    LaunchedEffect(state.playing, posMs, state.durationMs) {
        val dur = state.durationMs
        if (!state.playing && state.hasMedia && dur > 0 && posMs > 0 && posMs >= dur - 900) {
            val key = loadedKey ?: track.cacheKey
            TeachStats.markTrackDone(context, packId, key)
            loadedKey?.let { k -> tracks.firstOrNull { it.cacheKey == k }?.let { savePos(it, 0L) } }
        }
    }

    DisposableEffect(packId) {
        onDispose {
            loadedKey?.let { key -> tracks.firstOrNull { it.cacheKey == key }?.let { savePos(it, posMs) } }
            runCatching { playback.pause() }
            playback.release()
        }
    }

    // اگر پخشِ محلی (گاوصندوق) خطا داد، بی‌سروصدا از سرور ادامه بده.
    LaunchedEffect(state.error) {
        val err = state.error ?: return@LaunchedEffect
        if (!forceServer && loadedKey != null) {
            forceServer = true
            msg = "پخش محلی ممکن نشد — از سرور ادامه می‌دهیم."
            startTrack(track, autoplay = true, fromServer = true)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            // انتخاب ترک — سوییچ = توقف قبلی، ادامه/پخش جدید (فقط یکی).
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tracks.forEachIndexed { i, t ->
                    FilterChip(
                        selected = i == activeIdx,
                        onClick = {
                            if (i != activeIdx) {
                                loadedKey?.let { key -> tracks.firstOrNull { it.cacheKey == key }?.let { savePos(it, posMs) } }
                                activeIdx = i
                                store.putString("teach_${packId}_track", i.toString())
                                val wasPlaying = state.playing
                                runCatching { playback.pause() }
                                startTrack(tracks[i], autoplay = wasPlaying)
                            }
                        },
                        label = { Text((if (cached(t)) "✓ " else "") + t.label) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = {
                    if (quietOn()) {
                        msg = "🔇 «زمان درس» روشن است — از «بیشتر» خاموشش کن."
                    } else if (state.playing) {
                        playback.pause()
                        savePos(track, posMs)
                    } else {
                        // اگر سرویس/اتصال افتاده باشد (مثلاً بعد از مکث طولانی)،
                        // اول دوباره وصل و آماده می‌کنیم و بعد پخش — از همان جای حافظه.
                        scope.launch {
                            if (!playback.connect()) {
                                msg = "اتصال به سرویس پخش ممکن نشد؛ دوباره امتحان کن."
                                return@launch
                            }
                            if (loadedKey == track.cacheKey && state.hasMedia && !state.error.isNullOrBlank()) {
                                forceServer = true
                            }
                            if (loadedKey == track.cacheKey && state.hasMedia && !forceServer) {
                                playback.play()
                            } else {
                                startTrack(track, autoplay = true)
                            }
                        }
                    }
                }) { Text(if (state.playing && loadedKey == track.cacheKey) "⏸ توقف" else "▶ پخش") }
                TextButton(onClick = {
                    runCatching { playback.pause(); playback.seekTo(0L) }
                    savePos(track, 0L)
                    posMs = 0
                }) { Text("⏹") }
                if (loadedKey == track.cacheKey && state.durationMs > 0) {
                    Text(
                        "${teachMmss(if (dragMs >= 0) dragMs else posMs)} / ${teachMmss(state.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { startTrack(track, autoplay = state.playing, fromServer = true) }) {
                    Text("🌐 سرور")
                }
            }
            val seekUnlocked = remember { mutableStateOf(TeachStats.isDone(context, packId)) }
            var showSeekDialog by remember { mutableStateOf(false) }
            if (showSeekDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showSeekDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showSeekDialog = false }) { Text("باشه") }
                    },
                    title = { Text("تا یک بار کامل نشده، سیک قفل است 🔒") },
                    text = { Text("یک‌بار که تا آخر گوشش کنی، جابه‌جایی آزاد می‌شود — همون‌جوری که دوست داری 🌱") },
                )
            }
            if (loadedKey == track.cacheKey && state.durationMs > 0) {
                Box {
                    Slider(
                        value = (((if (dragMs >= 0) dragMs else posMs).toFloat()) / state.durationMs).coerceIn(0f, 1f),
                        enabled = seekUnlocked.value,
                        onValueChange = { dragMs = (it * state.durationMs).toLong() },
                    onValueChangeFinished = {
                        if (dragMs >= 0) {
                            if (abs(dragMs - posMs) > 3000) TeachStats.addJump(context, packId)
                            playback.seekTo(dragMs)
                            posMs = dragMs
                            savePos(track, dragMs)
                            dragMs = -1
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!seekUnlocked.value) {
                    // لایه‌ی لمس برای دیالوگِ قفلِ سیک (تا اتمام اولین دوره).
                    Box(
                        Modifier.matchParentSize().padding(vertical = 10.dp)
                            .androidClickable { showSeekDialog = true },
                    )
                }
                } // Box
            } // if(loadedKey)
            LaunchedEffect(Unit) { seekUnlocked.value = TeachStats.isDone(context, packId) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TEACH_SPEEDS.forEach { v ->
                    FilterChip(
                        selected = speed == v,
                        onClick = {
                            speed = v
                            runCatching { playback.setSpeed(v) }
                            store.putString("teach_${packId}_speed", v.toString())
                        },
                        label = { Text(teachSpeedLabel(v)) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    downloading -> {
                        LinearProgressIndicator(
                            progress = { (if (progressPct < 0) 0 else progressPct) / 100f },
                            modifier = Modifier.weight(1f).height(6.dp),
                        )
                        Text(
                            if (progressPct >= 0) "دانلود… ${toPersianDigits(progressPct.toString())}٪" else "دانلود…",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    cached(track) -> {
                        Text("✓ روی گوشی (رمزشده)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        var confirmDelete by remember { mutableStateOf(false) }
                        if (confirmDelete) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { confirmDelete = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        confirmDelete = false
                                        MediaVault.delete(context, track.cacheKey)
                                        cacheTick++
                                        msg = "فایل از حافظه‌ی گوشی حذف شد."
                                    }) { Text("حذف") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmDelete = false }) { Text("نگه‌دار") }
                                },
                                title = { Text("حذف فایل دانلودشده؟") },
                                text = { Text("پخش بعدی از سرور انجام می‌شود؛ هر وقت خواستی دوباره دانلود می‌کنی.") },
                            )
                        }
                        TextButton(onClick = { confirmDelete = true }) { Text("🗑 حذف") }
                    }
                    else -> TextButton(onClick = {
                        downloading = true; progressPct = -1
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    MediaVault.downloadEncrypted(
                                        context,
                                        StudyMedia.viewUrl(track.fileId),
                                        track.cacheKey,
                                    ) { pct -> progressPct = pct }
                                }
                                downloading = false; cacheTick++
                                msg = "دانلود شد — از این به بعد محلی و رمزشده پخش می‌شود."
                            } catch (e: Exception) {
                                downloading = false
                                msg = "دانلود ناموفق بود؛ اینترنت را چک کن."
                            }
                        }
                    }) { Text("⬇ دانلود برای پخش محلی") }
                }
            }
            if (quiet) {
                Text(
                    "🔇 زمان درس — پخش صوت دروس خاموش است (از «بیشتر» خاموش کن)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            msg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

// ------------------------------------------------------------- ویدیو

/** بخش ویدیوی تدریس — استریم از سرور یا پخش محلیِ رمزشده + شمارش تماشا/پرش. */
@Composable
private fun LessonVideoSection(packId: String, packTitle: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ids = remember(packId) { StudyMedia.videoIds(packId) }
    var current by remember(packId) { mutableIntStateOf(0) }
    var useLocal by remember(packId) { mutableStateOf(false) }
    var downloading by remember(packId) { mutableStateOf(false) }
    var progressPct by remember(packId) { mutableIntStateOf(-1) }
    var cacheTick by remember(packId) { mutableIntStateOf(0) }
    var msg by remember(packId) { mutableStateOf<String?>(null) }

    val fileId = ids[current]

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎬 ویدیو", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            if (ids.size > 1) {
                ids.indices.forEach { i ->
                    FilterChip(
                        selected = i == current,
                        onClick = { current = i; useLocal = false; msg = null },
                        label = { Text(toPersianDigits((i + 1).toString())) },
                    )
                }
            }
        }
        if (downloading) {
            LinearProgressIndicator(
                progress = { (if (progressPct < 0) 0 else progressPct) / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Text(
                if (progressPct >= 0) "دانلود ویدیو… ${toPersianDigits(progressPct.toString())}٪" else "دانلود ویدیو…",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val uri = if (cacheTick >= 0 && useLocal && MediaVault.isCached(context, fileId)) {
            MediaVault.localUrl(context, fileId)
        } else {
            StudyMedia.viewUrl(fileId)
        }
        LessonVideoPlayer(packId = packId, packTitle = packTitle, fileId = fileId, uri = uri)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { useLocal = false; msg = null }) { Text("🌐 از سرور") }
            when {
                downloading -> Unit
                MediaVault.isCached(context, fileId) -> {
                    Text("✓ روی گوشی", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (!useLocal) {
                        TextButton(onClick = { useLocal = true }) { Text("▶ محلی") }
                    }
                    var confirmDelV by remember { mutableStateOf(false) }
                    if (confirmDelV) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { confirmDelV = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    confirmDelV = false
                                    MediaVault.delete(context, fileId)
                                    useLocal = false; cacheTick++
                                    msg = "ویدیو از حافظه‌ی گوشی حذف شد."
                                }) { Text("حذف") }
                            },
                            dismissButton = { TextButton(onClick = { confirmDelV = false }) { Text("نگه‌دار") } },
                            title = { Text("حذف ویدیوی دانلودشده؟") },
                            text = { Text("پخش بعدی از سرور انجام می‌شود.") },
                        )
                    }
                    TextButton(onClick = { confirmDelV = true }) { Text("🗑") }
                }
                else -> TextButton(onClick = {
                    downloading = true; progressPct = -1
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                MediaVault.downloadEncrypted(context, StudyMedia.viewUrl(fileId), fileId) { pct -> progressPct = pct }
                            }
                            downloading = false; cacheTick++; useLocal = true
                            msg = "دانلود شد — پخش محلی رمزشده."
                        } catch (e: Exception) {
                            downloading = false
                            msg = "دانلود ناموفق بود."
                        }
                    }
                }) { Text("⬇ دانلود") }
            }
            msg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/**
 * پلیر ویدیو (ExoPlayer) — تماشای واقعی (هر ثانیه هنگام پخش) و پرش‌های >۳ ثانیه
 * شمرده می‌شود؛ اتمام ≥۹۵٪ = پایان اولین دوره‌ی تدریس.
 * نکته: player فقط روی رشته‌ی اصلی لمس می‌شود (تیک کوروتین، نه Timer).
 */
@Composable
private fun LessonVideoPlayer(packId: String, packTitle: String, fileId: String, uri: String) {
    val context = LocalContext.current
    var player by remember(fileId, uri) { mutableStateOf<ExoPlayer?>(null) }
    var watchAccum by remember(fileId, uri) { mutableLongStateOf(0L) }

    DisposableEffect(fileId, uri) {
        val p = ExoPlayer.Builder(context).build().apply {
            setMediaItem(
                MediaItem.Builder().setUri(uri).setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder().setTitle(packTitle).build(),
                ).build(),
            )
            prepare()
            playWhenReady = false
        }
        player = p
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (p.duration > 0) TeachStats.markTrackDone(context, packId, fileId)
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK &&
                    abs(newPosition.positionMs - oldPosition.positionMs) > 3000
                ) {
                    TeachStats.addJump(context, packId)
                }
            }
        }
        p.addListener(listener)
        onDispose {
            p.removeListener(listener)
            p.release()
            player = null
        }
    }

    // تیک تماشای واقعی — هر ثانیه فقط هنگام پخش؛ ثبت هر ۵ ثانیه؛ ۹۵٪ = پایان دوره.
    // «زمان درس» فعال = صدای ویدیو هم بی‌صدا می‌شود.
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        val qstore = LocalStore(context, "hamyar_teach")
        while (true) {
            delay(1000)
            p.volume = if (qstore.getString("quiet_mode", "0") == "1") 0f else 1f
            if (p.isPlaying) {
                watchAccum += 1000
                val durSec = (p.duration.takeIf { it > 0 } ?: 0L) / 1000
                if (watchAccum % 5000L == 0L) {
                    TeachStats.addVideo(context, packId, 5, durSec.toInt())
                    val d = p.duration
                    if (d > 0 && p.currentPosition * 100 / d >= 95) {
                        TeachStats.markTrackDone(context, packId, fileId)
                    }
                }
            }
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { useController = true } },
        update = { view -> view.player = player },
        modifier = Modifier.fillMaxWidth().height(210.dp),
    )
}

// ------------------------------------------------------------- کتاب (PDF)

private fun Modifier.androidClickable(onClick: () -> Unit): Modifier =
    this.pointerInput(Unit) { detectTapGestures { onClick() } }

private sealed class TeachPdfState {
    data object Idle : TeachPdfState()
    data class Downloading(val pct: Int) : TeachPdfState()
    data class Ready(val pageCount: Int) : TeachPdfState()
    data class Error(val message: String) : TeachPdfState()
}

/** کش LRU صفحه‌های PDF (~۸ صفحه) تا حافظه کنترل بماند. */
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

/**
 * کتابِ درس — دانلود PDF از باکت و رندر صفحه‌به‌صفحه (PdfRenderer).
 * اگر PDF هنوز روی سرور نبود، متن سکشن‌های غیرامتحانی جایگزین می‌شود.
 */
@Composable
internal fun TeachPdfPages(modifier: Modifier = Modifier, fileId: String, pack: StudyPack) {
    val ctx = LocalContext.current
    var state by remember(fileId) { mutableStateOf<TeachPdfState>(TeachPdfState.Idle) }
    val pageCache = remember(fileId) { TeachPageCache() }
    var renderer by remember(fileId) { mutableStateOf<PdfRenderer?>(null) }
    val renderLock = remember(fileId) { Any() }

    LaunchedEffect(fileId) {
        if (fileId.isBlank()) {
            state = TeachPdfState.Error("برای این بخش، کتابِ PDF جداگانه‌ای نیست.")
            return@LaunchedEffect
        }
        try {
            val cacheDir = File(ctx.filesDir, "media/pdf-cache").apply { mkdirs() }
            val target = File(cacheDir, fileId)
            // فایل ناقص/خراب قبلی → دوباره دانلود می‌شود (خودترمیمی).
            fun isValid(f: File) = f.length() > 1024 && runCatching {
                f.inputStream().use { val h = ByteArray(5); it.read(h); String(h) == "%PDF-" }
            }.getOrDefault(false)
            if (!isValid(target)) {
                target.delete()
                state = TeachPdfState.Downloading(0)
                val conn = (URL(StudyMedia.viewUrl(fileId)).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true
                }
                if (conn.responseCode !in 200..299) {
                    state = TeachPdfState.Error("دریافت PDF ممکن نشد (کد ${conn.responseCode}).")
                    return@LaunchedEffect
                }
                val total = conn.contentLengthLong
                val part = File(cacheDir, "$fileId.part")
                conn.inputStream.use { input ->
                    java.io.FileOutputStream(part).use { out ->
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
                if (!isValid(part)) {
                    part.delete()
                    state = TeachPdfState.Error("فایل PDF ناقص رسید؛ یک‌بار دیگر تلاش کن.")
                    return@LaunchedEffect
                }
                if (!part.renameTo(target)) {
                    part.copyTo(target, overwrite = true); part.delete()
                }
            }
            val fd = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
            val r = PdfRenderer(fd)
            synchronized(renderLock) { renderer = r }
            state = TeachPdfState.Ready(r.pageCount)
        } catch (e: Exception) {
            // کش خراب را پاک کن تا دفعه‌ی بعد از نو دانلود شود.
            runCatching { File(File(ctx.filesDir, "media/pdf-cache"), fileId).delete() }
            state = TeachPdfState.Error("بازکردن PDF ناموفق بود؛ دوباره تلاش کن.")
        }
    }

    when (val st = state) {
        is TeachPdfState.Error -> {
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
                pack.sections.filter { it.kind != "exam" }.forEach { sec ->
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
        is TeachPdfState.Downloading -> Card(modifier.fillMaxWidth()) {
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
