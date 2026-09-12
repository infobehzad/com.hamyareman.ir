package ir.behzad.roozhayeman.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.behzad.roozhayeman.ui.hub.HubCard
import ir.behzad.roozhayeman.ui.hub.hubTo
import ir.behzad.roozhayeman.ui.hub.HubHeader
import ir.behzad.roozhayeman.ui.hub.HubBody
import ir.behzad.roozhayeman.ui.navigation.Screen
import ir.behzad.platform.core.common.LocalStore

/**
 * «بیشتر» — همه‌چیز غیر از ۵ تب اصلی:
 * ارتباط با بابا، فضای امن، سرگرمی‌های خلاق، شخصی‌سازی (ظاهر و فونت) و تنظیمات.
 */
@Composable
fun MoreScreen(nav: NavController) {
    HubBody {
        HubHeader("بیشتر", "هر چیز دیگر که به کارت می‌آید")

        HubCard("💞", "قلب‌به‌قلب با بابا", "پیام، ویس، عکس و تماس — جای امنِ دوتایی") { nav.hubTo(Screen.Heart.route) }
        HubCard("🔗", "پیوند با بابا", "کد ۶ رقمی اتصال") { nav.hubTo(Screen.Pairing.route) }

        HubCard("🛟", "فضای امن من", "نوشتن، آلبوم، شماره‌های کمک") { nav.hubTo(Screen.SafeSpace.route) }
        HubCard("🚨", "شماره‌های کمک", "همیشه در دسترس") { nav.hubTo(Screen.Helplines.route) }

        HubCard("🎨", "نقاشی سیاه‌قلم", "ایده‌ی امروز و گالری") { nav.hubTo(Screen.Art.route) }
        HubCard("🍲", "آشپزی", "دستور پخت درخواستی") { nav.hubTo(Screen.Recipes.route) }
        HubCard("🏅", "امتیاز و بج", "فقط جنبه‌ی مثبت") { nav.hubTo(Screen.Badges.route) }
        HubCard("⏳", "زمان صفحه", "سقف خودانتخابی و حالت تمرکز") { nav.hubTo(Screen.ScreenTime.route) }

        QuietModeCard()

        HubCard("🎨", "ظاهر و فونت", "تم رنگی، حالت تاریک/روشن، فونت دانلودی") { nav.hubTo(Screen.Appearance.route) }
        HubCard("⚙️", "تنظیمات", "حریم، قفل، همگام‌سازی") { nav.hubTo(Screen.Settings.route) }

        Text(
            "همیار من — نسخه‌ی " + ir.behzad.roozhayeman.BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * «زمان درس» — کلیدِ سکوتِ سراسریِ پلیر دروس: وقتی روشن است، هیچ صدایی از
 * پلیر صوت/ویدیوی تدریس و خلاصه‌ها پخش نمی‌شود (خودکار متوقف و بی‌صدا می‌ماند).
 */
@Composable
private fun QuietModeCard() {
    val ctx = LocalContext.current
    val store = remember { LocalStore(ctx, "hamyar_teach") }
    var on by remember { mutableStateOf(store.getString("quiet_mode", "0") == "1") }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🔇 زمان درس", style = MaterialTheme.typography.titleMedium)
                Text(
                    "وقتی روشن است هیچ صدایی از پلیر دروس (صوت و ویدیوی تدریس) فعال نمی‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = on,
                onCheckedChange = {
                    on = it
                    store.putString("quiet_mode", if (it) "1" else "0")
                },
            )
        }
    }
}
