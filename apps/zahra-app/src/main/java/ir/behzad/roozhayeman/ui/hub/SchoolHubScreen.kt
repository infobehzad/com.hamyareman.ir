package ir.behzad.roozhayeman.ui.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.behzad.roozhayeman.ui.navigation.Screen

/**
 * هاب «مدرسه» — تمام دنیای آموزشی به‌صورت درختی (دسته‌بندی در تو در تو):
 * کتاب‌ها و دروس، برنامه‌ی هفتگی شیفت‌دار، آزمون‌ها، پلیر دروس،
 * کتابخانه و کتاب صوتی، جزوه (PDF)، آموزش هوش مصنوعی و پیشرفت.
 */
@Composable
fun SchoolHubScreen(nav: NavController) {
    HubBody {
        HubHeader("مدرسه 🎒", "همه‌چیزِ درس و یادگیری، مرتب و دسته‌بندی‌شده")

        MenuGroup("📚 کتاب‌ها و دروس", "درس‌های امروز و مسیر یادگیری") {
            HubCard("📖", "درس‌های من", "کتاب‌های پایه نهم، درس به درس با بازی و پیشرفت") { nav.hubTo(Screen.Learning.route) }
            HubCard("✏️", "مطالعه", "تمرکز، روتین مطالعه و ابزارهای درس") { nav.hubTo(Screen.StudyHome.route) }
            HubCard("🎯", "مطالعه‌ی عمیق — درس ۱: معرفی مجموعه", "فلش‌کارت، آزمون و حل تشریحی — ریاضی نهم فصل ۱") { nav.hubTo(Screen.LessonStudy.of("C905_E01-L01")) }
            HubCard("🎯", "مطالعه‌ی عمیق — درس ۲: مجموعه‌های برابر و نمایش", "زیرمجموعه، قاعده‌ی 2ⁿ، مجموعه‌های N و W و Z و Q") { nav.hubTo(Screen.LessonStudy.of("C905_E01-L02")) }
            HubCard("🎯", "مطالعه‌ی عمیق — درس ۳: اجتماع، اشتراک و تفاضل", "نمودار ون، فرمول n(A∪B) و مسائل کلامی") { nav.hubTo(Screen.LessonStudy.of("C905_E01-L03")) }
            HubCard("🎯", "مطالعه‌ی عمیق — درس ۴: مجموعه‌ها و احتمال", "فضای نمونه، P(A) = n(A)÷n(S) و دو تاس") { nav.hubTo(Screen.LessonStudy.of("C905_E01-L04")) }
            HubCard("🧪", "آزمون فصل ۱ — مجموعه‌ها", "آزمون بازه‌ای از ۴ درس با پیگیری ضعف‌ها") { nav.hubTo(Screen.LessonStudy.of("C905_E01-EXAM")) }

        MenuGroup("📚 مطالعه‌ی عمیق — درس اول همه‌ی کتاب‌ها", "۱۳ کتاب پایه نهم؛ هر درس با سکشن، فلش‌کارت، آزمون و حل تشریحی") {
            HubCard("🕌", "آموزش قرآن — این جهان راه است", "آیه‌ی ۱۰ زخرف، تدبر و روخوانی آیات") { nav.hubTo(Screen.LessonStudy.of("C901_L01")) }
            HubCard("📿", "تعلیمات اسلامی — تو را چگونه بشناسم؟", "شناخت صفات خدا، خطبه‌ی ۹۱، حمد و تسبیح") { nav.hubTo(Screen.LessonStudy.of("C902_E01-L01")) }
            HubCard("✒️", "فارسی — آفرینش همه تنبیه خداوند دل است", "قصیده‌ی سعدی، دانش ادبی و حکایت سفر") { nav.hubTo(Screen.LessonStudy.of("C903_E01-L01")) }
            HubCard("📝", "نگارش — نظام ذهنی پرورده و بند", "ساختار سه‌بخشی نوشته و نوشتن بند") { nav.hubTo(Screen.LessonStudy.of("C904_L01")) }
            HubCard("🔬", "علوم — مواد و نقش آنها در زندگی", "مواد خالص و مخلوط، فلزها، بسپارها") { nav.hubTo(Screen.LessonStudy.of("C906_E01-L01")) }
            HubCard("🌍", "مطالعات — زمین، مهد زیبای انسان‌ها", "مختصات جغرافیایی، مدارها و نصف‌النهارها") { nav.hubTo(Screen.LessonStudy.of("C907_E01-L01")) }
            HubCard("🎨", "فرهنگ و هنر — فضا و عمق", "پرسپکتیو، نقطه‌ی گریز و ترسیم حجم") { nav.hubTo(Screen.LessonStudy.of("C908_E01-L01")) }
            HubCard("📖", "عربی — مراجعة دروس الصف السابع والثامن", "واژه‌نامه‌ی ۱۸ کلمه و مرور قواعد") { nav.hubTo(Screen.LessonStudy.of("C909_L01")) }
            HubCard("🔤", "انگلیسی — Personality", "مکالمه، Practiceها، Language Melody و گرامر") { nav.hubTo(Screen.LessonStudy.of("C910_L01")) }
            HubCard("📓", "کتاب کار انگلیسی — Personality", "تمرین‌های to be و There is/are با پاسخ") { nav.hubTo(Screen.LessonStudy.of("C911_L01")) }
            HubCard("🛠", "کار و فناوری — الگوریتم", "حل مسئله، الگوریتم و روندنما") { nav.hubTo(Screen.LessonStudy.of("C917_E01-L01")) }
            HubCard("🧭", "تفکر و سبک زندگی — شروع یک ماجرا", "داستان یوسف (ع) و درمان حسادت") { nav.hubTo(Screen.LessonStudy.of("C941_L01")) }
        }
        }

        MenuGroup("🗓 برنامه‌ی هفتگی", "چرخش شیفت و درس‌های هر روز") {
            HubCard("⏰", "برنامه‌ی هفتگی من", "شیفت صبح/عصر/شب + جدول درس هر روز") { nav.hubTo(Screen.WeeklySchedule.route) }
            HubCard("🏫", "برنامه‌ی مدرسه", "زنگ‌ها و برنامه‌ی کلاسی") { nav.hubTo(Screen.School.route) }
        }

        MenuGroup("📝 آزمون و بازخورد", "سنجش خودت و دیدن نتیجه") {
            HubCard("🧪", "جزوه‌ها و آپلود PDF", "جزوه‌ی معلم را بده تا سوال ساخته شود") { nav.hubTo(Screen.Pdf.route) }
            HubCard("📈", "نمودار پیشرفت", "رشدت را ببین — بدون قضاوت") { nav.hubTo(Screen.Charts.route) }
        }

        MenuGroup("🎧 پلیرها و کتاب", "گوش دادن به درس و کتاب") {
            HubCard("📚", "کتابخانه", "کتاب‌های دیجیتال من") { nav.hubTo(Screen.Library.route) }
            HubCard("🔊", "کتاب صوتی (پلیر دروس)", "پخش‌کننده‌ی صوتی درس‌ها") { nav.hubTo(Screen.Audiobook.route) }
        }

        MenuGroup("🤖 آموزش هوش مصنوعی", "مسیر پیش‌نیازدار و ارزیابی") {
            HubCard("✨", "یادگیری با AI", "از پایه تا پروژه، درسِ روزانه‌شده") { nav.hubTo(Screen.AiLearning.route) }
        }
    }
}

/** گروه تاشو — دسته‌بندی در تو در تو. */
@Composable
private fun MenuGroup(title: String, subtitle: String, content: @Composable () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable { open = !open }) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = if (open) "بستن" else "بازکردن")
            }
            AnimatedVisibility(visible = open) {
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            }
        }
    }
}
