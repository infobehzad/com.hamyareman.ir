package com.hamyareman.ir.ui.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import java.io.File

private fun latinDigits(s: String): String = buildString {
    for (c in s) append(
        when (c) {
            '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
            '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
            else -> c
        }
    )
}

/**
 * منوی پروفایل کاربری — تمام مشخصات ثبت‌شده با امکان ویرایش:
 * نام/نام‌خانوادگی/سن/ایمیل/موبایل + مدرسه/استان/شهرستان + عکس پروفایل (محلی).
 * پایه: فقط نمایش (تغییر یک‌بار و از سمت پشتیبانی).
 * اشتراک (رایگان/یک‌ساله): فقط نمایش — تغییر از سمت پشتیبانی/سرور.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    profile: StudentProfile?,
    onBack: () -> Unit,
    onSave: (StudentProfile) -> Unit,
) {
    val ctx = LocalContext.current
    var avatarPath by remember { mutableStateOf(com.hamyareman.ir.ui.profile.StudentProfileState.avatarPath) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val f = File(ctx.filesDir, "avatar.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    f.outputStream().use { input.copyTo(it) }
                }
                com.hamyareman.ir.ui.profile.StudentProfileState.saveAvatarMirror(ctx, f.absolutePath)
                avatarPath = f.absolutePath
            }
        }
    }

    var firstName by remember { mutableStateOf(profile?.firstName.orEmpty()) }
    var lastName by remember { mutableStateOf(profile?.lastName.orEmpty()) }
    var ageText by remember { mutableStateOf((profile?.age ?: 0).takeIf { it > 0 }?.toString().orEmpty()) }
    var email by remember { mutableStateOf(profile?.email.orEmpty()) }
    var phone by remember { mutableStateOf(profile?.phone.orEmpty()) }
    var schoolName by remember { mutableStateOf(profile?.schoolName.orEmpty()) }
    var province by remember { mutableStateOf(profile?.province.orEmpty()) }
    var provinceOpen by remember { mutableStateOf(false) }
    var city by remember { mutableStateOf(profile?.city.orEmpty()) }
    var showErrors by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val bad = showErrors && (
        firstName.trim().length < 2 || lastName.trim().length < 2 ||
            ((ageText.toIntOrNull() ?: 0) !in 5..60) ||
            phone.isNotBlank() && !Regex("^9\\d{9}$").matches(phone)
        )

    Column(Modifier.fillMaxSize()) {
        AppTopBar("پروفایل من", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ─── عکس پروفایل (محلی — انتخاب از گالری) ───
            Box(Modifier.size(96.dp)) {
                val bmp = remember(avatarPath) {
                    avatarPath.takeIf { it.isNotBlank() }?.let { p ->
                        runCatching { BitmapFactory.decodeFile(p) }.getOrNull()
                    }
                }
                if (bmp != null) {
                    Image(
                        bmp.asImageBitmap(), contentDescription = "عکس پروفایل",
                        modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop,
                    )
                } else {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(profile?.firstName?.take(1).orEmpty().ifBlank { "؟" }, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
            TextButton(onClick = { pickImage.launch("image/*") }) { Text("انتخاب عکس پروفایل") }

            OutlinedTextField(
                value = firstName, onValueChange = { firstName = it },
                label = { Text("نام *") }, singleLine = true, isError = bad,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lastName, onValueChange = { lastName = it },
                label = { Text("نام خانوادگی *") }, singleLine = true, isError = bad,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = latinDigits(it).filter { c -> c.isDigit() }.take(2) },
                label = { Text("سن *") }, singleLine = true, isError = bad,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it.trim() },
                label = { Text("ایمیل") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = latinDigits(it).filter { c -> c.isDigit() }.take(10) },
                label = { Text("شماره همراه") },
                supportingText = { Text(if (phone.isBlank()) "اختیاری — ۱۰ رقم با ۹" else if (Regex("^9\\d{9}$").matches(phone)) "✓" else "۱۰ رقم با ۹") },
                singleLine = true, isError = bad,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                prefix = { Text("+98", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
            )

            // ─── مدرسه / استان / شهرستان ───
            OutlinedTextField(
                value = schoolName, onValueChange = { schoolName = it },
                label = { Text("نام مدرسه") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ExposedDropdownMenuBox(expanded = provinceOpen, onExpandedChange = { provinceOpen = it }) {
                OutlinedTextField(
                    value = province, onValueChange = {}, readOnly = true,
                    label = { Text("استان") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(provinceOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = provinceOpen, onDismissRequest = { provinceOpen = false }) {
                    IRAN_PROVINCES.forEach { pr ->
                        DropdownMenuItem(text = { Text(pr) }, onClick = { province = pr; provinceOpen = false })
                    }
                }
            }
            OutlinedTextField(
                value = city, onValueChange = { city = it },
                label = { Text("شهرستان") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ─── پایه (فقط نمایش) ───
            OutlinedTextField(
                value = (profile?.grade ?: com.hamyareman.ir.ui.profile.StudentProfileState.grade).fa,
                onValueChange = {}, readOnly = true,
                label = { Text("پایه تحصیلی") },
                supportingText = { Text("تغییر فقط یک‌بار، تا یک هفته پس از خرید، از طریق پشتیبانی") },
                modifier = Modifier.fillMaxWidth(),
            )

            // ─── وضعیت اشتراک (فقط نمایش — تغییر از سمت پشتیبانی) ───
            val sub = com.hamyareman.ir.ui.profile.StudentProfileState.subscription.ifBlank { "free" }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (sub == "yearly") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (sub == "yearly") "🎫 اشتراک: یک‌ساله" else "🎈 اشتراک: رایگان", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("تغییر از طریق پشتیبانی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = {
                    showErrors = true
                    val phoneOk = phone.isBlank() || Regex("^9\\d{9}$").matches(phone)
                    if (firstName.trim().length >= 2 && lastName.trim().length >= 2 &&
                        (ageText.toIntOrNull() ?: 0) in 5..60 && phoneOk
                    ) {
                        saving = true
                        onSave(
                            (profile ?: StudentProfile(
                                userId = "", email = email, firstName = "", lastName = "",
                                age = 0, grade = com.hamyareman.ir.ui.profile.StudentProfileState.grade, phone = "",
                            )).copy(
                                firstName = firstName.trim(), lastName = lastName.trim(),
                                age = ageText.toInt(), email = email.trim(), phone = phone,
                                schoolName = schoolName.trim(), province = province, city = city.trim(),
                            ),
                        )
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("ذخیره‌ی تغییرات", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}
