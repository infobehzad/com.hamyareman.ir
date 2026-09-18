package com.hamyareman.ir.ui.study

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.R
import com.hamyareman.ir.platform.core.appwrite.AppwriteAuthService
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.feature.hearttoheart.MediaFiles
import com.hamyareman.ir.ui.profile.StudentProfileState
import com.hamyareman.ir.ui.profile.loadOrientedBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class NoteFile(
    val id: String,
    val title: String,
    val mime: String,
    val ext: String,
    val sizeKb: Long,
    val addedIso: String,
    val localPath: String,
)

private const val KEY_FILES = "study_pdfs"
private const val KEY_NOTES = "lesson_notes_text"
private const val KEY_NOTES_AT = "lesson_notes_at"
private const val FREE_FILE_CAP = 10
private val GalleryGroups = listOf("عکس", "PDF", "متن", "سایر")


private val Lalezar = FontFamily(Font(R.font.lalezar))

internal fun readNoteFiles(store: LocalStore): List<NoteFile> = runCatching {
    val array = JSONArray(store.getString(KEY_FILES, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val name = o.optString("name")
            val path = o.optString("localPath")
            val ext = o.optString("ext").ifBlank { File(path).extension.ifBlank { "bin" } }
            add(
                NoteFile(
                    id = o.optString("id"),
                    title = o.optString("title").ifBlank { name.substringBeforeLast('.') },
                    mime = o.optString("mime").ifBlank { guessMime(ext) },
                    ext = ext,
                    sizeKb = o.optLong("sizeKb"),
                    addedIso = o.optString("addedIso"),
                    localPath = path,
                ),
            )
        }
    }
}.getOrDefault(emptyList())

private fun writeNoteFiles(store: LocalStore, items: List<NoteFile>) {
    val array = JSONArray()
    items.forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("name", "${item.title}.${item.ext}")
                .put("mime", item.mime)
                .put("ext", item.ext)
                .put("sizeKb", item.sizeKb)
                .put("addedIso", item.addedIso)
                .put("localPath", item.localPath)
                .put("reference", ""),
        )
    }
    store.putString(KEY_FILES, array.toString())
}

private fun galleryDir(context: android.content.Context): File =
    File(context.filesDir, "notes_gallery").apply { mkdirs() }

private fun guessMime(ext: String): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
        ?: when (ext.lowercase()) {
            "pdf" -> "application/pdf"
            "txt", "md" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }

private fun isImageMime(mime: String) = mime.startsWith("image/")

private fun fileGroup(item: NoteFile): String = when {
    isImageMime(item.mime) -> "عکس"
    item.mime == "application/pdf" || item.ext.equals("pdf", true) -> "PDF"
    item.mime.startsWith("text/") || item.ext.lowercase() in setOf("txt", "md", "rtf") -> "متن"
    else -> "سایر"
}

