package com.hamyareman.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamyareman.admin.LocalAdmin
import com.hamyareman.admin.adminIo
import com.hamyareman.ir.platform.core.appwrite.AdminStats
import com.hamyareman.ir.platform.core.appwrite.AdminUser
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.BillingStatus
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.launch

internal val GRADE_OPTIONS = listOf(
    "grade4" to "چهارم",
    "grade5" to "پنجم",
    "grade6" to "ششم",
    "grade7" to "هفتم",
    "grade8" to "هشتم",
    "grade9" to "نهم",
    "grade10" to "دهم",
    "grade11" to "یازدهم",
    "grade12" to "دوازدهم",
)

internal fun gradeFa(id: String): String =
    GRADE_OPTIONS.firstOrNull { it.first == id }?.second ?: id.ifBlank { "—" }

private enum class ConfirmOp { BLOCK, UNBLOCK, LOGOUT, CLEAR_DEVICES, RESET }

@Composable
fun AdminUserScreen(userId: String, onBack: () -> Unit) {
    val container = LocalAdmin.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf<AdminUser?>(null) }
    var confirm by remember { mutableStateOf<ConfirmOp?>(null) }
    var tempPass by remember { mutableStateOf<String?>(null) }

    fun apply(r: AppResult<AdminUser>) {
        when (r) {
            is AppResult.Ok -> {
                user = r.value
                if (r.value.tempPassword.isNotBlank()) tempPass = r.value.tempPassword
                error = null
            }
            is AppResult.Err -> error = r.error.userMessage
        }
    }

    fun load() {
        loading = true
        scope.launch {
            apply(adminIo { container.api.adminUser(userId) })
            loading = false
        }
    }

    fun run(op: suspend () -> AppResult<AdminUser>) {
        busy = true
        error = null
        scope.launch {
            apply(adminIo { op() })
            busy = false
        }
    }

    LaunchedEffect(userId) { load() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("پرونده کاربر", onBack = onBack)
        val u = user
        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
            u == null -> Text(error ?: "پیدا نشد", modifier = Modifier.padding(16.dp))
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                val p = u.profile
                Text(
                    (p.firstName + " " + p.lastName).ifBlank { u.name }.ifBlank { "بدون نام" },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Info("ایمیل", u.email.ifBlank { p.email }.ifBlank { "—" })
                Info("شناسه", u.userId)
                Info("موبایل", p.phone.ifBlank { "—" })
                Info("جنسیت", genderFaAdmin(p.gender))
                Info("مدرسه", p.schoolName.ifBlank { "—" })
                Info("استان / شهر", listOf(p.province, p.city).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "—" })
                Info("پایه قفل‌شده", gradeFa(u.hamyarGrade.ifBlank { p.grade }))
                Info("اشتراک", BillingStatus.chipFa(u.subscription.ifBlank { p.subscription }))
                Info("نشست‌های باز", toPersianDigits(u.sessionCount.toString()))
                if (u.blocked) {
                    Text("این حساب مسدود است.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
                tempPass?.let { pass ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("رمز موقت — فقط همین‌بار نشان داده می‌شود", fontWeight = FontWeight.Bold)
                            Text(pass, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
                            Text("برای دانش‌آموز بفرست؛ با ورود بعدی می‌تواند عوض کند.")
                        }
                    }
                }

                Text("تعویض پایه", fontWeight = FontWeight.Bold)
                Text("ایمیل بعد از این فقط همان پایه را باز می‌کند.")
                GRADE_OPTIONS.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (id, fa) ->
                            FilterChip(
                                selected = u.hamyarGrade == id || (u.hamyarGrade.isBlank() && p.grade == id),
                                onClick = { if (!busy) run { container.api.adminSetGrade(userId, id) } },
                                label = { Text(fa) },
                                enabled = !busy,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                Text("اشتراک", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (!busy) run { container.api.adminSetPremium(userId, true) } },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) { Text("پرمیوم") }
                    OutlinedButton(
                        onClick = { if (!busy) run { container.api.adminSetPremium(userId, false) } },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) { Text("مهمان") }
                }

                Text("دستگاه‌ها (سقف ۲)", fontWeight = FontWeight.Bold)
                if (u.devices.isEmpty()) {
                    Text("دستگاهی ثبت نشده.")
                } else {
                    u.devices.forEach { d ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(d.label.ifBlank { "دستگاه" }, fontWeight = FontWeight.Bold)
                                    if (d.id.isNotBlank()) Text(d.id.take(12) + "…", style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(
                                    onClick = { if (!busy) run { container.api.adminRevokeDevice(userId, d.id) } },
                                    enabled = !busy,
                                ) { Text("حذف") }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { confirm = ConfirmOp.CLEAR_DEVICES },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("پاک کردن همه دستگاه‌ها") }

                Button(
                    onClick = { confirm = ConfirmOp.LOGOUT },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("خروج اجباری از همه نشست‌ها") }

                if (u.blocked) {
                    OutlinedButton(
                        onClick = { confirm = ConfirmOp.UNBLOCK },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("رفع مسدودی") }
                } else {
                    OutlinedButton(
                        onClick = { confirm = ConfirmOp.BLOCK },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("مسدود کردن حساب") }
                }

                Button(
                    onClick = { confirm = ConfirmOp.RESET },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("بازیابی رمز — ساخت رمز موقت") }

                if (busy) {
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val pending = confirm
    if (pending != null) {
        val (title, body) = when (pending) {
            ConfirmOp.BLOCK -> "مسدود کردن؟" to "ورود این حساب تا رفع مسدودی بسته می‌شود و نشست‌های باز قطع می‌گردند."
            ConfirmOp.UNBLOCK -> "رفع مسدودی؟" to "حساب دوباره می‌تواند وارد شود."
            ConfirmOp.LOGOUT -> "خروج اجباری؟" to "همه نشست‌های باز این کاربر قطع می‌شود."
            ConfirmOp.CLEAR_DEVICES -> "پاک کردن دستگاه‌ها؟" to "سقف دو دستگاه خالی می‌شود تا بتواند با گوشی تازه وارد شود."
            ConfirmOp.RESET -> "رمز موقت؟" to "رمز تازه ساخته می‌شود، نشست‌ها قطع می‌گردد، و رمز فقط همین‌بار روی صفحه می‌آید."
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = null
                        when (pending) {
                            ConfirmOp.BLOCK -> run { container.api.adminBlock(userId, true) }
                            ConfirmOp.UNBLOCK -> run { container.api.adminBlock(userId, false) }
                            ConfirmOp.LOGOUT -> run { container.api.adminForceLogout(userId) }
                            ConfirmOp.CLEAR_DEVICES -> run { container.api.adminClearDevices(userId) }
                            ConfirmOp.RESET -> run { container.api.adminResetPassword(userId) }
                        }
                    },
                ) { Text("انجام بده") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("انصراف") }
            },
        )
    }
}

