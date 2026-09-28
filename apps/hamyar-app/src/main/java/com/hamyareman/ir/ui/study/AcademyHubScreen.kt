package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.ui.hub.HubBody
import com.hamyareman.ir.ui.hub.HubCoverGrid
import com.hamyareman.ir.ui.hub.HubCoverTile
import com.hamyareman.ir.ui.hub.HubHeader
import com.hamyareman.ir.ui.hub.layerTo
import com.hamyareman.ir.ui.navigation.Screen
import com.hamyareman.ir.ui.wellness.WellnessMenu

/**
 * هاب «آموزشگاه» — مهارت‌های زندگی، یادگیری و کلاس‌های مهارتی با کاشی‌های مربعی.
 */
@Composable
fun AcademyHubScreen(nav: NavController) {
    HubBody {
        HubHeader("آموزشگاه ✨", "مهارت‌های زندگی، یادگیری و کلاس‌های مهارتی", slotId = "hub.academy.header")
        HubCoverGrid(
            WellnessMenu.groupsOf(WellnessMenu.skillsIds).map { g ->
                HubCoverTile(
                    id = g.id,
                    title = g.title,
                    subtitle = g.subtitle,
                    onClick = { nav.layerTo(Screen.PracticeGroup.of(g.id)) },
                )
            },
        )
    }
}
