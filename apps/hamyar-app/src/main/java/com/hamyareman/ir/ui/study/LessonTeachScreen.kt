package com.hamyareman.ir.ui.study

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.feature.playback.PlaybackController
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.StudyPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs

/** سرعت‌های پخش v1.9 — ترتیب کاربر: x2/x1.5/x1/x0.75/x0.5 (زیر نوار سیک). */
internal val TEACH_SPEEDS = listOf(2f, 1.5f, 1f, 0.75f, 0.5f)

/** یک فایل صوتی قابل‌پخش در صفحه‌ی تدریس/خلاصه‌ها. */
internal data class TeachTrack(val label: String, val fileId: String, val cacheKey: String)

/**
 * v1.17 — قرارداد سراسری رسانه‌ها: هر پک «یک صوت» دارد:
 *  - اگر audioFileId پک پر باشد (authored/override حکایت سفر) از همان استفاده می‌شود؛
 *  - وگرنه نام قراردادی «<packId>_AUDIO.mp3» (در باکت برای همه‌ی ۲۰۹ پک موجود است).
 * ترک دوم (اینترو) حذف شد. هر ویدیو هم یک فایل قراردادی «<dash>-V01.mp4» است (StudyMedia).
 */
internal fun teachTracksOf(pack: StudyPack): List<TeachTrack> {
    val fid = pack.audioFileId.trim()
    if (fid.isBlank()) return emptyList()
    return listOf(TeachTrack("صوت درس", fid, fid))
}

/** شرط بازشدن مطالعه/سرعت تند = اتمام صوت تدریس؛ بدون فایل صوت قفل اعمال نمی‌شود. */
internal fun expectedTeachMedia(pack: StudyPack): Int = teachTracksOf(pack).size

/**
 * مقصد «لمس اعلان پخش» — سرویس رسانه PendingIntent به MainActivity می‌فرستد،
 * اینجا packId نگه داشته می‌شود و ZahraNavHost به صفحه‌ی تدریس همان درس می‌پرد.
 * قانون: صوت فقط داخل صفحه‌ی تدریس پخش می‌شود — پس پخش خودکار هم آنجا انجام می‌گیرد.
 */
object TeachLaunch {
    var pendingTeachPack by mutableStateOf<String?>(null)
}

internal fun teachMmss(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000
    return toPersianDigits(String.format(Locale.US, "%d:%02d", s / 60, s % 60))
}

