package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * زوم PDF روی لایهٔ نرم‌افزاری [PdfPageZoomView] — GPU/graphicsLayer روی بیت‌مپ
 * بزرگ گوشی را قفل می‌کند («اپ بسته شد»).
 */
@Composable
internal fun ZoomablePdfPage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onZoomed: (Boolean) -> Unit = {},
) {
    if (bitmap.isRecycled || bitmap.width < 1 || bitmap.height < 1) return
    AndroidView(
        factory = { ctx ->
            PdfPageZoomView(ctx).apply {
                this.onZoomed = onZoomed
                bind(bitmap)
            }
        },
        update = { view ->
            view.onZoomed = onZoomed
            view.bind(bitmap)
        },
        modifier = modifier.fillMaxSize(),
    )
}
