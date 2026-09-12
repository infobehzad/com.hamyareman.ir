package ir.behzad.roozhayeman.ui.study

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ir.behzad.platform.core.common.JalaliDate
import ir.behzad.platform.core.common.toPersianDigits
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.core.designsystem.PrimaryButton
import ir.behzad.platform.feature.study.BookModuleRegistry
import ir.behzad.roozhayeman.LocalAppContainer
import ir.behzad.roozhayeman.ui.hub.HubBody
import ir.behzad.roozhayeman.ui.hub.HubHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.Locale
import kotlin.math.abs

private val AUDIO_SPEEDS = listOf(0.75f, 0.9f, 1f, 1.25f)

private fun mmss(ms: Long): String {
    val s = (ms.coerceAtLeast(0L)) / 1000
    return toPersianDigits(String.format(Locale.US, "%d:%02d", s / 60, s % 60))
}

/**
 * «تدریس» هر درس — سه بخش روی هم:
 *  ۱) متن کامل درس (همان سکشن‌های ماژول کتاب) + دکمه‌ی PDF کتاب؛
 *  ۲) پلیر صوت درس: با دکمه‌ی «دریافت از سرور» فایل اگر کش نشده باشد دانلود و
 *     بعد پخش می‌شود — نه همه‌ی فایل‌ها یک‌جا. سرعت پخش ۰٫۷۵ تا ۱٫۲۵ و جابه‌جایی.
 *  ۳) آموزش تصویری: پلیر ویدیو با نوار دنبال‌کردن؛ پرش‌های نامتعارفِ بیش از ۳
 *     ثانیه شمرده و در گزارش همین درس ثبت می‌شود؛ نوار وضعیت زمانِ تماشای واقعی
 *     (نه طول ویدیو) را نشان می‌دهد.
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
    HubBody {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onStudy(packId) }, modifier = Modifier.weight(1f)) { Text("🎯 فلش‌کارت و آزمون") }
            OutlinedButton(onClick = { onPdf(packId) }, modifier = Modifier.weight(1f)) { Text("📄 جزوه و PDF") }
        }
        Spacer(Modifier.height(8.dp))

        LessonProgressStrip(packId = packId, packTitle = pack.title)

        Spacer(Modifier.height(8.dp))
        Text("متن درس", style = MaterialTheme.typography.titleMedium)
        pack.sections.forEach { s ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        s.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        if (pack.audioFileId.isNotBlank()) {
            LessonAudioPlayer(label = "صوت درس", fileId = pack.audioFileId, cacheKey = "${packId}_AUDIO.mp3")
            Spacer(Modifier.height(8.dp))
        }
        if (pack.audio2FileId.isNotBlank()) {
            LessonAudioPlayer(
                label = pack.audio2Title.ifBlank { "مقدمه‌ی درس" },
                fileId = pack.audio2FileId,
                cacheKey = "${packId}_INTRO.mp3",
            )
            Spacer(Modifier.height(8.dp))
        }

        val videos = remember(packId) { StudyMedia.videoIds(packId) }
        if (videos.isNotEmpty()) {
            Text("آموزش تصویری 🎬", style = MaterialTheme.typography.titleMedium)
            videos.forEachIndexed { idx, fid ->
                LessonVideoPlayer(
                    fileId = fid,
                    label = "ویدیوی ${toPersianDigits((idx + 1).toString())}",
                    packId = packId,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** نوار پیشرفتِ همین درس: تسلط (فلش‌کارت SM2) + کارت‌های امروز + آخرین آزمون‌ها. */
