package com.hamyareman.ir.platform.feature.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.InlineButton
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import kotlinx.coroutines.launch

private const val LOCAL_MODE_NOTE =
    "حالت محلی: شناسه‌ی پروژه‌ی Appwrite تنظیم نشده، پس پیوند فقط روی همین دستگاه شبیه‌سازی می‌شود. " +
        "برای پیوند واقعی بین دو گوشی، مقادیر APPWRITE_* را در local.properties بگذارید."

/** سمت زهرا: ساخت و مدیریت کد پیوند. */
@Composable
fun ZahraPairingScreen(repo: PairingRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf(repo.cachedLink()) }
    var code by remember { mutableStateOf(repo.cachedCode()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        when (val result = repo.currentLink()) {
            is AppResult.Ok -> link = result.value
            is AppResult.Err -> message = result.error.userMessage
        }
    }

    Scaffold(topBar = { AppTopBar("افزودن بابا", onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                "یک کد شش‌رقمی می‌سازم؛ بابا باید آن را در اپ خودش وارد کند.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!repo.isConfigured) {
                Spacer(Modifier.height(8.dp))
                LocalModeCard()
            }

            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("وضعیت پیوند", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(statusLabel(link), style = MaterialTheme.typography.bodyMedium)
                    if (link.isLinked) {
                        Text(
                            "همراه: ${link.partnerName ?: "بابا"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (code != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("کد را به بابا بده", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            toPersianDigits(code.orEmpty()),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "این کد یک‌بارمصرف است و بعد از استفاده باطل می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InlineButton(
                    text = if (code == null) "ساخت کد" else "کد جدید",
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            when (val result = repo.createCode()) {
                                is AppResult.Ok -> code = result.value
                                is AppResult.Err -> message = result.error.userMessage
                            }
                            busy = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                InlineButton(
                    text = "باطل‌کردن کد",
                    onClick = {
                        scope.launch {
                            repo.revokeCode()
                            code = null
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (link.isLinked) {
                PrimaryButton("قطع پیوند") {
                    busy = true
                    scope.launch {
                        repo.unlink()
                        link = PairingState()
                        code = null
                        busy = false
                    }
                }
            }

            if (busy) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
            message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** سمت پدر: واردکردن کد پیوند. */
@Composable
fun FatherPairingScreen(repo: PairingRepository, onPaired: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var linked by remember { mutableStateOf(repo.cachedLink()) }

    Scaffold(topBar = { AppTopBar("ورود کد پیوند") }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                "کدی که زهرا ساخته را وارد کن. بدون این کد به هیچ داده‌ای دسترسی نداری.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!repo.isConfigured) {
                Spacer(Modifier.height(8.dp))
                LocalModeCard()
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { raw ->
                    input = raw.filter { ch -> ch.isDigit() || ch in '۰'..'۹' || ch in '٠'..'٩' }
                        .take(AppwritePairingRepository.CODE_LENGTH)
                },
                label = { Text("کد شش‌رقمی") },
                singleLine = true,
                isError = message != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton("تأیید کد") {
                val normalized = AppwritePairingRepository.normalizeCode(input)
                if (normalized == null) {
                    message = "کد باید دقیقاً شش رقم باشد."
                } else {
                    busy = true
                    message = null
                    scope.launch {
                        when (val result = repo.redeemCode(normalized)) {
                            is AppResult.Ok -> {
                                linked = result.value
                                message = "پیوند فعال شد. خوش آمدی 🙂"
                                onPaired()
                            }

                            is AppResult.Err -> message = result.error.userMessage
                        }
                        busy = false
                    }
                }
            }

            if (linked.isLinked) {
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("پیوند فعال", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "با: ${linked.partnerName ?: "زهرا"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (busy) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
            message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LocalModeCard() {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(LOCAL_MODE_NOTE, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
}

private fun statusLabel(state: PairingState): String = when {
    state.isLinked -> "فعال"
    state.status == PairingState.STATUS_REVOKED -> "قطع شده"
    else -> "هنوز پیوندی نیست"
}
