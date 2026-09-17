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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
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
 * نام/نام‌خانوادگی/تاریخ تولد شمسی (سن خودکار)/ایمیل/موبایل + مدرسه/استان + عکس پروفایل (محلی).
 * پایه: فقط نمایش (تغییر یک‌بار و از سمت پشتیبانی).
 * اشتراک (رایگان/یک‌ساله): فقط نمایش — تغییر از سمت پشتیبانی/سرور.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    profile: StudentProfile?,
    onBack: () -> Unit,
    onSave: (StudentProfile) -> Unit,
    onLogout: () -> Unit = {},
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
    val initialBirth = remember { JalaliDate.parseJalali(profile?.birthDate.orEmpty()) }
    var birthYear by remember { mutableStateOf(initialBirth?.year) }
    var birthMonth by remember { mutableStateOf(initialBirth?.month) }
    var birthDay by remember { mutableStateOf(initialBirth?.day) }
    val birthJalali = remember(birthYear, birthMonth, birthDay) {
        val y = birthYear; val m = birthMonth; val d = birthDay
        if (y != null && m != null && d != null) JalaliDate.Jalali(y, m, d).takeIf { JalaliDate.isValid(it) } else null
    }
    val computedAge = remember(birthJalali) { birthJalali?.let { JalaliDate.ageYears(it) } }
    var email by remember { mutableStateOf(profile?.email.orEmpty()) }
    var phone by remember { mutableStateOf(profile?.phone.orEmpty()) }
    var schoolName by remember { mutableStateOf(profile?.schoolName.orEmpty()) }
    var province by remember { mutableStateOf(profile?.province.orEmpty()) }
    var county by remember { mutableStateOf(profile?.county.orEmpty()) }
    var city by remember { mutableStateOf(profile?.city.orEmpty()) }
    var gender by remember { mutableStateOf(profile?.gender.orEmpty()) }
    var showErrors by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    val bad = showErrors && (
        firstName.trim().length < 2 || lastName.trim().length < 2 ||
            (computedAge == null || computedAge !in 5..60) ||
            phone.isNotBlank() && !Regex("^9\\d{9}$").matches(phone) ||
            gender.isBlank()
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
            JalaliBirthDateFields(
                year = birthYear,
                month = birthMonth,
                day = birthDay,
                onChange = { y, m, d -> birthYear = y; birthMonth = m; birthDay = d },
                isError = showErrors && (computedAge == null || computedAge !in 5..60),
            )
            OutlinedTextField(
                value = computedAge?.let { toPersianDigits(it.toString()) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("سن") },
                isError = showErrors && (computedAge == null || computedAge !in 5..60),
                supportingText = { Text("از تاریخ تولد شمسی حساب می‌شود") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email, onValueChange = {},
                readOnly = true,
                label = { Text("ایمیل") }, singleLine = true,
                supportingText = { Text("تغییر ایمیل فعلاً از همین‌جا ممکن نیست") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
            }

            Text("جنسیت *", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudentGender.entries.forEach { g ->
                    FilterChip(
                        selected = gender == g.id,
                        onClick = { gender = g.id },
                        label = { Text(g.fa) },
                    )
                }
            }
            if (showErrors && gender.isBlank()) {
                Text("پسر یا دختر را انتخاب کن", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(
                value = schoolName, onValueChange = { schoolName = it },
                label = { Text("نام مدرسه") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            IranLocationFields(
                province = province, county = county, city = city,
                onProvince = { province = it }, onCounty = { county = it }, onCity = { city = it },
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
            val sub = StudentProfileState.subscription.ifBlank { "free" }
            val paid = StudentProfileState.isPaid(sub)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (paid) androidx.compose.ui.graphics.Color(0xFFDCFCE7) else androidx.compose.ui.graphics.Color(0xFFFEE2E2),
                border = BorderStroke(1.dp, if (paid) androidx.compose.ui.graphics.Color(0xFF166534) else androidx.compose.ui.graphics.Color(0xFFB91C1C)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (paid) "اشتراک: پرمیوم" else "اشتراک: رایگان",
                        fontWeight = FontWeight.Bold,
                        color = if (paid) androidx.compose.ui.graphics.Color(0xFF166534) else androidx.compose.ui.graphics.Color(0xFFB91C1C),
                    )
                    Spacer(Modifier.weight(1f))
                    Text("همگام با سرور — تغییر از پشتیبانی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = {
                    showErrors = true
                    val phoneOk = phone.isBlank() || Regex("^9\\d{9}$").matches(phone)
                    val age = computedAge
                    if (firstName.trim().length >= 2 && lastName.trim().length >= 2 &&
                        age != null && age in 5..60 && birthJalali != null && phoneOk && gender.isNotBlank()
                    ) {
                        saving = true
                        onSave(
                            (profile ?: StudentProfile(
                                userId = "", email = email, firstName = "", lastName = "",
                                age = 0, grade = com.hamyareman.ir.ui.profile.StudentProfileState.grade, phone = "",
                            )).copy(
                                firstName = firstName.trim(), lastName = lastName.trim(),
                                age = age, birthDate = birthJalali.isoLike, email = email.trim(), phone = phone,
                                schoolName = schoolName.trim(), province = province, county = county, city = city,
                                gender = gender,
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
            OutlinedButton(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("خروج از حساب") }
            Spacer(Modifier.height(14.dp))
        }
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("خروج از حساب؟") },
            text = { Text("نشست بسته می‌شود و برای ورود دوباره باید ایمیل یا گوگل را بزنی.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("خروج") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("انصراف") }
            },
        )
    }
}
