package com.hamyareman.ir.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.home.DashboardFonts

/**
 * صفحهٔ مشترکِ «به‌زودی» برای کارت‌های تازهٔ جعبه‌ابزارها.
 *
 * چهار کارتِ داشبورد (جعبه‌ابزار عمومی، آزمایشگاه شیمی، آزمایشگاه فیزیک، جعبه‌ابزار
 * ریاضی) فعلاً همین صفحه را باز می‌کنند و فهرستِ کاری که قرار است انجام شود را
 * نشان می‌دهند؛ محتوای واقعی در چرخه‌های بعدی به همین صفحه می‌نشیند.
 */
@Composable
fun ToolSoonScreen(
    emoji: String,
    title: String,
    subtitle: String,
    items: List<String>,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title, onBack)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F766E), Color(0xFF115E59), Color(0xFF1E3A8A)),
                            ),
                        )
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 40.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            title,
                            color = Color.White,
                            fontFamily = DashboardFonts.quote,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.22f),
                        ) {
                            Text(
                                "به‌زودی",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "چه چیزی این‌جا می‌آید؟",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = DashboardFonts.aria,
                        fontWeight = FontWeight.Bold,
                    )
                    items.forEach { line ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(line, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Text(
                "این بخش در نسخهٔ بعدی فعال می‌شود. اگر بخواهی ترتیب و محتوایش را عوض کنی، بگو تا همان را بسازم.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** جعبه‌ابزار عمومی — کارهای روزمره و ابزارهای پرکاربرد. */
@Composable
fun GeneralToolkitScreen(onBack: () -> Unit) = ToolSoonScreen(
    emoji = "🧰",
    title = "جعبه‌ابزار عمومی",
    subtitle = "یک‌جا: ماشین‌حساب، تقویم، تبدیل واحد، درصد و تخفیف، و ابزارهای روزمره.",
    items = listOf(
        "ماشین‌حساب علمی و ماشین‌حساب درصد/تخفیف",
        "تبدیل واحد (طول، وزن، حجم، دما، سرعت)",
        "تاریخ شمسی ↔ میلادی و شمارش روزها تا امتحان",
        "یادداشت سریع و چک‌لیست روزانه",
        "ابزار متن: شمارش کلمه، تغییر حروف، مرتب‌کردن فهرست",
    ),
    onBack = onBack,
)

/** آزمایشگاه شیمی — شبیه‌سازی‌های سادهٔ شیمی پایه نهم. */
@Composable
fun ChemistryLabScreen(onBack: () -> Unit) = ToolSoonScreen(
    emoji = "⚗️",
    title = "آزمایشگاه شیمی",
    subtitle = "آزمایش‌های درسِ شیمی علوم نهم، قدم‌به‌قدم و بی‌خطر — با توضیح و پرسش.",
    items = listOf(
        "جدول تناوبی تعاملی با اطلاعات هر عنصر",
        "ساخت اتم و مولکول (شبیه‌سازی ساده)",
        "آزمایش‌های کتاب: اسید و باز، شناساگر، رسانایی محلول‌ها",
        "مقایسهٔ خواص مواد و جدول حل‌شدهٔ تمرین‌های کتاب",
        "آزمون کوتاه پایان هر مبحث با پاسخ تشریحی",
    ),
    onBack = onBack,
)

/** آزمایشگاه فیزیک — شبیه‌سازی‌های سادهٔ فیزیک پایه نهم. */
@Composable
fun PhysicsLabScreen(onBack: () -> Unit) = ToolSoonScreen(
    emoji = "🔬",
    title = "آزمایشگاه فیزیک",
    subtitle = "نیرو، فشار، حرکت و الکتریسیته را با شبیه‌سازیِ ساده ببین و اندازه بگیر.",
    items = listOf(
        "اندازه‌گیری نیرو و فشار با مثال‌های کتاب",
        "حرکت: سرعت، شتاب و نمودار مکان-زمان",
        "مدار الکتریکی ساده و قانون اهم",
        "بازتاب و شکست نور با شکل و آزمایش",
        "حلِ تمرین‌های کتاب علوم نهم با راهنمای گام‌به‌گام",
    ),
    onBack = onBack,
)

/** جعبه‌ابزار ریاضی — محاسبهٔ سریع به‌همراه یادآوری مفاهیم ریاضی نهم. */
@Composable
fun MathToolkitScreen(onBack: () -> Unit) = ToolSoonScreen(
    emoji = "🧮",
    title = "جعبه‌ابزار ریاضی",
    subtitle = "ابزارِ حل‌کننده برای تمرین‌های ریاضی نهم — همراه با یادآوریِ قاعده و فرمول.",
    items = listOf(
        "حل معادلهٔ درجه‌اول و دوم با نمایش مرحله‌ها",
        "ساده‌کردن عبارت‌های جبری و اتحادها",
        "هندسه: مساحت و حجم شکل‌ها + قضیهٔ فیثاغورس",
        "ب.م.م و ک.م.م، اعداد اول و تجزیه",
        "ماشین‌حساب کسرها و اعداد اعشاری + نمودار تابع",
    ),
    onBack = onBack,
)
