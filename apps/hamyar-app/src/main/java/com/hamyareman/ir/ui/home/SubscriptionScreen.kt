package com.hamyareman.ir.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.profile.StudentProfileState

/** صفحهٔ نمایشی اشتراک — درگاه پرداخت بعداً وصل می‌شود. */
@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    val paid = StudentProfileState.isPaid()
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        AppTopBar("اشتراک همیار من", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (paid) "اشتراک فعال است." else "مهمان همیار من — برای درس‌های کامل، اشتراک سالانه لازم است.",
                fontFamily = DashboardFonts.quote,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text("وضعیت فعلی: ${StudentProfileState.subscription.ifBlank { "free" }}", fontFamily = DashboardFonts.quote)
            PlanCard("ماهانه", "۴۹٬۰۰۰ تومان", "دسترسی یک‌ماهه به تدریس و تمرین")
            PlanCard("سالانه", "۳۹۰٬۰۰۰ تومان", "صرفه‌جویی نسبت به ماهانه · پیشنهاد اصلی")
            PlanCard("اعتبار هدیه", "کد تخفیف", "کد را بعداً از پشتیبانی می‌گیری")
            OutlinedButton(onClick = { /* درگاه بعداً */ }, modifier = Modifier.fillMaxWidth(), enabled = false) {
                Text("خرید به‌زودی فعال می‌شود", fontFamily = DashboardFonts.quote)
            }
            Text(
                "این صفحه نمایشی است؛ مبلغ و درگاه واقعی هنوز وصل نشده.",
                fontFamily = DashboardFonts.quote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text("نسخهٔ نمایش برای ${StudentProfileState.firstName.ifBlank { "دانش‌آموز" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(ctx.packageName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanCard(title: String, price: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontFamily = DashboardFonts.greeting, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(price, fontFamily = DashboardFonts.lalezar, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Text(body, fontFamily = DashboardFonts.quote, style = MaterialTheme.typography.bodySmall)
        }
    }
}
