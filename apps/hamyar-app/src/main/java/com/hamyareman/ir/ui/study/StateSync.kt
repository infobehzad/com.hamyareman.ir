package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * سینکِ عمومیِ «وضعیتِ برنامه» روی جدول `app_state`.
 *
 * هر کلید (مثلاً برنامهٔ هفتگی، تنظیمات شیفت، کلاس مجازی، تیک‌های روزانه)
 * یک سطر با rowId قطعی دارد. آخرین نوشته برنده است (`updatedAt` بزرگ‌تر).
 * اگر سرور در دسترس نباشد، هیچ استثنایی بالا نمی‌آید و برنامه روی همان
 * نسخهٔ محلی ادامه می‌دهد.
 */
object StateSync {

    const val TABLE = com.hamyareman.ir.platform.core.common.TableIds.APP_STATE

    // --- کلیدها ---
    const val KEY_WEEK = "class_plan_week"
    const val KEY_SHIFT = "class_plan_shift"
    const val KEY_VIRTUAL = "virtual_class"
    const val KEY_CHECKS = "daily_checks"
    const val KEY_NOTE_TITLES = "note_titles"

    private const val PREF = "hamyar_state_sync"
    private const val KEY_LAST = "last_sync_at"

    fun rowId(uid: String, key: String) = "state_${uid}_$key"

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    /** آخرین زمانِ سینک موفق (برای نمایش در UI). */
    fun lastSyncAt(ctx: Context): Long = store(ctx).getLong(KEY_LAST, 0L)

    /** ذخیره روی سرور (نسخهٔ محلی هم به‌روز می‌شود). */
    suspend fun push(
        ctx: Context,
        tables: TablesDbService,
        uid: String,
        key: String,
        payload: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext false
        val now = System.currentTimeMillis()
        val ok = tables.upsert(
            TABLE,
            rowId(uid, key),
            mapOf(
                "userId" to uid,
                "key" to key,
                "payload" to payload,
                "updatedAt" to now,
            ),
            AppwriteClientProvider.ownerOnly(uid),
        ) is AppResult.Ok
        if (ok) {
            store(ctx).putString("local_$key", payload)
            store(ctx).putLong("at_$key", now)
            store(ctx).putLong(KEY_LAST, now)
        }
        ok
    }

    /**
     * خواندن از سرور؛ اگر سطر نبود → null. مقدارِ برگشتی = payload و updatedAt.
     */
    suspend fun pull(
        ctx: Context,
        tables: TablesDbService,
        uid: String,
        key: String,
    ): Pair<String, Long>? = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext null
        when (val r = tables.get(TABLE, rowId(uid, key))) {
            is AppResult.Ok -> {
                val row = r.value ?: return@withContext null
                val payload = row.string("payload")
                if (payload.isBlank()) return@withContext null
                payload to row.long("updatedAt")
            }
            is AppResult.Err -> null
        }
    }

    /**
     * یکی‌کردن: هر طرف جدیدتر بود برنده می‌شود و نتیجه روی هر دو طرف نوشته می‌شود.
     * [localAt] زمانِ آخرین تغییرِ محلی است.
     */
    suspend fun merge(
        ctx: Context,
        tables: TablesDbService,
        uid: String,
        key: String,
        localPayload: String,
        localAt: Long,
        onRemoteNewer: (String) -> Unit,
    ) {
        if (uid.isBlank()) return
        val remote = pull(ctx, tables, uid, key)
        if (remote == null) {
            push(ctx, tables, uid, key, localPayload)
            return
        }
        val (remotePayload, remoteAt) = remote
        if (remoteAt > localAt && remotePayload != localPayload) {
            onRemoteNewer(remotePayload)
        } else if (localAt >= remoteAt && remotePayload != localPayload) {
            push(ctx, tables, uid, key, localPayload)
        }
    }

    fun localAt(ctx: Context, key: String): Long = store(ctx).getLong("at_$key", 0L)
}
