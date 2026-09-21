package com.hamyareman.ir.ui.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.hamyareman.ir.BuildConfig
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import kotlinx.coroutines.launch

/**
 * کانالِ آپدیت — سه تکه:
 *
 * 1. [UpdateGateHost] — خودکار، یک بار پس از ورود و بازشدنِ قفل.
 * 2. [UpdateDialog] — دیالوگِ مشترک (توصیه‌ای/اجباری) با دانلودِ **درون‌برنامه‌ای**
 *    (نوار پیشرفت + درصد + سرعت + «x از y مگابایت») و نصبِ داخلِ اپ.
 * 3. [UpdateCheckCard] — بررسیِ **دستی** در صفحهٔ «بیشتر».
 *
 * متنِ «تغییراتِ نسخه» از فایلِ `update-notes.json` در مخزنِ عمومیِ انتشار خوانده
 * می‌شود (نه از کد) تا هر وقت خواستی همان‌جا ویرایشش کنی.
 */
@Composable
fun UpdateGateHost() {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current

    var decision by remember { mutableStateOf<UpdateDecision?>(null) }

    LaunchedEffect(Unit) {
        decision = UpdateChecker.decide(ctx, container.tables, BuildConfig.VERSION_CODE)
    }

    val d = decision
    val info: UpdateInfo = when (d) {
        is UpdateDecision.Forced -> d.info
        is UpdateDecision.Optional -> d.info
        else -> return
    }
    UpdateDialog(
        info = info,
        forced = d is UpdateDecision.Forced,
        onClose = { decision = UpdateDecision.None },
    )
}

/** نوارِ پیشرفتِ ساده (بدونِ وابستگی به نسخهٔ کتابخانه) — درصد را هم می‌گیرد. */
@Composable
private fun DownloadBar(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * دیالوگِ آپدیت — تنها جایی که «دانلود و نصب» واقعاً اجرا می‌شود:
 * دانلودِ درون‌برنامه‌ای با پیشرفتِ زنده → بررسیِ `sha256` → نصب‌کنندهٔ سیستم.
 * اگر مجوزِ نصب داده نشده باشد، صفحهٔ تنظیماتِ همان مجوز باز می‌شود.
 */
@Composable
fun UpdateDialog(info: UpdateInfo, forced: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<ApkProgress?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(ApkUpdate.isReady(ctx)) }
    var repoNotes by remember {
        mutableStateOf(UpdateNotes.notes(ctx, UpdatePlan.versionLabel(info)))
    }

    // متنِ تغییرات از ریپو (اگر اینترنت نبود، همان کشِ قبلی می‌ماند).
    LaunchedEffect(info.latest) {
        if (UpdateNotes.refresh(ctx)) {
            repoNotes = UpdateNotes.notes(ctx, UpdatePlan.versionLabel(info))
        }
    }
    val shownNotes = repoNotes.ifEmpty { info.notes }
    val versionFa = toPersianDigits(UpdatePlan.versionLabel(info))

    fun installNow() {
        note = null
        when {
            !ApkUpdate.isReady(ctx) -> note = "فایلِ نصبی آماده نیست؛ اول دانلودش کن."
            !ApkUpdate.verify(ctx, info.sha256) -> {
                ApkUpdate.clear(ctx)
                ready = false
                note = "فایلِ دانلودشده سالم نبود و پاک شد؛ یک‌بار دیگر دانلود کن."
            }
            !ApkUpdate.canInstall(ctx) -> {
                ApkUpdate.openInstallPermission(ctx)
                note = "اجازهٔ «نصب برنامه‌های ناشناس» را برای همیار من روشن کن و بعد «نصب» را بزن."
            }
            !ApkUpdate.install(ctx) -> {
                ApkUpdate.explain(ctx)
                note = "نصب‌کننده باز نشد؛ فایل را دستی نصب کن یا از «دانلود در مرورگر» استفاده کن."
            }
        }
    }

    fun startDownload() {
        note = null
        downloading = true
        progress = ApkProgress(0, info.size, 0)
        scope.launch {
            val f = ApkUpdate.download(ctx, info.url) { p -> progress = p }
            downloading = false
            when {
                f == null -> note = "دانلود کامل نشد؛ اینترنت را چک کن و دوباره بزن (از همان‌جا ادامه می‌دهد)."
                !ApkUpdate.verify(ctx, info.sha256) -> {
                    ApkUpdate.clear(ctx)
                    note = "فایلِ دانلودشده سالم نبود و پاک شد؛ یک‌بار دیگر بزن."
                }
                else -> {
                    ready = true
                    installNow()
                }
            }
        }
    }

    /** «بعداً»: تا نسخهٔ بعدی دیگر پرسیده نشود (فقط در حالتِ توصیه‌ای). */
    fun later() {
        UpdateChecker.skip(ctx, info.latest)
        onClose()
    }

    // در حالتِ اجباری، دکمهٔ back هم بی‌اثر است.
    BackHandler(enabled = forced) { }

    AlertDialog(
        onDismissRequest = { if (!forced) later() },
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
                        "  •  نسخه‌ی تازه: $versionFa" +
                        if (info.size > 0) "  •  حجم: ${UpdatePlan.sizeLabel(info.size)}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (forced) {
                    Text(
                        "برای ادامه‌ی کار باید نسخه‌ی تازه نصب شود. دانلودش اینترنت لازم دارد.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                shownNotes.take(8).forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyMedium)
                }

                // ---- دانلود درون‌برنامه‌ای با نوار پیشرفت ----
                if (downloading) {
                    val p = progress ?: ApkProgress(0, info.size, 0)
                    DownloadBar(p.percent / 100f)
                    Text(
                        "نسخهٔ $versionFa — " + toPersianDigits("${p.percent}") + "٪",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        p.sizeText + "  •  " + p.speedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (ready) {
                    Text(
                        "دانلود کامل شد ✅ — برای نصب، دکمه‌ی «نصب نسخه‌ی تازه» را بزن.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            when {
                downloading -> PrimaryButton("در حال دانلود…") { }
                ready -> PrimaryButton("نصب نسخه‌ی تازه") { installNow() }
                else -> PrimaryButton("دانلود و نصب") { startDownload() }
            }
        },
        // در حالتِ اجباری، دکمهٔ انصراف عمداً خالی است (فقط «دانلود و نصب» می‌ماند).
        dismissButton = {
            if (!forced) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (ready) {
                        TextButton(onClick = {
                            ApkUpdate.clear(ctx)
                            ready = false
                            startDownload()
                        }) { Text("دانلود مجدد") }
                    }
                    TextButton(onClick = { UpdateChecker.openInBrowser(ctx, info.url) }) {
                        Text("دانلود در مرورگر")
                    }
                    TextButton(onClick = { later() }) { Text("بعداً") }
                }
            }
        },
    )
}

