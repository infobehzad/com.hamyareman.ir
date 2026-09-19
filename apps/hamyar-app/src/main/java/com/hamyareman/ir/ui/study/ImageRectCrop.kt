package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * برش مستطیلی عکس جزوه/کتاب: زوم، جابه‌جایی، چرخش، و **کادر با اندازهٔ قابل‌تغییر**.
 */
@Composable
fun ImageRectCropDialog(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onCropped: (Bitmap) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var ox by remember { mutableFloatStateOf(0f) }
    var oy by remember { mutableFloatStateOf(0f) }
    var turns by remember { mutableIntStateOf(0) }
    var frameW by remember { mutableFloatStateOf(0f) }
    var frameH by remember { mutableFloatStateOf(0f) }
    val oriented = remember(bitmap, turns) { rotateBitmap(bitmap, turns * 90f) }
    val img = remember(oriented) { oriented.asImageBitmap() }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
        ) {
            val density = LocalDensity.current
            val viewW = with(density) { maxWidth.toPx() }
            val viewH = with(density) { maxHeight.toPx() }
            val minFrame = with(density) { 96.dp.toPx() }
            val fw = (if (frameW <= 1f) viewW * 0.88f else frameW).coerceIn(minFrame, viewW * 0.98f)
            val fh = (if (frameH <= 1f) viewH * 0.52f else frameH).coerceIn(minFrame, viewH * 0.86f)
            val baseFit = min(fw / oriented.width.coerceAtLeast(1), fh / oriented.height.coerceAtLeast(1))
            val drawW = oriented.width * baseFit
            val drawH = oriented.height * baseFit

            fun clamp(s: Float, x: Float, y: Float): Triple<Float, Float, Float> {
                val ns = s.coerceIn(1f, 5f)
                val maxX = ((drawW * ns - fw) / 2f).coerceAtLeast(0f)
                val maxY = ((drawH * ns - fh) / 2f).coerceAtLeast(0f)
                return Triple(ns, x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(oriented, fw, fh) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val t = clamp(scale * zoom, ox + pan.x, oy + pan.y)
                            scale = t.first
                            ox = t.second
                            oy = t.third
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = img,
                    contentDescription = null,
                    modifier = Modifier
                        .size(
                            width = with(density) { drawW.toDp() },
                            height = with(density) { drawH.toDp() },
                        )
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = ox
                            translationY = oy
                        },
                )
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                ) {
                    val left = (size.width - fw) / 2f
                    val top = (size.height - fh) / 2f
                    drawRect(Color(0xB3000000))
                    drawRect(
                        Color.Black,
                        topLeft = Offset(left, top),
                        size = Size(fw, fh),
                        blendMode = BlendMode.DstOut,
                    )
                    drawRect(
                        Color.White.copy(alpha = 0.92f),
                        topLeft = Offset(left, top),
                        size = Size(fw, fh),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
                val handle = 28.dp
                val leftPx = (viewW - fw) / 2f
                val topPx = (viewH - fh) / 2f
                CornerHandle(
                    x = leftPx,
                    y = topPx,
                    sizeDp = handle,
                    onDrag = { dx, dy ->
                        frameW = (fw - dx * 2f).coerceIn(minFrame, viewW * 0.98f)
                        frameH = (fh - dy * 2f).coerceIn(minFrame, viewH * 0.86f)
                    },
                )
                CornerHandle(
                    x = leftPx + fw,
                    y = topPx,
                    sizeDp = handle,
                    onDrag = { dx, dy ->
                        frameW = (fw + dx * 2f).coerceIn(minFrame, viewW * 0.98f)
                        frameH = (fh - dy * 2f).coerceIn(minFrame, viewH * 0.86f)
                    },
                )
                CornerHandle(
                    x = leftPx,
                    y = topPx + fh,
                    sizeDp = handle,
                    onDrag = { dx, dy ->
                        frameW = (fw - dx * 2f).coerceIn(minFrame, viewW * 0.98f)
                        frameH = (fh + dy * 2f).coerceIn(minFrame, viewH * 0.86f)
                    },
                )
                CornerHandle(
                    x = leftPx + fw,
                    y = topPx + fh,
                    sizeDp = handle,
                    onDrag = { dx, dy ->
                        frameW = (fw + dx * 2f).coerceIn(minFrame, viewW * 0.98f)
                        frameH = (fh + dy * 2f).coerceIn(minFrame, viewH * 0.86f)
                    },
                )
            }

            Text(
                "گوشه‌های سفید را بکش تا اندازهٔ کادر عوض شود. زوم و چرخش هم هست.",
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp, start = 16.dp, end = 16.dp),
            )
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("انصراف") }
                OutlinedButton(
                    onClick = {
                        turns = (turns + 3) % 4
                        scale = 1f; ox = 0f; oy = 0f
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("چرخش") }
                Button(
                    onClick = {
                        val cropped = cropRect(oriented, drawW, drawH, scale, ox, oy, fw, fh)
                        if (cropped != null) onCropped(cropped)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("برش") }
            }
        }
    }
}

@Composable
private fun CornerHandle(
    x: Float,
    y: Float,
    sizeDp: androidx.compose.ui.unit.Dp,
    onDrag: (Float, Float) -> Unit,
) {
    val density = LocalDensity.current
    val half = with(density) { sizeDp.toPx() / 2f }
    Box(
        Modifier
            .offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
            .size(sizeDp)
            .background(Color.White, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x, drag.y)
                }
            },
    )
}

internal fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
    if (degrees % 360f == 0f) return src
    val m = Matrix().apply { postRotate(degrees) }
    return runCatching {
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }.getOrDefault(src)
}

private fun cropRect(
    src: Bitmap,
    drawW: Float,
    drawH: Float,
    scale: Float,
    ox: Float,
    oy: Float,
    frameW: Float,
    frameH: Float,
): Bitmap? {
    if (drawW <= 0f || drawH <= 0f || scale <= 0f) return null
    val srcLeft = ((-frameW / 2f - ox) / scale + drawW / 2f) * (src.width / drawW)
    val srcTop = ((-frameH / 2f - oy) / scale + drawH / 2f) * (src.height / drawH)
    val srcW = (frameW / scale) * (src.width / drawW)
    val srcH = (frameH / scale) * (src.height / drawH)
    val left = srcLeft.toInt().coerceIn(0, src.width - 1)
    val top = srcTop.toInt().coerceIn(0, src.height - 1)
    val w = srcW.toInt().coerceAtLeast(1).coerceAtMost(src.width - left)
    val h = srcH.toInt().coerceAtLeast(1).coerceAtMost(src.height - top)
    if (w <= 0 || h <= 0) return null
    return Bitmap.createBitmap(src, left, top, w, h)
}

internal fun compressReadableJpeg(src: Bitmap, maxSide: Int = 2048, quality: Int = 82): Pair<Bitmap, Int> {
    val longest = max(src.width, src.height).toFloat().coerceAtLeast(1f)
    val scaled = if (longest > maxSide) {
        val s = maxSide / longest
        Bitmap.createScaledBitmap(src, (src.width * s).toInt().coerceAtLeast(1), (src.height * s).toInt().coerceAtLeast(1), true)
    } else src
    return scaled to quality.coerceIn(70, 90)
}
