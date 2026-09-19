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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

/** سیک از فهرست HTML تدریس → پلیر بالای صفحه. */
object TeachSeekBus {
    var requestMs by mutableStateOf<Long?>(null)
    fun seekMs(ms: Long) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            requestMs = ms.coerceAtLeast(0L)
        }
    }
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
    var doneBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var msg by remember { mutableStateOf<String?>(null) }
    var cacheTick by remember { mutableIntStateOf(0) }
    var loadedKey by remember { mutableStateOf<String?>(null) }
    // منبعِ آیتمِ داخلِ صف (true = پخش محلی از گاوصندوق) — برای تصمیم «ادامه» یا «ردوبالازدنِ دوباره».
    var loadedLocal by remember { mutableStateOf(false) }
    var listenAccumMs by remember { mutableLongStateOf(0L) }
    var lastSaveMs by remember { mutableLongStateOf(0L) }
    var dragMs by remember { mutableLongStateOf(-1L) }
    var quiet by remember { mutableStateOf(false) }
    var pendingStartKey by remember { mutableStateOf<String?>(null) }
    // اگر بعد از فرمانِ پخش، موقعیت به انتهای فایل چسبیده باشد → یک‌بار از سرِ فایل (تله‌ی انتها).
    var healArmKey by remember { mutableStateOf<String?>(null) }
    var pendingSeekMs by remember { mutableLongStateOf(-1L) }

    val track = tracks[activeIdx]
    LaunchedEffect(packId, tracks.size) {
        // قفل مطالعه و ×۱٫۵/×۲ با اتمام صوت همین صفحه باز می‌شود (ویدیو جدا است).
        TeachStats.expectMedia(context, packId, tracks.size.coerceAtLeast(1))
    }

    fun posKey(t: TeachTrack) = "teach_${packId}_${t.cacheKey}_pos"
    fun durKey(t: TeachTrack) = "teach_${packId}_${t.cacheKey}_dur"
    /**
     * «تله‌ی انتها»: اگر موقعیتِ ذخیره‌شده به انتهای فایلِ واقعی برسد یا از آن رد شده باشد
     * (مثلاً فایلِ درس کوتاه/جایگزین شده باشد ولی موقعیتِ نسخهٔ بزرگِ قبلی مانده باشد)،
     * پلیر از انتها شروع می‌کند: بی‌صدا، بدونِ هیچ خطایی. چنین موقعیتی را صفر می‌کنیم.
     */
    fun savedPos(t: TeachTrack): Long {
        val p = store.getString(posKey(t), "0").toLongOrNull() ?: 0L
        if (p <= 0) return 0L
        val d = store.getString(durKey(t), "0").toLongOrNull() ?: 0L
        return if (d > 3000 && p >= d - 1500) 0L else p
    }
    fun savePos(t: TeachTrack, p: Long) { if (p > 0) store.putString(posKey(t), p.toString()) else store.remove(posKey(t)) }
    fun cached(t: TeachTrack) = cacheTick >= 0 && MediaVault.isVerified(context, t.cacheKey)

    /** «زمان درس» — سکوتِ اجباری پلیر دروس (کلید سراسری از «بیشتر»). */
    var quietTick by remember { mutableIntStateOf(0) }
    fun quietOn(): Boolean = quietTick.let { store.getString("quiet_mode", "0") == "1" }

    val appContainer = com.hamyareman.ir.LocalAppContainer.current
    LaunchedEffect(packId) {
        TeachStats.enter(context, packId)
        runCatching { TeachCloud.push(appContainer.sync) }
    }

    // قانون v1.10: صوت تدریس فقط وقتی صفحه‌ی پلیر (تدریس/جزوه/نکات) باز است از اعلان
    // هم پخش می‌شود — سرویس با این پرچم پلیِ اعلان را می‌سنجد.
    DisposableEffect(packId) {
        com.hamyareman.ir.platform.feature.playback.TeachGate.enter()
        onDispose { com.hamyareman.ir.platform.feature.playback.TeachGate.exit() }
    }
    // ---- پلیرِ پشتیبانِ داخلیِ صفحه ----
    // اگر مسیرِ «سرویسِ پخش» روی این دستگاه پاسخ ندهد (اتصال/سشن/اعلان/تمرکزِ صوتی)،
    // صدا با همان پلیری پخش می‌شود که ویدیوهای تدریس را پخش می‌کند — در خودِ صفحه،
    // بدون سرویس و بدون binder. پخشِ درس به هر حال نباید به پس‌زمینه برود،
    // پس نبودِ اعلان در این مسیر ایراد ندارد؛ «صدا داشته باشیم» اولویت دارد.
    var fallback by remember { mutableStateOf<ExoPlayer?>(null) }
    var fbPlaying by remember { mutableStateOf(false) }
    var fbPos by remember { mutableLongStateOf(0L) }
    var fbDur by remember { mutableLongStateOf(0L) }

    fun stopFallback() {
        fallback?.let { p ->
            runCatching { p.stop() }
            runCatching { p.release() }
        }
        fallback = null
        fbPlaying = false
        fbPos = 0L
        fbDur = 0L
    }

    fun startFallback(t: TeachTrack, startMs: Long? = null) {
        stopFallback()
        val useLocal = MediaVault.isVerified(context, t.cacheKey)
        val uri = if (useLocal) MediaVault.localUrl(context, t.cacheKey)
        else StudyMedia.viewUrl(StudyMedia.resolveFileId(t.fileId))
        val start = startMs ?: savedPos(t)
        val p = ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                com.hamyareman.ir.platform.feature.playback.vaultAwareMediaSourceFactory(context),
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ false,
            )
            .build()
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && p.duration > 0) {
                    fbDur = p.duration
                    store.putString("teach_${packId}_${t.cacheKey}_dur", p.duration.toString())
                    // «تله‌ی انتها» — همان محافظتِ مسیرِ سرویس، اینجا هم لازم است.
                    if (p.currentPosition >= p.duration - 1500) {
                        p.seekTo(0L)
                        fbPos = 0L
                        savePos(t, 0L)
                        msg = "موقعیتِ ذخیره‌شده بیرون از انتهای فایل بود؛ از ابتدا پخش می‌شود."
                    }
                }
                if (playbackState == Player.STATE_ENDED) {
                    TeachStats.markTrackDone(context, packId, t.cacheKey)
                    savePos(t, 0L)
                    fbPlaying = false
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) { fbPlaying = isPlaying }

            override fun onPlayerError(error: PlaybackException) {
                msg = "خطای پخش: ${error.message ?: error.javaClass.simpleName}"
            }
        })
        p.setMediaItem(MediaItem.Builder().setUri(uri).setMediaId(t.cacheKey).build())
        p.setPlaybackSpeed(speed)
        if (start > 0) p.seekTo(start)
        p.prepare()
        p.playWhenReady = true
        fbPos = start
        fallback = p
    }

    DisposableEffect(Unit) { onDispose { stopFallback() } }

    // v1.25 — خروج از صفحه با هوم/پنجره‌ها/قفل صفحه هم = مکث پخش (شرط بازبودن صفحه).
    PauseOnStopEffect(
        pause = { runCatching { playback.pause() }; fallback?.pause() },
        stop = { runCatching { playback.stop() }; stopFallback() },
    )

    var forceServer by remember { mutableStateOf(false) }

    /**
     * آیا صوتِ این درس واقعاً روی باکت هست؟ (پیش‌نمایشِ HEAD)
     * null = هنوز بررسی نشده؛ false = فایل روی سرور نیست (پلیر قفل می‌ماند).
     */
    var availability by remember(packId) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(packId, tracks.size) {
        availability = withContext(Dispatchers.IO) {
            tracks.any { t ->
                MediaVault.isVerified(context, t.cacheKey) || StudyMedia.audioExists(t.fileId)
            }
        }
    }

    fun startTrack(t: TeachTrack, autoplay: Boolean, fromServer: Boolean = false, startMs: Long? = null) {
        msg = null
        if (availability == false) {
            msg = "صوت این درس هنوز روی سرور نیست؛ به‌زودی اضافه می‌شود."
            return
        }
        if (quietOn()) {
            msg = "🔇 «زمان درس» روشن است — تا خاموشش کنی، پخش صدا فعال نمی‌شود."
            return
        }
        // پلیرِ داخلیِ صفحه فعال است ⇒ همه‌ی فرمان‌های پخش از همان راه کار می‌کنند
        // (تغییر ترک و سیک هم همان‌جا اعمال می‌شود؛ سرویس دیگر در کار نیست).
        if (fallback != null) {
            startFallback(t, startMs)
            if (!autoplay) fallback?.pause()
            loadedKey = t.cacheKey
            loadedLocal = MediaVault.isVerified(context, t.cacheKey)
            lastSaveMs = 0L
            posMs = fbPos
            pendingStartKey = null
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
                    val useLocal = !forceServer && MediaVault.isVerified(context, tr.cacheKey) && !(fromServer && tr.cacheKey == t.cacheKey)
                    val remoteId = if (useLocal) tr.fileId else StudyMedia.resolveFileId(tr.fileId)
                    MediaItem.Builder()
                        .setMediaId(tr.cacheKey)
                        .setUri(if (useLocal) MediaVault.localUrl(context, tr.cacheKey) else StudyMedia.viewUrl(remoteId))
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(screenTitle).setArtist(bookTitle.ifBlank { tr.label }).build())
                        .build()
                }
            }
            val pos = startMs ?: if (cached(t) && !fromServer) savedPos(t) else 0L
            playback.setMediaItems(items, tracks.indexOf(t).coerceAtLeast(0), pos)
            // کاربر روی «پخش» زده ⇒ صفحه‌ی تدریس قطعاً باز است؛ دروازه را باز کن
            // تا سرویس پخش را بی‌صدا متوقف نکند (ضدِ انحرافِ شمارنده).
            com.hamyareman.ir.platform.feature.playback.TeachGate.pulse()
            com.hamyareman.ir.platform.feature.playback.TeachGate.currentPack = packId
            playback.setSpeed(speed)
            // v1.12: فوراً ترکِ جاری را ثبت کن — تا فال‌بکِ خطا (سرور) همیشه زنده باشد
            // و اگر پخش محلی شروع شد، واتچ‌داگ بتواند نتیجه را بسنجد.
            loadedKey = t.cacheKey
            loadedLocal = !forceServer && MediaVault.isVerified(context, t.cacheKey) && !fromServer
            healArmKey = t.cacheKey
            lastSaveMs = 0L
            posMs = pos
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

    val tocSeekMs = TeachSeekBus.requestMs
    LaunchedEffect(tocSeekMs) {
        val ms = tocSeekMs ?: return@LaunchedEffect
        TeachSeekBus.requestMs = null
        if (quietOn()) return@LaunchedEffect
        if (abs(ms - posMs) > 3000) TeachStats.addJump(context, packId)
        savePos(track, ms)
        pendingSeekMs = ms
        if (fallback != null) {
            fallback?.seekTo(ms)
            fbPos = ms
            pendingSeekMs = -1L
            fallback?.play()
            return@LaunchedEffect
        }
        if (loadedKey == track.cacheKey && state.hasMedia && state.durationMs > 0) {
            playback.seekTo(ms)
            posMs = ms
            pendingSeekMs = -1L
            if (!state.playing) playback.play()
        } else {
            startTrack(track, autoplay = true, startMs = ms)
        }
    }
    LaunchedEffect(state.durationMs, pendingSeekMs, loadedKey) {
        val want = pendingSeekMs
        if (want >= 0 && loadedKey == track.cacheKey && state.durationMs > 0) {
            playback.seekTo(want)
            posMs = want
            pendingSeekMs = -1L
            if (!state.playing) playback.play()
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
            // پلیرِ داخلی فعال است: موقعیت از fbPos می‌آید و صفِ سرویس مرجع نیست.
            if (fallback != null) { posMs = fbPos; continue }
            // v1.12: همگام‌سازی ترکِ جاری حتی وقتی پخش متوقف است (تا حالت‌های خطا هم synced بمانند).
            val curSync = playback.currentMediaId()
            if (curSync != null && curSync != loadedKey) {
                val idx = tracks.indexOfFirst { it.cacheKey == curSync }
                if (idx >= 0 && idx != activeIdx) {
                    activeIdx = idx
                    store.putString("teach_${packId}_track", idx.toString())
                }
                loadedKey = curSync
                loadedLocal = !forceServer && MediaVault.isVerified(context, curSync)
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
                loadedLocal = !forceServer && MediaVault.isVerified(context, cur)
                lastSaveMs = 0L
                posMs = 0L
            }
        }
    }

    // پلیرِ پشتیبان: نظرسنجیِ موقعیت، ذخیره و آمارِ شنیدن.
    LaunchedEffect(fallback) {
        if (fallback == null) return@LaunchedEffect
        while (true) {
            val p = fallback ?: break
            delay(500)
            fbPos = p.currentPosition.coerceAtLeast(0L)
            if (p.duration > 0) fbDur = p.duration
            listenAccumMs += 500
            if (listenAccumMs >= 5000) {
                TeachStats.addListen(context, packId, (listenAccumMs / 1000).toInt(), (fbDur / 1000).toInt())
                listenAccumMs = 0
            }
            if (fbPos - lastSaveMs >= 4000 || fbPos < lastSaveMs) {
                savePos(track, fbPos)
                lastSaveMs = fbPos
            }
        }
    }
    LaunchedEffect(fbPos, fbDur, fbPlaying) {
        if (fallback != null && fbDur > 0 && fbPos >= fbDur - 1500 && fbPos > 0 && !fbPlaying) {
            TeachStats.markTrackDone(context, packId, track.cacheKey)
            savePos(track, 0L)
        }
    }

    // تشخیص پایان ترک: STATE_ENDED، نزدیک انتهای فایل، یا ≥۹۵٪ ثانیه‌ی شنیده‌شده.
    // «ترمیمِ انتها» برای مسیرِ سرویس: بعد از فرمانِ پخش، اگر فایل آماده شد اما
    // موقعیت به انتهایش چسبیده بود و پخشی شروع نشد، از سرِ فایل ادامه می‌دهیم.
    LaunchedEffect(healArmKey, state.durationMs, state.playing) {
        val key = healArmKey ?: return@LaunchedEffect
        val dur = state.durationMs
        if (fallback != null || dur <= 0L) return@LaunchedEffect
        if (state.playing) { healArmKey = null; return@LaunchedEffect }
        val pos = playback.positionMs
        if (pos > 0L && pos < dur - 1500L) { healArmKey = null; return@LaunchedEffect }
        if (pos >= dur - 1500L) {
            healArmKey = null
            runCatching { playback.seekTo(0L) }
            tracks.firstOrNull { it.cacheKey == key }?.let { savePos(it, 0L) }
            posMs = 0L
            com.hamyareman.ir.platform.feature.playback.TeachGate.pulse()
            runCatching { playback.play() }
            msg = "موقعیتِ ذخیره‌شده بیرون از انتهای فایل بود؛ از ابتدا پخش می‌شود."
        }
    }

    LaunchedEffect(state.ended, state.playing, state.positionMs, state.durationMs, posMs) {
        if (fallback != null) return@LaunchedEffect
        val dur = state.durationMs
        if (dur > 0) loadedKey?.let { k -> store.putString("teach_${packId}_${k}_dur", dur.toString()) }
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
            runCatching { playback.stop() }
            playback.release()
            stopFallback()
        }
    }

    // اگر پخشِ محلی (گاوصندوق) خطا داد، بی‌سروصدا از سرور ادامه بده —
    // v1.12: بدون شرط loadedKey (پیشتر خطای آفلاین بن‌بست می‌شد و هیچی پخش نمی‌شد).
    LaunchedEffect(state.error) {
        val err = state.error ?: return@LaunchedEffect
        if (fallback != null) return@LaunchedEffect
        if (!forceServer) {
            forceServer = true
            pendingStartKey = null
            startTrack(track, autoplay = true, fromServer = true)
            msg = "پخش محلی با مشکل مواجه شد؛ از سرور ادامه می‌دهیم…"
        }
    }

    // واتچ‌داگ پخش — اگر ۵ ثانیه بعد از فرمان پخش هنوز چیزی پخش نمی‌شود
    // (مثلاً پخش آفلاین از سرور محلی راه نیفتاد)، بی‌سروصدا از سرور ادامه بده.
    LaunchedEffect(pendingStartKey) {
        val k = pendingStartKey ?: return@LaunchedEffect
        // گام ۱ (۴ ثانیه): اگر پخش شروع نشد، اول دروازه را تپش و دوباره play کن
        // — خیلی وقت‌ها مشکل فقط انحرافِ شمارنده‌ی «صفحه‌ی تدریس باز است» است.
        kotlinx.coroutines.delay(4000)
        if (pendingStartKey == k && !playback.state.value.playing && !forceServer) {
            com.hamyareman.ir.platform.feature.playback.TeachGate.pulse()
            playback.play()
            msg = "در حال تلاشِ دوباره برای پخش…"
        }
        // گام ۲ (۳ ثانیه‌ی دیگر): نه پخش و نه بارگذاری ⇒ معطلِ سرویس نشو —
        // پلیرِ داخلیِ خودِ صفحه را روشن کن (همان مسیری که ویدیوها با آن کار می‌کنند).
        kotlinx.coroutines.delay(3000)
        if (pendingStartKey == k && !playback.state.value.playing) {
            val st = playback.state.value
            if (!st.buffering) {
                runCatching { playback.stop() }
                tracks.firstOrNull { it.cacheKey == k }?.let { t ->
                    startFallback(t)
                    msg = "سرویسِ پخش پاسخ نداد؛ پلیرِ داخلیِ صفحه فعال شد."
                }
                pendingStartKey = null
                return@LaunchedEffect
            }
            if (!forceServer) {
                forceServer = true
                tracks.firstOrNull { it.cacheKey == k }?.let { startTrack(it, autoplay = true, fromServer = true) }
                msg = "پخش محلی شروع نشد؛ از سرور ادامه می‌دهیم…"
            }
        }
        // گام ۳: اگر هنوز پخش نیست، علت را دقیق بگو (دیگر چیزی پنهان نمی‌ماند)
        kotlinx.coroutines.delay(8000)
        if (pendingStartKey == k && !playback.state.value.playing) {
            val st = playback.state.value
            val err = st.error
            msg = when {
                !err.isNullOrBlank() -> "پخش شروع نشد: $err"
                st.buffering -> "فایل در حال بارگذاری است؛ اینترنت یا حجم فایل (حدود ۲۵ مگابایت) طول می‌کشد. صبر کن…"
                !st.hasMedia -> "آیتمِ صوت به پلیر نرسید (صف خالی است)."
                !st.connected -> "اتصال به سرویس پخش برقرار نشد."
                !st.playWhenReady -> "پلیر آماده بود اما پخش متوقف شد (توقفِ بی‌صدا)؛ دکمهٔ پخش را دوباره بزن."
                else -> "پخش شروع نشد؛ خطای پلیر ثبت نشد."
            }
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
                        msg = "\uD83D\uDD07 «زمان درس» روشن است — برای پخشِ صدا آن را خاموش کن."
                    } else if (fallback != null) {
                        val p = fallback!!
                        if (p.isPlaying) p.pause()
                        else {
                            com.hamyareman.ir.platform.feature.playback.TeachGate.pulse()
                            p.play()
                        }
                    } else if (state.playing) {
                        playback.pause()
                        savePos(track, posMs)
                    } else if (availability == false) {
                        msg = "صوت این درس هنوز روی سرور نیست؛ به‌زودی اضافه می‌شود."
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
                            // فقط وقتی منبعِ آیتمِ داخلِ صف (محلی/آنلاین) با وضعیت فعلی فایل هم‌خوانی
                            // دارد «ادامه» بده؛ وگرنه دوباره در صف بگذار — مثلاً بعد از دانلود،
                            // آیتمِ آنلاینِ قدیمی نباید در صف بماند و پخش محلی باید فعال شود.
                            if (loadedKey == track.cacheKey && state.hasMedia && !forceServer && (loadedLocal == cached(track))) {
                                com.hamyareman.ir.platform.feature.playback.TeachGate.pulse()
                                playback.play()
                            } else {
                                startTrack(track, autoplay = true)
                            }
                        }
                    }
                }) {
                    Text(
                        if (fallback?.isPlaying == true || (state.playing && loadedKey == track.cacheKey)) "⏸ توقف"
                        else "▶ پخش",
                    )
                }
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
                        if (totalBytes > 0) {
                            LinearProgressIndicator(
                                progress = { (doneBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.width(72.dp).height(6.dp),
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.width(72.dp).height(6.dp))
                        }
                        Column {
                            Text(
                                humanSize(doneBytes) + if (totalBytes > 0) " از ${humanSize(totalBytes)}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                            if (totalBytes > 0) {
                                Text(
                                    "${toPersianDigits(((doneBytes * 100L) / totalBytes).toString())}٪",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                )
                            }
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
                        downloading = true; progressPct = -1; doneBytes = 0L; totalBytes = 0L
                        scope.launch {
                            var ok = false
                            try {
                                withContext(Dispatchers.IO) {
                                    val remoteId = StudyMedia.resolveFileId(track.fileId)
                                    MediaVault.downloadEncrypted(
                                        context,
                                        StudyMedia.viewUrl(remoteId),
                                        track.cacheKey,
                                    ) { done, total ->
                                        doneBytes = done
                                        totalBytes = total
                                        progressPct = if (total > 0) ((done * 100L) / total).toInt() else -1
                                    }
                                }
                                ok = true
                                downloading = false; cacheTick++
                            } catch (e: Exception) {
                                downloading = false
                            }
                            msg = if (ok) "دانلود کامل شد؛ پخشِ بعدی آفلاین است." else "دانلود کامل نشد؛ دوباره تلاش کن."
                        }
                    }) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("دانلود", maxLines = 1)
                    }
                }
                if (fallback != null && fbDur > 0) {
                    Text(
                        "${teachMmss(if (dragMs >= 0) dragMs else fbPos)} / ${teachMmss(fbDur)}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                } else if (loadedKey == track.cacheKey && state.durationMs > 0) {
                    Text(
                        "${teachMmss(if (dragMs >= 0) dragMs else posMs)} / ${teachMmss(state.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            // ردیفِ تشخیصی: وضعیتِ واقعیِ پلیر همیشه روی صفحه است تا علتِ هر توقف
            // در یک نگاه معلوم شود (و با یک لمس رونوشت شود).
            val am = context.getSystemService(android.media.AudioManager::class.java)
            val mediaVol = am?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: -1
            val mediaMax = am?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: -1
            val btOn = runCatching { am?.isBluetoothA2dpOn == true }.getOrDefault(false)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "اتصال:${if (state.connected) "✓" else "✗"} · آیتم:${if (state.hasMedia) "✓" else "✗"} · " +
                        "پخش:${if (state.playing) "✓" else "✗"} · آماده:${if (state.playWhenReady) "✓" else "✗"} · " +
                        "بارگذاری:${if (state.buffering) "✓" else "✗"} · ولوم:$mediaVol/$mediaMax" +
                        (if (btOn) " · بلوتوث" else "") +
                        (if (fallback != null) " · پلیر:صفحه" else ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (fallback == null) {
                    TextButton(onClick = {
                        runCatching { playback.stop() }
                        startFallback(track, savedPos(track))
                        loadedKey = track.cacheKey
                        msg = "پلیرِ داخلیِ صفحه فعال شد."
                    }) { Text("پلیرِ صفحه", style = MaterialTheme.typography.labelSmall) }
                }
                TextButton(onClick = {
                    val st = state
                    val txt = listOf(
                        "اندروید: ${android.os.Build.VERSION.SDK_INT}",
                        "درس: $packId",
                        "فایل: ${track.fileId}",
                        "منبع: ${if (forceServer) "سرور" else if (MediaVault.isVerified(context, track.cacheKey)) "محلی(vault)" else "سرور"}",
                        "در دسترس: $availability",
                        "زمان درس (سکوت): ${quietOn()}",
                        "متصل: ${st.connected}",
                        "آیتم دارد: ${st.hasMedia}",
                        "در حال پخش: ${st.playing}",
                        "آماده‌ی پخش: ${st.playWhenReady}",
                        "در حال بارگذاری: ${st.buffering}",
                        "سرکوب‌شده: ${st.suppressed}",
                        "پلیرِ صفحه فعال: ${fallback != null}",
                        "ولومِ رسانه: " + (context.getSystemService(android.media.AudioManager::class.java)
                            ?.let { "${it.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)}/${it.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)}" } ?: "—"),
                        "بلوتوثِ صوتی: ${runCatching { context.getSystemService(android.media.AudioManager::class.java)?.isBluetoothA2dpOn == true }.getOrDefault(false)}",
                        "مدت: ${st.durationMs}",
                        "موقعیت: ${st.positionMs}",
                        "خطا: ${st.error ?: "—"}",
                    ).joinToString("\n")
                    val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("گزارش پخش", txt))
                    msg = "گزارش رونوشت شد؛ اینجا یا در پیام برایم بفرست."
                }) { Text("رونوشت", style = MaterialTheme.typography.labelSmall) }
            }
            if (mediaVol == 0) {
                Text(
                    "🔈 صدایِ رسانهٔ دستگاه روی صفر است — کمِ صدا را زیاد کن؛ هیچ پلیری با ولومِ صفر صدا ندارد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (btOn) {
                Text(
                    "🎧 خروجیِ صدا روی بلوتوث است — هدفون/اسپیکرِ بلوتوثی وصل است؟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            // «زمان درس» روشن باشد، پخشِ صدا عملاً غیرفعال است — این را واضح نشان می‌دهیم
            // و یک لمس برای خاموش‌کردن می‌گذاریم (قبلاً زدنِ پخش هیچ واکنشی نداشت).
            if (quietOn()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "\uD83D\uDD07 «زمان درس» روشن است — تا خاموش نشود، صدای تدریس پخش نمی‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        store.putString("quiet_mode", "0")
                        quietTick++
                        msg = null
                    }) { Text("خاموش کن") }
                }
            }
            if (state.suppressed && msg.isNullOrBlank()) {
                Text(
                    "پخش به‌خاطر تمرکزِ صوتیِ دستگاه موقتاً متوقف است (برنامهٔ دیگری در حال پخش است)؛ چند لحظه دیگر دوباره بزن.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            // وضعیتِ واقعیِ صوت به کاربر نشان داده می‌شود (قبلاً خطاها بی‌صدا قورت می‌شدند).
            val shownMsg = msg ?: state.error
            if (availability == false) {
                Text(
                    "صوت این درس هنوز روی سرور نیست — متن تدریس در دسترس است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = {
                    tracks.forEach { StudyMedia.forgetMissing(it.fileId) }
                    availability = null
                    msg = null
                }) { Text("بررسی دوباره") }
            } else if (availability == null) {
                Text(
                    "در حال بررسی صوتِ درس…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (!shownMsg.isNullOrBlank()) {
                Text(
                    shownMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = {
                    msg = null
                    if (fallback != null) { startFallback(track); return@TextButton }
                    forceServer = false
                    startTrack(track, autoplay = true, fromServer = true)
                }) { Text("تلاش دوباره") }
            }
            if (fallback != null && fbDur > 0) {
                Slider(
                    value = ((if (dragMs >= 0) dragMs else fbPos).toFloat() / fbDur).coerceIn(0f, 1f),
                    onValueChange = { dragMs = (it * fbDur).toLong() },
                    onValueChangeFinished = {
                        if (dragMs >= 0) {
                            if (abs(dragMs - fbPos) > 3000) TeachStats.addJump(context, packId)
                            fallback?.seekTo(dragMs)
                            fbPos = dragMs
                            savePos(track, dragMs)
                            dragMs = -1
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(26.dp),
                )
            } else if (loadedKey == track.cacheKey && state.durationMs > 0) {
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
                            fallback?.setPlaybackSpeed(v)
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
            var zoomed by remember { mutableStateOf(false) }
            val pager = rememberPagerState(pageCount = { st.pageCount })
            val seenPages = remember(fileId) { mutableSetOf<Int>() }
            val screenW = remember {
                ctx.resources.displayMetrics.widthPixels.coerceIn(640, 1080)
            }
            Text(
                "📕 کتاب درس — صفحه ${toPersianDigits((pager.currentPage + 1).toString())} از ${toPersianDigits(st.pageCount.toString())}",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(6.dp))
            VerticalPager(
                state = pager,
                modifier = Modifier.fillMaxWidth().weight(1f),
                userScrollEnabled = !zoomed,
                beyondViewportPageCount = 1,
            ) { index ->
                var bmp by remember(fileId, index) { mutableStateOf<Bitmap?>(pageCache[index]) }
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
                                        } else b
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val pageBmp = bmp
                    AndroidView(
                        factory = { c ->
                            PdfPageZoomView(c).apply {
                                onZoomed = { z -> zoomed = z }
                                bind(pageBmp)
                                tag = pageBmp
                            }
                        },
                        update = { v ->
                            v.onZoomed = { z -> zoomed = z }
                            if (v.tag !== pageBmp) {
                                v.tag = pageBmp
                                v.bind(pageBmp)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        TeachPdfState.Idle -> Card(modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

/** حجمِ خوانا برای نوارِ دانلود (مگابایت/کیلوبایت با ارقام فارسی). */
internal fun humanSize(bytes: Long): String {
    val mb = bytes / 1_048_576.0
    return if (mb >= 1) toPersianDigits("%.1f".format(mb)) + " مگابایت"
    else toPersianDigits((bytes / 1024).coerceAtLeast(0).toString()) + " کیلوبایت"
}
