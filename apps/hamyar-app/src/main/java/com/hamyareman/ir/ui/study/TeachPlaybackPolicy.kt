package com.hamyareman.ir.ui.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * v1.25 — «شرط پخش تدریس: صفحه‌ی تدریس باز باشد» به معنای واقعی‌اش:
 * با خروج از صفحه به هر روشی — دکمه‌ی هوم، دکمه‌ی پنجره‌ها، سوییچ اپ،
 * خاموش/قفل‌شدن صفحه — پخش مکث می‌شود (دکمه‌ی بک از قبل با onDispose همین کار را می‌کند).
 * برگشت به صفحه پخش را از سر نمی‌گیرد؛ کاربر خودش پلی می‌زند (موقعیت حافظه‌دار است).
 */
@Composable
fun PauseOnStopEffect(pause: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) pause()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