internal fun teachSpeedLabel(v: Float): String = when (v) {
    2f -> "×۲"
    1.5f -> "×۱٫۵"
    1f -> "×۱"
    0.75f -> "×۰٫۷۵"
    0.5f -> "×۰٫۵"
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
    // v1.25 — نام کتاب برای اعلان پخش («نام کتاب و درس»).
    val bookTitle = remember(packId) {
        BookModuleRegistry.modules.firstOrNull { m -> m.packs.any { it.packId == packId } }?.title.orEmpty()
    }
    if (pack == null) {
        AppTopBar(title = "تدریس درس", onBack = onBack)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { Text("این درس پیدا نشد.") }
        return
    }
    val ctx = LocalContext.current
    if (LessonAccess.gate(ctx, pack.bookCode, packId) == LessonAccess.Gate.NeedSub) {
        NeedSubScreen(onBack = onBack)
        return
    }
    if (pack.bookCode == "C905") {
        MathLessonScreen(packId = packId, initialTab = 0, onBack = onBack)
        return
    }

    AppTopBar(title = "تدریس — ${pack.title}", onBack = onBack)
    // پلیر همیشه «بالای صفحه» ثابت می‌ماند و کتاب (PDF) زیرش اسکرول می‌شود.
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val tracks = teachTracksOf(pack)
        if (tracks.isNotEmpty()) TeachAudioBar(packId = packId, screenTitle = pack.title, bookTitle = bookTitle, tracks = tracks)

        // v1.18: ویدیو به صفحه‌ی مجزای «ویدیوی تدریس» منتقل شد (VideoTeachScreen).
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
internal fun TeachAudioBar(packId: String, screenTitle: String, bookTitle: String, tracks: List<TeachTrack>) {
    val context = LocalContext.current
    val store = remember { LocalStore(context, "hamyar_teach") }
    val scope = rememberCoroutineScope()
    val playback = remember(packId) { PlaybackController(context) }
    val state by playback.state.collectAsState()

    var activeIdx by remember {
        mutableIntStateOf(store.getString("teach_${packId}_track", "0").toIntOrNull()?.coerceIn(0, tracks.size - 1) ?: 0)
    }
    // v1.14: تا اتمام اولین دوره، سرعت‌های تندتر از ۱x فعال نیستند (حتی اگر قبلاً ذخیره شده بود).
    var speed by remember {
        val saved = store.getString("teach_${packId}_speed", "1").toFloatOrNull() ?: 1f
        mutableFloatStateOf(if (TEACH_SPEEDS.contains(saved)) saved else 1f)
    }
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
    var pendingStartKey by remember { mutableStateOf<String?>(null) }

    val track = tracks[activeIdx]
    LaunchedEffect(packId, tracks.size) {
        // قفل مطالعه و ×۱٫۵/×۲ با اتمام صوت همین صفحه باز می‌شود (ویدیو جدا است).
        TeachStats.expectMedia(context, packId, tracks.size.coerceAtLeast(1))
    }

    fun posKey(t: TeachTrack) = "teach_${packId}_${t.cacheKey}_pos"
    fun savedPos(t: TeachTrack) = store.getString(posKey(t), "0").toLongOrNull() ?: 0L
    fun savePos(t: TeachTrack, p: Long) { if (p > 0) store.putString(posKey(t), p.toString()) else store.remove(posKey(t)) }
    fun cached(t: TeachTrack) = cacheTick >= 0 && MediaVault.isCached(context, t.cacheKey)

    /** «زمان درس» — سکوتِ اجباری پلیر دروس (کلید سراسری از «بیشتر»). */
    fun quietOn(): Boolean = store.getString("quiet_mode", "0") == "1"

    val appContainer = com.hamyareman.ir.LocalAppContainer.current
    LaunchedEffect(packId) {
        TeachStats.enter(context, packId)
        runCatching { TeachCloud.push(appContainer.sync) }
    }

    // قانون v1.10: صوت تدریس فقط وقتی صفحه‌ی پلیر (تدریس/جزوه/نکات) باز است از اعلان
    // هم پخش می‌شود — سرویس با این پرچم پلیِ اعلان را می‌سنجد.
    DisposableEffect(packId) {
        com.hamyareman.ir.platform.feature.playback.TeachGate.teachPageOpen = true
        onDispose { com.hamyareman.ir.platform.feature.playback.TeachGate.teachPageOpen = false }
    }
    // v1.25 — خروج از صفحه با هوم/پنجره‌ها/قفل صفحه هم = مکث پخش (شرط بازبودن صفحه).
    PauseOnStopEffect { playback.pause() }

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
            val items = withContext(Dispatchers.IO) {
                tracks.map { tr ->
                    val useLocal = !forceServer && MediaVault.isCached(context, tr.cacheKey) && !(fromServer && tr.cacheKey == t.cacheKey)
                    val remoteId = if (useLocal) tr.fileId else StudyMedia.resolveFileId(tr.fileId)
                    MediaItem.Builder()
                        .setMediaId(tr.cacheKey)
                        .setUri(if (useLocal) MediaVault.localUrl(context, tr.cacheKey) else StudyMedia.viewUrl(remoteId))
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(screenTitle).setArtist(bookTitle.ifBlank { tr.label }).build())
                        .build()
                }
            }
            playback.setMediaItems(items, tracks.indexOf(t).coerceAtLeast(0), if (cached(t) && !fromServer) savedPos(t) else 0L)
            com.hamyareman.ir.platform.feature.playback.TeachGate.currentPack = packId
            playback.setSpeed(speed)
            // v1.12: فوراً ترکِ جاری را ثبت کن — تا فال‌بکِ خطا (سرور) همیشه زنده باشد
            // و اگر پخش محلی شروع شد، واتچ‌داگ بتواند نتیجه را بسنجد.
            loadedKey = t.cacheKey
            lastSaveMs = 0L
            posMs = if (cached(t) && !fromServer) savedPos(t) else 0L
            pendingStartKey = if (autoplay) t.cacheKey else null
            if (autoplay) playback.play()
        }
    }

    // لمس اعلان/دکمه‌ی پلی در اعلان → کاربر به همین صفحه‌ی تدریس آمده؛ پخش را اینجا شروع کن.
    LaunchedEffect(packId) {
        if (TeachLaunch.pendingTeachPack == packId) {
            TeachLaunch.pendingTeachPack = null
            startTrack(track, autoplay = true)
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
            // v1.12: همگام‌سازی ترکِ جاری حتی وقتی پخش متوقف است (تا حالت‌های خطا هم synced بمانند).
            val curSync = playback.currentMediaId()
            if (curSync != null && curSync != loadedKey) {
                val idx = tracks.indexOfFirst { it.cacheKey == curSync }
                if (idx >= 0 && idx != activeIdx) {
                    activeIdx = idx
                    store.putString("teach_${packId}_track", idx.toString())
                }
                loadedKey = curSync
                lastSaveMs = 0L
                posMs = 0L
            }
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

    // تشخیص پایان ترک: STATE_ENDED، نزدیک انتهای فایل، یا ≥۹۵٪ ثانیه‌ی شنیده‌شده.
    LaunchedEffect(state.ended, state.playing, state.positionMs, state.durationMs, posMs) {
        val dur = state.durationMs
        val pos = maxOf(posMs, state.positionMs)
        val nearEnd = dur > 0 && pos >= dur - 1500
        val listenSnap = TeachStats.snap(context, packId)
        val listenedEnough = listenSnap.audioDurSec > 0 &&
            listenSnap.listenSec >= (listenSnap.audioDurSec * 95 / 100)
        if (state.ended || listenedEnough || (!state.playing && state.hasMedia && nearEnd)) {
            val key = loadedKey ?: track.cacheKey
            TeachStats.markTrackDone(context, packId, key)
            if (state.ended || nearEnd) {
                loadedKey?.let { k -> tracks.firstOrNull { it.cacheKey == k }?.let { savePos(it, 0L) } }
            }
        }
    }

    DisposableEffect(packId) {
        onDispose {
            loadedKey?.let { key -> tracks.firstOrNull { it.cacheKey == key }?.let { savePos(it, posMs) } }
            runCatching { playback.pause() }
            playback.release()
        }
    }

    // اگر پخشِ محلی (گاوصندوق) خطا داد، بی‌سروصدا از سرور ادامه بده —
    // v1.12: بدون شرط loadedKey (پیشتر خطای آفلاین بن‌بست می‌شد و هیچی پخش نمی‌شد).
    LaunchedEffect(state.error) {
        val err = state.error ?: return@LaunchedEffect
        if (!forceServer) {
            forceServer = true
            pendingStartKey = null
            startTrack(track, autoplay = true, fromServer = true)
        }
    }

    // واتچ‌داگ پخش — اگر ۵ ثانیه بعد از فرمان پخش هنوز چیزی پخش نمی‌شود
    // (مثلاً پخش آفلاین از سرور محلی راه نیفتاد)، بی‌سروصدا از سرور ادامه بده.
    LaunchedEffect(pendingStartKey) {
        val k = pendingStartKey ?: return@LaunchedEffect
        kotlinx.coroutines.delay(5000)
        if (pendingStartKey == k && !playback.state.value.playing && !forceServer) {
            forceServer = true
            pendingStartKey = null
            tracks.firstOrNull { it.cacheKey == k }?.let { startTrack(it, autoplay = true, fromServer = true) }
        }
        if (pendingStartKey == k) pendingStartKey = null
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            // انتخاب ترک — فقط وقتی درس چند صوت دارد (v1.9: عنوان تک‌صوت حذف شد).
            if (tracks.size > 1) {
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
                            label = { Text(t.label) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            // ردیف پخش: ▶/⏸ + زمان — v1.9: دکمه‌ی Stop حذف شد (پخش/توقف کافی است).
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = {
                    if (quietOn()) {
                        // «زمان درس» روشن است — بدون پیامِ اضافه، پخش نمی‌شود.
                    } else if (state.playing) {
                        playback.pause()
                        savePos(track, posMs)
                    } else {
                        // اگر سرویس/اتصال افتاده باشد (مثلاً بعد از مکث طولانی)،
                        // اول دوباره وصل و آماده می‌کنیم و بعد پخش — از همان جای حافظه.
                        scope.launch {
                            if (!playback.connect()) {
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
                // v1.11: منبع (پخش آنلاین/پخش آفلاین) با آیکون + دانلود/حذف آفلاین —
                // بالا و سمت چپِ دکمه‌ی توقف.
                val online = !cached(track)
                val srcColor = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                Icon(
                    if (online) Icons.Outlined.Cloud else Icons.Outlined.Smartphone,
                    contentDescription = null,
                    tint = srcColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    if (online) "پخش آنلاین" else "پخش آفلاین",
                    style = MaterialTheme.typography.labelMedium,
                    color = srcColor,
                    maxLines = 1,
                )
                when {
                    downloading -> {
                        LinearProgressIndicator(
                            progress = { (if (progressPct < 0) 0 else progressPct) / 100f },
                            modifier = Modifier.width(72.dp).height(6.dp),
                        )
                        if (progressPct >= 0) {
                            Text(
                                "${toPersianDigits(progressPct.toString())}٪",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                        }
                    }
                    cached(track) -> {
                        var confirmDelete by remember { mutableStateOf(false) }
                        if (confirmDelete) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { confirmDelete = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        confirmDelete = false
                                        MediaVault.delete(context, track.cacheKey)
                                        cacheTick++
                                    }) { Text("حذف") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmDelete = false }) { Text("نگه‌دار") }
                                },
                                title = { Text("حذف فایل آفلاین؟") },
                                text = { Text("فایل از حافظه‌ی گوشی پاک می‌شود و پخش بعدی آنلاین انجام می‌گیرد؛ هر وقت خواستی دوباره دانلود می‌کنی.") },
                            )
                        }
                        TextButton(
                            onClick = { confirmDelete = true },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("حذف آفلاین", maxLines = 1)
                        }
                    }
                    else -> TextButton(onClick = {
                        downloading = true; progressPct = -1
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    val remoteId = StudyMedia.resolveFileId(track.fileId)
                                    MediaVault.downloadEncrypted(
                                        context,
                                        StudyMedia.viewUrl(remoteId),
                                        track.cacheKey,
                                    ) { pct -> progressPct = pct }
                                }
                                downloading = false; cacheTick++
                            } catch (e: Exception) {
                                downloading = false
                            }
                        }
                    }) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("دانلود", maxLines = 1)
                    }
                }
                if (loadedKey == track.cacheKey && state.durationMs > 0) {
                    Text(
                        "${teachMmss(if (dragMs >= 0) dragMs else posMs)} / ${teachMmss(state.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.weight(1f))
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
                    modifier = Modifier.fillMaxWidth().height(26.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TEACH_SPEEDS.forEach { v ->
                    FilterChip(
                        selected = speed == v,
                        onClick = {
                            speed = v
                            runCatching { playback.setSpeed(v) }
                            store.putString("teach_${packId}_speed", v.toString())
                        },
                        label = { Text(teachSpeedLabel(v), style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        }
    }
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

/** خطای قابل‌گزارش (دانلود/اعتبارسنجی) — پیامش مستقیم به کاربر نشان داده می‌شود. */
private class PdfUnavailable(message: String) : Exception(message)

/** دانلود/اعتبارسنجی/بازکردن PDF — فقط روی IO صدا زده می‌شود. */
/**
 * v1.29 — تصویر صفحه‌ی PDF با زوم لمسی دقیقاً مثل گالری اندروید:
 *  - پینچ دوانگشتی ۱x تا ۵x **دور نقطه‌ی انگشت‌ها**؛
 *  - در حالت زوم، کشیدن تک‌انگشتی = پن با محدوده‌بندی (تصویر از قاب بیرون نمی‌زند)؛
 *  - دوبار-لمس = زوم/بازگشت؛ برگشت به ۱x خودکار وسط‌چین.
 */
@Composable
private fun ZoomablePageImage(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    contentDescription: String,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onTransform: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaleRef = rememberUpdatedState(scale)
    val oxRef = rememberUpdatedState(offsetX)
    val oyRef = rememberUpdatedState(offsetY)
    val onT = rememberUpdatedState(onTransform)
    val aspect = if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height.toFloat() else 1f
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(aspect.coerceAtLeast(0.2f))
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val cur = scaleRef.value
                    val newScale = (cur * zoom).coerceIn(1f, 5f)
                    val f = if (cur > 0f) newScale / cur else 1f
                    val nx = centroid.x - (centroid.x - oxRef.value) * f + pan.x
                    val ny = centroid.y - (centroid.y - oyRef.value) * f + pan.y
                    val maxX = size.width * (newScale - 1f) / 2f
                    val maxY = size.height * (newScale - 1f) / 2f
                    val cx = if (newScale <= 1.001f) 0f else nx.coerceIn(-maxX, maxX)
                    val cy = if (newScale <= 1.001f) 0f else ny.coerceIn(-maxY, maxY)
                    onT.value(newScale, cx, cy)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scaleRef.value > 1f) onT.value(1f, 0f, 0f)
                        else onT.value(2.5f, 0f, 0f)
                    },
                )
            },
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
        )
    }
}

