package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

/**
 * زوم/پن PDF روی خودِ ImageView.
 * لایهٔ نرم‌افزاری (نه GPU) تا زوم روی صفحات بزرگ گوشی را قفل/کرش نکند.
 */
internal class PdfPageZoomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {

    var onZoomed: ((Boolean) -> Unit)? = null

    private val mat = Matrix()
    private val tmp = FloatArray(9)
    private var minScale = 1f
    private var maxScale = 3.2f
    private var bmpW = 1f
    private var bmpH = 1f
    private val last = PointF()
    private var panning = false

    private val scaleDet = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            runCatching {
                val cur = currentScale()
                var f = detector.scaleFactor
                if (!f.isFinite() || f <= 0f) return true
                val next = (cur * f).coerceIn(minScale, maxScale)
                f = next / cur
                if (!f.isFinite() || f <= 0f) return true
                mat.postScale(f, f, detector.focusX, detector.focusY)
                clamp()
                imageMatrix = mat
                notifyZoom()
            }
            return true
        }
    })

    private val tapDet = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            runCatching {
                val cur = currentScale()
                if (cur > minScale * 1.05f) {
                    fit()
                } else {
                    val target = (minScale * 2.2f).coerceAtMost(maxScale)
                    val f = target / cur
                    if (f.isFinite() && f > 0f) {
                        mat.postScale(f, f, e.x, e.y)
                        clamp()
                        imageMatrix = mat
                        notifyZoom()
                    }
                }
            }
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
        // HARDWARE روی بیت‌مپ بزرگ + ماتریس زوم = کرش GPU روی بعضی گوشی‌ها.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true
    }

    private var bound: Bitmap? = null

    fun bind(bitmap: Bitmap?) {
        runCatching {
            if (bitmap != null && bitmap === bound && drawable != null) return
            if (bitmap != null && bitmap.isRecycled) {
                setImageBitmap(null)
                bound = null
                return
            }
            bound = bitmap
            setImageBitmap(bitmap)
            if (bitmap == null) {
                bmpW = 1f; bmpH = 1f
                return
            }
            bmpW = bitmap.width.toFloat().coerceAtLeast(1f)
            bmpH = bitmap.height.toFloat().coerceAtLeast(1f)
            post { runCatching { fit() } }
        }
    }

    private fun fit() {
        val vw = width.toFloat().coerceAtLeast(1f)
        minScale = vw / bmpW
        if (!minScale.isFinite() || minScale <= 0f) minScale = 1f
        maxScale = (minScale * 3.2f).coerceAtMost(minScale * 4f)
        mat.reset()
        mat.postScale(minScale, minScale)
        imageMatrix = mat
        notifyZoom()
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
            fit()
            return
        }
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val dw = bmpW * sc
        val dh = bmpH * sc
        tx = if (dw <= vw) 0f else min(0f, max(tx, vw - dw))
        ty = if (dh <= vh) 0f else min(0f, max(ty, vh - dh))
        if (!tx.isFinite()) tx = 0f
        if (!ty.isFinite()) ty = 0f
        tmp[Matrix.MTRANS_X] = tx
        tmp[Matrix.MTRANS_Y] = ty
        mat.setValues(tmp)
    }

    private fun notifyZoom() {
        runCatching {
            val z = currentScale() > minScale * 1.02f
            post { runCatching { onZoomed?.invoke(z) } }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return try {
            scaleDet.onTouchEvent(event)
            tapDet.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    last.set(event.x, event.y)
                    panning = true
                    if (currentScale() > minScale * 1.02f) parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (panning && !scaleDet.isInProgress && currentScale() > minScale * 1.02f) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        mat.postTranslate(event.x - last.x, event.y - last.y)
                        clamp()
                        imageMatrix = mat
                    }
                    last.set(event.x, event.y)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    panning = false
                    if (currentScale() <= minScale * 1.02f) parent?.requestDisallowInterceptTouchEvent(false)
                    notifyZoom()
                }
            }
            true
        } catch (t: Throwable) {
            true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (drawable != null && w > 0) runCatching { fit() }
    }
}
