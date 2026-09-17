package com.hamyareman.ir.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.ui.profile.StudentProfileState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ProfileClockAvatar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline
    val time = remember(now) { LocalDateTime.ofInstant(Instant.ofEpochMilli(now), JalaliDate.TEHRAN) }
    val avatarPath = StudentProfileState.avatarPath
    val initial = StudentProfileState.firstName.take(1).ifBlank { "؟" }
    val bmp = remember(avatarPath) {
        avatarPath.takeIf { it.isNotBlank() }?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }
    Box(
        modifier
            .size(108.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(primary.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r,
                ),
            )
            drawCircle(
                color = primary.copy(alpha = 0.85f),
                radius = r - 1.5.dp.toPx(),
                style = Stroke(width = 2.5.dp.toPx()),
            )
            for (i in 0 until 12) {
                val rad = Math.toRadians(i * 30.0 - 90.0)
                val outer = r - 3.dp.toPx()
                val inner = outer - if (i % 3 == 0) 8.dp.toPx() else 5.dp.toPx()
                drawLine(
                    color = if (i % 3 == 0) primary else outline,
                    start = Offset(cx + cos(rad).toFloat() * inner, cy + sin(rad).toFloat() * inner),
                    end = Offset(cx + cos(rad).toFloat() * outer, cy + sin(rad).toFloat() * outer),
                    strokeWidth = if (i % 3 == 0) 2.4.dp.toPx() else 1.4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            val sec = time.second + time.nano / 1_000_000_000f
            val min = time.minute + sec / 60f
            val hour = (time.hour % 12) + min / 60f
            fun hand(angleDeg: Float, length: Float, back: Float, color: Color, width: Float) {
                rotate(angleDeg, Offset(cx, cy)) {
                    drawLine(
                        brush = Brush.verticalGradient(
                            listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.95f), color.copy(alpha = 0.4f)),
                        ),
                        start = Offset(cx, cy + back),
                        end = Offset(cx, cy - length),
                        strokeWidth = width,
                        cap = StrokeCap.Round,
                    )
                }
            }
            hand(hour * 30f, r * 0.32f, r * 0.10f, primary, 4.2.dp.toPx())
            hand(min * 6f, r * 0.40f, r * 0.12f, secondary, 3.1.dp.toPx())
            hand(sec * 6f, r * 0.44f, r * 0.14f, tertiary, 1.6.dp.toPx())
            drawCircle(color = primary, radius = 4.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color = Color.White.copy(alpha = 0.55f), radius = 1.8.dp.toPx(), center = Offset(cx, cy))
        }
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            if (bmp != null) {
                Image(
                    bmp.asImageBitmap(),
                    contentDescription = "عکس پروفایل",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(initial, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}
