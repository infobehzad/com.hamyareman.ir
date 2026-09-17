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
 * زوم/پن PDF روی خودِ ImageView (لایهٔ سخت‌افزاری) — بدون State کامپوز در هر فریم.
 * وقتی زوم > ۱ است، اسکرول والد گرفته می‌شود تا پن روان بماند.
 */
internal class PdfPageZoomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {

    var onZoomed: ((Boolean) -> Unit)? = null

    private val mat = Matrix()
    private val tmp = FloatArray(9)
    private var minScale = 1f
    private var maxScale = 5f
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
            val cur = currentScale()
            var f = detector.scaleFactor
            val next = (cur * f).coerceIn(minScale, maxScale)
            f = next / cur
            mat.postScale(f, f, detector.focusX, detector.focusY)
            clamp()
            imageMatrix = mat
            notifyZoom()
            return true
        }
    })

    private val tapDet = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val cur = currentScale()
            if (cur > minScale * 1.05f) {
                fit()
            } else {
                val target = (minScale * 2.4f).coerceAtMost(maxScale)
                val f = target / cur
                mat.postScale(f, f, e.x, e.y)
                clamp()
                imageMatrix = mat
                notifyZoom()
            }
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isClickable = true
    }

    fun bind(bitmap: Bitmap?) {
        setImageBitmap(bitmap)
        if (bitmap == null) {
            bmpW = 1f; bmpH = 1f
            return
        }
        bmpW = bitmap.width.toFloat().coerceAtLeast(1f)
        bmpH = bitmap.height.toFloat().coerceAtLeast(1f)
        post { fit() }
    }

    private fun fit() {
        val vw = width.toFloat().coerceAtLeast(1f)
        minScale = vw / bmpW
        maxScale = minScale * 5f
        mat.reset()
        mat.postScale(minScale, minScale)
        imageMatrix = mat
        notifyZoom()
    }

    private fun currentScale(): Float {
        mat.getValues(tmp)
        return tmp[Matrix.MSCALE_X].coerceAtLeast(0.01f)
    }

    private fun clamp() {
        mat.getValues(tmp)
        var tx = tmp[Matrix.MTRANS_X]
        var ty = tmp[Matrix.MTRANS_Y]
        val sc = tmp[Matrix.MSCALE_X]
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val dw = bmpW * sc
        val dh = bmpH * sc
        if (dw <= vw) tx = 0f
        else tx = min(0f, max(tx, vw - dw))
        if (dh <= vh) ty = 0f
        else ty = min(0f, max(ty, vh - dh))
        tmp[Matrix.MTRANS_X] = tx
        tmp[Matrix.MTRANS_Y] = ty
        mat.setValues(tmp)
    }

    private fun notifyZoom() {
        val z = currentScale() > minScale * 1.02f
        onZoomed?.invoke(z)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDet.onTouchEvent(event)
        tapDet.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                last.set(event.x, event.y)
                panning = true
                if (currentScale() > minScale * 1.02f) parent?.requestDisallowInterceptTouchEvent(true)
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
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (drawable != null && w > 0) fit()
    }
}
