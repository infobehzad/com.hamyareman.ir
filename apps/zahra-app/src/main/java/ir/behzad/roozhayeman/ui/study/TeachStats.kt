package ir.behzad.roozhayeman.ui.study

import android.content.Context
import ir.behzad.platform.core.common.LocalStore
import org.json.JSONObject

/**
 * موتور آمارِ «تدریس» — ثبتِ خودکارِ رویدادها + محاسبه‌ی فقط‌خواندنی.
 *
 * اصول طراحی (مصوب):
 *  - فقط رویداد را می‌توان ثبت کرد (ثانیه‌ی پخش، پرش، نشست، اتمام دوره)؛
 *    هیچ API ای برای ویرایش/حذف عدد قبلی وجود ندارد → آمار «غیرقابل ویرایش».
 *  - هر پک یک سطر JSON در LocalStore("hamyar_teach_stats") دارد.
 *  - سنجه‌ها: نشست‌ها، ثانیه‌ی شنیدن صوت، ثانیه‌ی تماشای ویدیو، پرش‌های >۳s،
 *    اتمام «اولین دوره‌ی تدریس» (صوت تا انتها یا ویدیو ≥۹۵٪) در چند نشست.
 */
object TeachStats {

    data class Snap(
        val sessions: Int = 0,
        val listenSec: Int = 0,
        val videoSec: Int = 0,
        val jumps: Int = 0,
        val done: Boolean = false,
        val startedAtMs: Long = 0L,
        val completedAtMs: Long = 0L,
        val audioDurSec: Int = 0,
        val videoDurSec: Int = 0,
    ) {
        /** کل زمان تدریس‌شده (صوت + ویدیو) به ثانیه. */
        val watchedSec: Int get() = listenSec + videoSec

        /** کل طول محتوایی که تاکنون دیده/شنیده‌ایم (بیشینه‌ی طول‌های گزارش‌شده). */
        val totalSec: Int get() = audioDurSec + videoDurSec

        /** ثانیه‌ی باقی‌مانده تا پایان اولین دوره (هرگز منفی نمی‌شود). */
        val remainSec: Int get() = (totalSec - watchedSec).coerceAtLeast(0)

        /** درصد پیشرفت دوره‌ی اول (۰ تا ۱۰۰). */
        val passPct: Int
            get() = if (totalSec <= 0) 0 else ((watchedSec * 100L) / totalSec).toInt().coerceIn(0, 100)
    }

    private fun store(ctx: Context) = LocalStore(ctx.applicationContext, "hamyar_teach_stats")

    private fun key(packId: String) = "ts_$packId"

    private fun read(ctx: Context, packId: String): JSONObject =
        runCatching { JSONObject(store(ctx).getString(key(packId), "{}")) }.getOrDefault(JSONObject())

    private fun write(ctx: Context, packId: String, o: JSONObject) {
        store(ctx).putString(key(packId), o.toString())
    }

    /** شروع یک نشست تدریس — هر بازشدن صفحه‌ی تدریس یک بار. */
    fun enter(ctx: Context, packId: String) {
        val o = read(ctx, packId)
        o.put("s", o.optInt("s") + 1)
        if (o.optLong("st") == 0L) o.put("st", System.currentTimeMillis())
        write(ctx, packId, o)
    }

    /** ثبت ثانیه‌های شنیدن صوت (رویدادی از پلیر؛ [sec] ذخیره‌ی دوره‌ای است). */
    fun addListen(ctx: Context, packId: String, sec: Int, trackDurationSec: Int) {
        if (sec <= 0) return
        val o = read(ctx, packId)
        o.put("ls", o.optInt("ls") + sec)
        if (trackDurationSec > o.optInt("ad")) o.put("ad", trackDurationSec)
        write(ctx, packId, o)
    }

    /** ثبت ثانیه‌های تماشای واقعی ویدیو. */
    fun addVideo(ctx: Context, packId: String, sec: Int, trackDurationSec: Int) {
        if (sec <= 0) return
        val o = read(ctx, packId)
        o.put("vs", o.optInt("vs") + sec)
        if (trackDurationSec > o.optInt("vd")) o.put("vd", trackDurationSec)
        write(ctx, packId, o)
    }

    /** ثبت یک پرشِ بیش از ۳ ثانیه در پلیر. */
    fun addJump(ctx: Context, packId: String) {
        val o = read(ctx, packId)
        o.put("j", o.optInt("j") + 1)
        write(ctx, packId, o)
    }

    /** پایان اولین دوره‌ی تدریس — فقط بار اول زمان ثبت می‌شود (تاریخ اتمام). */
    fun markFirstPassDone(ctx: Context, packId: String) {
        val o = read(ctx, packId)
        if (!o.optBoolean("d")) {
            o.put("d", true)
            o.put("ca", System.currentTimeMillis())
            write(ctx, packId, o)
        }
    }

    fun isDone(ctx: Context, packId: String): Boolean = read(ctx, packId).optBoolean("d")

    fun snap(ctx: Context, packId: String): Snap {
        val o = read(ctx, packId)
        return Snap(
            sessions = o.optInt("s"),
            listenSec = o.optInt("ls"),
            videoSec = o.optInt("vs"),
            jumps = o.optInt("j"),
            done = o.optBoolean("d"),
            startedAtMs = o.optLong("st"),
            completedAtMs = o.optLong("ca"),
            audioDurSec = o.optInt("ad"),
            videoDurSec = o.optInt("vd"),
        )
    }

    /** همه‌ی پک‌هایی که آمار دارند (برای صفحه‌ی پیشرفت). */
    fun allPackIds(ctx: Context): List<String> {
        val s = store(ctx)
        // LocalStore همه‌ی کلیدها را ندارد؛ پس کلیدهای شناخته‌شده‌ی رجیستری را می‌پیماییم.
        val ids = ir.behzad.platform.feature.study.BookModuleRegistry.modules
            .asSequence().flatMap { it.packs }.map { it.packId }.toList()
        return ids.filter { read(ctx, it).length() > 0 }
    }
}
