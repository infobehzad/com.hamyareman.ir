package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.content.Context

/**
 * رندر امن صفحهٔ PDF — سقفِ پیکسل تا گوشی‌های ضعیف با زوم قفل/کرش نکنند.
 * خطای حافظه یا رندر، null برمی‌گرداند نه استثنا.
 */
internal object PdfSafe {

    const val MAX_W = 1080
    const val MAX_H = 1920

    fun renderPage(page: PdfRenderer.Page, maxW: Int = MAX_W, maxH: Int = MAX_H): Bitmap? {
        return try {
            val pw = page.width.coerceAtLeast(1)
            val ph = page.height.coerceAtLeast(1)
            var scale = maxW.toFloat() / pw.toFloat()
            var w = (pw * scale).toInt().coerceIn(1, maxW)
            var h = (ph * scale).toInt().coerceAtLeast(1)
            if (h > maxH) {
                scale = maxH.toFloat() / ph.toFloat()
                w = (pw * scale).toInt().coerceIn(1, maxW)
                h = maxH
            }
            // سقف مطلق پیکسل (حدود ۱۶ مگاپیکسل) تا GPU/OOM نترکاند.
            val px = w.toLong() * h.toLong()
            if (px > 16L * 1024L * 1024L) {
                val f = kotlin.math.sqrt((12L * 1024L * 1024L).toDouble() / px.toDouble()).toFloat()
                w = (w * f).toInt().coerceAtLeast(1)
                h = (h * f).toInt().coerceAtLeast(1)
                scale = w.toFloat() / pw.toFloat()
            }
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            b.eraseColor(Color.WHITE)
            val m = android.graphics.Matrix().apply { setScale(scale, scale) }
            page.render(b, null, m, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            b
        } catch (oom: OutOfMemoryError) {
            runCatching { System.gc() }
            null
        } catch (t: Throwable) {
            null
        }
    }

    fun rotate(src: Bitmap, deg: Int): Bitmap {
        if (deg % 360 == 0) return src
        return try {
            val m = android.graphics.Matrix().apply { postRotate(deg.toFloat()) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        } catch (t: Throwable) {
            src
        }
    }

    fun decodeCover(ctx: Context, bookCode: String, maxSide: Int = 320): Bitmap? = runCatching {
        val path = "book-covers/$bookCode.jpg"
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val h = bounds.outHeight.coerceAtLeast(1)
        val w = bounds.outWidth.coerceAtLeast(1)
        while (h / sample > maxSide || w / sample > maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        ctx.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()

    fun decodeFileCapped(path: String, maxSide: Int = 1600): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        val h = bounds.outHeight.coerceAtLeast(1)
        val w = bounds.outWidth.coerceAtLeast(1)
        while (h / sample > maxSide || w / sample > maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(path, opts)
    }.getOrNull()
}
