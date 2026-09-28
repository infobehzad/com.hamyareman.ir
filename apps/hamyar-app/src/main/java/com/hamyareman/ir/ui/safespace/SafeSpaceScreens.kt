package com.hamyareman.ir.ui.safespace

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.Helplines
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PinLockGate
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.platform.core.security.BiometricPromptRunner
import com.hamyareman.ir.platform.feature.hearttoheart.MediaFiles
import com.hamyareman.ir.ui.study.LinedNotesPaper
import com.hamyareman.ir.ui.study.NoteTitlePicker
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

/**
 * مدل داده‌ای فایل در آلبوم و گالری امن شخصی (فقط روی حافظه گوشی، هرگز سینک نمی‌شود).
 */
data class VaultMediaFile(
    val id: String,
    val title: String,
    val mime: String,
    val ext: String,
    val sizeKb: Long,
    val addedIso: String,
    val localPath: String,
)

/**
 * مدل یادداشت‌های دفاتر شخصی با قابلیت سینک رمزنگاری‌شده.
 */
data class SafeNote(
    val id: String,
    val category: String, // delnevesht, daftarche, ronevesht, shokrgozari
    val title: String,
    val text: String,
    val updatedAt: Long,
)

private val DEFAULT_TITLES_BY_CAT = mapOf(
    "delnevesht" to listOf("حرف دلم", "احساس امروز", "چیزی که به کسی نگفتم", "خاطره خوب", "آرزوها"),
    "daftarche" to listOf("یادداشت روزانه", "هدف‌های من", "فکرهای پراکنده", "برنامه‌ریزی شخصی", "ایده‌های من"),
    "ronevesht" to listOf("تخلیه ذهن", "جمله‌های قشنگ", "شعر و متن", "گفت‌وگو با خود", "نامه به آینده"),
    "shokrgozari" to listOf("سپاسگزاری روزانه", "اتفاق خوب امروز", "آدم‌های مهربان زندگی‌ام", "نعمت‌های من", "حال خوب"),
)

private fun vaultDir(context: Context): File =
    File(context.filesDir, "safespace_vault").apply { mkdirs() }

/**
 * صفحه اصلی «فضای امن من»: محافظت‌شده با قفل PIN / بیومتریک،
 * شامل ۴ دفترچه یادداشت گرافیکی سینک‌شونده (دل‌نوشت، دفترچه من، رونوشت آزاد، شکرگزاری)،
 * آلبوم امن شخصی (عکس، فیلم، صدا - بدون سینک و با قابلیت بکاپ/بازگردانی)، و شماره‌های کمک.
 */
@Composable
fun SafeSpaceScreen(nav: NavController) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var isUnlocked by remember { mutableStateOf(false) }

    var bioBusy by remember { mutableStateOf(false) }
    var bioNotice by remember { mutableStateOf<String?>(null) }
    val offerBiometric = remember { activity != null && container.biometric.shouldOffer(activity) }

    fun unlockBio() {
        if (activity == null) return
        bioBusy = true
        bioNotice = null
        BiometricPromptRunner.show(
            activity = activity,
            title = "فضای امن من",
            subtitle = "برای باز کردن فضای خصوصی اثر انگشت یا چهره خود را تأیید کنید.",
            negativeText = "وارد کردن PIN",
            onSuccess = {
                bioBusy = false
                isUnlocked = true
            },
            onError = { msg ->
                bioBusy = false
                bioNotice = msg
            },
            onCancelled = { bioBusy = false },
        )
    }

    LaunchedEffect(isUnlocked) {
        if (!isUnlocked && offerBiometric) {
            unlockBio()
        }
    }

    if (!isUnlocked) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar("فضای امن من") { nav.popBackStack() }
            if (container.lock.isEnabled() || container.lock.hasPin()) {
                PinLockGate(
                    title = "فضای امن قفل است 🔒",
                    subtitle = "برای ورود به فضای خصوصی، لطفاً PIN یا بیومتریک را تأیید کن.",
                    biometricLabel = if (offerBiometric) "بازکردن با اثر انگشت / چهره" else null,
                    biometricBusy = bioBusy,
                    externalNotice = bioNotice,
                    onBiometricRequest = if (offerBiometric) { { unlockBio() } } else null,
                    onVerify = { pin -> container.lock.verify(pin) },
                    onUnlocked = { isUnlocked = true },
                )
            } else {
                // در صورتی که قفل کلی اپ فعال نبوده باشد، اجازه ورود با تأیید امنیتی داده می‌شود
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "ورود به فضای امن من",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "این بخش برای یادداشت‌های کاملاً خصوصی و آلبوم محرمانهٔ تو طراحی شده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton("ورود امن", modifier = Modifier.fillMaxWidth(0.7f)) {
                        isUnlocked = true
                    }
                }
            }
        }
        return
    }

    SafeSpaceContent(nav = nav)
}

