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
import androidx.media3.common.MediaItem
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

    val track = tracks[activeIdx]

    fun posKey(t: TeachTrack) = "teach_${packId}_${t.cacheKey}_pos"
    fun savedPos(t: TeachTrack) = store.getString(posKey(t), "0").toLongOrNull() ?: 0L
    fun savePos(t: TeachTrack, p: Long) { if (p > 0) store.putString(posKey(t), p.toString()) else store.remove(posKey(t)) }
    fun cached(t: TeachTrack) = cacheTick >= 0 && MediaVault.isCached(context, t.cacheKey)

    LaunchedEffect(packId) { TeachStats.enter(context, packId) }

    fun startTrack(t: TeachTrack, autoplay: Boolean, fromServer: Boolean = false) {
        msg = null
        scope.launch {
            val ok = playback.connect()
            if (!ok) {
                msg = "اتصال به سرویس پخش ممکن نشد؛ یک‌بار دیگر امتحان کن."
                return@launch
            }
            val local = cached(t) && !fromServer
            loadedKey = t.cacheKey
            // موقعیتِ حافظه فقط برای پخش محلی اعمال می‌شود (سرورِ ما Range را درست جواب می‌دهد).
            playback.setMedia(
                uri = if (local) MediaVault.localUrl(context, t.cacheKey) else StudyMedia.viewUrl(t.fileId),
                title = "$screenTitle — ${t.label}",
                startPositionMs = if (local) savedPos(t) else 0L,
            )
            playback.setSpeed(speed)
            if (autoplay) playback.play()
        }
    }

    // نظرسنجی موقعیت + آمار شنیدن + ذخیره‌ی دوره‌ای موقعیت (~۴ ثانیه).
    LaunchedEffect(state.playing, loadedKey) {
        while (state.playing) {
            delay(500)
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
        }
    }

    // تشخیص پایان ترک = پایان اولین دوره‌ی تدریس (حتی در چند نشست، وقتی تا انتها برسد).
    LaunchedEffect(state.playing, posMs, state.durationMs) {
        val dur = state.durationMs
        if (!state.playing && state.hasMedia && dur > 0 && posMs > 0 && posMs >= dur - 900) {
            TeachStats.markFirstPassDone(context, packId)
            loadedKey?.let { key -> tracks.firstOrNull { it.cacheKey == key }?.let { savePos(it, 0L) } }
        }
    }

    DisposableEffect(packId) {
        onDispose {
            loadedKey?.let { key -> tracks.firstOrNull { it.cacheKey == key }?.let { savePos(it, posMs) } }
            runCatching { playback.pause() }
            playback.release()
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
                    if (state.playing) {
                        playback.pause()
                        savePos(track, posMs)
                    } else if (loadedKey == track.cacheKey && state.hasMedia) {
                        playback.play()
                    } else {
                        startTrack(track, autoplay = true)
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
            if (loadedKey == track.cacheKey && state.durationMs > 0) {
                Slider(
                    value = (((if (dragMs >= 0) dragMs else posMs).toFloat()) / state.durationMs).coerceIn(0f, 1f),
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
            }
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
                        TextButton(onClick = {
                            MediaVault.delete(context, track.cacheKey)
                            cacheTick++
                            msg = "فایل از حافظه‌ی گوشی حذف شد."
                        }) { Text("🗑 حذف") }
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
                    TextButton(onClick = {
                        MediaVault.delete(context, fileId)
                        useLocal = false; cacheTick++
                        msg = "ویدیو از حافظه‌ی گوشی حذف شد."
                    }) { Text("🗑") }
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
        var watchAccum = 0L
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (p.duration > 0) TeachStats.markFirstPassDone(context, packId)
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
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            delay(1000)
            if (p.isPlaying) {
                watchAccum += 1000
                val durSec = (p.duration.takeIf { it > 0 } ?: 0L) / 1000
                if (watchAccum % 5000L == 0L) {
                    TeachStats.addVideo(context, packId, 5, durSec.toInt())
                    val d = p.duration
                    if (d > 0 && p.currentPosition * 100 / d >= 95) {
                        TeachStats.markFirstPassDone(context, packId)
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

