package com.hamyareman.ir.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.MathAnswerScript
import com.hamyareman.ir.platform.feature.study.MathExamLedger
import com.hamyareman.ir.platform.feature.study.StudyPack

internal val MATH_TABS = listOf("تدریس", "مطالعه", "فلش‌کارت", "خلاصه", "آزمون")

/**
 * پنج سربرگ درس ریاضی — هر درس هر فصل جدا.
 * قفل: فقط تدریس تا اتمام صوت باز است.
 */
@Composable
fun MathLessonScreen(
    packId: String,
    initialTab: Int = 0,
    onBack: () -> Unit,
) {
    val pack = remember(packId) { BookModuleRegistry.pack(packId) }
    val bookTitle = remember(packId) {
        BookModuleRegistry.modules.firstOrNull { m -> m.packs.any { it.packId == packId } }?.title.orEmpty()
    }
    if (pack == null) {
        AppTopBar(title = "درس ریاضی", onBack = onBack)
        Text("این درس پیدا نشد.", Modifier.padding(16.dp))
        return
    }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val teachDone = remember(packId) {
        TeachStats.expectMedia(ctx, packId, expectedTeachMedia(pack))
        TeachStats.isDone(ctx, packId)
    }
    var tab by rememberSaveable(packId) {
        val want = initialTab.coerceIn(0, 4)
        mutableIntStateOf(if (!teachDone && want >= 1) 0 else want)
    }
    var lockMsg by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = pack.title, onBack = onBack)
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
            MATH_TABS.forEachIndexed { i, label ->
                val locked = i >= 1 && !teachDone
                Tab(
                    selected = tab == i,
                    onClick = {
                        if (locked) lockMsg = true else tab = i
                    },
                    text = { Text(if (locked) "🔒 $label" else label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
        if (lockMsg) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { lockMsg = false },
                confirmButton = { TextButton(onClick = { lockMsg = false }) { Text("باشه") } },
                title = { Text("اول تدریس") },
                text = { Text("مطالعه، فلش‌کارت، خلاصه و آزمون بعد از اتمام صوت تدریس باز می‌شوند.") },
            )
        }
        when (tab) {
            0 -> MathTeachTab(pack, bookTitle)
            1 -> MathStudyTab(pack)
            2 -> MathFlashTab(pack)
            3 -> MathSummaryTab(pack)
            else -> MathExamTab(pack)
        }
    }
}

