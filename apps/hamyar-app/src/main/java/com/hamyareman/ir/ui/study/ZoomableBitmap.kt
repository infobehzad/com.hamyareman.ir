package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
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
 * زوم امن روی بیت‌مپ — فقط تبدیل GPU. بیت‌مپ بازیافتی یا اندازهٔ صفر کرش نمی‌کند.
 * سقف زوم ۲٫۶ تا حافظه/GPU قفل نشود.
 */
@Composable
internal fun ZoomableBitmap(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onZoomed: (Boolean) -> Unit = {},
) {
    if (bitmap.isRecycled || bitmap.width < 1 || bitmap.height < 1) return
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var ox by remember(bitmap) { mutableFloatStateOf(0f) }
    var oy by remember(bitmap) { mutableFloatStateOf(0f) }
    LaunchedEffect(scale) { onZoomed(scale > 1.02f) }
    Box(modifier.fillMaxSize().clipToBounds()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = if (scale.isFinite()) scale.coerceIn(1f, 2.6f) else 1f
                    scaleX = s
                    scaleY = s
                    translationX = if (ox.isFinite()) ox else 0f
                    translationY = if (oy.isFinite()) oy else 0f
                }
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        runCatching {
                            if (size.width < 8 || size.height < 8) return@runCatching
                            if (bitmap.isRecycled) return@runCatching
                            val z = if (zoom.isFinite() && zoom > 0f) zoom else 1f
                            val next = (scale * z).coerceIn(1f, 2.6f)
                            scale = next
                            if (next > 1.02f) {
                                val limX = size.width.toFloat()
                                val limY = size.height.toFloat()
                                val nx = ox + (if (pan.x.isFinite()) pan.x else 0f)
                                val ny = oy + (if (pan.y.isFinite()) pan.y else 0f)
                                ox = nx.coerceIn(-limX, limX)
                                oy = ny.coerceIn(-limY, limY)
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
}
