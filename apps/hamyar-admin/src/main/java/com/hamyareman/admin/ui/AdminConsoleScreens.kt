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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamyareman.admin.LocalAdmin
import com.hamyareman.admin.adminIo
import com.hamyareman.ir.platform.core.appwrite.AdminUser
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.BillingConfig
import com.hamyareman.ir.platform.core.common.BillingStatus
import com.hamyareman.ir.platform.core.common.toPersianDigits
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun AdminPresentUsersScreen(onOpen: (String) -> Unit) {
    val api = LocalAdmin.current.api
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    fun load() {
        loading = true; error = null
        scope.launch {
            when (val r = adminIo { api.presentUsers() }) {
                is AppResult.Ok -> users = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("کاربران حاضر — نشست باز یعنی آنلاین", style = MaterialTheme.typography.titleMedium)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        OutlinedButton(onClick = { load() }, modifier = Modifier.fillMaxWidth()) { Text("تازه‌سازی") }
        users.forEach { u ->
            Card(onClick = { onOpen(u.userId) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text((u.profile.firstName + " " + u.profile.lastName).ifBlank { u.name }.ifBlank { u.email }, fontWeight = FontWeight.Bold)
                    Text(u.email.ifBlank { u.userId })
                    Text(
                        "نشست: ${toPersianDigits(u.sessionCount.toString())} · پایه ${gradeFa(u.hamyarGrade)} · ${BillingStatus.chipFa(u.subscription)}" +
                            if (u.sessionCount > 0) " · آنلاین" else " · آفلاین",
                    )
                    if (u.blocked) Text("مسدود", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminInstallmentsScreen(onOpenUser: (String) -> Unit) {
    val api = LocalAdmin.current.api
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var userId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var total by remember { mutableStateOf(BillingConfig.YEARLY.priceToman.toString()) }
    var count by remember { mutableStateOf("3") }
    var note by remember { mutableStateOf("") }
    fun load() {
        loading = true; error = null
        scope.launch {
            when (val r = adminIo { api.listInstallments() }) {
                is AppResult.Ok -> rows = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("اقساط اشتراک", style = MaterialTheme.typography.titleMedium)
        Text("بدون پرداخت کامل، اشتراک را قسطی ثبت کن. با قسط آخر پرمیوم فعال می‌شود.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(userId, { userId = it.trim() }, label = { Text("شناسه کاربر") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it.trim() }, label = { Text("ایمیل") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(total, { total = it.filter { ch -> ch.isDigit() } }, label = { Text("مبلغ کل تومان") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(count, { count = it.filter { ch -> ch.isDigit() } }, label = { Text("تعداد قسط") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("یادداشت") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                scope.launch {
                    val t = total.toIntOrNull() ?: 0
                    val n = count.toIntOrNull() ?: 0
                    when (val r = adminIo { api.createInstallment(userId, email, "yearly", t, n, note) }) {
                        is AppResult.Ok -> load()
                        is AppResult.Err -> error = r.error.userMessage
                    }
                }
            },
            enabled = userId.isNotBlank() && (total.toIntOrNull() ?: 0) > 0 && (count.toIntOrNull() ?: 0) > 0,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ثبت قسط") }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        rows.forEach { row ->
            val id = row.optString("\$id")
            val paid = row.optInt("paidCount")
            val all = row.optInt("installmentCount")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(row.optString("email").ifBlank { row.optString("userId") }, fontWeight = FontWeight.Bold)
                    Text("قسط ${toPersianDigits(paid.toString())} از ${toPersianDigits(all.toString())} · هر قسط ${toPersianDigits(row.optInt("amountEach").toString())} تومان")
                    Text("وضعیت: ${row.optString("status")}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    when (val r = adminIo { api.payInstallment(id) }) {
                                        is AppResult.Ok -> load()
                                        is AppResult.Err -> error = r.error.userMessage
                                    }
                                }
                            },
                            enabled = row.optString("status") != "done",
                        ) { Text("ثبت قسط بعدی") }
                        OutlinedButton(onClick = { onOpenUser(row.optString("userId")) }) { Text("پرونده") }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDatabaseScreen() {
    val api = LocalAdmin.current.api
    val scope = rememberCoroutineScope()
    var tables by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var table by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var edit by remember { mutableStateOf<JSONObject?>(null) }
    var json by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    fun loadTables() {
        scope.launch {
            when (val r = adminIo { api.listTables() }) {
                is AppResult.Ok -> tables = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
        }
    }
    fun loadRows(id: String) {
        loading = true
        scope.launch {
            when (val r = adminIo { api.listRows(id) }) {
                is AppResult.Ok -> rows = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { loadTables() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("دیتابیس ZahraDB — خواندن، ویرایش، نوشتن، پشتیبان", style = MaterialTheme.typography.titleMedium)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    when (val r = adminIo { api.backupDatabase() }) {
                        is AppResult.Ok -> info = "پشتیبان گرفته شد (${r.value.optString("fileName")})"
                        is AppResult.Err -> error = r.error.userMessage
                    }
                }
            }) { Text("بکاپ") }
            OutlinedButton(onClick = {
                scope.launch {
                    val dump = runCatching { JSONObject(json) }.getOrNull()
                    if (dump == null) { error = "JSON پشتیبان را در کادر پایین بچسبان."; return@launch }
                    when (val r = adminIo { api.restoreDatabase(dump) }) {
                        is AppResult.Ok -> info = r.value
                        is AppResult.Err -> error = r.error.userMessage
                    }
                }
            }) { Text("ریستور از JSON") }
        }
        val e = edit
        if (e != null && table != null) {
            Text("ویرایش سطر ${e.optString("\$id")}", fontWeight = FontWeight.Bold)
            OutlinedTextField(json, { json = it }, modifier = Modifier.fillMaxWidth().height(220.dp), label = { Text("JSON") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val data = runCatching { JSONObject(json) }.getOrNull()
                    if (data == null) { error = "JSON نامعتبر"; return@Button }
                    val clean = JSONObject()
                    data.keys().forEach { k -> if (!k.startsWith("$")) clean.put(k, data.get(k)) }
                    scope.launch {
                        when (val r = adminIo { api.saveRow(table!!, e.optString("\$id"), clean, false) }) {
                            is AppResult.Ok -> { edit = null; loadRows(table!!) }
                            is AppResult.Err -> error = r.error.userMessage
                        }
                    }
                }) { Text("ذخیره") }
                OutlinedButton(onClick = {
                    scope.launch {
                        when (val r = adminIo { api.deleteRow(table!!, e.optString("\$id")) }) {
                            is AppResult.Ok -> { edit = null; loadRows(table!!) }
                            is AppResult.Err -> error = r.error.userMessage
                        }
                    }
                }) { Text("حذف") }
                OutlinedButton(onClick = { edit = null }) { Text("بستن") }
            }
            return
        }
        if (table == null) {
            tables.forEach { (id, name) ->
                Card(onClick = { table = id; loadRows(id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(name.ifBlank { id }, fontWeight = FontWeight.Bold)
                        Text(id, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            OutlinedButton(onClick = { table = null; rows = emptyList() }) { Text("بازگشت به جدول‌ها") }
            Text("جدول $table — ${toPersianDigits(rows.size.toString())} سطر")
            if (loading) CircularProgressIndicator()
            rows.forEach { row ->
                Card(onClick = { edit = row; json = row.toString(2) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(row.toString().take(180), modifier = Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun AdminStorageScreen() {
    val api = LocalAdmin.current.api
    val scope = rememberCoroutineScope()
    var buckets by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var bucket by remember { mutableStateOf<String?>(null) }
    var files by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    fun loadB() {
        scope.launch {
            when (val r = adminIo { api.listBuckets() }) {
                is AppResult.Ok -> buckets = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
        }
    }
    fun loadF(id: String) {
        scope.launch {
            when (val r = adminIo { api.listFiles(id) }) {
                is AppResult.Ok -> files = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
        }
    }
    LaunchedEffect(Unit) { loadB() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Storage", style = MaterialTheme.typography.titleMedium)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val b = bucket
        if (b == null) {
            buckets.forEach { o ->
                Card(onClick = { bucket = o.optString("\$id"); loadF(o.optString("\$id")) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(o.optString("name"), fontWeight = FontWeight.Bold)
                        Text(o.optString("\$id"))
                    }
                }
            }
        } else {
            OutlinedButton(onClick = { bucket = null; files = emptyList() }) { Text("بازگشت") }
            files.forEach { f ->
                val id = f.optString("\$id")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(f.optString("name").ifBlank { id }, fontWeight = FontWeight.Bold)
                        Text("${toPersianDigits((f.optLong("sizeOriginal") / 1024).toString())} کیلوبایت")
                        Text(api.fileView(b, id), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            scope.launch {
                                when (val r = adminIo { api.deleteFile(b, id) }) {
                                    is AppResult.Ok -> loadF(b)
                                    is AppResult.Err -> error = r.error.userMessage
                                }
                            }
                        }) { Text("حذف فایل") }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminFunctionsScreen() {
    val api = LocalAdmin.current.api
    val scope = rememberCoroutineScope()
    var fns by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var execs by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var body by remember { mutableStateOf("{\"action\":\"admin_ping\"}") }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun load() {
        scope.launch {
            when (val r = adminIo { api.listFunctions() }) {
                is AppResult.Ok -> fns = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Functions", style = MaterialTheme.typography.titleMedium)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        result?.let { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
        fns.forEach { f ->
            val id = f.optString("\$id")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${f.optString("name")} ($id)", fontWeight = FontWeight.Bold)
                    Text(if (f.optBoolean("enabled", true)) "فعال" else "خاموش")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                adminIo { api.setFunctionEnabled(id, !f.optBoolean("enabled", true)) }
                                load()
                            }
                        }) { Text(if (f.optBoolean("enabled", true)) "خاموش" else "روشن") }
                        Button(onClick = {
                            selected = id
                            scope.launch {
                                when (val r = adminIo { api.listExecutions(id) }) {
                                    is AppResult.Ok -> execs = r.value
                                    is AppResult.Err -> error = r.error.userMessage
                                }
                            }
                        }) { Text("اجراها") }
                    }
                }
            }
        }
        val sid = selected
        if (sid != null) {
            OutlinedTextField(body, { body = it }, label = { Text("بدنه JSON") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    when (val r = adminIo { api.executeFunction(sid, body) }) {
                        is AppResult.Ok -> result = r.value.toString(2).take(2000)
                        is AppResult.Err -> error = r.error.userMessage
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("اجرای تابع $sid") }
            execs.take(10).forEach { e ->
                Text("${e.optString("\$id")} · ${e.optString("status")} · ${e.optInt("responseStatusCode")}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AdminAuthScreen(onOpen: (String) -> Unit) {
    val api = LocalAdmin.current.api
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    fun load() {
        scope.launch {
            when (val r = adminIo { api.presentUsers() }) {
                is AppResult.Ok -> users = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Auth — ساخت و حذف حساب", style = MaterialTheme.typography.titleMedium)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        info?.let { Text(it) }
        OutlinedTextField(name, { name = it }, label = { Text("نام") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it.trim() }, label = { Text("ایمیل") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("رمز (حداقل ۸)") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                scope.launch {
                    when (val r = adminIo { api.createUser(name, email, password) }) {
                        is AppResult.Ok -> { info = "ساخته شد: ${r.value}"; load() }
                        is AppResult.Err -> error = r.error.userMessage
                    }
                }
            },
            enabled = email.contains("@") && password.length >= 8,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ساخت کاربر") }
        users.forEach { u ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(u.email.ifBlank { u.userId }, fontWeight = FontWeight.Bold)
                    Text("برچسب: ${u.labels.joinToString().ifBlank { "—" }}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onOpen(u.userId) }) { Text("پرونده") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                when (val r = adminIo { api.deleteUser(u.userId) }) {
                                    is AppResult.Ok -> load()
                                    is AppResult.Err -> error = r.error.userMessage
                                }
                            }
                        }) { Text("حذف حساب") }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
