package com.hamyareman.ir.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * دروازه‌ی ورود — تا کاربر وارد نشود، هیچ محتوایی رندر نمی‌شود.
 * مسیر اصلی: ایمیل + رمز (ثبت‌نام / ورود). گوگل مسیر دوم است.
 */
@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    onGoogle: () -> Unit,
    onEmailSignIn: (email: String, password: String) -> Unit,
    onEmailSignUp: (name: String, email: String, password: String) -> Unit,
) {
    var modeSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        localError = null
        val em = email.trim()
        val pw = password
        if (!em.contains("@") || em.length < 5) {
            localError = "یک ایمیل معتبر بنویس."
            return
        }
        if (pw.length < 8) {
            localError = "رمز باید حداقل ۸ کاراکتر باشد."
            return
        }
        if (modeSignUp) {
            if (name.trim().length < 2) {
                localError = "نام را کامل بنویس."
                return
            }
            onEmailSignUp(name.trim(), em, pw)
        } else {
            onEmailSignIn(em, pw)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("همیار من 💜", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "دفترچه‌ی خصوصی توست؛ یک‌بار با ایمیل وارد شو تا فقط خودت به آن دسترسی داشته باشی.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    if (modeSignUp) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("نام") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = { Text("ایمیل") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("رمز عبور") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { submit() }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (modeSignUp) "ساخت حساب و ورود" else "ورود با ایمیل")
                    }
                    TextButton(onClick = { modeSignUp = !modeSignUp; localError = null }) {
                        Text(if (modeSignUp) "حساب داری؟ ورود" else "حساب نداری؟ ثبت‌نام")
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = onGoogle, modifier = Modifier.fillMaxWidth()) {
                        Text("ورود با گوگل")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "گوگل مسیر دوم است؛ اگر قبلاً با گوگل آمده‌ای همان حساب را باز می‌کند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                val shown = localError ?: error
                shown?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "نسخه‌ی " + com.hamyareman.ir.BuildConfig.VERSION_NAME + " — اگر این عدد را نمی‌بینی، APK قدیمی نصب است.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "🔒 چرخه، ژورنال و چتِ خام هرگز به سرور نمی‌رود؛ لاگین فقط هویت تو را تأیید می‌کند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
