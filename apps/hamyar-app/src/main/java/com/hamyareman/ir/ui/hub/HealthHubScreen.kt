package com.hamyareman.ir.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** هاب «سلامتی»: حرکات، آب، خواب، چرخه، یادآور دارو. */
@Composable
fun HealthHubScreen(nav: NavController) {
    HubBody {
        HubHeader("سلامتی 💚", "بدنت دوست توست — هر روز یک قدم مهربانی")
        HubCard("📊", "پیشرفت سلامتی", "آب/ورزش/نقاشی + آمار تدریس، مرور و آزمون‌ها") { nav.hubTo(Screen.HealthProgress.route) }
        HubCard("🧘", "حرکات ورزشی و یوگا", "۴۳ حرکت با راهنمای صوتی آزرا و تایمر") { nav.hubTo(Screen.Wellness.route) }
        HubCard("💧", "آب بنوش", "لیوان‌های امروز را ثبت کن") { nav.hubTo(Screen.Water.route) }
        HubCard("😴", "خواب من", "خوابیدن و بیدار شدن را ثبت کن؛ رشته‌ات را نگه دار") { nav.hubTo(Screen.SleepLog.route) }
        HubCard("🌸", "چرخه و حال‌ها", "تقویم چرخه، ثبت حال و ذهن‌آگاهی") { nav.hubTo(Screen.Cycle.route) }
        HubCard("💊", "یادآور دارو و مراقبت", "دارو یا مراقبت روزانه با هشدار سرِ وقت") { nav.hubTo(Screen.Meds.route) }
        HubCard("🌤", "روتین روز", "بلوک‌های روزت را ببین یا روز سبک انتخاب کن") { nav.hubTo(Screen.Routine.route) }

        HubMenuGroup("🪷 آگاهی", "حال‌خوب و ذهن‌آگاهی — همین‌جا کنار سلامتی") {
            HubCard("📓", "دفترچه‌ی من", "حرف‌های بلندتر؛ روزنوشت آزاد") { nav.hubTo(Screen.Journal.route) }
            HubCard("🌬", "تمرین نفس", "با شمارش صوتی و انیمیشن") { nav.hubTo(Screen.Breath.route) }
            HubCard("🧠", "ذهن‌آگاهی", "تمرین‌های کوتاه حضور") { nav.hubTo(Screen.Mindfulness.route) }
            HubCard("💛", "آرامش سریع", "امواج، جنگل بارانی و ریست طلایی") { nav.hubTo(Screen.Calm.route) }
            HubCard("🧘", "حرکات آرامش", "جلسه‌های صوتی کامل آرامش") { nav.hubTo(Screen.Wellness.route) }
        }
    }
}

/**
 * ثبت خواب ساده و مهربان: ساعت خواب/بیداری هر شب + رشته‌ی شب‌های پیوسته.
 * داده فقط همین‌جا (LocalStore) می‌ماند.
 */
@Composable
fun SleepLogScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { LocalStore(context, "hamyar_health") }
    var bedtime by remember { mutableStateOf(store.getString("sleep_bed_${todayKey()}", "")) }
    var waketime by remember { mutableStateOf(store.getString("sleep_wake_${todayKey()}", "")) }

    HubBody {
        HubHeader("خواب من 😴", "امشب هم به بدنت آرامش بده", onBack)
        Text(
            "نیمی از شارژِ فردا در خوابِ امشب ذخیره می‌شود. ساعت‌ها را حدودی بنویس؛ کافی است.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = bedtime,
                onValueChange = { v -> if (v.length <= 5) { bedtime = v; store.putString("sleep_bed_${todayKey()}", v) } },
                label = { Text("خوابیدم (مثلاً 22:30)") },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = waketime,
                onValueChange = { v -> if (v.length <= 5) { waketime = v; store.putString("sleep_wake_${todayKey()}", v) } },
                label = { Text("بیدار شدم") },
                modifier = Modifier.weight(1f),
            )
        }
        val streak = remember { sleepStreak(store) }
        Text("🌙 رشته‌ی شب‌های ثبت‌شده: $streak شب پیوسته", style = MaterialTheme.typography.titleMedium)
        Text(
            "ثبتِ ناقص هم اشکال ندارد؛ مهم این است که زنجیره پاره نشود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

internal fun sleepStreak(store: LocalStore): Int {
    var streak = 0
    val cal = Calendar.getInstance()
    repeat(60) {
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        if (store.getString("sleep_bed_$key", "").isNotBlank() || store.getString("sleep_wake_$key", "").isNotBlank()) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else if (it == 0) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else return streak
    }
    return streak
}
