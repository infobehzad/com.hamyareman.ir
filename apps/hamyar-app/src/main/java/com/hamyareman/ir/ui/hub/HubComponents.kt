package com.hamyareman.ir.ui.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.ui.appearance.FontCatalog

/** سربرگ مشترک صفحات هاب با دکمه‌ی بازگشت. */
@Composable
fun HubHeader(title: String, subtitle: String, onBack: (() -> Unit)? = null, slotId: String = FontCatalog.ROLE_HEADING) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
        }
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontFamily = AppTypography.title.family, fontWeight = AppTypography.title.weight, fontSize = AppTypography.title.size)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = AppTypography.titleSub.family, fontWeight = AppTypography.titleSub.weight, fontSize = AppTypography.titleSub.size)
        }
    }
}

/** آیتم منوی هاب — کارت قابل‌کلیک با ایموجی و توضیح. */
@Composable
fun HubCard(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    slotId: String = FontCatalog.ROLE_TILE,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = AppTypography.cardTitle.family, fontWeight = AppTypography.cardTitle.weight, fontSize = AppTypography.cardTitle.size)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = AppTypography.cardSub.family, fontWeight = AppTypography.cardSub.weight, fontSize = AppTypography.cardSub.size)
            }
        }
    }
}

/** بدنه‌ی اسکرول‌شونده‌ی استاندارد هاب‌ها. */
@Composable
fun HubBody(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/** ناوبری استاندارد به یک مسیر از داخل هاب. */
fun NavController.hubTo(route: String) {
    navigate(route) { launchSingleTop = true }
}

/** گروه تاشوی منوی هاب — دسته‌بندی در تو در تو (مشترک بین مدرسه/آموزشگاه/سلامتی). */
@Composable
/**
 * گروه منوی هاب — نسخه‌ی مستقل (حافظه‌دارِ خودش) یا کنترل‌شده برای آکاردئون:
 * اگر open/onToggle داده شود، وضعیتش را والد نگه می‌دارد («یکی باز شد، اونیکی بسته»).
 */
fun HubMenuGroup(
    title: String,
    subtitle: String,
    open: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    slotId: String = FontCatalog.ROLE_HEADING,
    content: @Composable () -> Unit,
) {
    var selfOpen by rememberSaveable { mutableStateOf(false) }
    val isOpen = open ?: selfOpen
    Card(modifier = Modifier.fillMaxWidth().clickable {
        if (onToggle != null) onToggle() else selfOpen = !selfOpen
    }) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = AppTypography.accordionTitle.family, fontWeight = AppTypography.accordionTitle.weight, fontSize = AppTypography.accordionTitle.size)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = AppTypography.accordionSub.family, fontWeight = AppTypography.accordionSub.weight, fontSize = AppTypography.accordionSub.size)
                }
                Icon(if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = if (isOpen) "بستن" else "بازکردن")
            }
            AnimatedVisibility(visible = isOpen) {
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            }
        }
    }
}