private fun openTeachPdf(ctx: android.content.Context, fileId: String, onProgress: (Int) -> Unit): PdfRenderer {
    val cacheDir = File(ctx.filesDir, "media/pdf-cache").apply { mkdirs() }
    val target = File(cacheDir, fileId)
    fun isValid(f: File) = f.length() > 1024 && runCatching {
        f.inputStream().use { val h = ByteArray(5); it.read(h); String(h) == "%PDF-" }
    }.getOrDefault(false)
    if (!isValid(target)) {
        target.delete()
        val remoteId = StudyMedia.resolveFileId(fileId)
        val conn = (URL(StudyMedia.viewUrl(remoteId)).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true
        }
        conn.connect()
        if (conn.responseCode !in 200..299) throw PdfUnavailable("دریافت PDF ممکن نشد (کد ${conn.responseCode}).")
        val total = conn.contentLengthLong
        val part = File(cacheDir, "$fileId.part")
        conn.inputStream.use { input ->
            java.io.FileOutputStream(part).use { out ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                var done = 0L
                while (input.read(buf).also { read = it } > 0) {
                    out.write(buf, 0, read); done += read
                    if (total > 0) onProgress(((done * 100) / total).toInt())
                }
            }
        }
        if (!isValid(part)) {
            part.delete()
            throw PdfUnavailable("فایل PDF ناقص رسید؛ یک‌بار دیگر تلاش کن.")
        }
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true); part.delete()
        }
    }
    val fd = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
    return PdfRenderer(fd)
}

