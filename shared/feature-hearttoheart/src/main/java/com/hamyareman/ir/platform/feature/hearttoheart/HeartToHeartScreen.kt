package com.hamyareman.ir.platform.feature.hearttoheart

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.InlineButton
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val MAX_RECORD_SECONDS = 120

/**
 * صفحه‌ی گفت‌وگوی «حرف دل».
 *
 * [myDirection] جهتی است که پیام‌های *من* با آن برچسب می‌خورند:
 * در اپ زهرا `TO_FATHER` و در اپ پدر `TO_ZAHRA`.
 */
@Composable
fun HeartToHeartScreen(
    repo: HeartRepository,
    myDirection: MessageDirection,
    onStartCall: () -> Unit,
    onDialTel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf(repo.messages()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var playingId by remember { mutableStateOf<String?>(null) }

    val recorder = remember { VoiceRecorder(context) }
    val player = remember { VoicePlayer() }
    var recording by remember { mutableStateOf(false) }
    var recordedSeconds by remember { mutableStateOf(0) }
    var micGranted by remember { mutableStateOf(hasMicPermission(context)) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        micGranted = granted
        if (!granted) notice = "برای ضبط ویس باید اجازه‌ی میکروفون بدهی."
    }

    val attachLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val file = MediaFiles.copyToCache(context, uri, "media")
            when {
                file == null -> notice = "انتخاب فایل ناموفق بود."
                !MediaFiles.isWithinLimit(file) -> notice = "حجم این فایل بیشتر از ۱۰ مگابایت است."
                else -> {
                    val mime = context.contentResolver.getType(uri).orEmpty()
                    val type = when {
                        mime.startsWith("video") -> MessageType.VIDEO
                        mime.startsWith("image") -> MessageType.PHOTO
                        else -> MessageType.FILE
                    }
                    val caption = MediaFiles.displayName(context, uri).orEmpty()
                    when (val result = repo.sendMedia(file, type, caption, myDirection)) {
                        is AppResult.Ok ->
                            notice = if (result.value.synced) null else "ذخیره شد؛ با اولین اتصال ارسال می‌شود."

                        is AppResult.Err -> notice = result.error.userMessage
                    }
                    messages = repo.messages()
                }
            }
            busy = false
        }
    }

    // بازکردن صفحه: اول کش محلی، بعد سرور، بعد ارسال پیام‌های معطل.
    LaunchedEffect(Unit) {
        messages = repo.messages()
        if (repo.isConfigured) {
            val refreshed = repo.refresh()
            if (refreshed is AppResult.Ok) messages = refreshed.value
            repo.pushPending()
            messages = repo.messages()
        }
    }

    // به‌روزرسانی زنده (Realtime): وقتی پدر پیامی می‌فرستد، بدون refresh دستی
    // روی صفحه می‌آید. اگر Realtime در دسترس نباشد این جریان خالی است و همان
    // رفتار قبلی (خواندن هنگام بازکردن صفحه + دکمه‌ی تازه‌سازی) باقی می‌ماند.
    LaunchedEffect(Unit) {
        repo.liveUpdates().collect {
            val refreshed = repo.refresh()
            if (refreshed is AppResult.Ok) messages = refreshed.value
        }
    }

    fun finishVoice() {
        val clip = recorder.stop()
        recordedSeconds = 0
        if (clip == null) {
            notice = "ضبطی وجود نداشت."
            return
        }
        if (clip.durationMs < 1_000) {
            runCatching { File(clip.path).delete() }
            notice = "ویس خیلی کوتاه بود؛ دوباره امتحان کن."
            return
        }
        busy = true
        scope.launch {
            when (val result = repo.sendVoice(clip, myDirection)) {
                is AppResult.Ok ->
                    notice = if (result.value.synced) null else "ویس ذخیره شد؛ با اولین اتصال ارسال می‌شود."

                is AppResult.Err -> notice = result.error.userMessage
            }
            messages = repo.messages()
            busy = false
        }
    }

    // تایمر ضبط + قطع خودکار در سقف دو دقیقه
    LaunchedEffect(recording) {
        while (recording) {
            delay(1_000)
            recordedSeconds += 1
            if (recordedSeconds >= MAX_RECORD_SECONDS) {
                recording = false
                finishVoice()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            player.stop()
        }
    }

    Scaffold(topBar = { AppTopBar("حرف دل") }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "متن، ویس، عکس و فایل — فقط بین تو و بابا.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    busy = true
                    scope.launch {
                        val refreshed = repo.refresh()
                        if (refreshed is AppResult.Ok) messages = refreshed.value
                        repo.pushPending()
                        messages = repo.messages()
                        busy = false
                    }
                }) { Icon(Icons.Filled.Refresh, contentDescription = "به‌روزرسانی") }
                IconButton(onClick = onStartCall) { Icon(Icons.Filled.Call, contentDescription = "تماس آنلاین") }
                IconButton(onClick = onDialTel) { Icon(Icons.Filled.Phone, contentDescription = "زنگ با تلفن معمولی") }
            }

            val pending = messages.count { !it.synced }
            if (pending > 0) {
                Text(
                    toPersianDigits("$pending پیام در انتظار ارسال (آفلاین ذخیره شده)."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (messages.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "هنوز پیامی نیست. اولین حرف دلت را بنویس 🙂",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            mine = message.direction == myDirection,
                            playing = playingId == message.id,
                            onPlay = {
                                scope.launch {
                                    val source = repo.mediaUrl(message)
                                    if (source == null) {
                                        notice = "رسانه‌ی این پیام در دسترس نیست."
                                    } else {
                                        val isPlaying = player.play(message.id, source) { playingId = null }
                                        playingId = if (isPlaying) message.id else null
                                    }
                                }
                            },
                            onOpen = { openMedia(context, message) },
                        )
                    }
                }
            }

            notice?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))

            if (recording) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Row(
                        Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("در حال ضبط… ${toPersianDigits(recordedSeconds.toString())} ثانیه")
                        Row {
                            IconButton(onClick = {
                                recording = false
                                recorder.cancel()
                                recordedSeconds = 0
                            }) { Icon(Icons.Filled.Stop, contentDescription = "لغو ضبط") }
                            IconButton(onClick = {
                                recording = false
                                finishVoice()
                            }) { Icon(Icons.Filled.Send, contentDescription = "ارسال ویس") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("پیام") },
                    maxLines = 4,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        if (!micGranted) {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            notice = null
                            recordedSeconds = 0
                            val started = recorder.start()
                            if (started.isFailure) {
                                notice = "ضبط صدا شروع نشد."
                            } else {
                                recording = true
                            }
                        }
                    }) { Icon(Icons.Filled.Mic, contentDescription = "ضبط ویس") }

                    IconButton(onClick = { attachLauncher.launch("*/*") }) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "پیوست عکس یا فایل")
                    }

                    PrimaryButton(
                        text = if (busy) "در حال ارسال…" else "بفرست",
                        onClick = {
                            val body = input.trim()
                            if (body.isEmpty() || busy) return@PrimaryButton
                            busy = true
                            scope.launch {
                                when (val result = repo.sendText(body, myDirection)) {
                                    is AppResult.Ok -> {
                                        input = ""
                                        notice = if (result.value.synced) null else "ذخیره شد؛ بعداً ارسال می‌شود."
                                    }

                                    is AppResult.Err -> notice = result.error.userMessage
                                }
                                messages = repo.messages()
                                busy = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (busy) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MessageBubble(
    message: HeartMessage,
    mine: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.84f),
            colors = CardDefaults.cardColors(
                containerColor = if (mine) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(typeLabel(message.type), style = MaterialTheme.typography.labelMedium)
                    Text(
                        JalaliDate.stampFa(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(message.text.ifBlank { "(بدون متن)" }, style = MaterialTheme.typography.bodyLarge)

                when (message.type) {
                    MessageType.VOICE -> Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPlay) {
                            Icon(
                                if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) "توقف پخش" else "پخش ویس",
                            )
                        }
                        message.durationMs?.let {
                            Text(formatDuration(it), style = MaterialTheme.typography.labelSmall)
                        }
                        if (!message.synced) {
                            Spacer(Modifier.width(8.dp))
                            Text("در انتظار آپلود", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    MessageType.PHOTO, MessageType.VIDEO, MessageType.FILE -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InlineButton("بازکردن", onClick = onOpen)
                        if (!message.synced) {
                            Spacer(Modifier.width(8.dp))
                            Text("در انتظار آپلود", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt()
    return toPersianDigits("%02d:%02d".format(totalSeconds / 60, totalSeconds % 60))
}

private fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private fun openMedia(context: Context, message: HeartMessage) {
    val path = message.mediaPath ?: return
    val file = File(path)
    if (!file.exists()) return
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun typeLabel(type: MessageType): String = when (type) {
    MessageType.TEXT -> "متن"
    MessageType.VOICE -> "ویس"
    MessageType.PHOTO -> "عکس"
    MessageType.VIDEO -> "ویدیو"
    MessageType.FILE -> "فایل"
    MessageType.CALL_EVENT -> "تماس"
}
