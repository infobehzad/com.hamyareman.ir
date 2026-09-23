package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * زوم PDF با Canvas روی خودِ View — بدون LAYER_TYPE_SOFTWARE و بدون graphicsLayer.
 *
 * ریشهٔ ANR قبلی:
 *  ۱) لایهٔ نرم‌افزاری هر فریم پینچ کل بیت‌مپ را روی CPU می‌کشید → هنگ
 *  ۲) onZoomed در هر فریم، Pager را از نو می‌ساخت و fit() زوم را صفر می‌کرد → حلقه
 * این کلاس فقط وقتی پرچم زوم عوض شود خبر می‌دهد و fit را وسط ژست تکرار نمی‌کند.
 */
internal class PdfPageZoomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onZoomed: ((Boolean) -> Unit)? = null

    private val mat = Matrix()
    private val tmp = FloatArray(9)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var minScale = 1f
    private var maxScale = 2.8f
    private var bmpW = 1f
    private var bmpH = 1f
    private val last = PointF()
    private var panning = false
    private var fitted = false
    private var lastZoomed: Boolean? = null
    private var bitmap: Bitmap? = null

    private val scaleDet = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val cur = currentScale()
            var f = detector.scaleFactor
            if (!f.isFinite() || f <= 0f) return true
            val next = (cur * f).coerceIn(minScale, maxScale)
            f = next / cur
            if (!f.isFinite() || f <= 0f) return true
            mat.postScale(f, f, detector.focusX, detector.focusY)
            clamp()
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            notifyZoomIfChanged()
        }
    })

    private val tapDet = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val cur = currentScale()
            if (cur > minScale * 1.05f) {
                fit(force = true)
            } else {
                val target = (minScale * 2.0f).coerceAtMost(maxScale)
                val f = target / cur
                if (f.isFinite() && f > 0f) {
                    mat.postScale(f, f, e.x, e.y)
                    clamp()
                    invalidate()
                }
            }
            notifyZoomIfChanged()
            return true
        }
    })

    fun bind(bmp: Bitmap?) {
        if (bmp != null && bmp === bitmap && fitted) return
        if (bmp != null && bmp.isRecycled) {
            bitmap = null
            fitted = false
            invalidate()
            return
        }
        val same = bmp === bitmap
        bitmap = bmp
        if (bmp == null) {
            bmpW = 1f; bmpH = 1f
            fitted = false
            invalidate()
            return
        }
        bmpW = bmp.width.toFloat().coerceAtLeast(1f)
        bmpH = bmp.height.toFloat().coerceAtLeast(1f)
        if (!same || !fitted) {
            if (width > 0 && height > 0) fit(force = true) else post { if (width > 0) fit(force = true) }
        }
        invalidate()
    }

    private fun fit(force: Boolean) {
        if (!force && fitted && currentScale() > minScale * 1.02f) return
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val sx = vw / bmpW
        val sy = vh / bmpH
        minScale = min(sx, sy).let { if (!it.isFinite() || it <= 0f) 1f else it }
        maxScale = (minScale * 2.8f)
        mat.reset()
        mat.postScale(minScale, minScale)
        val dw = bmpW * minScale
        val dh = bmpH * minScale
        mat.postTranslate((vw - dw) / 2f, (vh - dh) / 2f)
        fitted = true
        lastZoomed = false
        invalidate()
    }

    private fun currentScale(): Float {
        mat.getValues(tmp)
        val s = tmp[Matrix.MSCALE_X]
        return if (s.isFinite() && s > 0f) s else minScale.coerceAtLeast(0.01f)
    }

    private fun clamp() {
        mat.getValues(tmp)
        var tx = tmp[Matrix.MTRANS_X]
        var ty = tmp[Matrix.MTRANS_Y]
        val sc = tmp[Matrix.MSCALE_X]
        if (!sc.isFinite() || sc <= 0f) {
            fit(force = true)
            return
        }
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val dw = bmpW * sc
        val dh = bmpH * sc
        tx = if (dw <= vw) (vw - dw) / 2f else min(0f, max(tx, vw - dw))
        ty = if (dh <= vh) (vh - dh) / 2f else min(0f, max(ty, vh - dh))
        if (!tx.isFinite()) tx = 0f
        if (!ty.isFinite()) ty = 0f
        tmp[Matrix.MTRANS_X] = tx
        tmp[Matrix.MTRANS_Y] = ty
        mat.setValues(tmp)
    }

    private fun notifyZoomIfChanged() {
        val z = currentScale() > minScale * 1.04f
        if (lastZoomed == z) return
        lastZoomed = z
        val cb = onZoomed
        post { runCatching { cb?.invoke(z) } }
    }

    override fun onDraw(canvas: Canvas) {
        val b = bitmap
        if (b == null || b.isRecycled) return
        canvas.drawBitmap(b, mat, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDet.onTouchEvent(event)
        tapDet.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                last.set(event.x, event.y)
                panning = true
                if (currentScale() > minScale * 1.04f) parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_MOVE -> {
                if (panning && !scaleDet.isInProgress && currentScale() > minScale * 1.04f) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    mat.postTranslate(event.x - last.x, event.y - last.y)
                    clamp()
                    invalidate()
                }
                last.set(event.x, event.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                panning = false
                if (currentScale() <= minScale * 1.04f) parent?.requestDisallowInterceptTouchEvent(false)
                notifyZoomIfChanged()
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bitmap != null && w > 0 && h > 0) {
            val wasZoomed = lastZoomed == true
            if (!wasZoomed) fit(force = true)
        }
    }
}
