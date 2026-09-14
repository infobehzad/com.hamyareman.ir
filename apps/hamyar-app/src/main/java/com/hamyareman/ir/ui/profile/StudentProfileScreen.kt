package com.hamyareman.ir.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.hamyareman.ir.platform.core.designsystem.AppTopBar

/** فقط ارقام لاتین — کیبورد فارسی هم ممکن است ۰-۹ بدهد؛ همه را لاتین می‌کنیم. */
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
 * فرم ثبت‌نام دانش‌آموز — بلافاصله پس از اولین ورود موفق گوگل.
 * قواعد اجباری: نام و نام‌خانوادگی فارسی، سن عددی، پایه (دراپ‌داون)،
 * موبایل ۱۰ رقمی با +98 ثابت. تغییر پایه فقط یک‌بار و تا یک هفته پس از
 * خرید و فقط از طریق پشتیبانی (تغییر سمت سرور) — با اعلان واضح و بولد.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    email: String,
    saving: Boolean,
    error: String?,
    onSubmit: (firstName: String, lastName: String, age: Int, grade: GradeLevel, phone: String) -> Unit,
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf(GradeLevel.G9) }
    var gradeOpen by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    val firstNameBad = showErrors && firstName.trim().length < 2
    val lastNameBad = showErrors && lastName.trim().length < 2
    val ageBad = showErrors && ((ageText.toIntOrNull() ?: 0) !in 5..60)
    val phoneBad = showErrors && !Regex("^9\\d{9}$").matches(phone)

    Column(Modifier.fillMaxSize()) {
        AppTopBar("ثبت‌نام دانش‌آموز", onBack = {})

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "خوش آمدی! برای همگام‌سازی پیشرفتت، این فرم را یک‌بار کامل کن.",
                style = MaterialTheme.typography.bodyMedium,
            )

            // ایمیل از حساب گوگل — فقط نمایشی (غیرقابل ویرایش).
            OutlinedTextField(
                value = email,
                onValueChange = {},
                label = { Text("ایمیل (از حساب گوگل)") },
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("نام *") },
                isError = firstNameBad,
                supportingText = { if (firstNameBad) Text("نام را فارسی و کامل بنویس") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("نام خانوادگی *") },
                isError = lastNameBad,
                supportingText = { if (lastNameBad) Text("نام خانوادگی را کامل بنویس") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = latinDigits(it).filter { c -> c.isDigit() }.take(2) },
                label = { Text("سن *") },
                isError = ageBad,
                supportingText = { if (ageBad) Text("سن بین ۵ تا ۶۰") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            // دراپ‌داون پایه — همان گزینه‌های مصوب.
            ExposedDropdownMenuBox(expanded = gradeOpen, onExpandedChange = { gradeOpen = it }) {
                OutlinedTextField(
                    value = grade.fa,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("پایه تحصیلی *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gradeOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = gradeOpen, onDismissRequest = { gradeOpen = false }) {
                    GradeLevel.entries.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g.fa, fontWeight = if (g == grade) FontWeight.Bold else null) },
                            onClick = { grade = g; gradeOpen = false },
                        )
                    }
                }
            }

            // موبایل: +98 ثابت و غیرقابل حذف + دقیقاً ۱۰ رقم.
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = latinDigits(it).filter { c -> c.isDigit() }.take(10) },
                label = { Text("شماره همراه *") },
                isError = phoneBad,
                supportingText = {
                    Text(if (phoneBad) "دقیقاً ۱۰ رقم — با ۹ شروع شود" else "۱۰ رقم؛ ۹۸ به‌صورت خودکار اولش هست")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                prefix = { Text("+98", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
            )

            // اعلان واضح و بولد — سیاست تغییر پایه.
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "⚠️ در انتخاب پایه دقت کن: تغییر پایه فقط «یک‌بار» و فقط تا «یک هفته پس از خرید» و صرفاً از طریق پشتیبانی (پروایدر) و با تغییر سمت سرور ممکن است.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.padding(10.dp),
                )
            }

            if (!error.isNullOrBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    showErrors = true
                    val ok = firstName.trim().length >= 2 && lastName.trim().length >= 2 &&
                        (ageText.toIntOrNull() ?: 0) in 5..60 && Regex("^9\\d{9}$").matches(phone)
                    if (ok && !saving) onSubmit(firstName.trim(), lastName.trim(), ageText.toInt(), grade, phone)
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("ثبت و ورود به همیار", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