@Composable
fun AdminStatsScreen() {
    val container = LocalAdmin.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<AdminStats?>(null) }

    fun load() {
        loading = true
        error = null
        scope.launch {
            when (val r = adminIo { container.api.adminStats() }) {
                is AppResult.Ok -> stats = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        val s = stats
        if (s != null) {
            StatCard("کاربران", s.usersTotal)
            StatCard("صف پرداخت", s.pendingPay)
            StatCard("صف بازگشت وجه", s.pendingRefund)
            StatCard("پرداخت تأییدشده", s.approved)
            StatCard("بازگشت انجام‌شده", s.refunded)
            if (s.paidProfiles >= 0) StatCard("پروفایل با اشتراک سالانه", s.paidProfiles)
        }
        OutlinedButton(onClick = { load() }, modifier = Modifier.fillMaxWidth()) { Text("تازه‌سازی") }
    }
}

@Composable
private fun StatCard(label: String, value: Int) {
    val shown = if (value < 0) "—" else toPersianDigits(value.toString())
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label)
            Text(shown, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
internal fun Info(label: String, value: String) {
    Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
}

internal fun genderFaAdmin(raw: String): String = when (raw.trim().lowercase()) {
    "boy", "male" -> "پسر"
    "girl", "female" -> "دختر"
    else -> raw.ifBlank { "—" }
}
