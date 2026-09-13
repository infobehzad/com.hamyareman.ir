package com.hamyareman.ir.ui.study

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.appwrite.AppwriteAuthService
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.common.BucketIds
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.platform.feature.hearttoheart.MediaFiles
import com.hamyareman.ir.LocalAppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * جزوه‌ی PDF.
 *
 * رفتار واقعی: فایل از گوشی انتخاب می‌شود، یک نسخه‌ی محلی در حافظه‌ی خصوصی اپ
 * (`files/media/pdfs`) می‌ماند، و **فقط** اگر Appwrite پیکربندی شده باشد به سطل
 * `zahra-private` می‌رود — سطلی که پدر هیچ دسترسی خواندنی به آن ندارد.
 */
internal data class PdfItem(
    val id: String,
    val name: String,
    val sizeKb: Long,
    val addedIso: String,
    val localPath: String,
    val reference: String,
) {
    val isOnServer: Boolean get() = reference.isNotBlank()
}

private const val KEY_PDFS = "study_pdfs"

internal fun readPdfs(store: LocalStore): List<PdfItem> = runCatching {
    val array = JSONArray(store.getString(KEY_PDFS, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(
                PdfItem(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    sizeKb = o.optLong("sizeKb"),
                    addedIso = o.optString("addedIso"),
                    localPath = o.optString("localPath"),
                    reference = o.optString("reference"),
                ),
            )
        }
    }
}.getOrDefault(emptyList())

private fun writePdfs(store: LocalStore, items: List<PdfItem>) {
    val array = JSONArray()
    items.forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id).put("name", item.name).put("sizeKb", item.sizeKb)
                .put("addedIso", item.addedIso).put("localPath", item.localPath)
                .put("reference", item.reference),
        )
    }
    store.putString(KEY_PDFS, array.toString())
}

private fun pdfDir(context: android.content.Context): File =
    File(context.filesDir, "media/pdfs").apply { mkdirs() }

@Composable
fun PdfUploadScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = container.store
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf(readPdfs(store)) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked == null) return@rememberLauncherForActivityResult
        busy = true
        notice = null
        scope.launch {
            val name = MediaFiles.displayName(context, picked) ?: "joozve.pdf"
            val copied: File? = withContext(Dispatchers.IO) {
                runCatching {
                    val target = File(pdfDir(context), "pdf_${System.currentTimeMillis()}.pdf")
                    context.contentResolver.openInputStream(picked)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    target.takeIf { it.exists() && it.length() > 0 }
                }.getOrNull()
            }
            if (copied == null) {
                notice = "کپی‌گرفتن از فایل ممکن نشد."
                busy = false
                return@launch
            }
            if (copied.length() > MediaFiles.MAX_BYTES) {
                copied.delete()
                notice = "فایل بزرگ‌تر از ۱۰ مگابایت است؛ کوچک‌ترش کن."
                busy = false
                return@launch
            }

            // آپلود به سطل خصوصی — فقط وقتی بک‌اند پیکربندی شده باشد.
            var reference = ""
            if (container.storage.isConfigured) {
                val userId = store.getString(AppwriteAuthService.KEY_USER_ID)
                val permissions = if (userId.isBlank()) emptyList() else AppwriteClientProvider.ownerOnly(userId)
                when (val upload = container.storage.upload(BucketIds.ZAHRA_PRIVATE, copied.absolutePath, permissions)) {
                    is AppResult.Ok -> reference = upload.value.reference
                    is AppResult.Err -> notice = "روی دستگاه ذخیره شد، ولی آپلود ناموفق بود: ${upload.error.userMessage}"
                }
            }

            val item = PdfItem(
                id = "pdf_${System.currentTimeMillis()}",
                name = name,
                sizeKb = (copied.length() / 1024).coerceAtLeast(1),
                addedIso = JalaliDate.todayIso(),
                localPath = copied.absolutePath,
                reference = reference,
            )
            items = listOf(item) + items
            writePdfs(store, items)
            notice = if (reference.isBlank()) {
                "فایل روی دستگاه می‌ماند (بک‌اند وصل نیست یا آپلود نشد)."
            } else {
                "ذخیره شد — هم روی دستگاه، هم در سطل خصوصی تو در سرور."
            }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("جزوه PDF", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionCard(
                title = "حریم خصوصی این فایل",
                body = "فایل در حافظه‌ی خصوصی اپ می‌ماند و اگر Sync روشن باشد به سطل `zahra-private` می‌رود؛ " +
                    "پدر هیچ دسترسی خواندنی به آن سطل ندارد.",
            ) { }

            PrimaryButton(if (busy) "در حال ذخیره…" else "انتخاب فایل PDF") {
                if (!busy) picker.launch(arrayOf("application/pdf"))
            }
            notice?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            if (items.isEmpty()) {
                Text("هنوز جزوه‌ای اضافه نکردی.", style = MaterialTheme.typography.bodySmall)
            }
            items.forEach { item ->
                SectionCard(
                    title = item.name,
                    body = "${item.sizeKb} کیلوبایت · ${JalaliDate.formatFaLong(item.addedIso)} · " +
                        if (item.isOnServer) "روی سرور و دستگاه" else "فقط روی دستگاه",
                ) { }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        if (item.isOnServer) {
                            val stored = com.hamyareman.ir.platform.core.appwrite.StoredFile.parse(item.reference)
                            if (stored != null) {
                                scope.launch {
                                    val url = container.storage.viewUrl(stored.bucketId, stored.fileId)
                                    if (url == null) {
                                        notice = "بازکردن از سرور ممکن نشد."
                                    } else {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)),
                                            )
                                        }.onFailure { notice = "مرورگر باز نشد." }
                                    }
                                }
                            }
                        } else {
                            runCatching {
                                val file = File(item.localPath)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                )
                            }.onFailure { notice = "برنامه‌ای برای بازکردن PDF پیدا نشد." }
                        }
                    }) { Text(if (item.isOnServer) "بازکردن از سرور" else "بازکردن") }

                    TextButton(onClick = {
                        scope.launch {
                            if (item.isOnServer) {
                                com.hamyareman.ir.platform.core.appwrite.StoredFile.parse(item.reference)?.let { stored ->
                                    container.storage.delete(stored.bucketId, stored.fileId)
                                }
                            }
                            withContext(Dispatchers.IO) { runCatching { File(item.localPath).delete() } }
                            items = items.filterNot { it.id == item.id }
                            writePdfs(store, items)
                            notice = "حذف شد."
                        }
                    }) { Text("حذف") }
                }
            }

            PrimaryButton("بازگشت", onBack)
        }
    }
}