@Composable
private fun LessonProgressStrip(packId: String, packTitle: String) {
    val container = LocalAppContainer.current
    val pack = remember(packId) { BookModuleRegistry.pack(packId) } ?: return
    val today = remember { JalaliDate.todayIso() }
    val mastery = remember(packId) { runCatching { container.studyProgress.masteryPct(pack) }.getOrDefault(0) }
    val due = remember(packId) { runCatching { container.studyProgress.dueCards(pack, today).size }.getOrDefault(0) }
    val attempts = remember(packId) { runCatching { container.studyProgress.attempts(packId) }.getOrDefault(emptyList()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("نمودار پیشرفت این درس", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("تسلط", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(progress = { mastery / 100f }, modifier = Modifier.weight(1f).height(8.dp))
                Spacer(Modifier.width(8.dp))
                Text("${toPersianDigits(mastery.toString())}٪", style = MaterialTheme.typography.labelMedium)
            }
            Text(
                buildString {
                    append("کارت‌های امروز برای مرور: ")
                    append(toPersianDigits(due.toString()))
                    if (attempts.isNotEmpty()) {
                        append(" · آزمون‌ها: ")
                        append(toPersianDigits(attempts.size.toString()))
                        val last = attempts.last()
                        append(" · آخرین آزمون: ")
                        append(toPersianDigits(last.scorePct.toString()))
                        append("٪ (")
                        append(toPersianDigits(last.total.toString()))
                        append(" سؤال)")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * پلیر صوت یک فایل: اول «دریافت از سرور» (اگر کش نشده) → پخش/توقف، سرعت، سیک.
 */
@Composable
private fun LessonAudioPlayer(label: String, fileId: String, cacheKey: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var prepared by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableFloatStateOf(1f) }
    var downloading by remember { mutableStateOf(false) }
    var downloaded by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(-1) }
    var msg by remember { mutableStateOf<String?>(null) }

    val cacheFile = remember(cacheKey) { File(context.cacheDir, "lesson-media/$cacheKey") }
    downloaded = cacheFile.exists() && cacheFile.length() > 0

    DisposableEffect(cacheKey) { onDispose { player?.release(); player = null } }

    LaunchedEffect(playing, prepared) {
        while (playing && prepared) {
            player?.let { pos = it.currentPosition.toLong() }
            delay(500)
        }
    }

    fun applySpeed(p: MediaPlayer?, v: Float) {
        p ?: return
        runCatching { p.playbackParams = p.playbackParams.setSpeed(v) }
    }

    fun startPlay(file: File) {
        player?.release()
        val p = MediaPlayer()
        player = p
        prepared = false
        runCatching {
            p.setDataSource(file.absolutePath)
            p.setOnPreparedListener { mp ->
                prepared = true
                dur = mp.duration.toLong()
                applySpeed(mp, speed)
                mp.start()
                playing = true
            }
            p.setOnCompletionListener { playing = false }
            p.prepareAsync()
        }.onFailure { msg = "پخش این فایل ممکن نشد."; playing = false }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))

            if (!downloaded && !downloading) {
                PrimaryButton("📥 دریافت از سرور و پخش") {
                    downloading = true; msg = null
                    scope.launch {
                        try {
                            val dir = File(context.cacheDir, "lesson-media"); dir.mkdirs()
                            withContext(Dispatchers.IO) {
                                val conn = URL(StudyMedia.viewUrl(fileId)).openConnection()
                                conn.connect()
                                val total = conn.contentLength
                                conn.getInputStream().use { input ->
                                    val tmp = File(dir, "$cacheKey.part")
                                    tmp.outputStream().use { out ->
                                        val buf = ByteArray(32 * 1024)
                                        var read: Int; var done = 0
                                        while (input.read(buf).also { read = it } > 0) {
                                            out.write(buf, 0, read); done += read
                                            if (total > 0) progress = done * 100 / total
                                        }
                                    }
                                    tmp.renameTo(cacheFile)
                                }
                            }
                            downloaded = true
                            startPlay(cacheFile)
                        } catch (e: Exception) {
                            msg = "دریافت از سرور ناموفق بود؛ اینترنت را چک کن و دوباره بزن."
                        }
                        downloading = false; progress = -1
                    }
                }
            }
            if (downloading) {
                if (progress >= 0) {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("دریافت… ${toPersianDigits(progress.toString())}٪", style = MaterialTheme.typography.bodySmall)
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("در حال دریافت از سرور…", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (downloaded) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val p = player
                        if (p != null && prepared) {
                            if (playing) { p.pause(); playing = false } else { p.start(); playing = true }
                        } else {
                            startPlay(cacheFile)
                        }
                    }) { Text(if (playing) "⏸ توقف" else "▶ پخش") }
                    TextButton(onClick = {
                        player?.release(); player = null; playing = false; prepared = false; pos = 0
                    }) { Text("⏹") }
                    if (prepared) {
                        Text("${mmss(pos)} / ${mmss(dur)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (prepared) {
                    Slider(
                        value = if (dur > 0) (pos.toFloat() / dur) else 0f,
                        onValueChange = { frac ->
                            val target = (frac * dur).toInt()
                            player?.seekTo(target)
                            pos = target.toLong()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AUDIO_SPEEDS.forEach { v ->
                        FilterChip(
                            selected = speed == v,
                            onClick = { speed = v; applySpeed(player, v) },
                            label = { Text(if (v == 0.9f) "۰٫۹×" else "${v}×") },
                        )
                    }
                }
            }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/**
 * پلیر ویدیو (Media3): استریم مستقیم از سرور، نوار سیک خود پلیر، شمارش
 * پرش‌های نامتعارف (>۳ ثانیه با سیک) و نوار «زمان تماشای واقعی».
 */
@Composable
private fun LessonVideoPlayer(fileId: String, label: String, packId: String) {
    val context = LocalContext.current
    val store = remember { ir.behzad.platform.core.common.LocalStore(context, "hamyar_teach") }
    val watchKey = "teach_${packId}_watch"
    val jumpKey = "teach_${packId}_jumps"

    var player by remember(fileId) { mutableStateOf<ExoPlayer?>(null) }
    var watchMs by remember(fileId) { mutableLongStateOf(store.getString(watchKey, "0").toLongOrNull() ?: 0L) }
    var jumps by remember(fileId) { mutableIntStateOf(store.getString(jumpKey, "0").toIntOrNull() ?: 0) }
    var durationMs by remember(fileId) { mutableLongStateOf(0L) }
    var failed by remember(fileId) { mutableStateOf(false) }

    DisposableEffect(fileId) {
        val exo = ExoPlayer.Builder(context).build()
        player = exo
        exo.setMediaItem(MediaItem.fromUri(StudyMedia.viewUrl(fileId)))
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) durationMs = exo.duration.coerceAtLeast(0L)
                if (state == Player.STATE_ENDED) store.putString(watchKey, watchMs.toString())
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK &&
                    abs(newPosition.positionMs - oldPosition.positionMs) > 3_000
                ) {
                    jumps += 1
                    store.putString(jumpKey, jumps.toString())
                }
            }
        })
        exo.prepare()
        onDispose {
            store.putString(watchKey, watchMs.toString())
            store.putString(jumpKey, jumps.toString())
            exo.release()
            player = null
        }
    }

    // شمارش زمان تماشای واقعی: فقط وقتی واقعاً در حال پخش است؛ هر ۵ ثانیه ذخیره.
    LaunchedEffect(fileId) {
        while (true) {
            delay(1000)
            val p = player
            if (p != null && p.isPlaying) {
                watchMs += 1000
                if (watchMs % 5000L < 1000L) store.putString(watchKey, watchMs.toString())
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            if (failed) {
                Text(
                    "این ویدیو هنوز روی سرور نیست.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx) },
                    update = { view -> view.player = player },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
                Spacer(Modifier.height(6.dp))
                val pct = if (durationMs > 0) ((watchMs * 100) / durationMs).toInt().coerceIn(0, 100) else 0
                LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                Text(
                    "تماشای واقعی: ${mmss(watchMs)} از ${mmss(durationMs)} (${toPersianDigits(pct.toString())}٪) · پرش‌های نامتعارف: ${toPersianDigits(jumps.toString())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
