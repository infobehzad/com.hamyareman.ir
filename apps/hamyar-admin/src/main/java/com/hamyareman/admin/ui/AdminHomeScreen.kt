package com.hamyareman.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hamyareman.admin.LocalAdmin
import com.hamyareman.ir.platform.core.appwrite.BillingOrder
import com.hamyareman.ir.platform.core.appwrite.BillingProfile
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.launch

private const val MEDIA =
    "https://fra.cloud.appwrite.io/v1/storage/buckets/6aa1eaae00303400117b/files"
private const val PROJECT = "6a9d59e3002751cc3ea8"

private fun fileView(id: String): String =
    if (id.isBlank()) "" else "$MEDIA/$id/view?project=$PROJECT"

private enum class AdminTab { PAY, REFUND, SEARCH }

@Composable
fun AdminHomeScreen(onLogout: () -> Unit) {
    val container = LocalAdmin.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(AdminTab.PAY) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var orders by remember { mutableStateOf<List<BillingOrder>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<BillingProfile>>(emptyList()) }

    fun load() {
        loading = true
        error = null
        scope.launch {
            val queue = if (tab == AdminTab.REFUND) "refund" else "pay"
            when (val r = container.billing.adminList(queue)) {
                is AppResult.Ok -> orders = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }

    LaunchedEffect(tab) {
        if (tab != AdminTab.SEARCH) load()
    }

    val detailId = selected
    if (detailId != null) {
        OrderDetailScreen(
            orderId = detailId,
            refundQueue = tab == AdminTab.REFUND,
            onBack = { selected = null; load() },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("صف ادمین همیار", onBack = null)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == AdminTab.PAY, onClick = { tab = AdminTab.PAY }, label = { Text("پرداخت‌ها") })
            FilterChip(selected = tab == AdminTab.REFUND, onClick = { tab = AdminTab.REFUND }, label = { Text("بازگشت وجه") })
            FilterChip(selected = tab == AdminTab.SEARCH, onClick = { tab = AdminTab.SEARCH }, label = { Text("جستجو") })
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onLogout) { Text("خروج") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        if (loading) {
            CircularProgressIndicator(Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
        }
        when (tab) {
            AdminTab.SEARCH -> Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("ایمیل یا شناسه کاربر") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        loading = true; error = null
                        scope.launch {
                            when (val r = container.billing.adminSearch(query.trim())) {
                                is AppResult.Ok -> hits = r.value
                                is AppResult.Err -> error = r.error.userMessage
                            }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("جستجو") }
                hits.forEach { p ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text((p.firstName + " " + p.lastName).ifBlank { p.name }.ifBlank { "بدون نام" }, fontWeight = FontWeight.Bold)
                            Text(p.email.ifBlank { p.userId })
                            Text("پایه: ${p.grade} · جنسیت: ${p.gender} · اشتراک: ${p.subscription.ifBlank { "—" }}")
                            Text("موبایل: ${p.phone.ifBlank { "—" }}")
                        }
                    }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (orders.isEmpty() && !loading) {
                    item { Text("صف خالی است.") }
                }
                items(orders, key = { it.id }) { o ->
                    Card(onClick = { selected = o.id }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text((o.firstName + " " + o.lastName).ifBlank { o.email }.ifBlank { o.userId }, fontWeight = FontWeight.Bold)
                            Text("${o.planTitle.ifBlank { o.planId }} · ${toPersianDigits(o.amountToman.toString())} تومان")
                            Text(o.email)
                            Text("وضعیت: ${o.status}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailScreen(orderId: String, refundQueue: Boolean, onBack: () -> Unit) {
    val container = LocalAdmin.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var order by remember { mutableStateOf<BillingOrder?>(null) }
    var profile by remember { mutableStateOf<BillingProfile?>(null) }
    var avatarId by remember { mutableStateOf("") }

    fun reload() {
        loading = true
        scope.launch {
            when (val r = container.billing.adminGet(orderId)) {
                is AppResult.Ok -> {
                    order = r.value.first
                    profile = r.value.second
                    avatarId = r.value.third
                }
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }
    LaunchedEffect(orderId) { reload() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("جزئیات سفارش", onBack = onBack)
        val o = order
        val p = profile
        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
            o == null -> Text(error ?: "پیدا نشد", modifier = Modifier.padding(16.dp))
            else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val url = fileView(avatarId)
                if (url.isNotBlank()) {
                    AsyncImage(
                        model = url,
                        contentDescription = "عکس پروفایل",
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column {
                    Text((p?.firstName.orEmpty() + " " + p?.lastName.orEmpty()).ifBlank { o.firstName + " " + o.lastName }.ifBlank { "بدون نام" }, fontWeight = FontWeight.Bold)
                    Text(p?.email?.ifBlank { o.email } ?: o.email)
                }
            }
            Info("شناسه", o.userId)
            Info("پایه", (p?.grade ?: o.grade).ifBlank { "—" })
            Info("جنسیت", genderFa(p?.gender ?: o.gender))
            Info("موبایل", (p?.phone ?: o.phone).ifBlank { "—" })
            Info("مدرسه", p?.schoolName.orEmpty().ifBlank { "—" })
            Info("استان / شهر", listOf(p?.province, p?.city).filter { !it.isNullOrBlank() }.joinToString(" / ").ifBlank { "—" })
            Info("سن", if ((p?.age ?: o.age) > 0) toPersianDigits((p?.age ?: o.age).toString()) else "—")
            Info("طرح", o.planTitle.ifBlank { o.planId })
            Info("مبلغ", toPersianDigits(o.amountToman.toString()) + " تومان")
            Info("صاحب حساب مبدأ", o.payerName.ifBlank { "—" })
            Info("متن واریز", o.payText.ifBlank { "—" })
            if (o.receiptFileId.isNotBlank()) {
                Text("فیش واریز", fontWeight = FontWeight.Bold)
                AsyncImage(
                    model = fileView(o.receiptFileId),
                    contentDescription = "فیش",
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            if (refundQueue || o.status == "refund_pending") {
                Info("شبا", o.refundShaba.ifBlank { "—" })
                Info("کارت بازگشت", o.refundCard.ifBlank { "—" })
                Info("نام حساب بازگشت", o.refundAccountName.ifBlank { "—" })
                Info("دلیل", o.refundReason.ifBlank { "—" })
                Button(
                    onClick = {
                        busy = true; error = null
                        scope.launch {
                            when (val r = container.billing.adminRefundOk(o.id)) {
                                is AppResult.Ok -> onBack()
                                is AppResult.Err -> error = r.error.userMessage
                            }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (busy) "…" else "تأیید استرداد — اشتراک خاموش شود") }
            } else {
                Button(
                    onClick = {
                        busy = true; error = null
                        scope.launch {
                            when (val r = container.billing.adminApprove(o.id)) {
                                is AppResult.Ok -> onBack()
                                is AppResult.Err -> error = r.error.userMessage
                            }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (busy) "…" else "تأیید پرداخت و فعال‌سازی پرمیوم") }
                OutlinedButton(
                    onClick = {
                        busy = true; error = null
                        scope.launch {
                            when (val r = container.billing.adminReject(o.id)) {
                                is AppResult.Ok -> onBack()
                                is AppResult.Err -> error = r.error.userMessage
                            }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("رد کردن") }
            }
            Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
}

private fun genderFa(raw: String): String = when (raw.trim().lowercase()) {
    "boy", "male" -> "پسر"
    "girl", "female" -> "دختر"
    else -> raw.ifBlank { "—" }
}
