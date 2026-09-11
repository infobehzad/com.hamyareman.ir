package ir.behzad.roozhayeman.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.behzad.roozhayeman.ui.hub.HubCard
import ir.behzad.roozhayeman.ui.hub.hubTo
import ir.behzad.roozhayeman.ui.hub.HubHeader
import ir.behzad.roozhayeman.ui.hub.HubBody
import ir.behzad.roozhayeman.ui.navigation.Screen

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

        HubCard("🎨", "ظاهر و فونت", "تم رنگی، حالت تاریک/روشن، فونت دانلودی") { nav.hubTo(Screen.Appearance.route) }
        HubCard("⚙️", "تنظیمات", "حریم، قفل، همگام‌سازی") { nav.hubTo(Screen.Settings.route) }

        Text(
            "همیار من — نسخه‌ی ۱.۰",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