/** کش LRU صفحه‌های PDF (~۴ صفحهٔ نزدیک) تا اسکرول سبک بماند. */
private class TeachPageCache(private val maxPages: Int = 4) {
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
        state = TeachPdfState.Downloading(0)
        try {
            // PdfRenderer فقط از یک نخ باید دیده شود — همهٔ کار روی IO.
            val r = withContext(Dispatchers.IO) {
                openTeachPdf(ctx, fileId) { pct ->
                    state = TeachPdfState.Downloading(pct)
                }
            }
            synchronized(renderLock) { renderer = r }
            state = TeachPdfState.Ready(r.pageCount)
        } catch (e: PdfUnavailable) {
            state = TeachPdfState.Error(e.message ?: "PDF در دسترس نیست.")
        } catch (e: Exception) {
            // کش دانلودشده را پاک نکن — شاید رندر مشکل داشت نه فایل.
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
            // v1.29 — زوم لمسی دوانگشتی (مثل گالری): وقتی صفحه‌ای زوم است،
            // لیست اسکرول نمی‌شود تا جابه‌جایی/پن دست کاربر باشد.
            var blockingZoom by remember { mutableStateOf(false) }
            val seenPages = remember(fileId) { mutableSetOf<Int>() }
            val screenW = remember {
                ctx.resources.displayMetrics.widthPixels.coerceIn(720, 1280)
            }
            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = !blockingZoom,
            ) {
                items(st.pageCount, key = { it }) { index ->
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            var bmp by remember(fileId, index) { mutableStateOf<Bitmap?>(pageCache[index]) }
                            var zoomScale by remember(fileId, index) { mutableFloatStateOf(1f) }
                            var zoomX by remember(fileId, index) { mutableFloatStateOf(0f) }
                            var zoomY by remember(fileId, index) { mutableFloatStateOf(0f) }
                            LaunchedEffect(fileId, index) {
                                if (bmp == null) {
                                    val rendered: Bitmap? = withContext(Dispatchers.IO) {
                                        try {
                                            synchronized(renderLock) {
                                                val r = renderer ?: return@synchronized null
                                                r.openPage(index).use { page ->
                                                    val targetW = screenW
                                                    val scale = targetW.toFloat() / page.width.toFloat()
                                                    val w = targetW
                                                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                                                    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                                    b.eraseColor(Color.WHITE)
                                                    page.render(b, null, android.graphics.Matrix().apply { setScale(scale, scale) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
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
                                    }
                                    if (rendered != null) {
                                        synchronized(pageCache) { pageCache[index] = rendered }
                                        bmp = rendered
                                        if (seenPages.add(index)) {
                                            StudyActivity.add(ctx, pack.packId, "pdf", "مشاهده صفحه ${index + 1} کتاب درسی")
                                        }
                                    }
                                }
                            }
                            if (bmp == null) {
                                Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.padding(16.dp))
                                }
                            } else {
                                ZoomablePageImage(
                                    bitmap = bmp!!.asImageBitmap(),
                                    contentDescription = "صفحه ${index + 1}",
                                    scale = zoomScale,
                                    offsetX = zoomX,
                                    offsetY = zoomY,
                                    onTransform = { s, x, y ->
                                        zoomScale = s; zoomX = x; zoomY = y
                                        blockingZoom = s > 1.01f
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
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
