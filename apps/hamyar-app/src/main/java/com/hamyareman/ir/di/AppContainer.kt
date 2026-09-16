package com.hamyareman.ir.di

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.appwrite.services.Account
import com.hamyareman.ir.platform.core.appwrite.AppwriteAuthService
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.appwrite.AppwriteFunctionsService
import com.hamyareman.ir.platform.core.appwrite.AppwriteRealtimeFeed
import com.hamyareman.ir.platform.core.appwrite.AppwriteStorageService
import com.hamyareman.ir.platform.core.appwrite.AppwriteTablesDbService
import com.hamyareman.ir.platform.core.appwrite.ServerActions
import com.hamyareman.ir.platform.feature.study.StudyPackRepository
import com.hamyareman.ir.platform.feature.study.StudyProgressRepository
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.ScreenTimeTracker
import com.hamyareman.ir.platform.core.common.UserRole
import com.hamyareman.ir.platform.core.notifications.QuietHoursManager
import com.hamyareman.ir.platform.core.notifications.ReminderScheduler
import com.hamyareman.ir.platform.core.security.AppLock
import com.hamyareman.ir.platform.core.security.BiometricUnlock
import com.hamyareman.ir.platform.core.security.Encryptor
import com.hamyareman.ir.platform.core.sync.SyncEngine
import com.hamyareman.ir.platform.feature.calls.CallEngine
import com.hamyareman.ir.platform.feature.calls.CallSignaling
import com.hamyareman.ir.platform.feature.hearttoheart.AlbumAuthor
import com.hamyareman.ir.platform.feature.hearttoheart.AlbumRepository
import com.hamyareman.ir.platform.feature.hearttoheart.HeartRepository
import com.hamyareman.ir.platform.feature.pairing.AppwritePairingRepository
import com.hamyareman.ir.platform.feature.playback.LessonMediaProgressRepository
import com.hamyareman.ir.ui.wellness.WellnessLogRepository
import com.hamyareman.ir.ui.wellness.WellnessMoveRepository
import com.hamyareman.ir.ui.wellness.WellnessTimingProvider
import com.hamyareman.ir.BuildConfig
import com.hamyareman.ir.ui.chatbot.AiCompanion
import com.hamyareman.ir.ui.content.CatalogRepository
import com.hamyareman.ir.platform.feature.calls.IncomingCallWatcher

/**
 * گراف وابستگی دستی اپ زهرا (بدون Hilt/Koin تا بیلد ساده و قابل‌اشکال‌زدایی بماند).
 *
 * هر سرویس طوری ساخته شده که اگر Appwrite پیکربندی نشده باشد، اپ در «حالت محلی»
 * کار کند و کاربر پیام صادقانه بگیرد.
 */
class AppContainer(context: Context) {

    /** داده‌های اپ زهرا (تنظیمات، کش، رمز PIN). */
    val store = LocalStore(context)

    /** کانتکست اپلیکیشن برای صفحاتی که Context لازم دارند (مثل پلیر). */
    val appContext = context.applicationContext

    /** ظاهر اپ (تم/حالت رنگ/فونت) — سراسری و پایدار. */
    val uiPrefs = com.hamyareman.ir.ui.appearance.UiPrefs(context)

    /** استور مشترک پیوند — اپ پدر روی همان دستگاه هم آن را می‌خواند. */
    private val pairingStore = LocalStore(context, AppwritePairingRepository.PAIRING_STORE)

    val appwrite = AppwriteClientProvider(
        context = context,
        endpoint = BuildConfig.APPWRITE_ENDPOINT,
        projectId = BuildConfig.APPWRITE_PROJECT_ID,
        databaseId = BuildConfig.APPWRITE_DATABASE_ID,
    )