/**
 * کارتِ «آپدیت اپ» برای صفحهٔ «بیشتر»: نسخهٔ فعلی را نشان می‌دهد و با لمس،
 * **همین حالا** از سرور می‌پرسد (بدونِ توجه به کشِ ۶ساعته و «بعداً»های قبلی).
 * اگر نسخهٔ تازه بود، همان دیالوگِ بالا باز می‌شود.
 */
@Composable
fun UpdateCheckCard() {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf<UpdateInfo?>(null) }
    var forcedPrompt by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🔄 آپدیت اپ", style = MaterialTheme.typography.titleMedium)
            Text(
                "نسخه‌ی نصب‌شده: ${BuildConfig.VERSION_NAME}" +
                    if (UpdateChecker.cached(ctx) != null) {
                        "  •  آخرین اعلام‌شده: " + UpdatePlan.versionLabel(UpdateChecker.cached(ctx)!!)
                    } else {
                        ""
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            status?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    status = null
                    scope.launch {
                        val info = UpdateChecker.refresh(ctx, container.tables)
                        if (info == null || info.url.isBlank() || info.latest <= 0) {
                            status = "الان نتوانستم از سرور بپرسم؛ بعداً دوباره امتحان کن."
                        } else if (info.latest <= BuildConfig.VERSION_CODE) {
                            status = "همین نسخه را داری؛ «" + UpdatePlan.versionLabel(info) +
                                "» آخرین نسخه است ✅"
                        } else {
                            // بررسیِ دستی، از رول‌آوت و «بعداً» عبور می‌کند: کاربر خودش خواسته.
                            val d = UpdatePlan.decisionFor(BuildConfig.VERSION_CODE, info, bucket = 0)
                            forcedPrompt = d is UpdateDecision.Forced
                            prompt = info
                        }
                        busy = false
                    }
                },
            ) {
                Text(if (busy) "در حال بررسی…" else "بررسیِ نسخه‌ی تازه")
            }
        }
    }

    prompt?.let {
        UpdateDialog(info = it, forced = forcedPrompt, onClose = { prompt = null })
    }
}
