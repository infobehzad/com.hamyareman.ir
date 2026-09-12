package ir.behzad.roozhayeman.ui.study

/**
 * نقشه‌ی رسانه‌های درس‌ها — صوت/ویدیوی هر پک از باکت عمومی Appwrite
 * بر حسب قرارداد نام‌گذاری بارگذاری می‌شود:
 *  - صوت:  `<packId>_AUDIO.mp3` (فیلد audioFileId خود پک معتبر است)
 *  - اینترو: `<packId>_INTRO_AUDIO.mp3`
 *  - ویدیو: `<packId با خط‌تیره>-V01.mp4` (تا V03)
 */
object StudyMedia {
    const val BUCKET = "6aa1eaae00303400117b"
    private const val PROJECT = "6a9d59e3002751cc3ea8"
    private const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"

    /** تعداد ویدیوی موجود در گیت برای هر پک (صفر یعنی فعلاً ویدیو نداریم). */
    private val videoCounts: Map<String, Int> = mapOf(
        "C909_L01" to 3,
        "C907_E01-L01" to 3,
        "C910_L01" to 3,
        "C911_L01" to 3,
        "C902_E01-L01" to 3,
        "C903_E01-L01" to 0,
        "C908_E01-L01" to 3,
        "C917_E01-L01" to 3,
        "C905_E01-L01" to 3,
        "C905_E01-L02" to 3,
        "C905_E01-L03" to 3,
        "C905_E01-L04" to 3,
        "C905_E01-EXAM" to 0,
        "C904_L01" to 3,
        "C901_L01" to 3,
        "C906_E01-L01" to 3,
        "C941_L01" to 3,
    )

    fun videoIds(packId: String): List<String> {
        val base = packId.replace("_", "-")
        val n = videoCounts[base] ?: 0
        return (1..n).map { "$base-V0$it.mp4" }
    }

    /** URL نمای فایل (باکت با دسترسی خواندن عمومی) — قابل پخش مستقیم در پلیر. */
    fun viewUrl(fileId: String): String =
        "$ENDPOINT/storage/buckets/$BUCKET/files/$fileId/view?project=$PROJECT"
}