    init {
        // سینک ابری آمار تدریس: هر رویداد → صف outbox؛ ارسال در لحظه‌های مناسب.
        com.hamyareman.ir.ui.study.TeachStats.cloudSink = { packId ->
            com.hamyareman.ir.ui.study.TeachCloud.enqueue(context, sync, auth.cachedUserId(), packId)
            CoroutineScope(Dispatchers.IO).launch { runCatching { sync.pushAll() } }
        }
        StudyProgressRepository.afterWrite = { _ ->
            CoroutineScope(Dispatchers.IO).launch { runCatching { sync.pushAll() } }
        }
        // «Ping» Appwrite: یک درخواست واقعی در شروع اپ تا اتصال در کنسول دیده شود.
        // (SDK اندروید متد ping() ندارد؛ Account.get() سبک‌ترین درخواست احرازشده است.)
        if (appwrite.isConfigured) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { Account(appwrite.client).get() }
                    .onSuccess { Log.i("AppwritePing", "اتصال Appwrite تأیید شد (پروژه: ${BuildConfig.APPWRITE_PROJECT_NAME})") }
                    .onFailure { Log.w("AppwritePing", "Appwrite پاسخ داد: ${it.message}") }
            }
        }
    }

    val functions = AppwriteFunctionsService(appwrite)

    /**
     * کلاینتِ تایپ‌شده‌ی توابع سرور (`ServerActions`).
     *
     * منطق‌هایی که باید **سمت سرور** باشند تا یک اپ دستکاری‌شده نتواند دورشان بزند:
     * خبردادن به پدر، تأیید خاطره‌ی آلبوم، انتخاب درس امروز، اثر انگشت کاتالوگ
     * و خلاصه‌ی روزانه. اگر تابعی deploy نشده باشد، همه‌ی فراخوانی‌ها `Err` می‌دهند
     * و اپ روی مسیر محلی خودش می‌ماند.
     */
    val serverActions = ServerActions(functions)
    val tables = AppwriteTablesDbService(appwrite)
    val storage = AppwriteStorageService(appwrite)

    /**
     * به‌روزرسانی زنده (WebSocket) روی TablesDB.
     *
     * کانال‌ها: `tablesdb.<db>.tables.<table>.rows`. اگر بک‌اند تنظیم نشده باشد
     * یا socket خطا بدهد، Flowها خالی می‌مانند و اپ روی همان مسیر قبلی
     * (خواندن دوره‌ای/دستی) کار می‌کند — یعنی Realtime بهینه‌سازی است، نه وابستگی.
     */
    val realtime = AppwriteRealtimeFeed(appwrite)

    val auth = AppwriteAuthService(
        provider = appwrite,
        fallbackRole = UserRole.ZAHRA,
        store = store,
        functions = functions,
    )

    val pairing = AppwritePairingRepository(
        functions = functions,
        tables = tables,
        auth = auth,
        pairingStore = pairingStore,
        role = UserRole.ZAHRA,
    )

    val heart = HeartRepository(
        store = store,
        tables = tables,
        storage = storage,
        provider = appwrite,
        partnerUserId = { pairing.cachedLink().partnerId },
        realtime = realtime,
    )

    /**
     * آلبوم خاطرات مشترک با پدر.
     *
     * مالک آلبوم همیشه زهراست؛ خاطره‌ی پدر با `approved=false` ساخته می‌شود
     * و تا زهرا تأیید نکند در آلبوم او نمی‌نشیند.
     */
    val album = AlbumRepository(
        store = store,
        tables = tables,
        storage = storage,
        author = AlbumAuthor.ZAHRA,
        selfId = { store.getString(AppwriteAuthService.KEY_USER_ID) },
        zahraId = { store.getString(AppwriteAuthService.KEY_USER_ID) },
        realtime = realtime,
        serverActions = serverActions,
        fatherId = { pairing.cachedLink().partnerId },
    )

    val signaling = CallSignaling(tables, appwrite, realtime)

    val calls = CallEngine(
        context = context,
        signaling = signaling,
        selfUserId = { auth.currentUserId() },
    )

    /**
     * نگهبانِ زنگِ تماس ورودی.
     *
     * تا پیش از این `acceptIncoming` وجود داشت ولی هیچ‌وقت صدا زده نمی‌شد، یعنی یک طرف
     * زنگ می‌زد و طرف دیگر هیچ‌وقت نمی‌دید. این کلاس سطرهای `call_sessions` با
     * `status="ringing"` را از دو مسیر می‌خواند (Realtime + polling به‌عنوان تور ایمنی)
     * و زنگ/لرزش و صفحه‌ی «پاسخ/رد» را بالا می‌آورد.
     *
     * فقط وقتی اپ باز است کار می‌کند؛ برای زنگ در حالت بسته‌بودن اپ به سرویس
     * پیش‌زمینه‌ی دائمی + FCM نیاز است (عمداً اضافه نشد: باتری و حریم خصوصی).
     */
    val incomingCalls = IncomingCallWatcher(
        context = context,
        signaling = signaling,
        selfUserId = { auth.currentUserId() },
        realtime = realtime,
    )

    /**
     * زمان صفحه با UsageStatsManager. داده‌اش در `neverSyncTables` است و هرگز به
     * سرور نمی‌رود؛ اگر مجوز ویژه داده نشده باشد، عدد جعلی نشان نمی‌دهیم.
     */
    val screenTime = ScreenTimeTracker(context, store)

    /**
     * کاتالوگ محتوای خواندنی (آشپزی، درس، آزمون، نقشه‌ی راه، ایده‌ی نقاشی).
     * اولویت: سرور → کش محلی → محتوای داخلی اپ (آفلاین).
     */
    val catalog = CatalogRepository(tables, store, serverActions)

    val lock = AppLock(store)

    /** موتور سینک outbox — قبل از همه‌ی ریپوهایی که به آن ارجاع می‌دهند. */
    val sync = SyncEngine(store, tables)

    /**
     * پرامپت ۰۱: حافظه‌ی پیشرفت پلیر ویدیو/صوت.
     *
     * اگر بک‌اند تنظیم نشده باشد، همه‌ی نوشتن‌ها فقط در کش محلی انجام می‌شود و وقتی
     * Appwrite پیکربندی شد، صف outbox در [SyncEngine] آن‌ها را بالاخره می‌فرستد.
     */
    val mediaProgress = LessonMediaProgressRepository(
        store = store,
        tables = tables,
        provider = appwrite,
        sync = sync,
    )

    /**
     * پرامپت ۰۲: کاتالوگ حرکات سلامتی + گزارش جلسه‌ها.
     * الگوی سه‌لایه‌ای: سرور → کش محلی → کاتالوگ داخلی.
     */
    val wellnessMoves = WellnessMoveRepository(
        store = store,
        tables = tables,
        provider = appwrite,
    )
    val wellnessLogs = WellnessLogRepository(
        store = store,
        tables = tables,
        provider = appwrite,
        sync = sync,
    )
    val wellnessTiming = WellnessTimingProvider()

    /** بیومتریک فقط «راه جایگزینِ بازکردن همان قفل PIN» است؛ بدون PIN فعال نمی‌شود. */
    val biometric = BiometricUnlock(store, lock)
    val quiet = QuietHoursManager(store)
    val reminders = ReminderScheduler(context)

    /** کلید در Android Keystore است؛ هیچ بایتی از آن روی دیسک نمی‌ماند. */
    val encryptor = Encryptor()

    /**
     * لایه‌ی AI «همراه زهرا».
     *
     * کلید مدل **در اپ نیست**: فقط تابع سرور `ai-companion` آن را دارد.
     * اگر بک‌اند تنظیم نشده باشد یا AI خاموش باشد، گفت‌وگو با قواعد محلی ادامه پیدا می‌کند
     * و پیام‌های بحران هیچ‌وقت به سرور نمی‌روند.
     * حافظه‌ی گفت‌وگو هم مثل ژورنال با همین [encryptor] رمز می‌شود (بعد از آن تعریف شده
     * چون ترتیب مقداردهی اولیه در کلاس مهم است).
     */
    val ai = AiCompanion(functions, store, encryptor, serverActions)

    /**
     * پرامپت ۰۴: بسته‌های مطالعه از assets (پایلوت: C905_E01-L01).
     * آفلاین کامل — بعداً با محتوای کامل کاربر جایگزین می‌شود.
     */
    val studyPacks = StudyPackRepository(context)

    /**
     * پرامپت ۰۴: پیشرفت مطالعه (وضعیت SRS فلش‌کارت‌ها + تاریخچه‌ی آزمون‌ها).
     * نوشتن اول در کش محلی؛ سپس صف outbox در [SyncEngine] به جدول
     * `study_progress` می‌فرستد. بعد از `sync` تعریف شده چون به آن نیاز دارد.
     */
    val studyProgress = StudyProgressRepository(
        store = store,
        sync = sync,
        userIdProvider = { auth.cachedUserId().orEmpty() },
    )

    val role: UserRole get() = auth.cachedRole()
    val partnerId: String? get() = pairing.cachedLink().partnerId
    val fatherTel: String get() = store.getString("father_tel", "")
    val isBackendConfigured: Boolean get() = appwrite.isConfigured
}