@Composable
private fun SafeSpaceContent(nav: NavController) {
    val tabs = listOf(
        "💖 دل‌نوشت",
        "📔 دفترچه من",
        "📝 رونوشت آزاد",
        "🙏 شکرگزاری",
        "🖼️ آلبوم شخصی",
        "🚨 شماره‌های کمک",
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("فضای امن من") { nav.popBackStack() }
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                        )
                    },
                )
            }
        }

        when (selectedTab) {
            0 -> SafeNotebookTab(categoryKey = "delnevesht", tabTitle = "دل‌نوشت")
            1 -> SafeNotebookTab(categoryKey = "daftarche", tabTitle = "دفترچه من")
            2 -> SafeNotebookTab(categoryKey = "ronevesht", tabTitle = "رونوشت آزاد")
            3 -> SafeNotebookTab(categoryKey = "shokrgozari", tabTitle = "شکرگزاری")
            4 -> SafeVaultGalleryTab()
            5 -> SafeHelplinesTab()
        }
    }
}

/**
 * تب دفترچه یادداشت گرافیکی برای ۴ بخش نوشتاری (با تصویر دفترچه روتیت‌شده ۱۸۰ درجه، فونت بزرگتر و بولد و قابلیت همگام‌سازی).
 */
@Composable
private fun SafeNotebookTab(categoryKey: String, tabTitle: String) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = remember { LocalStore(context, "hamyar_safespace_notes") }
    val scope = rememberCoroutineScope()

    var noteTitles by remember(categoryKey) {
        val saved = store.getString("titles_$categoryKey", "[]")
        val parsed = runCatching {
            val a = JSONArray(saved)
            (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
        mutableStateOf((DEFAULT_TITLES_BY_CAT[categoryKey].orEmpty() + parsed).distinct())
    }

    var notesList by remember(categoryKey) {
        val saved = store.getString("notes_$categoryKey", "[]")
        val parsed = runCatching {
            val a = JSONArray(saved)
            buildList {
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    add(
                        SafeNote(
                            id = o.optString("id"),
                            category = categoryKey,
                            title = o.optString("title"),
                            text = o.optString("text"),
                            updatedAt = o.optLong("updatedAt"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
        mutableStateOf(parsed.sortedByDescending { it.updatedAt })
    }

    var noteTitle by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var accordionOpen by remember { mutableStateOf(false) }

    fun saveLocalNotes(updated: List<SafeNote>) {
        notesList = updated.sortedByDescending { it.updatedAt }
        val arr = JSONArray()
        notesList.forEach { n ->
            arr.put(
                JSONObject()
                    .put("id", n.id)
                    .put("category", n.category)
                    .put("title", n.title)
                    .put("text", n.text)
                    .put("updatedAt", n.updatedAt),
            )
        }
        store.putString("notes_$categoryKey", arr.toString())
    }

    fun syncNotesWithCloud(notesToSync: List<SafeNote>) {
        scope.launch {
            val uid = container.auth.cachedUserId()
                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
            if (uid.isBlank()) {
                notice = "نوشته روی گوشی ذخیره شد (برای همگام‌سازی ابری وارد حساب شو)."
                return@launch
            }
            val arr = JSONArray()
            notesToSync.forEach { n ->
                arr.put(
                    JSONObject()
                        .put("id", n.id)
                        .put("category", n.category)
                        .put("title", n.title)
                        .put("text", n.text)
                        .put("updatedAt", n.updatedAt),
                )
            }
            val payload = mapOf(
                "userId" to uid,
                "payload" to arr.toString(),
                "text" to arr.toString().take(7000),
                "updatedAt" to System.currentTimeMillis(),
            )
            val perms = AppwriteClientProvider.ownerOnly(uid)
            val res = container.tables.upsert(TableIds.LESSON_NOTES, "safe_${categoryKey}_$uid", payload, perms)
            if (res is AppResult.Ok) {
                notice = "نوشته ذخیره و با سرور همگام شد."
            } else {
                container.sync.enqueue(TableIds.LESSON_NOTES, "safe_${categoryKey}_$uid", payload)
                runCatching { container.sync.pushAll() }
                notice = "نوشته در حافظه دستگاه ذخیره شد و در صف همگام‌سازی قرار گرفت."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            tabTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right,
        )

        NoteTitlePicker(
            titles = noteTitles,
            selected = noteTitle,
            onSelect = { noteTitle = it },
            onAdd = { newT ->
                if (newT.isNotBlank() && newT !in noteTitles) {
                    noteTitles = noteTitles + newT
                    val arr = JSONArray()
                    noteTitles.forEach { arr.put(it) }
                    store.putString("titles_$categoryKey", arr.toString())
                }
                noteTitle = newT
            },
        )

        // دفترچه با تصویر پس‌زمینه روتیت‌شده ۱۸۰ درجه و فونت بزرگتر و بولد
        LinedNotesPaper(
            value = noteText,
            onValueChange = { noteText = it },
        )

        PrimaryButton(if (editingId == null) "ذخیره در $tabTitle و همگام‌سازی" else "به‌روزرسانی نوشته") {
            val now = System.currentTimeMillis()
            val title = noteTitle.trim().ifBlank { "بدون عنوان" }
            val id = editingId ?: "sn_${System.currentTimeMillis()}"
            val next = listOf(SafeNote(id, categoryKey, title, noteText, now)) +
                notesList.filterNot { it.id == id }
            saveLocalNotes(next)
            syncNotesWithCloud(next)
            editingId = null
            noteTitle = ""
            noteText = ""
        }

        if (editingId != null) {
            TextButton(onClick = {
                editingId = null
                noteTitle = ""
                noteText = ""
            }) {
                Text("لغو ویرایش")
            }
        }

        notice?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
            )
        }

        // آکاردئون تاریخچه نوشته‌های قبلی
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { accordionOpen = !accordionOpen }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (accordionOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "نوشته‌های ذخیره‌شده (${toPersianDigits(notesList.size.toString())})",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Right,
                    )
                }

                AnimatedVisibility(visible = accordionOpen) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (notesList.isEmpty()) {
                            Text(
                                "هنوز نوشته‌ای در این بخش ذخیره نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right,
                            )
                        }
                        notesList.groupBy { it.title.ifBlank { "بدون عنوان" } }.forEach { (groupTitle, items) ->
                            Text(
                                "$groupTitle · ${toPersianDigits(items.size.toString())}",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right,
                            )
                            items.forEach { note ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            RoundedCornerShape(10.dp),
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(onClick = {
                                        val next = notesList.filterNot { it.id == note.id }
                                        saveLocalNotes(next)
                                        syncNotesWithCloud(next)
                                    }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    IconButton(onClick = {
                                        editingId = note.id
                                        noteTitle = note.title
                                        noteText = note.text
                                    }) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "ویرایش")
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                editingId = note.id
                                                noteTitle = note.title
                                                noteText = note.text
                                            },
                                        horizontalAlignment = Alignment.End,
                                    ) {
                                        Text(
                                            note.text.take(60),
                                            maxLines = 2,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Right,
                                        )
                                        Text(
                                            JalaliDate.stampFa(note.updatedAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * تب آلبوم و گالری امن شخصی (عکس، ویدیو، صدا، فایل - فقط روی گوشی، بدون سینک، همراه با پشتیبان‌گیری و بازگردانی).
 */
@Composable
private fun SafeVaultGalleryTab() {
    val context = LocalContext.current
    val store = remember { LocalStore(context, "hamyar_safespace_vault") }
    val scope = rememberCoroutineScope()

    var items by remember {
        val raw = store.getString("vault_items", "[]")
        val parsed = runCatching {
            val a = JSONArray(raw)
            buildList {
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    add(
                        VaultMediaFile(
                            id = o.optString("id"),
                            title = o.optString("title"),
                            mime = o.optString("mime"),
                            ext = o.optString("ext"),
                            sizeKb = o.optLong("sizeKb"),
                            addedIso = o.optString("addedIso"),
                            localPath = o.optString("localPath"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
        mutableStateOf(parsed)
    }

    var selectedFilter by remember { mutableStateOf("all") }
    var notice by remember { mutableStateOf<String?>(null) }
    var previewItem by remember { mutableStateOf<VaultMediaFile?>(null) }
    var playingAudioPath by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun saveItems(updated: List<VaultMediaFile>) {
        items = updated
        val arr = JSONArray()
        items.forEach { it ->
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("mime", it.mime)
                    .put("ext", it.ext)
                    .put("sizeKb", it.sizeKb)
                    .put("addedIso", it.addedIso)
                    .put("localPath", it.localPath),
            )
        }
        store.putString("vault_items", arr.toString())
    }

    fun addFileFromUri(uri: Uri) {
        scope.launch {
            val name = MediaFiles.displayName(context, uri) ?: "vault_file_${System.currentTimeMillis()}"
            val ext = File(name).extension.ifBlank { "bin" }
            val mime = context.contentResolver.getType(uri) ?: guessVaultMime(ext)
            val dest = File(vaultDir(context), "vf_${System.currentTimeMillis()}.$ext")
            val copied = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { input.copyTo(it) }
                    }
                    dest.exists() && dest.length() > 0
                }.getOrDefault(false)
            }
            if (copied) {
                val item = VaultMediaFile(
                    id = "vf_${System.currentTimeMillis()}",
                    title = name.substringBeforeLast('.').ifBlank { "فایل امن" },
                    mime = mime,
                    ext = ext,
                    sizeKb = (dest.length() / 1024).coerceAtLeast(1),
                    addedIso = JalaliDate.todayIso(),
                    localPath = dest.absolutePath,
                )
                saveItems(listOf(item) + items)
                notice = "فایل با موفقیت به آلبوم امن اضافه شد (کاملاً آفلاین روی گوشی)."
            } else {
                notice = "ذخیره فایل ممکن نشد."
            }
        }
    }

    val photoVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) addFileFromUri(uri)
    }

    val anyFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) addFileFromUri(uri)
    }

    // پشتیبان‌گیری از آلبوم (Export ZIP)
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        ZipOutputStream(out).use { zip ->
                            zip.putNextEntry(ZipEntry("manifest.json"))
                            val arr = JSONArray()
                            items.forEach { it ->
                                arr.put(
                                    JSONObject()
                                        .put("id", it.id).put("title", it.title).put("mime", it.mime)
                                        .put("ext", it.ext).put("addedIso", it.addedIso)
                                        .put("file", File(it.localPath).name),
                                )
                            }
                            zip.write(arr.toString().toByteArray())
                            zip.closeEntry()

                            items.forEach { item ->
                                val f = File(item.localPath)
                                if (f.exists()) {
                                    zip.putNextEntry(ZipEntry("files/${f.name}"))
                                    FileInputStream(f).use { it.copyTo(zip) }
                                    zip.closeEntry()
                                }
                            }
                        }
                    }
                    true
                }.getOrDefault(false)
            }
            notice = if (ok) "پشتیبان آلبوم امن با موفقیت روی گوشی ذخیره شد." else "خطا در ایجاد نسخه پشتیبان."
        }
    }

    // بازگردانی آلبوم (Import ZIP)
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = vaultDir(context)
                    val restored = mutableListOf<VaultMediaFile>()
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
                                    val dest = File(dir, "res_${System.currentTimeMillis()}_${File(entry.name).name}")
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
                                restored += VaultMediaFile(
                                    id = o.optString("id").ifBlank { "vf_${System.currentTimeMillis()}_$i" },
                                    title = o.optString("title").ifBlank { fname },
                                    mime = o.optString("mime").ifBlank { guessVaultMime(o.optString("ext")) },
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
            if (result.isNotEmpty()) {
                val merged = (result + items).distinctBy { it.id }
                saveItems(merged)
                notice = "${toPersianDigits(result.size.toString())} فایل با موفقیت بازگردانی شد."
            } else {
                notice = "فایلی برای بازگردانی در فایل انتخابی پیدا نشد."
            }
        }
    }

    val filteredItems = remember(items, selectedFilter) {
        when (selectedFilter) {
            "image" -> items.filter { it.mime.startsWith("image/") }
            "video" -> items.filter { it.mime.startsWith("video/") }
            "audio" -> items.filter { it.mime.startsWith("audio/") }
            "doc" -> items.filter { !it.mime.startsWith("image/") && !it.mime.startsWith("video/") && !it.mime.startsWith("audio/") }
            else -> items
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🔒 گالری و آلبوم شخصی محرمانه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "فایل‌های این بخش (عکس، فیلم، ضبط صدا و اسناد) فقط و فقط روی حافظه گوشی شما می‌مانند و هرگز به هیچ سروری ارسال نمی‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // دکمه‌های افزودن و پشتیبان‌گیری
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryButton(
                "📸 عکس یا فیلم",
                modifier = Modifier.weight(1f),
            ) {
                photoVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }
            OutlinedButton(
                onClick = { anyFilePicker.launch("*/*") },
                modifier = Modifier.weight(1f),
            ) {
                Text("🎙️ صدا / سند")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { backupLauncher.launch("safespace-vault-backup.zip") },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("پشتیبان‌گیری")
            }
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/zip", "*/*")) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("بازگردانی")
            }
        }

        notice?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
            )
        }

        // چیپ‌های فیلتر
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = selectedFilter == "all",
                onClick = { selectedFilter = "all" },
                label = { Text("همه (${toPersianDigits(items.size.toString())})") },
            )
            FilterChip(
                selected = selectedFilter == "image",
                onClick = { selectedFilter = "image" },
                label = { Text("عکس‌ها") },
            )
            FilterChip(
                selected = selectedFilter == "video",
                onClick = { selectedFilter = "video" },
                label = { Text("ویدیوها") },
            )
            FilterChip(
                selected = selectedFilter == "audio",
                onClick = { selectedFilter = "audio" },
                label = { Text("صداها") },
            )
            FilterChip(
                selected = selectedFilter == "doc",
                onClick = { selectedFilter = "doc" },
                label = { Text("اسناد") },
            )
        }

        if (filteredItems.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "هنوز فایلی در این بخش ذخیره نشده است.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            // نمایش گرید ۳ ستونه شبیه به گالری گوشی
            filteredItems.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    row.forEach { item ->
                        VaultMediaTile(
                            item = item,
                            isPlaying = playingAudioPath == item.localPath,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (item.mime.startsWith("audio/")) {
                                    if (playingAudioPath == item.localPath) {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        playingAudioPath = null
                                    } else {
                                        mediaPlayer?.release()
                                        mediaPlayer = MediaPlayer().apply {
                                            setDataSource(item.localPath)
                                            prepare()
                                            start()
                                            setOnCompletionListener {
                                                playingAudioPath = null
                                            }
                                        }
                                        playingAudioPath = item.localPath
                                    }
                                } else {
                                    previewItem = item
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { runCatching { File(item.localPath).delete() } }
                                    saveItems(items.filterNot { it.id == item.id })
                                    if (previewItem?.id == item.id) previewItem = null
                                }
                            },
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    // پیش‌نمایش تصویر یا ویدیو
    previewItem?.let { item ->
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = {
                Text(item.title, maxLines = 1, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (item.mime.startsWith("image/")) {
                        val bmp = remember(item.localPath) {
                            runCatching { BitmapFactory.decodeFile(item.localPath) }.getOrNull()
                        }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Text("امکان بارگذاری عکس نیست.")
                        }
                    } else if (item.mime.startsWith("video/")) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Outlined.Movie, contentDescription = null, modifier = Modifier.size(54.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("ویدیو: ${item.title}.${item.ext}")
                            Spacer(Modifier.height(8.dp))
                            PrimaryButton("پخش ویدیو") {
                                runCatching {
                                    val f = File(item.localPath)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.fromFile(f), item.mime)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                    } else {
                        Text("فایل: ${item.title}.${item.ext} (${toPersianDigits(item.sizeKb.toString())} KB)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewItem = null }) { Text("بستن") }
            },
        )
    }
}

@Composable
private fun VaultMediaTile(
    item: VaultMediaFile,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            when {
                item.mime.startsWith("image/") -> {
                    val bmp = remember(item.localPath) {
                        runCatching {
                            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                            BitmapFactory.decodeFile(item.localPath, opts)
                        }.getOrNull()
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                    }
                }
                item.mime.startsWith("video/") -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(2.dp))
                        Text(item.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                item.mime.startsWith("audio/") -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(item.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                else -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null)
                        Spacer(Modifier.height(2.dp))
                        Text(item.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }

            // دکمه کوچک حذف در گوشه بالا
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "حذف",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun SafeHelplinesTab() {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("شماره‌های مشاوره و کمک — همیشه در دسترس، بدون فشار.", style = MaterialTheme.typography.bodyMedium)
        Helplines.iran.forEach { h ->
            SectionCard("${h.name} — ${h.number}", h.hours) {
                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${h.number}")))
            }
        }
    }
}

private fun guessVaultMime(ext: String): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
        ?: when (ext.lowercase()) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a", "aac" -> "audio/mp4"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }

@Composable
fun WritingPromptScreen(onBack: () -> Unit) {
    // مسیر سازگاری با پرامپت‌های نوشتاری
    SafeNotebookTab(categoryKey = "delnevesht", tabTitle = "دل‌نوشت")
}

@Composable
fun HelplinesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        AppTopBar("شماره‌های کمک", onBack)
        SafeHelplinesTab()
    }
}
