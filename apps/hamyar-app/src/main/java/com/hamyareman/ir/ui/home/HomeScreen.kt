package com.hamyareman.ir.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.ui.hub.HubCard
import com.hamyareman.ir.ui.hub.hubTo
import com.hamyareman.ir.ui.navigation.Screen
import com.hamyareman.ir.ui.profile.StudentProfileState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
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

private val gregMonth = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec",
)

@Composable
fun HomeScreen(nav: NavController) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val time = remember(now) { LocalDateTime.ofInstant(Instant.ofEpochMilli(now), JalaliDate.TEHRAN) }
    val jalali = remember(now) { JalaliDate.toJalali(now) }
    val iso = remember(now) { JalaliDate.todayIso() }
    val weekday = JalaliDate.weekDayFa(iso)
    val row1 = toPersianDigits("$weekday ${jalali.day} ${JalaliDate.monthName(jalali.month)} ${jalali.year}")
    val h24 = time.hour
    val h12 = val12(h24)
    val period = if (h24 < 12) "قبل از ظهر" else "بعد از ظهر"
    val row2Time = toPersianDigits("%d:%02d".format(h12, time.minute)) + " $period"
    val row2Greg = "${time.year}/${gregMonth[time.monthValue - 1]}/${time.dayOfMonth}"
    val holiday = IranOfficialHolidays.occasion(jalali)
    val who = StudentProfileState.firstName.ifBlank { "دوست من" }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { nav.navigate(Screen.Calm.route) }) { Text("💛") }
    }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GreetingBanner(title = "${greeting()} $who جان", subtitle = "همیار من کنارت است؛ از مدرسه تا آرامش")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Column(Modifier.weight(1f).padding(end = 36.dp)) {
                                Text(
                                    row1,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    row2Time,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(row2Greg, style = MaterialTheme.typography.bodyLarge)
                                }
                                if (!holiday.isNullOrBlank()) {
                                    Text(holiday, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(8.dp))
                                SubscriptionChip(StudentProfileState.subscription)
                            }
                        }
                        Spacer(Modifier.width(28.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { nav.navigate(Screen.UserProfile.route) },
                        ) {
                            ProfileClockAvatar(onClick = { nav.navigate(Screen.UserProfile.route) })
                            Text(
                                "پروفایل من",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            WisdomCard()

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("🎒", "مدرسه", Modifier.weight(1f)) { nav.hubTo(Screen.Study.route) }
                QuickTile("💚", "سلامتی", Modifier.weight(1f)) { nav.hubTo(Screen.HealthHub.route) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("🪷", "آگاهی", Modifier.weight(1f)) { nav.hubTo(Screen.AwarenessHub.route) }
                QuickTile("🤖", "همراه من", Modifier.weight(1f)) { nav.hubTo(Screen.Chat.route) }
            }

            ClassPlanCard(
                onOpenPlan = { nav.navigate(Screen.WeeklySchedule.route) },
                onOpenPrep = { nav.navigate(Screen.TomorrowPrep.route) },
            )

            SectionCard("حالت امروز چطوره؟", "با یک ایموجی ثبتش کن — اختیاریه.") { nav.navigate(Screen.Mood.route) }

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
private fun GreetingBanner(title: String, subtitle: String) {
    val shape = RoundedCornerShape(28.dp)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0F766E), Color(0xFF115E59), Color(0xFF1E3A8A)),
                    ),
                )
                .padding(horizontal = 22.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontFamily = DashboardFonts.greeting,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.92f),
                    fontFamily = DashboardFonts.quote,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun WisdomCard() {
    val ctx = LocalContext.current
    val all = remember { WisdomQuotes.load(ctx) }
    var line by remember { mutableStateOf(WisdomQuotes.current(ctx, all)) }
    LaunchedEffect(line) {
        delay(WisdomQuotes.remainingMs(ctx))
        line = WisdomQuotes.current(ctx, all)
    }
    val body = if (line.author.isBlank()) line.text else line.oneLine()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            val n = body.length.coerceAtLeast(1)
            val sp = (maxWidth.value / (n * 0.62f)).coerceIn(11f, 18f)
            Text(
                body,
                fontFamily = DashboardFonts.quote,
                fontSize = sp.sp,
                color = Color(0xFF9A3412),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun val12(h24: Int): Int {
    val h = h24 % 12
    return if (h == 0) 12 else h
}

@Composable
internal fun SubscriptionChip(raw: String) {
    val paid = StudentProfileState.isPaid(raw)
    val bg = if (paid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
    val fg = if (paid) Color(0xFF166534) else Color(0xFFB91C1C)
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            if (paid) "پرمیوم" else "رایگان",
            color = fg,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
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
