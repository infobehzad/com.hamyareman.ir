package com.hamyareman.ir.ui.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.hamyareman.ir.BuildConfig
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton

/**
 * میزبانِ «کانالِ آپدیت» — یک بار پس از ورود و بازشدنِ قفل اجرا می‌شود:
 *
 * ۱. تنظیماتِ انتشار را از ردیفِ `app_release` در `app_state` می‌خواند (با کشِ ۶ساعته).
 * ۲. اگر نسخهٔ سرور تازه‌تر بود، دیالوگ نشان می‌دهد: توصیه‌ای (با «بعداً») یا
 *    اجباری (بدونِ بستن).
 * ۳. «دانلود و نصب» = دانلودِ درون‌برنامه‌ای + نصب‌کنندهٔ سیستم؛ اگر نشد، همان
 *    نشانی در مرورگر باز می‌شود.
 *
 * عمداً **پس از ورود و بازشدنِ قفل** اجرا می‌شود: پیامِ آپدیت نباید مسیرِ ورود،
 * بازیابیِ رمز یا کارِ کاربرِ آفلاین را ببندد.
 */
@Composable
fun UpdateGateHost() {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current

    var decision by remember { mutableStateOf<UpdateDecision?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        decision = UpdateChecker.decide(ctx, container.tables, BuildConfig.VERSION_CODE)
    }

    // پایانِ دانلود → نصب‌کننده. گیرندهٔ زمانِ اجرا، فقط تا وقتی دانلودی در جریان است.
    DisposableEffect(downloading) {
        if (!downloading) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                downloading = false
                if (ApkUpdate.isReady(ctx)) {
                    if (!ApkUpdate.install(ctx)) {
                        ApkUpdate.explain(ctx)
                        note = "نصب‌کننده باز نشد؛ فایلِ دانلودشده را دستی نصب کن."
                    }
                } else {
                    note = "دانلود کامل نشد؛ با «دانلود در مرورگر» دوباره امتحان کن."
                }
            }
        }
        runCatching {
            ContextCompat.registerReceiver(
                ctx,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }

    val d = decision
    val info: UpdateInfo = when (d) {
        is UpdateDecision.Forced -> d.info
        is UpdateDecision.Optional -> d.info
        else -> return
    }
    val forced = d is UpdateDecision.Forced

    // در حالتِ اجباری، دکمهٔ back هم بی‌اثر است.
    BackHandler(enabled = forced) { }

    AlertDialog(
        onDismissRequest = { if (!forced) dismiss(ctx, info) { decision = UpdateDecision.None } },
        properties = DialogProperties(
            dismissOnBackPress = !forced,
            dismissOnClickOutside = !forced,
        ),
        title = {
            Text(if (forced) "به‌روزرسانیِ همیار من لازم است" else "نسخه‌ی تازه‌ی همیار من آماده است")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "نسخه‌ی تو: ${BuildConfig.VERSION_NAME}" +
                        "  •  نسخه‌ی تازه: ${UpdatePlan.versionLabel(info)}" +
                        if (info.size > 0) "  •  حجم: ${UpdatePlan.sizeLabel(info.size)}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (forced) {
                    Text(
                        "برای ادامه‌ی کار باید نسخه‌ی تازه نصب شود. دانلودش اینترنت لازم دارد.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                info.notes.take(6).forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyMedium)
                }
                if (downloading) {
                    Text("در حال دانلود… وقتی تمام شد، نصب‌کننده خودش باز می‌شود.")
                }
                note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            if (downloading) {
                PrimaryButton("در حال دانلود…") { }
            } else {
                PrimaryButton("دانلود و نصب") {
                    note = null
                    if (ApkUpdate.download(ctx, info.url)) {
                        downloading = true
                    } else {
                        UpdateChecker.openInBrowser(ctx, info.url)
                    }
                }
            }
        },
        // در حالتِ اجباری، دکمهٔ انصراف عمداً خالی است (فقط «دانلود و نصب» می‌ماند).
        dismissButton = {
            if (!forced) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { UpdateChecker.openInBrowser(ctx, info.url) }) {
                        Text("دانلود در مرورگر")
                    }
                    TextButton(onClick = { dismiss(ctx, info) { decision = UpdateDecision.None } }) {
                        Text("بعداً")
                    }
                }
            }
        },
    )
}

/** «بعداً»: تا نسخهٔ بعدی دیگر پرسیده نشود (فقط برای حالتِ توصیه‌ای). */
private fun dismiss(ctx: Context, info: UpdateInfo, onDone: () -> Unit) {
    UpdateChecker.skip(ctx, info.latest)
    onDone()
}
