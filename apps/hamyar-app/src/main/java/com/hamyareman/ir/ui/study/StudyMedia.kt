package com.hamyareman.ir.ui.study

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

    /**
     * v1.17 — قرارداد سراسری ویدیو: هر پک دقیقاً یک ویدیو دارد:
     * «<packId با خط‌تیره>-V01.mp4» (برای همه‌ی ۲۰۹ پک در باکت موجود است).
     */
    fun videoIds(packId: String): List<String> =
        listOf("${packId.replace("_", "-")}-V01.mp4")

    fun viewUrl(fileId: String): String =
        "$ENDPOINT/storage/buckets/$BUCKET/files/$fileId/view?project=$PROJECT"
}
