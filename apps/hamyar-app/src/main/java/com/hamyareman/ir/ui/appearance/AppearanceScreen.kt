package com.hamyareman.ir.ui.appearance

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.platform.core.designsystem.BrandTheme
import com.hamyareman.ir.platform.core.designsystem.ThemeGender
import com.hamyareman.ir.platform.core.designsystem.themeGender
import com.hamyareman.ir.platform.core.designsystem.swatches
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import java.io.File

/** صفحه‌ی «ظاهر و فونت»: تم رنگ، اندازهٔ سراسری، پنج نقش فونت، ۵ اسلات بکاپ. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val prefs = LocalUiPrefs.current
    val context = LocalContext.current
    var pendingTheme by remember { mutableStateOf<BrandTheme?>(null) }
    var confirmClear by remember { mutableStateOf<Int?>(null) }

    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val txt = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull()
        val parsed = txt?.let { FontTheme.parse(it) }
        if (parsed == null) {
            Toast.makeText(context, "فایل JSON معتبر نبود", Toast.LENGTH_SHORT).show()
        } else {
            prefs.updateFontTheme(parsed)
            Toast.makeText(context, "تم «${parsed.name}» بارگذاری شد", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("ظاهر و فونت", onBack)
        Column(
            Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("تم، اندازهٔ نوشته و فونت را خودت انتخاب کن ✨", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("اندازهٔ نوشتهٔ سراسری", style = MaterialTheme.typography.titleMedium)
            Text(
                if (prefs.textSizeOffset == 0) "استاندارد (۱۳)"
                else {
                    val n = 13 + prefs.textSizeOffset
                    val sign = if (prefs.textSizeOffset > 0) "+" else ""
                    "پایه ۱۳  ·  ${toPersianDigits(sign + prefs.textSizeOffset.toString())}  ·  ${toPersianDigits(n.toString())}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = prefs.textSizeOffset.toFloat(),
                onValueChange = { prefs.updateTextSizeOffset(it.toInt()) },
                valueRange = -6f..6f,
                steps = 11,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("نمونه: این جمله با اندازهٔ انتخابی دیده می‌شود.", style = MaterialTheme.typography.bodyLarge)

            Text("حالت رنگ", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "سیستم", "light" to "روشن", "dark" to "تاریک").forEach { (key, label) ->
                    FilterChip(
                        selected = prefs.darkMode == key,
                        onClick = { prefs.updateDarkMode(key) },
                        label = { Text(label) },
                    )
                }
            }

            Text("تم‌های دخترانه", style = MaterialTheme.typography.titleMedium)
            ThemeGroup(BrandTheme.entries.filter { it.themeGender == ThemeGender.GIRL }, prefs) { pendingTheme = it }
            Text("تم‌های پسرانه", style = MaterialTheme.typography.titleMedium)
            ThemeGroup(BrandTheme.entries.filter { it.themeGender == ThemeGender.BOY }, prefs) { pendingTheme = it }

            pendingTheme?.let { brand ->
                AlertDialog(
                    onDismissRequest = { pendingTheme = null },
                    title = { Text("پیش‌نمایش تم «${brand.label}»") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                brand.swatches().forEach { c ->
                                    Box(Modifier.size(36.dp).background(c, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                                }
                            }
                            Text("اگر تأیید کنی، تم همین حالا روی کل اپ اعمال می‌شود.")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { prefs.updateTheme(brand); pendingTheme = null }) { Text("اعمال تم") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingTheme = null }) { Text("انصراف") }
                    },
                )
            }

            Text("فونت هر بخش", style = MaterialTheme.typography.titleMedium)
            Text(
                "پنج نقش جدا: خوش‌آمد، ساعت، عنوان، کاشی، متن. فونت‌ها داخل اپ‌اند و اینترنت نمی‌خواهند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FontTheme.ROLES.forEach { (role, title) ->
                RoleFontCard(
                    title = title,
                    sample = FontTheme.SAMPLES[role].orEmpty(),
                    fontKey = prefs.fontTheme.fontOf(role),
                    size = prefs.fontTheme.sizeOf(role),
                    onFont = { prefs.updateRoleFont(role, it) },
                    onSize = { prefs.updateRoleSize(role, it) },
                )
            }

            Text("نام تم فونت و بکاپ", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = prefs.fontTheme.name,
                onValueChange = { prefs.updateThemeName(it) },
                label = { Text("نام تم فونت") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        runCatching { shareFontTheme(context, prefs.fontTheme) }
                            .onFailure {
                                Toast.makeText(context, "بکاپ ساخته نشد", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("بکاپ JSON") }
                OutlinedButton(
                    onClick = { importJson.launch("*/*") },
                    modifier = Modifier.weight(1f),
                ) { Text("بارگذاری JSON") }
            }
            Text(
                "تا ۵ تنظیم ذخیره می‌شود. خالی را بزن تا ذخیره شود؛ پر را بزن تا بار شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                prefs.fontSlots.forEachIndexed { i, slot ->
                    SlotCard(
                        index = i,
                        theme = slot,
                        selected = slot != null && slot == prefs.fontTheme,
                        onSave = { prefs.saveSlot(i) },
                        onLoad = { prefs.loadSlot(i) },
                        onClear = { confirmClear = i },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            confirmClear?.let { idx ->
                AlertDialog(
                    onDismissRequest = { confirmClear = null },
                    title = { Text("حذف اسلات ${toPersianDigits((idx + 1).toString())}؟") },
                    text = { Text("تنظیم ذخیره‌شده پاک می‌شود.") },
                    confirmButton = {
                        TextButton(onClick = { prefs.clearSlot(idx); confirmClear = null }) { Text("حذف") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmClear = null }) { Text("انصراف") }
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeGroup(brands: List<BrandTheme>, prefs: UiPrefs, onPreview: (BrandTheme) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        brands.forEach { brand ->
            val selected = prefs.theme == brand
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth().clickable { onPreview(brand) },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        brand.swatches().forEach { c ->
                            Box(Modifier.size(22.dp).background(c, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(brand.label, style = MaterialTheme.typography.titleMedium)
                        if (selected) Text("انتخاب‌شده ✓", style = AppTypography.caption, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleFontCard(
    title: String,
    sample: String,
    fontKey: String,
    size: Int,
    onFont: (String) -> Unit,
    onSize: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val face = remember(fontKey) { EmbeddedFonts.face(fontKey) }
    val family = remember(fontKey) { EmbeddedFonts.family(fontKey) }
    val sign = if (size > 0) "+" else ""
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                sample,
                fontFamily = family,
                fontSize = (16 + size).coerceIn(8, 40).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
                OutlinedTextField(
                    value = face.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("فونت") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    EmbeddedFonts.catalog.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Text(item.label, fontFamily = EmbeddedFonts.family(item.key))
                            },
                            onClick = { onFont(item.key); open = false },
                        )
                    }
                }
            }
            Text(
                "سایز ${toPersianDigits(sign + size.toString())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = size.toFloat(),
                onValueChange = { onSize(it.toInt()) },
                valueRange = -20f..20f,
                steps = 39,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SlotCard(
    index: Int,
    theme: FontTheme?,
    selected: Boolean,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        theme != null -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable { if (theme == null) onSave() else onLoad() },
    ) {
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(toPersianDigits((index + 1).toString()), style = MaterialTheme.typography.titleMedium)
            Text(
                theme?.name ?: "خالی",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (theme != null) {
                Text(
                    "حذف",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable(onClick = onClear),
                )
            } else {
                Text("ذخیره", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun shareFontTheme(context: android.content.Context, theme: FontTheme) {
    val dir = File(context.cacheDir, "font_themes").apply { mkdirs() }
    val safe = theme.name.replace(Regex("[^\\w\\u0600-\\u06FF-]+"), "_").ifBlank { "hamyar-font" }
    val file = File(dir, "$safe.json")
    file.writeText(theme.toJson().toString(2))
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "بکاپ فونت همیار")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "بکاپ JSON فونت"))
}
