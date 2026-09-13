package com.hamyareman.ir.ui.study

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.hamyareman.ir.ui.hub.HubBody
import com.hamyareman.ir.ui.hub.HubMenuGroup
import com.hamyareman.ir.ui.hub.HubCard
import com.hamyareman.ir.ui.hub.HubHeader
import com.hamyareman.ir.ui.hub.hubTo
import com.hamyareman.ir.ui.navigation.Screen

/**
 * هاب «آموزشگاه» — همه‌چیزِ غیر از دروسِ مدرسه:
 * مطالعات آزاد، پلیرها و کتاب‌ها، آموزش‌های هوش مصنوعی و متفرقه.
 */
@Composable
fun AcademyHubScreen(nav: NavController) {
    HubBody {
        HubHeader("آموزشگاه ✨", "مطالعات آزاد، پلیرها، کتاب‌ها و آموزش هوش")

        HubMenuGroup("🧠 مطالعات آزاد", "تمرکز و روتین شخصی، جدا از درس مدرسه") {
            HubCard("✏️", "مطالعه", "تمرکز، روتین مطالعه و ابزارهای درس") { nav.hubTo(Screen.StudyHome.route) }
        }

        HubMenuGroup("🎧 پلیرها و کتاب", "گوش دادن و خواندن") {
            HubCard("📚", "کتابخانه", "کتاب‌های دیجیتال من") { nav.hubTo(Screen.Library.route) }
            HubCard("🔊", "کتاب صوتی (پلیر)", "پخش‌کننده با سرعت، بوکمارک و تایمر خواب") { nav.hubTo(Screen.Audiobook.route) }
            HubCard("🧩", "گوشه‌ی مطالعه", "لحظه‌های کوتاه خواندن") { nav.hubTo(Screen.ReadingCorner.route) }
        }

        HubMenuGroup("🤖 آموزش هوش مصنوعی", "مسیر پیش‌نیازدار و ارزیابی") {
            HubCard("📖", "درس‌های من", "محتوای تعاملی یادگیری، آزمون تعیین سطح و نقشه‌ی راه") { nav.hubTo(Screen.Learning.route) }
            HubCard("✨", "یادگیری با AI", "از پایه تا پروژه، درسِ روزانه‌شده") { nav.hubTo(Screen.AiLearning.route) }
            HubCard("🗺", "مسیرهای یادگیری", "فهرست مسیرها بر اساس علاقه") { nav.hubTo(Screen.Roadmap.of("")) }
        }

        HubMenuGroup("🎨 متفرقه", "خلاقیت و آرامش") {
            HubCard("🎨", "هنر روزانه", "هر روز یک تمرین کوچک هنری") { nav.hubTo(Screen.Art.route) }
        }
    }
}
