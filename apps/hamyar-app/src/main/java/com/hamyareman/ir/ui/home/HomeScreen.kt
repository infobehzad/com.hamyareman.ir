package com.hamyareman.ir.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
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
import com.hamyareman.ir.ui.hub.HubCard
import com.hamyareman.ir.ui.hub.hubTo
import com.hamyareman.ir.ui.navigation.Screen
import com.hamyareman.ir.ui.profile.StudentProfileState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.util.Calendar

/** مارجینِ کناریِ بلوک‌های داشبورد (کارتِ سخن بزرگان عمداً پهن‌تر و بی‌مارجین‌تر است). */
private val HomeSide = 16.dp

/**
 * فونتِ متنِ «سخن بزرگان».
 *
 * فونتِ درخواستی («بدخط») در مخزن و در پوشهٔ `res/font` اپ نیست؛ تا فایلش برسد،
 * نزدیک‌ترین فونتِ دست‌نویسِ خودِ اپ (هیلدا) استفاده می‌شود. برای سوئیچ، فقط
 * همین یک خط را به فونتِ تازه عوض کن (یا `DashboardFonts.badkhat` را بساز).
 */
private val WISDOM_FONT = DashboardFonts.hilda

/** مارجینِ کناریِ کارتِ «سخن بزرگان» — یک‌پنجمِ حالتِ معمول. */
private val WisdomSide = 1.dp

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
            Modifier.fillMaxSize().padding(pad).padding(vertical = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GreetingBanner(
                title = "${greeting()} $who جان",
                subtitle = "همیار من کنارت است؛ از مدرسه تا آرامش",
                modifier = Modifier.padding(horizontal = HomeSide),
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = HomeSide),
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
                                SubscriptionChip(StudentProfileState.subscription) { nav.navigate(Screen.Subscription.route) }
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

            WisdomCard(Modifier.padding(horizontal = WisdomSide))

            Row(Modifier.fillMaxWidth().padding(horizontal = HomeSide), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("🎒", "مدرسه", Modifier.weight(1f)) { nav.hubTo(Screen.Study.route) }
                QuickTile("📖", "کتاب متنی و صوتی", Modifier.weight(1f)) { nav.navigate(Screen.FreeReading.route) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = HomeSide), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("🪷", "آگاهی", Modifier.weight(1f)) { nav.hubTo(Screen.AwarenessHub.route) }
                QuickTile("💛", "آرامش", Modifier.weight(1f)) { nav.navigate(Screen.CalmHub.route) }
            }

            ClassPlanCard(
                modifier = Modifier.padding(horizontal = HomeSide),
                onOpenPlan = { nav.navigate(Screen.ClassPlan.route) },
                onOpenPrep = { nav.navigate(Screen.TomorrowPrep.route) },
                onOpenAlarm = { nav.navigate(Screen.ClassPlanShift.route) },
            )

            // چهار کارتِ کم‌عرض در یک ردیف — جای کارتِ «مطالعه آزاد» (مطالعه از تب «مدرسه» در دسترس است).
            Row(
                Modifier.fillMaxWidth().padding(horizontal = HomeSide),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToolTile("🧰", "جعبه‌ابزار عمومی", Modifier.weight(1f)) { nav.navigate(Screen.GeneralToolkit.route) }
                ToolTile("⚗️", "آزمایشگاه شیمی", Modifier.weight(1f)) { nav.navigate(Screen.ChemistryLab.route) }
                ToolTile("🔬", "آزمایشگاه فیزیک", Modifier.weight(1f)) { nav.navigate(Screen.PhysicsLab.route) }
                ToolTile("🧮", "جعبه‌ابزار ریاضی", Modifier.weight(1f)) { nav.navigate(Screen.MathToolkit.route) }
            }

            Text("امروز", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = HomeSide))
            HubCard("🌤", "روتین امروز", "بلوک‌های روزت را ببین", Modifier.padding(horizontal = HomeSide)) { nav.navigate(Screen.Routine.route) }
            HubCard("💧", "آب بنوش", "لیوان‌های امروزت را ثبت کن", Modifier.padding(horizontal = HomeSide)) { nav.navigate(Screen.Water.route) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GreetingBanner(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Card(
        modifier = modifier.fillMaxWidth(),
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontFamily = DashboardFonts.greeting,
                    fontSize = 22.sp,
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
private fun WisdomCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val all = remember { WisdomQuotes.load(ctx) }
    var line by remember { mutableStateOf(WisdomQuotes.current(ctx, all)) }
    LaunchedEffect(line) {
        delay(WisdomQuotes.remainingMs(ctx))
        line = WisdomQuotes.current(ctx, all)
    }
    val body = if (line.author.isBlank()) line.text else line.oneLine()

    // ---- انیمیشن‌های کارتِ «تم فلسفی» ----
    val anim = rememberInfiniteTransition(label = "wisdom-glow")
    val spin by anim.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 9000, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by anim.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val drift by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 12000, easing = LinearEasing)),
        label = "drift",
    )

    Box(modifier.fillMaxWidth()) {
        // لایهٔ افکت: ستاره‌های چشمک‌زن + هالهٔ نفس‌کش + قوسِ طلاییِ چرخان
        Box(
            Modifier.matchParentSize().drawBehind {
                drawRoundRect(
                    color = Color(0xFFFBBF24).copy(alpha = 0.05f + 0.16f * pulse),
                    topLeft = Offset(-1.dp.toPx(), -1.dp.toPx()),
                    size = Size(size.width + 2.dp.toPx(), size.height + 2.dp.toPx()),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 6.dp.toPx()),
                )
                // ستاره‌ها: جای ثابت (از روی شمارهٔ ستاره) و چشمکِ آرام
                for (i in 0 until 30) {
                    val fx = ((i * 37) % 97) / 97f
                    val fy = ((i * 61) % 89) / 89f
                    val tw = 0.45f + 0.55f * kotlin.math.abs(kotlin.math.sin((drift * 6.283f) + i))
                    drawCircle(
                        color = Color(0xFFFFF7DB).copy(alpha = 0.06f + 0.16f * tw),
                        radius = (0.7f + (i % 3) * 0.45f).dp.toPx(),
                        center = Offset(fx * size.width, fy * size.height),
                    )
                }
                // قوسِ طلاییِ چرخان روی لبه
                val c = Offset(size.width / 2f, size.height / 2f)
                val shader = android.graphics.SweepGradient(
                    c.x,
                    c.y,
                    intArrayOf(
                        Color(0xFFFDE68A).toArgb(),
                        Color(0xFFF59E0B).toArgb(),
                        Color(0xFF8B5CF6).toArgb(),
                        Color(0xFF1E1B4B).toArgb(),
                        Color(0xFFFDE68A).toArgb(),
                    ),
                    null,
                )
                shader.setLocalMatrix(android.graphics.Matrix().apply { setRotate(spin, c.x, c.y) })
                drawRoundRect(
                    brush = ShaderBrush(shader),
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                    cornerRadius = CornerRadius(21.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
            },
        )

        // خودِ کارت: آسمانِ شب با گرادیانِ بنفش/نیلی و متنِ طلایی
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF111633), Color(0xFF1E1B4B), Color(0xFF3B0764)),
                    ),
                )
                .clickable { line = WisdomQuotes.next(ctx, all) }
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            // نشانِ نقل‌قولِ تزئینی، پشتِ متن
            Text(
                "❝",
                fontSize = 46.sp,
                color = Color(0xFFFBBF24).copy(alpha = 0.16f + 0.10f * pulse),
                modifier = Modifier.align(Alignment.TopStart),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val n = body.length.coerceAtLeast(1)
                    val sp = (maxWidth.value / (n * 0.56f)).coerceIn(12f, 22f)
                    Text(
                        body,
                        fontFamily = WISDOM_FONT,
                        fontSize = sp.sp,
                        color = Color(0xFFFDE68A),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (line.author.isBlank()) "برای جملهٔ بعدی بزن" else "— ${line.author} · برای جملهٔ بعدی بزن",
                    fontSize = 10.sp,
                    color = Color(0xFFE9D5FF).copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun val12(h24: Int): Int {
    val h = h24 % 12
    return if (h == 0) 12 else h
}

@Composable
/**
 * برچسب وضعیت اشتراک — قابل لمس است و به صفحه‌ی اشتراک می‌رود
 * (پیش‌تر فقط متن بود و هیچ‌جا لینک نمی‌شد).
 */
internal fun SubscriptionChip(raw: String, onClick: () -> Unit = {}) {
    val paid = StudentProfileState.isPaid(raw)
    val bg = if (paid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
    val fg = if (paid) Color(0xFF166534) else Color(0xFFB91C1C)
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        onClick = onClick,
    ) {
        Text(
            if (paid) "اشتراک فعال" else "مهمان همیار من",
            color = fg,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/**
 * کارتِ کم‌عرضِ داشبورد برای جعبه‌ابزارها — چهار عدد در یک ردیف؛
 * متن دو خط می‌شکند و در ارتفاعِ ثابت وسط‌چین می‌ماند.
 */
@Composable
private fun ToolTile(emoji: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontFamily = DashboardFonts.aria,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
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
