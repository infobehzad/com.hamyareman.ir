package ir.behzad.platform.feature.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * سرویس پخش کتاب صوتی (Media3).
 *
 * چرا سرویس و نه `MediaPlayer` داخل صفحه:
 *  - با خاموش‌شدن صفحه، رفتن به اپ دیگر یا بستن اپ از recents، **پخش ادامه دارد**؛
 *  - اعلان سیستمی با کنترل پخش/توقف و جابه‌جایی ساخته می‌شود (`DefaultMediaNotificationProvider`)؛
 *  - مدیریت AudioFocus و «کشیدن هدفون = توقف» به خود Media3 سپرده می‌شود.
 *
 * حریم خصوصی: سرویس در منیفست `exported="false"` است، یعنی هیچ اپ دیگری نمی‌تواند به
 * session وصل شود و فایل‌های خصوصی زهرا را بخواند.
 *
 * `@UnstableApi` چون `MediaSessionService`/`DefaultMediaNotificationProvider` در Media3
 * با این علامت آمده‌اند؛ این یک قرارداد نسخه‌گذاری است، نه نشانه‌ی ناپایداری عملکرد.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // کتاب صوتی یعنی «گفتار»: اکولایزر/افکت سیستم با این نوع محتوا درست رفتار می‌کند.
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            // کشیدن هدفون/قطع بلوتوث ⇒ توقف؛ نه پخش ناگهانی با بلندگو در جمع.
            .setHandleAudioBecomingNoisy(true)
            // فایل‌ها محلی‌اند (از حافظه‌ی گوشی)؛ wake lock محلی کافی است.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            // لمس اعلان پخش → باز شدن صفحه‌ی تدریس همان درس (حتی وقتی اپ بسته است).
            .setSessionActivity(teachPendingIntent(currentPackOf(player)))
            .build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaSession?.setSessionActivity(teachPendingIntent(currentPackOf(player)))
            }
        })

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.ic_stat_audiobook)
            },
        )
    }

    /** packId از mediaId (مثل «C903_E01-L02_AUDIO.mp3» → «C903_E01-L02»). */
    private fun currentPackOf(player: Player?): String? {
        val id = player?.currentMediaItem?.mediaId ?: return null
        return id.removeSuffix("_AUDIO.mp3").removeSuffix("_INTRO.mp3").takeIf { it.isNotBlank() }
    }

    /** لمس اعلان → MainActivity با extra درس جاری؛ بقیه‌اش را nav اپ انجام می‌دهد. */
    private fun teachPendingIntent(packId: String?): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent().apply {
                setClassName(packageName, TEACH_ACTIVITY)
                action = TEACH_OPEN_ACTION
                putExtra(TEACH_OPEN_EXTRA, packId)
                putExtra(TEACH_OPEN_AUTOPLAY, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * وقتی کاربر اپ را از recents می‌بندد: اگر چیزی در حال پخش است سرویس زنده می‌ماند
     * (وگرنه کل مفهوم «پخش در پس‌زمینه» از بین می‌رفت)؛ اگر پخش متوقف است، سرویس را
     * می‌بندیم تا باتری و اعلان الکی نماند.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        /** ۳۰ ثانیه — همان مقداری که UI هم برای دکمه‌های «عقب/جلو» استفاده می‌کند. */
        const val SEEK_INCREMENT_MS = 30_000L

        /** لمس اعلان پخش → باز شدن صفحه‌ی تدریس همان درس (v1.9). */
        const val TEACH_OPEN_ACTION = "ir.behzad.roozhayeman.OPEN_TEACH"
        const val TEACH_OPEN_EXTRA = "open_pack"
        const val TEACH_OPEN_AUTOPLAY = "open_pack_autoplay"
        const val TEACH_ACTIVITY = "ir.behzad.roozhayeman.MainActivity"
    }
}
