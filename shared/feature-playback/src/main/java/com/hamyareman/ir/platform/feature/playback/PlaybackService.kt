package com.hamyareman.ir.platform.feature.playback

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
import androidx.media3.session.SessionResult

/**
 * وضعیت «صفحه‌ی تدریس باز است» — پل بین سرویس رسانه و UI (همان پروسه).
 * قانون v1.10: صوت تدریس فقط داخل صفحه‌ی تدریس پخش می‌شود؛ تا این صفحه باز
 * نشده، دکمه‌ی پلی اعلان به‌جای پخش، همان صفحه را باز می‌کند.
 */
object TeachGate {
    @Volatile var teachPageOpen: Boolean = false

    /** درسی که باید در باز شدن بعدی اپ، صفحه‌ی تدریسش باز شود. */
    @Volatile var requestedPack: String? = null

    /** v1.25 — درسی که همین الان در سرویس پخش است؛ مرجع واحد «همان درس» برای لمس اعلان. */
    @Volatile var currentPack: String? = null
}

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

        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            // کشیدن هدفون/قطع بلوتوث ⇒ توقف؛ نه پخش ناگهانی با بلندگو در جمع.
            .setHandleAudioBecomingNoisy(true)
            // فایل‌ها محلی‌اند (از حافظه‌ی گوشی)؛ wake lock محلی کافی است.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()
        // فقط پلی/مکث روی اعلان — prev/next را از فرمان‌های پلیر برمی‌داریم،
        // بدون فیلتر session (setMediaItems را هرگز محدود نکن).
        val player = object : ForwardingPlayer(exo) {
            override fun getAvailableCommands(): Player.Commands =
                Player.Commands.Builder()
                    .addAll(super.getAvailableCommands())
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .remove(Player.COMMAND_SEEK_TO_NEXT)
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .build()
        }

        mediaSession = MediaSession.Builder(this, player)
            // لمس خود اعلان → باز شدن صفحه‌ی تدریس همان درس (حتی وقتی اپ بسته است).
            .setSessionActivity(teachPendingIntent(currentPackOf(player)))
            // دکمه‌ی پلی اعلان هم مثل لمس اعلان: اول صفحه‌ی پلیر (تدریس) باز شود، بعد پخش.
            .setCallback(object : MediaSession.Callback {
                override fun onPlayerCommandRequest(
                    mediaSession: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    playerCommand: Int,
                ): Int {
                    val pack = currentPackOf(mediaSession.player) ?: TeachGate.currentPack
                    if (playerCommand == Player.COMMAND_PLAY_PAUSE && pack != null && !TeachGate.teachPageOpen) {
                        TeachGate.requestedPack = pack
                        mediaSession.setSessionActivity(teachPendingIntent(pack))
                        // PendingIntent تازه با packId همین رسانه — نه نسخه‌ی قدیمیِ کش‌شده
                        runCatching { teachPendingIntent(pack).send() }
                        return SessionResult.RESULT_ERROR_UNKNOWN
                    }
                    return super.onPlayerCommandRequest(mediaSession, controllerInfo, playerCommand)
                }
            })
            .build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val pack = currentPackOf(player)
                TeachGate.currentPack = pack
                mediaSession?.setSessionActivity(teachPendingIntent(pack))
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val pack = currentPackOf(player)
                    TeachGate.currentPack = pack
                    mediaSession?.setSessionActivity(teachPendingIntent(pack))
                }
                enforceForegroundOnly(player)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                enforceForegroundOnly(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                enforceForegroundOnly(player)
            }

            override fun onPositionDiscontinuity(
                oldPosition: androidx.media3.common.Player.PositionInfo,
                newPosition: androidx.media3.common.Player.PositionInfo,
                reason: Int,
            ) {
                val pack = currentPackOf(player)
                if (pack != TeachGate.currentPack) {
                    TeachGate.currentPack = pack
                    mediaSession?.setSessionActivity(teachPendingIntent(pack))
                }
            }
        })

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.ic_stat_audiobook)
            },
        )
    }

    /**
     * packId از mediaId (مثل «C903_E01-L02_AUDIO.mp3» → «C903_E01-L02»).
     * ترک‌های بخش‌بندی‌شده‌ی فارسی (…-1/…-2) هم به پک درسِ مادر برمی‌گردند.
     */
    private fun currentPackOf(player: Player?): String? {
        val id = player?.currentMediaItem?.mediaId ?: return null
        var pid = id.removeSuffix("_AUDIO.mp3").removeSuffix("_INTRO.mp3")
        if (pid.endsWith("-1") || pid.endsWith("-2")) pid = pid.dropLast(2)
        return pid.takeIf { it.isNotBlank() }
    }

    /** لمس اعلان → MainActivity با extra درس جاری؛ بقیه‌اش را nav اپ انجام می‌دهد. */
    private fun teachPendingIntent(packId: String?): PendingIntent =
        PendingIntent.getActivity(
            this,
            (packId ?: "none").hashCode(),
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

    /** اگر صفحه‌ی پخش باز نیست، هرگز صدا ادامه پیدا نکند. */
    private fun enforceForegroundOnly(player: Player) {
        if ((player.playWhenReady || player.isPlaying) && !TeachGate.teachPageOpen) {
            player.pause()
        }
    }

    /**
     * بستن اپ از recents = توقف کامل پخش (قانون: هرگز در پس‌زمینه).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.pause()
        stopSelf()
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
        const val TEACH_OPEN_ACTION = "com.hamyareman.ir.OPEN_TEACH"
        const val TEACH_OPEN_EXTRA = "open_pack"
        const val TEACH_OPEN_AUTOPLAY = "open_pack_autoplay"
        const val TEACH_ACTIVITY = "com.hamyareman.ir.MainActivity"
    }
}
