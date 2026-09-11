package ir.behzad.roozhayeman.ui.appearance

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.behzad.platform.core.designsystem.SectionCard
import kotlinx.coroutines.launch

/** صفحه‌ی «ظاهر و فونت»: انتخاب تم، حالت روشن/تاریک، فونت دانلودی. */
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val prefs = LocalUiPrefs.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ظاهر و فونت", style = MaterialTheme.typography.headlineMedium)
        Text("تم، حالت رنگ و فونت اپ را خودت انتخاب کن ✨", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // ---------- تم‌ها ----------
        Text("تم رنگی", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ir.behzad.platform.core.designsystem.BrandTheme.entries
                .filter { it != ir.behzad.platform.core.designsystem.BrandTheme.CalmFather }
                .forEach { brand ->
                    val selected = prefs.theme == brand
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth().clickable { prefs.setTheme(brand) },
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ThemeSwatches(brand)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(brand.label, style = MaterialTheme.typography.titleMedium)
                                if (selected) Text("انتخاب‌شده ✓", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
        }

        // ---------- روشن/تاریک ----------
        Text("حالت رنگ", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "سیستم", "light" to "روشن", "dark" to "تاریک").forEach { (key, label) ->
                FilterChip(
                    selected = prefs.darkMode == key,
                    onClick = { prefs.setDarkMode(key) },
                    label = { Text(label) },
                )
            }
        }

        // ---------- فونت‌ها ----------
        Text("فونت", style = MaterialTheme.typography.titleMedium)
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FontRow(
                    title = "فونت سیستم (پیش‌فرض)",
                    downloaded = true,
                    selected = prefs.fontKey.isBlank(),
                    onSelect = { prefs.setFontKey("") },
                )
                FontLibrary.catalog.forEach { entry ->
                    val downloaded = FontLibrary.isDownloaded(context, entry.key)
                    FontRow(
                        title = entry.title + if (!downloaded) "  ⬇ دانلود از اینترنت" else "",
                        downloaded = downloaded,
                        selected = prefs.fontKey == entry.key,
                        onSelect = {
                            if (downloaded) {
                                prefs.setFontKey(entry.key)
                            } else {
                                busy = entry.key
                                scope.launch {
                                    val r = FontLibrary.download(context, entry)
                                    busy = null
                                    if (r.isSuccess) prefs.setFontKey(entry.key)
                                }
                            }
                        },
                    )
                }
                busy?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("در حال دانلود…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    "فونت‌ها فقط یک‌بار دانلود و برای همیشه روی گوشی ذخیره می‌شوند؛ دفعه‌ی بعد بدون اینترنت هم کار می‌کنند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FontRow(title: String, downloaded: Boolean, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
    }
}

@Composable
private fun ThemeSwatches(brand: ir.behzad.platform.core.designsystem.BrandTheme) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        swatchColors(brand).forEach { c ->
            Box(Modifier.size(22.dp).background(c, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

private fun swatchColors(brand: ir.behzad.platform.core.designsystem.BrandTheme): List<Color> = when (brand) {
    ir.behzad.platform.core.designsystem.BrandTheme.DollStage -> listOf(Color(0xFFE96A8D), Color(0xFFF4916B), Color(0xFFA77BD4))
    ir.behzad.platform.core.designsystem.BrandTheme.Stitch -> listOf(Color(0xFF2F7FD6), Color(0xFFF26FA7), Color(0xFF27B5A8))
    ir.behzad.platform.core.designsystem.BrandTheme.MoonNight -> listOf(Color(0xFF7C6BD9), Color(0xFFE8B84B), Color(0xFFC97BA8))
    ir.behzad.platform.core.designsystem.BrandTheme.Mint -> listOf(Color(0xFF2FA37A), Color(0xFF8FBF3C), Color(0xFF4FA8D9))
    ir.behzad.platform.core.designsystem.BrandTheme.CalmFather -> listOf(Color(0xFF5F8A78), Color(0xFFC39A6A), Color(0xFF9C8FC9))
}
