package com.hamyareman.ir.ui.calmdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.home.DashboardFonts

@Composable
fun CalmHubScreen(nav: NavController, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("آرامش", onBack)
        TabRow(selectedTabIndex = tab) {
            listOf("سفر ذهنی", "آرامش با تنفس", "تصویرسازی ذهنی").forEachIndexed { i, l ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(l, fontFamily = DashboardFonts.quote, fontSize = DashboardFonts.bump(DashboardFonts.quote, 14)) })
            }
        }
        when (tab) {
            1 -> BreathingScreen(onBack = onBack)
            else -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (tab == 0) "سفر ذهنی — فهرست تمرین‌ها به‌زودی." else "تصویرسازی ذهنی — فهرست به‌زودی.",
                    fontFamily = DashboardFonts.quote, fontSize = DashboardFonts.bump(DashboardFonts.quote, 14),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "از منوی آرامش سریع هم می‌توانی تنفس را شروع کنی.",
                    fontFamily = DashboardFonts.quote, fontSize = DashboardFonts.bump(DashboardFonts.quote, 14),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
