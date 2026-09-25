package com.hamyareman.ir.ui.wellness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.ui.hub.HubBody
import com.hamyareman.ir.ui.hub.HubCoverGrid
import com.hamyareman.ir.ui.hub.HubCoverTile
import com.hamyareman.ir.ui.hub.HubHeader
import com.hamyareman.ir.ui.hub.hubTo
import com.hamyareman.ir.ui.navigation.Screen

/** هاب ریشه‌ی یک شاخه — جلدهای مربعی ۲ در هر ردیف، مثل کتاب‌ها. */
@Composable
fun PracticeHubScreen(
    nav: NavController,
    rootIds: List<String>,
    title: String,
    subtitle: String,
    headerSlot: String,
    accKey: String = "",
    onBack: (() -> Unit)? = null,
    extraTop: @Composable () -> Unit = {},
) {
    val groups = remember(rootIds) { WellnessMenu.groupsOf(rootIds) }
    HubBody {
        HubHeader(title, subtitle, onBack, slotId = headerSlot)
        extraTop()
        HubCoverGrid(
            tiles = groups.map { g ->
                HubCoverTile(
                    id = g.id,
                    title = g.title,
                    subtitle = g.subtitle,
                    onClick = { nav.hubTo(Screen.PracticeGroup.of(g.id)) },
                )
            },
        )
    }
}

@Composable
fun PracticeGroupScreen(nav: NavController, groupId: String, onBack: () -> Unit) {
    val group = WellnessMenu.group(groupId)
    HubBody {
        HubHeader(
            title = group?.title ?: "تمرین",
            subtitle = group?.subtitle ?: "",
            onBack = onBack,
            slotId = "hub.practice.header",
        )
        if (group == null) {
            Text("این بخش پیدا نشد.", style = AppTypography.pageBody.style)
            return@HubBody
        }
        val tiles = buildList {
            group.items.forEach { item ->
                add(
                    HubCoverTile(
                        id = item.id,
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = { openPractice(nav, item) },
                    ),
                )
            }
            group.childGroupIds.forEach { cid ->
                val child = WellnessMenu.group(cid) ?: return@forEach
                add(
                    HubCoverTile(
                        id = child.id,
                        title = child.title,
                        subtitle = child.subtitle,
                        onClick = { nav.hubTo(Screen.PracticeGroup.of(child.id)) },
                    ),
                )
            }
        }
        HubCoverGrid(tiles)
    }
}

@Composable
fun PracticeItemScreen(
    itemId: String,
    onBack: () -> Unit,
    onWellness: (String) -> Unit,
    onRoute: (String) -> Unit,
) {
    val item = WellnessMenu.item(itemId)
    val move = item?.wellnessSlug?.takeIf { it.isNotBlank() }?.let { WellnessCatalog.bySlug(it) }
    val (body, steps) = if (item != null) WellnessMenu.resolveInstructions(item) else "" to emptyList()

    Column(Modifier.fillMaxSize()) {
        com.hamyareman.ir.platform.core.designsystem.AppTopBar(
            item?.title ?: "تمرین",
            onBack,
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (item == null) {
                Text("این تمرین پیدا نشد.", style = AppTypography.pageBody.style)
                return@Column
            }
            Text(item.subtitle, style = AppTypography.pageHeading.style)
            if (item.minutes.isNotBlank()) {
                Text(
                    item.minutes,
                    style = AppTypography.pageBody.style,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (body.isNotBlank()) {
                Text(body, style = AppTypography.pageBody.style)
            }
            steps.forEachIndexed { i, s ->
                Text(
                    toPersianDigits((i + 1).toString()) + ". " + s,
                    style = AppTypography.pageBody.style,
                )
            }
            if (move != null) {
                Spacer(Modifier.height(8.dp))
                PrimaryButton("شروع با راهنمای حرکات سلامتی") { onWellness(move.category.wire) }
            }
            if (item.route.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                PrimaryButton("باز کردن صفحه‌ی مرتبط") { onRoute(item.route) }
            }
        }
    }
}

@Composable
fun BetweenLessonsHubScreen(nav: NavController, onBack: () -> Unit) {
    PracticeHubScreen(
        nav = nav,
        rootIds = WellnessMenu.betweenIds,
        title = "${WellnessMenu.BETWEEN_ICON} تمرینات بین دروس",
        subtitle = "رفع خستگی، ۲ تا ۵ دقیقه — بعد برگرد سر درس",
        headerSlot = "hub.between.header",
        accKey = "acc_between",
        onBack = onBack,
    )
}

internal fun openPractice(nav: NavController, item: PracticeItem) {
    when {
        item.childGroupId.isNotBlank() -> nav.hubTo(Screen.PracticeGroup.of(item.childGroupId))
        item.route.isNotBlank() && item.steps.isEmpty() && item.body.isBlank() && item.wellnessSlug.isBlank() ->
            nav.hubTo(item.route)
        item.wellnessSlug.isNotBlank() && item.steps.isEmpty() && item.body.isBlank() ->
            nav.hubTo(Screen.PracticeItem.of(item.id))
        item.steps.isNotEmpty() || item.body.isNotBlank() ->
            nav.hubTo(Screen.PracticeItem.of(item.id))
        item.route.isNotBlank() -> nav.hubTo(item.route)
        else -> nav.hubTo(Screen.PracticeItem.of(item.id))
    }
}