@Composable
fun PdfUploadScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = container.store
    val scope = rememberCoroutineScope()
    val paid = StudentProfileState.isPaid()

    var items by remember { mutableStateOf(readNoteFiles(store)) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf(store.getString(KEY_NOTES, "")) }
    var cropBmp by remember { mutableStateOf<Bitmap?>(null) }
    var titleDraft by remember { mutableStateOf<Pair<File, String>?>(null) }
    var titleText by remember { mutableStateOf("") }
    var needSubMsg by remember { mutableStateOf<String?>(null) }
    var imageAlbum by remember { mutableStateOf<List<NoteFile>?>(null) }
    var imageStart by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val uid = store.getString(AppwriteAuthService.KEY_USER_ID)
        if (uid.isBlank()) return@LaunchedEffect
        when (val remote = container.tables.get(TableIds.LESSON_NOTES, "notes_$uid")) {
            is AppResult.Ok -> {
                val row = remote.value ?: return@LaunchedEffect
                val remoteText = row.string("text")
                val remoteAt = row.long("updatedAt")
                val localAt = store.getLong(KEY_NOTES_AT, 0L)
                if (remoteText.isNotBlank() && remoteAt >= localAt) {
                    notes = remoteText
                    store.putString(KEY_NOTES, remoteText)
                    store.putLong(KEY_NOTES_AT, remoteAt)
                }
            }
            is AppResult.Err -> { }
        }
    }

    fun canAddMore(): Boolean {
        if (paid || items.size < FREE_FILE_CAP) return true
        needSubMsg = "تا ۱۰ فایل برای «مهمان همیار من» رایگان است. از فایل یازدهم اشتراک فعال لازم است."
        return false
    }

    fun persist(file: File, title: String, mime: String) {
        val ext = file.extension.ifBlank { MimeTypeMap.getSingleton().getExtensionFromMimeType(mime).orEmpty().ifBlank { "bin" } }
        val item = NoteFile(
            id = "nf_${System.currentTimeMillis()}",
            title = title.trim().ifBlank { "جزوه" },
            mime = mime,
            ext = ext,
            sizeKb = (file.length() / 1024).coerceAtLeast(1),
            addedIso = JalaliDate.todayIso(),
            localPath = file.absolutePath,
        )
        items = listOf(item) + items
        writeNoteFiles(store, items)
        notice = "فقط روی همین گوشی ذخیره شد — فایل‌ها هرگز به سرور نمی‌روند."
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (!canAddMore()) return@rememberLauncherForActivityResult
        cropBmp = loadOrientedBitmap(context, uri, maxSide = 2400)
        if (cropBmp == null) notice = "خواندن عکس ممکن نشد."
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked == null) return@rememberLauncherForActivityResult
        if (!canAddMore()) return@rememberLauncherForActivityResult
        busy = true
        notice = null
        scope.launch {
            val name = MediaFiles.displayName(context, picked) ?: "file.bin"
            val mime = context.contentResolver.getType(picked) ?: guessMime(File(name).extension)
            if (isImageMime(mime)) {
                cropBmp = loadOrientedBitmap(context, picked, maxSide = 2400)
                busy = false
                return@launch
            }
            val copied = withContext(Dispatchers.IO) {
                runCatching {
                    val ext = File(name).extension.ifBlank { MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin" }
                    val target = File(galleryDir(context), "f_${System.currentTimeMillis()}.$ext")
                    context.contentResolver.openInputStream(picked)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    target.takeIf { it.exists() && it.length() > 0 }
                }.getOrNull()
            }
            busy = false
            if (copied == null) {
                notice = "کپی فایل ممکن نشد."
                return@launch
            }
            titleText = name.substringBeforeLast('.')
            titleDraft = copied to mime
        }
    }

    val backupCreate = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        ZipOutputStream(out).use { zip ->
                            zip.putNextEntry(ZipEntry("manifest.json"))
                            zip.write(JSONArray().also { arr ->
                                items.forEach { it ->
                                    arr.put(
                                        JSONObject()
                                            .put("id", it.id).put("title", it.title).put("mime", it.mime)
                                            .put("ext", it.ext).put("addedIso", it.addedIso)
                                            .put("file", File(it.localPath).name),
                                    )
                                }
                            }.toString().toByteArray())
                            zip.closeEntry()
                            items.forEach { item ->
                                val f = File(item.localPath)
                                if (!f.exists()) return@forEach
                                zip.putNextEntry(ZipEntry("files/${f.name}"))
                                FileInputStream(f).use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                    true
                }.getOrDefault(false)
            }
            notice = if (ok) "بکاپ روی گوشی ذخیره شد." else "بکاپ ساخته نشد."
        }
    }

    val restoreOpen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = galleryDir(context)
                    val restored = mutableListOf<NoteFile>()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        ZipInputStream(input).use { zip ->
                            var entry = zip.nextEntry
                            var manifest: JSONArray? = null
                            val blobs = mutableMapOf<String, File>()
                            while (entry != null) {
                                if (entry.isDirectory) {
                                    entry = zip.nextEntry
                                    continue
                                }
                                if (entry.name == "manifest.json") {
                                    manifest = JSONArray(zip.readBytes().decodeToString())
                                } else {
                                    val dest = File(dir, "r_${System.currentTimeMillis()}_${File(entry.name).name}")
                                    dest.outputStream().use { zip.copyTo(it) }
                                    blobs[File(entry.name).name] = dest
                                }
                                zip.closeEntry()
                                entry = zip.nextEntry
                            }
                            val arr = manifest ?: JSONArray()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val fname = o.optString("file")
                                val f = blobs[fname] ?: continue
                                restored += NoteFile(
                                    id = o.optString("id").ifBlank { "nf_${System.currentTimeMillis()}_$i" },
                                    title = o.optString("title").ifBlank { fname },
                                    mime = o.optString("mime").ifBlank { guessMime(o.optString("ext")) },
                                    ext = o.optString("ext").ifBlank { File(fname).extension },
                                    sizeKb = (f.length() / 1024).coerceAtLeast(1),
                                    addedIso = o.optString("addedIso").ifBlank { JalaliDate.todayIso() },
                                    localPath = f.absolutePath,
                                )
                            }
                        }
                    }
                    restored
                }.getOrDefault(emptyList())
            }
            if (result.isEmpty()) {
                notice = "بازگردانی چیزی پیدا نکرد."
            } else {
                items = (result + items).distinctBy { it.id }
                writeNoteFiles(store, items)
                notice = "${toPersianDigits(result.size.toString())} فایل بازگردانده شد."
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("جزوه‌های شخصی و آزمونی", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("ثبت نکات درسی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LinedNotesPaper(
                value = notes,
                onValueChange = { notes = it },
            )
            PrimaryButton("ذخیره نکات و همگام با سرور") {
                val now = System.currentTimeMillis()
                store.putString(KEY_NOTES, notes)
                store.putLong(KEY_NOTES_AT, now)
                val uid = store.getString(AppwriteAuthService.KEY_USER_ID)
                if (uid.isBlank()) {
                    notice = "نکات روی دستگاه ذخیره شد. برای سینک با سرور وارد شو."
                } else {
                    val payload = mapOf(
                        "userId" to uid,
                        "text" to notes,
                        "updatedAt" to now,
                    )
                    scope.launch {
                        val perms = AppwriteClientProvider.ownerOnly(uid)
                        when (val saved = container.tables.upsert(TableIds.LESSON_NOTES, "notes_$uid", payload, perms)) {
                            is AppResult.Ok -> notice = "نکات ذخیره شد و با سرور همگام شد."
                            is AppResult.Err -> {
                                container.sync.enqueue(TableIds.LESSON_NOTES, "notes_$uid", payload)
                                runCatching { container.sync.pushAll() }
                                notice = "نکات روی دستگاه ماند؛ سینک بعدی: ${saved.error.userMessage}"
                            }
                        }
                    }
                }
            }

            Text("گالری جزوه — فقط همین گوشی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "عکس جزوه و کتاب، متن، PDF یا هر فرمت دیگر. فایل‌ها هرگز به سرور نمی‌روند. مهمان همیار من تا ۱۰ فایل؛ از یازدهم اشتراک فعال. بکاپ و بازگردانی هم با اشتراک فعال.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "الان ${toPersianDigits(items.size.toString())} فایل" +
                    if (!paid) " از ${toPersianDigits(FREE_FILE_CAP.toString())} سهمیهٔ مهمان" else "",
                style = MaterialTheme.typography.labelMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (!busy && canAddMore()) imagePicker.launch("image/*") },
                    modifier = Modifier.weight(1f),
                ) { Text("عکس + برش") }
                OutlinedButton(
                    onClick = { if (!busy && canAddMore()) filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f),
                ) { Text("هر فایل") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (!paid) needSubMsg = "بکاپ جزوه‌ها فقط با اشتراک فعال."
                        else backupCreate.launch("hamyar-joozve-${JalaliDate.todayIso()}.zip")
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("بکاپ روی گوشی") }
                OutlinedButton(
                    onClick = {
                        if (!paid) needSubMsg = "بازگردانی جزوه‌ها فقط با اشتراک فعال."
                        else restoreOpen.launch(arrayOf("application/zip", "*/*"))
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("بازگردانی") }
            }
            notice?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (busy) Text("در حال ذخیره…", style = MaterialTheme.typography.bodySmall)

            if (items.isEmpty()) {
                Text("گالری خالی است.", style = MaterialTheme.typography.bodySmall)
            }
            GalleryGroups.forEach { group ->
                val groupItems = items.filter { fileGroup(it) == group }
                if (groupItems.isEmpty()) return@forEach
                Text(
                    "$group · ${toPersianDigits(groupItems.size.toString())}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                groupItems.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { item ->
                            GalleryTile(
                                item = item,
                                modifier = Modifier.weight(1f),
                                onOpen = {
                                    if (isImageMime(item.mime)) {
                                        val album = groupItems.filter { isImageMime(it.mime) }
                                        imageStart = album.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                        imageAlbum = album
                                    } else {
                                        runCatching {
                                            val file = File(item.localPath)
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW).setDataAndType(uri, item.mime)
                                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                            )
                                        }.onFailure { notice = "برنامه‌ای برای بازکردن این فایل پیدا نشد." }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { runCatching { File(item.localPath).delete() } }
                                        items = items.filterNot { it.id == item.id }
                                        writeNoteFiles(store, items)
                                        notice = "از گالری حذف شد."
                                    }
                                },
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    cropBmp?.let { bmp ->
        ImageRectCropDialog(
            bitmap = bmp,
            onCancel = { cropBmp = null },
            onCropped = { out ->
                scope.launch {
                    val saved = withContext(Dispatchers.IO) {
                        val (scaled, q) = compressReadableJpeg(out)
                        val target = File(galleryDir(context), "img_${System.currentTimeMillis()}.jpg")
                        target.outputStream().use { os -> scaled.compress(Bitmap.CompressFormat.JPEG, q, os) }
                        target.takeIf { it.exists() && it.length() > 0 }
                    }
                    cropBmp = null
                    if (saved == null) {
                        notice = "ذخیرهٔ عکس ممکن نشد."
                    } else {
                        titleText = "عکس جزوه"
                        titleDraft = saved to "image/jpeg"
                    }
                }
            },
        )
    }

    imageAlbum?.let { album ->
        ImageGalleryPager(
            album = album,
            start = imageStart,
            onClose = { imageAlbum = null },
            onDelete = { gone ->
                scope.launch {
                    withContext(Dispatchers.IO) { runCatching { File(gone.localPath).delete() } }
                    items = items.filterNot { it.id == gone.id }
                    writeNoteFiles(store, items)
                    val next = album.filterNot { it.id == gone.id }
                    imageAlbum = next.ifEmpty { null }
                    notice = "از گالری حذف شد."
                }
            },
        )
    }

    titleDraft?.let { (file, mime) ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("عنوان فایل") },
            text = {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("عنوان + فرمت در گالری دیده می‌شود") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    persist(file, titleText, mime)
                    titleDraft = null
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = {
                    file.delete()
                    titleDraft = null
                }) { Text("انصراف") }
            },
        )
    }

    needSubMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { needSubMsg = null },
            title = { Text("نیاز به اشتراک فعال") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { needSubMsg = null }) { Text("متوجه شدم") } },
        )
    }
}

