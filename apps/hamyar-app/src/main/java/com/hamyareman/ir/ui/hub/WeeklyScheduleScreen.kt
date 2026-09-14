package com.hamyareman.ir.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import org.json.JSONArray
import org.json.JSONObject

private val Days = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
private val Shifts = listOf("صبح", "عصر", "شب")

/**
 * برنامه‌ی هفتگی با چرخش شیفت:
 * - شیفت کلی هفته (صبح/عصر/شب) انتخاب می‌شود (برای مدارس شیفت‌دار/کلاس‌های غروب).
 * - برای هر روز، درس‌ها به‌صورت خط با کاما ثبت می‌شود.
 * همه‌چیز فقط همین‌جا (LocalStore) ذخیره می‌شود.
 */
@Composable
fun WeeklyScheduleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore(context, "hamyar_school") }
    var shift by remember { mutableStateOf(store.getString("week_shift", "صبح")) }
    var editingDay by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }

    fun lessonsOf(day: String): List<String> = runCatching {
        val arr = JSONArray(store.getString("week_$day", "[]"))
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    fun saveLessons(day: String, lessons: List<String>) {
        val arr = JSONArray(); lessons.forEach { arr.put(it) }
        store.putString("week_$day", arr.toString())
    }

    HubBody {
        HubHeader("برنامه‌ی هفتگی 🗓", "شیفت این هفته + درس‌های هر روز", onBack)

        Text("شیفت این هفته", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shifts.forEach { s ->
                FilterChip(
                    selected = shift == s,
                    onClick = { shift = s; store.putString("week_shift", s) },
                    label = { Text(s) },
                )
            }
        }
        Text(
            "هر هفته که شیفت عوض شد، فقط همین‌جا یک لمس کافی است؛ یادآورها و روتین‌ها با همین انتخاب هماهنگ می‌شوند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // فهرست کشویی: دروس هر ۱۳ کتاب + «ورزش» — ورزش فقط همین‌جا ثبت می‌شود.
        val subjectItems = remember {
            BookModuleRegistry.modules.filter { com.hamyareman.ir.ui.profile.GradeGate.canSeeBook(it.bookCode) }.flatMap { m -> m.packs.map { p -> p.title } } + "ورزش 🏃‍♀️"
        }
        var menuOpen by remember { mutableStateOf(false) }
        Text("درس‌های روزها", style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { menuOpen = true }) { Text("➕ انتخاب از فهرست (دروس + ورزش)") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                subjectItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            if (editingDay == null) editingDay = Days.first()
                            if (draft.isBlank()) draft = item else draft = "$draft، $item"
                            menuOpen = false
                        },
                    )
                }
            }
        }
        Days.forEach { day ->
            val lessons = lessonsOf(day)
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("📍 $day", style = MaterialTheme.typography.titleMedium)
                    if (lessons.isNotEmpty()) {
                        Text(lessons.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("— هنوز خالی —", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (editingDay == day) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = { Text("درس‌ها را با کاما جدا کن") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                saveLessons(day, draft.split("،", ",").map { it.trim() }.filter { it.isNotBlank() })
                                editingDay = null; draft = ""
                            }) { Text("ذخیره") }
                            OutlinedButton(onClick = { editingDay = null; draft = "" }) { Text("بی‌خیال") }
                        }
                    } else {
                        OutlinedButton(onClick = { editingDay = day; draft = lessons.joinToString(", ") }) { Text("ویرایش") }
                    }
                }
            }
        }
    }
}
