package com.hamyareman.ir.ui.study

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.hamyareman.ir.ui.hub.HubBody
import com.hamyareman.ir.ui.hub.HubMenuGroup
import com.hamyareman.ir.ui.hub.HubCard
import com.hamyareman.ir.ui.hub.HubCatalog
import com.hamyareman.ir.ui.hub.HubHeader
import com.hamyareman.ir.ui.hub.hubTo

/**
 * هاب «آموزشگاه» — آیتم‌ها از [HubCatalog] می‌آیند تا هر پایه جدا تنظیم شود.
 */
@Composable
fun AcademyHubScreen(nav: NavController) {
    val gender = HubCatalog.gender()
    HubBody {
        HubHeader("آموزشگاه ✨", "آموزش هوش مصنوعی و کلاس‌های مهارتی")
        HubCatalog.academy().forEach { group ->
            HubMenuGroup(group.title, group.subtitle) {
                group.items.forEach { item ->
                    HubCard(item.emojiFor(gender), item.title, item.subtitle) { nav.hubTo(item.route) }
                }
            }
        }
    }
}