@Composable
private fun LinedNotesPaper(value: String, onValueChange: (String) -> Unit) {
    val density = LocalDensity.current
    val lineSp = with(density) { 44.dp.toSp() }
    Box(
        Modifier
            .fillMaxWidth()
            .height(236.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.notes_lined_paper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                val lines = raw.replace("\r", "").split('\n')
                onValueChange(lines.take(5).joinToString("\n"))
            },
            textStyle = TextStyle(
                fontFamily = Lalezar,
                fontSize = 18.sp,
                lineHeight = lineSp,
                color = Color(0xFF1E3A5F),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Bottom,
                    trim = LineHeightStyle.Trim.None,
                ),
            ),
            cursorBrush = SolidColor(Color(0xFF1E3A5F)),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ImageGalleryPager(
    album: List<NoteFile>,
    start: Int,
    onClose: () -> Unit,
    onDelete: (NoteFile) -> Unit,
) {
    val pager = rememberPagerState(
        initialPage = start.coerceIn(0, (album.size - 1).coerceAtLeast(0)),
        pageCount = { album.size.coerceAtLeast(1) },
    )
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                val item = album.getOrNull(page)
                val bmp = remember(item?.localPath) {
                    item?.localPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = item?.title,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text("خوانده نشد", color = Color.White)
                    }
                }
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val cur = album.getOrNull(pager.currentPage)
                Text(cur?.title.orEmpty(), color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "${toPersianDigits((pager.currentPage + 1).toString())} از ${toPersianDigits(album.size.toString())}",
                    color = Color.White.copy(alpha = 0.8f),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("بستن") }
                    OutlinedButton(
                        onClick = { cur?.let(onDelete) },
                        modifier = Modifier.weight(1f),
                    ) { Text("حذف") }
                }
            }
        }
    }
}

@Composable
private fun GalleryTile(
    item: NoteFile,
    modifier: Modifier,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val thumb = remember(item.localPath) {
        if (isImageMime(item.mime)) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(item.localPath, opts)
            }.getOrNull()
        } else null
    }
    Column(modifier.clickable(onClick = onOpen)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (thumb != null) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(item.ext.uppercase().ifBlank { "FILE" }, fontWeight = FontWeight.Bold)
            }
        }
        Text(item.title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        Text(
            "${item.ext.uppercase()} · ${toPersianDigits(item.sizeKb.toString())}کب",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        TextButton(onClick = onDelete) { Text("حذف") }
    }
}
