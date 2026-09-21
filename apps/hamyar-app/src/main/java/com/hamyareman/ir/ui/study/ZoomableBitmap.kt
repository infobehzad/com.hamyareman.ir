package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
/**
 * زوم امن روی بیت‌مپ — فقط تبدیل GPU (graphicsLayer)، بدون لایهٔ نرم‌افزاری
 * ImageView که روی صفحات بزرگ گوشی را قفل/کرش می‌کرد.
 */
@Composable
internal fun ZoomableBitmap(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onZoomed: (Boolean) -> Unit = {},
) {
    if (bitmap.isRecycled) return
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var ox by remember(bitmap) { mutableFloatStateOf(0f) }
    var oy by remember(bitmap) { mutableFloatStateOf(0f) }
    LaunchedEffect(scale) { onZoomed(scale > 1.02f) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = ox
                translationY = oy
            }
            .pointerInput(bitmap) {
                detectTransformGestures { _, pan, zoom, _ ->
                    runCatching {
                        val z = if (zoom.isFinite() && zoom > 0f) zoom else 1f
                        val next = (scale * z).coerceIn(1f, 2.6f)
                        scale = next
                        if (next > 1.02f) {
                            val limX = size.width.toFloat()
                            val limY = size.height.toFloat()
                            ox = (ox + pan.x).coerceIn(-limX, limX)
                            oy = (oy + pan.y).coerceIn(-limY, limY)
                        } else {
                            ox = 0f
                            oy = 0f
                        }
                    }
                }
            }
            .pointerInput(bitmap) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            ox = 0f
                            oy = 0f
                        } else {
                            scale = 2f
                        }
                    },
                )
            },
    )
}
