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
 * آموزش‌های هوش مصنوعی و کلاس‌های مهارتی.
 *
 * کارت‌های «مطالعه آزاد»، «پلیرها و کتاب» و «کتاب‌ها» طبقِ درخواست از این صفحه
 * برداشته شدند (مطالعه آزاد از تبِ «مدرسه» و داشبورد در دسترس است).
 */
@Composable
fun AcademyHubScreen(nav: NavController) {
    HubBody {
        HubHeader("آموزشگاه ✨", "آموزش هوش مصنوعی و کلاس‌های مهارتی")

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
