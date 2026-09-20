package com.hamyareman.ir.ui.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * دانلود و نصبِ درون‌برنامه‌ایِ APK.
 *
 * مسیرِ فایل زیرِ `getExternalFilesDir(null)/updates` است و همین مسیر در
 * `res/xml/file_paths.xml` به FileProvider معرفی شده تا نصب‌کنندهٔ سیستم اجازهٔ
 * خواندنش را داشته باشد. هر مرحله اگر شکست بخورد `false` برمی‌گردد و فراخوان
 * همان نشانی را در مرورگر باز می‌کند — کاربر هیچ‌وقت پشتِ دیوار نمی‌ماند.
 *
 * چرا DownloadManager: دانلود در پس‌زمینه ادامه پیدا می‌کند، اعلانِ پیشرفت
 * دارد و اگر کاربر بین راه اپ را ببندد نیمه‌کاره نمی‌ماند.
 */
object ApkUpdate {

    const val DIR = "updates"
    const val FILE_NAME = "hamyar-update.apk"
    const val MIME_APK = "application/vnd.android.package-archive"

    fun file(ctx: Context): File = File(File(ctx.getExternalFilesDir(null), DIR), FILE_NAME)

    /** شروعِ دانلود؛ `false` = نشد (فراخوان باید مرورگر را باز کند). */
    fun download(ctx: Context, url: String): Boolean = runCatching {
        val dir = File(ctx.getExternalFilesDir(null), DIR)
        dir.mkdirs()
        // فایلِ نیمه‌کارهٔ دانلودِ قبلی جلوی دانلودِ تازه را نگیرد.
        dir.listFiles()?.forEach { it.delete() }
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("همیار من")
            .setDescription("دانلودِ نسخهٔ تازه…")
            .setMimeType(MIME_APK)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(ctx, null, "$DIR/$FILE_NAME")
        val manager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        true
    }.getOrDefault(false)

    /** فایلِ دانلودشده هست و حجمِ معقولی دارد؟ */
    fun isReady(ctx: Context): Boolean = runCatching {
        val f = file(ctx)
        f.exists() && f.length() > 1024L * 100L
    }.getOrDefault(false)

    /** اجرای نصب‌کنندهٔ سیستم روی فایلِ دانلودشده. */
    fun install(ctx: Context): Boolean {
        val f = file(ctx)
        if (!f.exists() || f.length() <= 0L) return false
        return runCatching {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, MIME_APK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    /** خروجیِ کوتاه برای کاربر وقتی نصب‌کننده باز نمی‌شود. */
    fun explain(ctx: Context) {
        runCatching {
            Toast.makeText(
                ctx,
                "برای نصب، اجازهٔ «نصب برنامه‌های ناشناس» را برای همیار من روشن کن.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
