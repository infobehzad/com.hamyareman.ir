package ir.behzad.roozhayeman.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * دروازه‌ی ورود — تا کاربر وارد نشود، هیچ محتوایی رندر نمی‌شود.
 * ورود با گوگل (OAuth از طریق Appwrite) راه اصلی است؛ «ورود مهمان» فقط برای
 * مواقع اضطراری است و روی سرور نقش guest می‌گیرد.
 */
@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    onGoogle: () -> Unit,
    onGuest: () -> Unit,
) {
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
            "دفترچه‌ی خصوصی توست؛ برای بازکردنش اول وارد شو تا فقط خودت به آن دسترسی داشته باشی.",
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
                    Button(onClick = onGoogle, modifier = Modifier.fillMaxWidth()) {
                        Text("ورود با گوگل")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onGuest, modifier = Modifier.fillMaxWidth()) {
                        Text("ورود مهمان (بدون همگام‌سازی)")
                    }
                }
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "🔒 چرخه، ژورنال و چتِ خام هرگز به سرور نمی‌رود؛ لاگین فقط هویت تو را تأیید می‌کند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
