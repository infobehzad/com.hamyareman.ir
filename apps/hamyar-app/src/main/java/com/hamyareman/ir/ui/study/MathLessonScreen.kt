package com.hamyareman.ir.ui.study

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.MathAnswerScript
import com.hamyareman.ir.platform.feature.study.MathExamLedger
import com.hamyareman.ir.platform.feature.study.StudyPack
import kotlinx.coroutines.delay

internal data class MathTab(val key: String, val label: String)

/**
 * سربرگ درس ریاضی.
 * درس عادی: تدریس / تمرینات کتابی / خلاصه — فلش و آزمون فقط در جمع‌بندی فصل.
 * جمع‌بندی: تدریس / فلش‌کارت / خلاصه / آزمون — بدون تمرینات کتابی.
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
    val html = remember(packId) { MathHtmlAssets.of(packId) }
    val isSum = html?.isSum == true || pack.lessonId.contains("SUM")
    val chapter = html?.chapter ?: Regex("""E(\d+)""").find(pack.packId)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
    val hasPdf = pack.pdfFileName.isNotBlank()
    val tabs = remember(isSum, hasPdf) {
        val base = if (isSum) listOf(
            MathTab("teach", "تدریس"),
            MathTab("flash", "فلش‌کارت"),
            MathTab("summary", "خلاصه"),
            MathTab("exam", "آزمون"),
        ) else listOf(
            MathTab("teach", "تدریس"),
            MathTab("book", "تمرینات کتابی"),
            MathTab("summary", "خلاصه"),
        )
        if (hasPdf) base + MathTab("pdf", "کتاب درسی") else base
    }
    var tab by rememberSaveable(packId, isSum) {
        mutableIntStateOf(initialTab.coerceIn(0, tabs.lastIndex))
    }
    if (tab > tabs.lastIndex) tab = 0
    val chromeStore = remember { com.hamyareman.ir.platform.core.common.LocalStore(ctx, "hamyar_math_ui") }
    var autoHide by rememberSaveable(packId) { mutableStateOf(chromeStore.getBool("autohide_$packId", true)) }
    var chromeHidden by remember { mutableStateOf(false) }
    var hideGen by remember { mutableIntStateOf(0) }

    LaunchedEffect(autoHide, chromeHidden, hideGen, tab) {
        if (!autoHide) {
            chromeHidden = false
            return@LaunchedEffect
        }
        if (chromeHidden) return@LaunchedEffect
        delay(3000)
        chromeHidden = true
    }

    if (pack.pdfOnly || pack.lessonId == "TOC") {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = pack.title, onBack = onBack)
            TeachPdfPages(modifier = Modifier.weight(1f), fileId = pack.pdfFileName, pack = pack)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        if (chromeHidden) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            chromeHidden = false
                            hideGen++
                        },
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("▾", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(pack.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        } else {
            AppTopBar(title = pack.title, onBack = onBack)
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = autoHide,
                    onCheckedChange = {
                        autoHide = it
                        chromeStore.putBool("autohide_$packId", it)
                        if (!it) chromeHidden = false else hideGen++
                    },
                )
                Text("جمع شود", style = MaterialTheme.typography.labelSmall)
            }
            ScrollableTabRow(selectedTabIndex = tab.coerceIn(0, tabs.lastIndex), edgePadding = 8.dp) {
                tabs.forEachIndexed { i, t ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        }
        val currentKey = tabs.getOrNull(tab)?.key ?: "teach"
        val currentLabel = tabs.getOrNull(tab)?.label ?: "تدریس"
        DisposableEffect(pack.packId, currentKey) {
            val start = System.currentTimeMillis()
            StudyActivity.add(ctx, pack.packId, "tab", "باز کردن سربرگ $currentLabel")
            onDispose {
                val sec = ((System.currentTimeMillis() - start) / 1000L).toInt()
                if (sec >= 2) {
                    StudyActivity.add(ctx, pack.packId, "dwell", "سربرگ $currentLabel — ${sec} ثانیه")
                }
            }
        }
        if (currentKey == "teach") {
            val tracks = teachTracksOf(pack)
            if (tracks.isNotEmpty()) {
                Box(Modifier.then(if (chromeHidden) Modifier.height(0.dp) else Modifier)) {
                    TeachAudioBar(packId = pack.packId, screenTitle = pack.title, bookTitle = bookTitle, tracks = tracks)
                }
            }
        }
        when (currentKey) {
            "teach" -> MathTeachTab(pack, bookTitle, showPlayer = false)
            "book" -> MathBookHtmlTab(pack, html)
            "flash" -> MathFlashHtmlTab(pack, html)
            "summary" -> MathSummaryTab(pack, isSum = isSum, chapter = chapter)
            "exam" -> MathExamHtmlTab(pack, html)
            "pdf" -> TeachPdfPages(modifier = Modifier.fillMaxSize(), fileId = pack.pdfFileName, pack = pack)
            else -> MathTeachTab(pack, bookTitle, showPlayer = false)
        }
    }
}

@Composable
private fun MathBookHtmlTab(pack: StudyPack, html: MathHtmlAssets.Spec?) {
    val asset = html?.bookAsset
    if (!asset.isNullOrBlank()) {
        MathInteractiveHtml(packId = pack.packId, kind = "book", assetPath = asset, modifier = Modifier.fillMaxSize())
    } else {
        MathStudyTab(pack)
    }
}

@Composable
private fun MathFlashHtmlTab(pack: StudyPack, html: MathHtmlAssets.Spec?) {
    val asset = html?.flashAsset
    if (!asset.isNullOrBlank()) {
        MathInteractiveHtml(packId = pack.packId, kind = "flash", assetPath = asset, modifier = Modifier.fillMaxSize())
    } else {
        MathFlashTab(pack)
    }
}

@Composable
private fun MathExamHtmlTab(pack: StudyPack, html: MathHtmlAssets.Spec?) {
    val asset = html?.examAsset
    if (!asset.isNullOrBlank()) {
        MathInteractiveHtml(packId = pack.packId, kind = "exam", assetPath = asset, modifier = Modifier.fillMaxSize())
    } else {
        MathExamTab(pack)
    }
}

@Composable
private fun MathTeachTab(pack: StudyPack, bookTitle: String, showPlayer: Boolean = true) {
    val tracks = teachTracksOf(pack)
    val body = pack.teachText.ifBlank {
        pack.sections.filter { it.kind != "exam" }.joinToString("\n\n") { "«${it.title}»\n${it.body}" }
            .ifBlank { "متن تدریس این درس به‌زودی از پوشهٔ Books اضافه می‌شود." }
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showPlayer && tracks.isNotEmpty()) {
            TeachAudioBar(packId = pack.packId, screenTitle = pack.title, bookTitle = bookTitle, tracks = tracks)
        }
        Card(Modifier.fillMaxWidth().weight(1f)) {
            if (pack.teachHtml.isNotBlank()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            setBackgroundColor(android.graphics.Color.WHITE)
                        }
                    },
                    update = { wv ->
                        wv.loadDataWithBaseURL(
                            "https://local.hamyar/",
                            pack.teachHtml,
                            "text/html",
                            "utf-8",
                            null,
                        )
                    },
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                )
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    Text("متن تدریس", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(body, style = MaterialTheme.typography.bodyMedium)
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val today = remember { JalaliDate.todayIso() }
    val ime = LocalSoftwareKeyboardController.current
    val bookQs = pack.questions.filter { it.topic == "book" && it.type == "mcq" }
    if (pack.exercises.isNotEmpty()) {
        /* جای‌خالی + کیبورد نماد — فقط وقتی تمرین تایپی در پک باشد */
    } else if (bookQs.isNotEmpty()) {
        MathBookMcqPane(pack, bookQs, modifier)
        return
    } else {
        Card(modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("تمرین‌های کتاب", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "ساختار تمرین آماده است؛ فایل HTML این درس هنوز در پوشهٔ Books نیست.",
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
                        StudyActivity.add(ctx, pack.packId, "item", "تمرین ${ex.id} — ${if (ok) "درست" else "نادرست"}")
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
private fun MathBookMcqPane(pack: StudyPack, qs: List<StudyPack.Question>, modifier: Modifier) {
    val container = LocalAppContainer.current
    val appCtx = androidx.compose.ui.platform.LocalContext.current
    val today = remember { JalaliDate.todayIso() }
    var idx by rememberSaveable(pack.packId) { mutableIntStateOf(0) }
    var pick by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var lastOk by remember { mutableStateOf<Boolean?>(null) }
    val q = qs[idx.coerceIn(0, qs.lastIndex)]
    LaunchedEffect(q.id) { pick = null; feedback = null; lastOk = null }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "تمرین کتاب ${toPersianDigits((idx + 1).toString())} از ${toPersianDigits(qs.size.toString())}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(q.text, style = MaterialTheme.typography.bodyMedium)
                q.options.forEach { opt ->
                    Row(
                        Modifier.fillMaxWidth().clickable { pick = opt; lastOk = null; feedback = null },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = pick == opt, onClick = { pick = opt; lastOk = null; feedback = null })
                        Text(opt, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(
                    onClick = {
                        val ok = com.hamyareman.ir.platform.feature.study.QuizGrader.grade(q, pick.orEmpty()).second
                        container.studyProgress.recordExercise(pack.packId, q.id, ok, today)
                        StudyActivity.add(appCtx, pack.packId, "item", "تمرین ${q.id} — ${if (ok) "درست" else "نادرست"}")
                        lastOk = ok
                        feedback = if (ok) "درست بود ✓\n${q.explanation}"
                        else "نادرست. پاسخ درست: ${q.answer}\n${q.explanation}"
                    },
                    enabled = pick != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("بررسی جواب") }
                if (feedback != null) {
                    Text(
                        feedback!!,
                        color = if (lastOk == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { idx = (idx - 1).coerceAtLeast(0) }, enabled = idx > 0, modifier = Modifier.weight(1f)) { Text("قبلی") }
            OutlinedButton(onClick = { idx = (idx + 1).coerceAtMost(qs.lastIndex) }, enabled = idx < qs.lastIndex, modifier = Modifier.weight(1f)) { Text("بعدی") }
        }
    }
}

@Composable
private fun MathFlashTab(pack: StudyPack) {
    Text(
        "فلش‌کارت این فصل هنوز به‌صورت HTML در پوشهٔ Books نیست.",
        Modifier.padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun MathSummaryTab(pack: StudyPack, isSum: Boolean, chapter: Int) {
    val summary = pack.summary.ifBlank {
        pack.sections.filter { it.kind == "exam" }.lastOrNull()?.body
            ?: "خلاصه‌ی چندسطری این درس به‌زودی از پوشهٔ Books نوشته می‌شود."
    }
    val tips = pack.examTips.ifBlank {
        pack.sections.filter { it.kind == "exam" }.joinToString("\n\n") { it.body }
    }
    val html = remember(pack.packId, pack.teachHtml, summary, tips, isSum, chapter) {
        if (pack.teachHtml.contains("<html", ignoreCase = true)) pack.teachHtml
        else htmlSummaryDocument(
            teachHtml = pack.teachHtml,
            isSum = isSum,
            fallback = mathSummaryHtml(pack.title, summary, tips, isSum, chapter),
        )
    }
    Column(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.defaultTextEncodingName = "utf-8"
                    setBackgroundColor(android.graphics.Color.WHITE)
                }
            },
            update = { wv ->
                wv.loadDataWithBaseURL("https://local.hamyar/", html, "text/html", "utf-8", null)
            },
            modifier = Modifier.weight(1f).padding(4.dp),
        )
    }
}

/** جدول خلاصه + نکات + SVG/شکل‌های همان HTML تدریس، با استایل اصلی. */
internal fun htmlSummaryDocument(teachHtml: String, isSum: Boolean, fallback: String): String {
    if (teachHtml.isBlank() || !teachHtml.contains("<html", ignoreCase = true)) return fallback
    if (isSum) return teachHtml
    val styles = Regex("(?is)<style[^>]*>.*?</style>").findAll(teachHtml).joinToString("\n") { it.value }
    val markers = listOf("جدول خلاصه", "خلاصه‌ی فرمول", "summary-table", "نکات امتحانی مهم")
    val hit = markers.map { teachHtml.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: return teachHtml
    val sec = teachHtml.lastIndexOf("<section", hit).takeIf { it >= 0 } ?: hit
    val end = listOf("</main>", "<footer", "</body>").map { teachHtml.indexOf(it, sec) }.filter { it > sec }.minOrNull()
        ?: teachHtml.length
    val fragment = teachHtml.substring(sec, end)
    if (fragment.length < 80) return teachHtml
    return """
<!DOCTYPE html><html dir="rtl" lang="fa"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
$styles
</head><body>
<div class="container">$fragment</div>
</body></html>
""".trimIndent()
}

private fun mathSummaryHtml(title: String, summary: String, tips: String, isSum: Boolean, chapter: Int): String {
    val ch = toPersianDigits(chapter.toString())
    val pointer = if (isSum) {
        "<p>فلش‌کارت و نمونه سوالات همین فصل در سربرگ‌های این جمع‌بندی است.</p>"
    } else {
        "<div class='note'>🎴 آزمون و فلش‌کارت این درس در <strong>انتهای فصل $ch</strong> — کارت «جمع‌بندی فصل $ch» — آمده است. خودِ درس فلش و آزمون جدا ندارد.</div>"
    }
    val tipsBlock = if (tips.isBlank()) "" else "<h2>نکات امتحانی</h2><p>${tips.replace("\n", "<br>")}</p>"
    return """
<!DOCTYPE html><html dir="rtl" lang="fa"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
@import url('https://fonts.googleapis.com/css2?family=Vazirmatn:wght@400;700;900&display=swap');
body{font-family:'Vazirmatn',Tahoma,sans-serif;background:linear-gradient(135deg,#eef2ff,#fdf2f8 50%,#ecfeff);color:#1e293b;margin:0;padding:12px;line-height:1.9}
.box{max-width:900px;margin:0 auto;background:#fff;border-radius:24px;box-shadow:0 20px 60px rgba(30,41,59,.15);overflow:hidden}
header{background:linear-gradient(135deg,#4f46e5,#7c3aed,#ec4899);color:#fff;padding:22px 20px;text-align:center}
h1{font-size:1.25rem;margin:0 0 6px;font-weight:900}
main{padding:18px 16px 28px}
h2{color:#4f46e5;font-size:1.05rem;border-bottom:2px dashed #c7d2fe;padding-bottom:6px}
.note{background:#e0f2fe;border-right:4px solid #0284c7;padding:12px 14px;border-radius:12px;margin:12px 0}
</style></head><body><div class="box">
<header><h1>خلاصه — $title</h1></header>
<main>
$pointer
<h2>خلاصه درس</h2>
<p>${summary.replace("\n", "<br>")}</p>
$tipsBlock
</main></div></body></html>
""".trimIndent()
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
                "نمونه سوالات این فصل در کارت جمع‌بندی همان فصل است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ledger.repeatCount > 0) ExamLedgerCard(ledger)
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
