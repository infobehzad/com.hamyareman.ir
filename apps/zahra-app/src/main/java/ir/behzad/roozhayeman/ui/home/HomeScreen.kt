package ir.behzad.roozhayeman.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.behzad.platform.core.designsystem.SectionCard
import ir.behzad.roozhayeman.R
import ir.behzad.roozhayeman.ui.hub.HubCard
import ir.behzad.roozhayeman.ui.hub.hubTo
import ir.behzad.roozhayeman.ui.navigation.Screen
import java.util.Calendar

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..11 -> "صبح‌ت بخیر"
        in 12..16 -> "ظهرت بخیر"
        in 17..20 -> "عصرت بخیر"
        else -> "شبت بخیر"
    }
}

/**
 * داشبورد «همیار من» — ترکیبی:
 * سربرگ هوشمند (سلام بر اساس ساعت + ماسموت) + ثبت حال
 * + شبکه‌ی میان‌بر ۴گانه + کارت‌های «امروز».
 */
@Composable
fun HomeScreen(nav: NavController) {
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { nav.navigate(Screen.Calm.route) }) { Text("💛") }
    }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------- سربرگ ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.mascot_hamyar),
                    contentDescription = "ماسموت همیار من",
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("${greeting()} زهرا جان 🌸", style = MaterialTheme.typography.headlineMedium)
                    Text("همیار من کنارت است؛ از مدرسه تا آرامش", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ---------- حال امروز ----------
            SectionCard("حالت امروز چطوره؟", "با یک ایموجی ثبتش کن — اختیاریه.") { nav.navigate(Screen.Mood.route) }

            // ---------- شبکه‌ی میان‌بر ۴گانه ----------
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("🎒", "مدرسه", Modifier.weight(1f)) { nav.hubTo(Screen.Study.route) }
                QuickTile("💚", "سلامتی", Modifier.weight(1f)) { nav.hubTo(Screen.HealthHub.route) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("🪷", "آگاهی", Modifier.weight(1f)) { nav.hubTo(Screen.AwarenessHub.route) }
                QuickTile("🤖", "همراه من", Modifier.weight(1f)) { nav.hubTo(Screen.Chat.route) }
            }

            // ---------- کارت‌های امروز ----------
            Text("امروز", style = MaterialTheme.typography.titleMedium)
            HubCard("🌤", "روتین امروز", "بلوک‌های روزت را ببین") { nav.navigate(Screen.Routine.route) }
            HubCard("💧", "آب بنوش", "لیوان‌های امروزت را ثبت کن") { nav.navigate(Screen.Water.route) }
            HubCard("💬", "حرف دل با بابا", "پیام، ویس، عکس یا تماس") { nav.navigate(Screen.Heart.route) }
            HubCard("💛", "آرامش سریع", "سه دقیقه تا حال بهتر") { nav.navigate(Screen.Calm.route) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickTile(emoji: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(96.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
