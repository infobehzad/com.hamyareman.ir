package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * زوم PDF روی [PdfPageZoomView] (Canvas، بدون لایهٔ نرم‌افزاری).
 * update فقط وقتی بیت‌مپ عوض شود bind می‌کند تا Pager وسط پینچ صفحه را از نو نسازد.
 */
@Composable
internal fun ZoomablePdfPage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onZoomed: (Boolean) -> Unit = {},
) {
    if (bitmap.isRecycled || bitmap.width < 1 || bitmap.height < 1) return
    val zoomCb = rememberUpdatedState(onZoomed)
    AndroidView(
        factory = { ctx ->
            PdfPageZoomView(ctx).apply {
                this.onZoomed = { z -> zoomCb.value(z) }
                bind(bitmap)
            }
        },
        update = { view ->
            view.onZoomed = { z -> zoomCb.value(z) }
            view.bind(bitmap)
        },
        modifier = modifier.fillMaxSize(),
    )
}