@Composable
private fun MathTeachTab(pack: StudyPack, bookTitle: String) {
    val tracks = teachTracksOf(pack)
    val body = pack.teachText.ifBlank {
        pack.sections.filter { it.kind != "exam" }.joinToString("\n\n") { "«${it.title}»\n${it.body}" }
            .ifBlank { "متن تدریس این درس به‌زودی از پوشهٔ Books اضافه می‌شود." }
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (tracks.isNotEmpty()) {
            TeachAudioBar(packId = pack.packId, screenTitle = pack.title, bookTitle = bookTitle, tracks = tracks)
        }
        Card(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                Text("متن تدریس", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium)
                if (pack.teachSpeech.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "متن تبدیل به صوت (برای ساخت فایل صوتی):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(pack.teachSpeech, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MathStudyTab(pack: StudyPack) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TeachPdfPages(modifier = Modifier.weight(1f), fileId = pack.pdfFileName, pack = pack)
        MathExercisesPane(pack, Modifier.weight(1f))
    }
}

@Composable
private fun MathExercisesPane(pack: StudyPack, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val today = remember { JalaliDate.todayIso() }
    val ime = LocalSoftwareKeyboardController.current
    if (pack.exercises.isEmpty()) {
        Card(modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("تمرین‌های کتاب", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "ساختار تمرین آماده است؛ جای‌خالی‌های این درس به‌زودی از پوشهٔ Books پر می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    var idx by rememberSaveable(pack.packId) { mutableIntStateOf(0) }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var showKeys by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var lastOk by remember { mutableStateOf<Boolean?>(null) }
    val ex = pack.exercises[idx.coerceIn(0, pack.exercises.lastIndex)]
    LaunchedEffect(ex.id) { field = TextFieldValue(""); feedback = null; lastOk = null; showKeys = false; ime?.hide() }

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "تمرین کتاب ${toPersianDigits((idx + 1).toString())} از ${toPersianDigits(pack.exercises.size.toString())}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Text(ex.prompt, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = field,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(ex.id) {
                            detectTapGestures {
                                ime?.hide()
                                showKeys = true
                            }
                        },
                    label = { Text("جواب — لمس برای کیبورد نماد") },
                    singleLine = true,
                )
                if (showKeys) {
                    Spacer(Modifier.height(4.dp))
                    MathSymbolKeyboard(value = field, onValue = { field = it; lastOk = null; feedback = null })
                    TextButton(onClick = { showKeys = false }) { Text("بستن کیبورد") }
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        val ok = MathAnswerScript.grade(ex, field.text)
                        container.studyProgress.recordExercise(pack.packId, ex.id, ok, today)
                        lastOk = ok
                        val stats = container.studyProgress.exerciseStats(pack.packId).optJSONObject(ex.id)
                        val bad = stats?.optInt("bad") ?: 0
                        val good = stats?.optInt("ok") ?: 0
                        val plan = MathAnswerScript.repeatPlan(ok, if (ok) 0 else bad, if (ok) good else 0)
                        feedback = if (ok) plan.message
                        else "${plan.message}\nنکته: ${ex.hint.ifBlank { "دوباره از روی کتاب نگاه کن." }}"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("بررسی جواب") }
                if (feedback != null) {
                    Text(
                        feedback!!,
                        color = if (lastOk == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (lastOk == false) {
                        OutlinedButton(onClick = { field = TextFieldValue(""); lastOk = null; feedback = null; showKeys = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("حل دوباره")
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { idx = (idx - 1).coerceAtLeast(0) }, enabled = idx > 0, modifier = Modifier.weight(1f)) { Text("قبلی") }
            OutlinedButton(onClick = { idx = (idx + 1).coerceAtMost(pack.exercises.lastIndex) }, enabled = idx < pack.exercises.lastIndex, modifier = Modifier.weight(1f)) { Text("بعدی") }
        }
    }
}

@Composable
private fun MathFlashTab(pack: StudyPack) {
    val container = LocalAppContainer.current
    val progress = container.studyProgress
    val today = remember { JalaliDate.todayIso() }
    var refresh by remember { mutableIntStateOf(0) }
    var showArchive by remember { mutableStateOf(false) }
    val archive = remember(refresh, pack.packId) { progress.archivedCards(pack) }
    val queue = remember(refresh, pack.packId, today) { progress.dueCards(pack, today) }

    if (showArchive) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("آرشیو فلش‌کارت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("موارد درست‌پاسخ‌داده‌شده — هر وقت خواستی دوباره ببین.", style = MaterialTheme.typography.bodySmall)
            if (archive.isEmpty()) Text("هنوز کارتی در آرشیو نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            archive.forEach { c ->
                var open by remember(c.id) { mutableStateOf(false) }
                Card(Modifier.fillMaxWidth().clickable { open = !open }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(c.front, fontWeight = FontWeight.Bold)
                        if (open) {
                            Spacer(Modifier.height(6.dp))
                            Text(c.back)
                            TextButton(onClick = {
                                progress.setArchived(pack.packId, c.id, false)
                                refresh++
                            }) { Text("برگرداندن به مرور") }
                        }
                    }
                }
            }
            OutlinedButton(onClick = { showArchive = false }, modifier = Modifier.fillMaxWidth()) { Text("بازگشت به مرور") }
        }
        return
    }

    val card = queue.firstOrNull()
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("امروز: ${toPersianDigits(queue.size.toString())} کارت", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showArchive = true }) {
                Text("آرشیو (${toPersianDigits(archive.size.toString())})")
            }
        }
        if (pack.flashcards.isEmpty()) {
            Text("فلش‌کارت این درس به‌زودی از پوشهٔ Books اضافه می‌شود.", style = MaterialTheme.typography.titleSmall)
            return
        }
        if (card == null) {
            Text("✓ کارت سررسیدی نمانده.", style = MaterialTheme.typography.titleMedium)
            Text("از آرشیو می‌توانی دوباره ببینی.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        var flipped by remember(card.id, refresh) { mutableStateOf(false) }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth().height(220.dp).clickable { flipped = !flipped }) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                if (card.topic.isNotBlank()) Text(card.topic, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(if (flipped) card.back else card.front, style = MaterialTheme.typography.titleMedium, fontWeight = if (flipped) FontWeight.Normal else FontWeight.Bold)
                if (!flipped) Text("لمس برای پاسخ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (!flipped) {
            Button(onClick = { flipped = true }, modifier = Modifier.fillMaxWidth()) { Text("نمایش پاسخ") }
        } else {
            Text("چقدر خوب یاد داشتی؟", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            val btns = listOf("بلد نبودم" to 1, "سخت بود" to 3, "خوب" to 4, "عالی" to 5)
            btns.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, q) ->
                        OutlinedButton(
                            onClick = {
                                progress.reviewCardMath(pack.packId, card.id, q, today)
                                refresh++
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MathSummaryTab(pack: StudyPack) {
    val summary = pack.summary.ifBlank {
        pack.sections.filter { it.kind == "exam" }.lastOrNull()?.body
            ?: "خلاصه‌ی چندسطری این درس به‌زودی از پوشهٔ Books نوشته می‌شود."
    }
    val tips = pack.examTips.ifBlank {
        pack.sections.filter { it.kind == "exam" }.joinToString("\n\n") { it.body }
            .ifBlank { "نکات امتحانی به‌زودی اضافه می‌شود." }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("خلاصه درس", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("نکات امتحانی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(tips, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MathExamTab(pack: StudyPack) {
    val container = LocalAppContainer.current
    val today = remember { JalaliDate.todayIso() }
    val mcq = remember(pack.packId) { MathExamLedger.mcqOf(pack) }
    var tick by remember { mutableIntStateOf(0) }
    val ledger = remember(pack.packId, tick) { container.studyProgress.examState(pack.packId) }
    var answers by remember(pack.packId) { mutableStateOf(mapOf<String, String>()) }
    var done by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("نمونه سوال چهارگزینه‌ای", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (mcq.isEmpty()) {
            Text(
                "ساختار آزمون آماده است؛ سوال‌ها و کلید به‌زودی از پوشهٔ Books می‌آیند. تصحیح، ثبت نتیجه و جایگزینی در نمودار از همین‌جا کار می‌کند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ledger.repeatCount > 0) {
                ExamLedgerCard(ledger)
            }
            return
        }
        if (done) {
            ExamLedgerCard(ledger)
            Button(onClick = { answers = emptyMap(); done = false }, modifier = Modifier.fillMaxWidth()) {
                Text("تجدید آزمون (نتیجهٔ نمودار جایگزین می‌شود)")
            }
            return
        }
        mcq.forEachIndexed { i, q ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${toPersianDigits((i + 1).toString())}. ${q.text}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    q.options.forEach { opt ->
                        Row(
                            Modifier.fillMaxWidth().clickable { answers = answers + (q.id to opt) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = answers[q.id] == opt, onClick = { answers = answers + (q.id to opt) })
                            Text(opt, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                container.studyProgress.recordExamSitting(pack, answers, today)
                tick++
                done = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = answers.size == mcq.size,
        ) { Text("تصحیح آزمون") }
    }
}

@Composable
internal fun ExamLedgerCard(ledger: MathExamLedger.State) {
    val last = ledger.latest ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("نتیجهٔ نمودار (آخرین نشست)", fontWeight = FontWeight.Bold)
            Text("نمره: ${toPersianDigits(last.scorePct.toString())}٪ از ${toPersianDigits(last.total.toString())} سوال")
            Text("تعداد تکرار: ${toPersianDigits(ledger.repeatCount.toString())}")
            ledger.sittings.forEach { s ->
                val wrong = if (s.wrongNumbers.isEmpty()) "بدون غلط"
                else "غلط: ${s.wrongNumbers.joinToString("، ") { toPersianDigits(it.toString()) }}"
                Text(
                    "نشست ${toPersianDigits(s.n.toString())}: ${toPersianDigits(s.scorePct.toString())}٪ — $wrong",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
