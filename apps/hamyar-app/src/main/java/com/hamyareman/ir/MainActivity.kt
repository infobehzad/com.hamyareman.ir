package com.hamyareman.ir

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.designsystem.BrandTheme
import com.hamyareman.ir.platform.core.designsystem.PinLockGate
import com.hamyareman.ir.platform.core.designsystem.PlatformTheme
import com.hamyareman.ir.platform.core.notifications.NotificationPermissions
import com.hamyareman.ir.platform.core.security.AppLock
import com.hamyareman.ir.platform.core.security.BiometricPromptRunner
import com.hamyareman.ir.di.AppContainer
import com.hamyareman.ir.ui.appearance.FontLibrary
import com.hamyareman.ir.ui.appearance.LocalUiPrefs
import com.hamyareman.ir.platform.feature.calls.IncomingCallsHost
import com.hamyareman.ir.ui.auth.LoginScreen
import com.hamyareman.ir.ui.navigation.ZahraNavHost
import kotlinx.coroutines.launch

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer missing") }

/**
 * [FragmentActivity] به‌جای ComponentActivity: سازنده‌ی `androidx.biometric.BiometricPrompt`
 * فقط FragmentActivity (یا Fragment) می‌گیرد. FragmentActivity خودش از ComponentActivity
 * ارث می‌برد، پس `enableEdgeToEdge()` و `setContent {}` دقیقاً مثل قبل کار می‌کنند.
 *
 * ترتیب بازکردن قفل:
 * 1. اگر قفل فعال نیست → مستقیم داخل اپ.
 * 2. اگر بیومتریک روشن است و دستگاه آماده → پرامپت به‌صورت خودکار بالا می‌آید.
 * 3. وگرنه (یا اگر کاربر «واردکردن PIN» را زد) → همان PIN همیشگی.
 *
 * نکته‌ی امنیتی: بیومتریک هرگز جای PIN را نمی‌گیرد؛ فقط `AppLock.markUnlocked()` را صدا می‌زند.
 */
class MainActivity : FragmentActivity() {

    /**
     * وضعیت قفل به‌صورت state نگه داشته می‌شود تا در `onResume` (بعد از بازگشت از
     * پس‌زمینه) دوباره ارزیابی شود و تایم‌اوت قفل خودکار واقعاً کار کند.
     */
    private val unlocked = mutableStateOf(true)

    /** null = در حال بررسی سشن؛ true = وارد شده؛ false = باید صفحه‌ی ورود ببیند. */
    private val loggedIn = mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as HamyarApplication
        enableEdgeToEdge()
        unlocked.value = !app.container.lock.isLockedNow()
        // لمس اعلان پخش (حتی با اپ کاملاً بسته) → بعد از لاگین/باز شدن قفل،
        // صفحه‌ی تدریس همان درس باز و پخش همان‌جا شروع می‌شود.
        captureTeachIntent(intent)

        setContent {
            val container = app.container
            val activity = this@MainActivity

            val notificationLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { /* اگر کاربر نداد، یادآورها بی‌صدا می‌مانند؛ اصرار نمی‌کنیم */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationPermissions.isGranted(activity)
                ) {
                    notificationLauncher.launch(NotificationPermissions.POST_NOTIFICATIONS)
                }
            }

            val isUnlocked by unlocked
            var bioBusy by remember { mutableStateOf(false) }
            var bioNotice by remember { mutableStateOf<String?>(null) }

            // هر بار که صفحه‌ی قفل نشان داده می‌شود دوباره بررسی می‌شود (مثلاً کاربر همین
            // الان در تنظیمات بیومتریک را روشن کرده باشد یا حسگر موقتاً از دسترس خارج شده باشد).
            val offerBiometric = remember(isUnlocked) { container.biometric.shouldOffer(activity) }

            fun unlockWithBiometric() {
                bioBusy = true
                bioNotice = null
                BiometricPromptRunner.show(
                    activity = activity,
                    title = "همیار من",
                    subtitle = "برای بازکردن دفترچه‌ات اثر انگشت یا چهره‌ات را تأیید کن.",
                    negativeText = "واردکردن PIN",
                    onSuccess = {
                        container.lock.markUnlocked()
                        bioBusy = false
                        unlocked.value = true
                    },
                    onError = { message ->
                        bioBusy = false
                        bioNotice = message
                    },
                    onCancelled = { bioBusy = false },
                )
            }

            // وقتی اپ قفل است و بیومتریک فعال، پرامپت خودش بالا می‌آید تا کاربر معطل نشود.
            LaunchedEffect(isUnlocked, offerBiometric) {
                if (!isUnlocked && offerBiometric) unlockWithBiometric()
            }

            val scope = androidx.compose.runtime.rememberCoroutineScope()
            var loginLoading by remember { mutableStateOf(false) }
            var loginError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                // مصوب: اولِ هر اجرای اپ، دروازه‌ی ورود (گوگل/مهمان) نشان داده می‌شود —
                // سشنِ مانده‌ی قبلی به‌صورت خودکار وارد نمی‌کند.
                if (loggedIn.value != false) loggedIn.value = false
            }

            // v1.14: با ورود، همه‌ی آمار مدرسه (تدریس/فلش‌کارت/آزمون/نمودار پیشرفت)
            // از سرور بازیابی و ادغام می‌شود — تعویض گوشی یا نصب مجدد هیچ‌چیز را از دست نمی‌دهد.
            LaunchedEffect(loggedIn.value == true) {
                if (loggedIn.value == true) {
                    val uid = runCatching { container.auth.currentUserId() }.getOrNull()
                    runCatching {
                        com.hamyareman.ir.ui.study.SchoolSync.restoreAll(activity, container.tables, container.sync, uid)
                    }
                }
            }

            val uiPrefs = container.uiPrefs
            CompositionLocalProvider(LocalUiPrefs provides uiPrefs) {
                PlatformTheme(
                    brand = uiPrefs.theme,
                    darkTheme = uiPrefs.darkTheme,
                    fontFamily = FontLibrary.fontFamilyFor(activity, uiPrefs.fontKey),
                ) {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    Surface(Modifier.fillMaxSize()) {
                        when {
                            // ۰) هنوز وضعیت سشن نامعلوم است — لحظه‌ای خالی تا پرش نبینیم.
                            loggedIn.value == null -> Unit

                            // ۱) وارد نشده: دروازه‌ی لاگین (گوگل/مهمان) قبل از هر محتوایی.
                            loggedIn.value == false -> LoginScreen(
                                loading = loginLoading,
                                error = loginError,
                                onGoogle = {
                                    // v1.21: مسیر استاندارد Appwrite OAuth2 — گوگل Secret را با
                                    // کنسول چک می‌کند، صفحه‌ی انتخاب اکانت باز می‌شود و با
                                    // دیپلینک به اپ برمی‌گردد. (کد native لیست‌اکانت‌ها در
                                    // googleIdTokenFlow محفوظ مانده؛ اگر روزی لازم شد سوئیچ می‌شود.)
                                    loginLoading = true; loginError = null
                                    scope.launch {
                                        when (val r = container.auth.signInWithGoogle(activity)) {
                                            is AppResult.Ok -> loggedIn.value = true
                                            is AppResult.Err -> { loginError = r.error.userMessage; loginLoading = false }
                                        }
                                    }
                                },
                                onGuest = {
                                    loginLoading = true; loginError = null
                                    scope.launch {
                                        when (val r = container.auth.signInAsGuest()) {
                                            is AppResult.Ok -> loggedIn.value = true
                                            is AppResult.Err -> { loginError = r.error.userMessage; loginLoading = false }
                                        }
                                    }
                                },
                            )

                            // ۲) وارد شده و قفل باز: اپ.
                            isUnlocked -> {
                            ZahraNavHost()

                            // زنگِ تماس ورودی روی هر صفحه‌ای بالا می‌آید — ولی فقط بعد از
                            // بازشدن قفل: تا اپ قفل است حتی نام تماس‌گیرنده نشان داده نمی‌شود.
                            IncomingCallsHost(
                                watcher = container.incomingCalls,
                                engine = container.calls,
                                phoneFallback = container.fatherTel,
                                remoteLabel = "بابا",
                                remoteUserId = container.partnerId,
                            )
                            }
                            // ۳) وارد شده ولی قفل فعال: صفحه‌ی PIN.
                            else -> {
                            PinLockGate(
                                title = "همیار من قفل است",
                                subtitle = "برای دیدن دفترچه‌ات PIN را وارد کن.",
                                minLength = AppLock.MIN_PIN,
                                maxLength = AppLock.MAX_PIN,
                                biometricLabel = if (offerBiometric) "بازکردن با اثر انگشت/چهره" else null,
                                biometricBusy = bioBusy,
                                externalNotice = bioNotice,
                                onBiometricRequest = if (offerBiometric) {
                                    { unlockWithBiometric() }
                                } else {
                                    null
                                },
                                onVerify = { pin ->
                                    container.lock.verify(pin).also { ok -> if (ok) container.lock.markUnlocked() }
                                },
                                onUnlocked = { unlocked.value = true },
                            )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val container = (application as HamyarApplication).container
        unlocked.value = !container.lock.isLockedNow()
    }

    /**
     * ورود استاندارد گوگل روی اندروید (Credential Manager) — بدون مرورگر و بدون
     * دیالوگ واسط: خود سیستم لیست اکانت‌های گوگلِ روی گوشی را مستقیم نشان می‌دهد.
     * idToken برای صحت‌سنجی و ساخت سشن به تابع سرور «google-auth» می‌رود.
     */
    private fun googleIdTokenFlow(onToken: (String, String) -> Unit, onError: (String) -> Unit) {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (webClientId.isBlank()) {
            onError("شناسه‌ی وب گوگل (googleWebClientId) تنظیم نشده است.")
            return
        }
        val nonce = java.util.UUID.randomUUID().toString().replace("-", "").take(24)
        val hashedNonce = java.security.MessageDigest.getInstance("SHA-256")
            .digest(nonce.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val option = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            // false = همه‌ی اکانت‌های روی گوشی لیست شوند (نه فقط قبلی‌ها)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = androidx.credentials.GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val cm = androidx.credentials.CredentialManager.create(this)
        lifecycleScope.launch {
            try {
                val resp = cm.getCredential(this@MainActivity, request)
                val cred = resp.credential
                if (cred is androidx.credentials.CustomCredential &&
                    cred.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val gc = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(cred.data)
                    onToken(gc.idToken, hashedNonce)
                } else {
                    onError("نوع اعتبار پشتیبانی نشد.")
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                onError("") // کاربر بست — بی‌پیام
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                onError("انتخاب اکانت ممکن نشد: " + (e.localizedMessage ?: e.javaClass.simpleName))
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "خطای ناشناخته در ورود گوگل")
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureTeachIntent(intent)
    }

    /** extra درسِ اعلان پخش → [TeachLaunch] (nav به صفحه‌ی تدریس می‌پرد). */
    private fun captureTeachIntent(intent: android.content.Intent?) {
        val action = com.hamyareman.ir.platform.feature.playback.PlaybackService.TEACH_OPEN_ACTION
        val packId = intent?.getStringExtra(com.hamyareman.ir.platform.feature.playback.PlaybackService.TEACH_OPEN_EXTRA)
        if (intent?.action == action && !packId.isNullOrBlank()) {
            com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack = packId
            com.hamyareman.ir.platform.feature.playback.TeachGate.requestedPack = null
            return
        }
        // پلیِ اعلان وقتی اکتیویتی بدون extra بالا آمده (همان پروسه) — از گیتِ سرویس بخوان.
        val req = com.hamyareman.ir.platform.feature.playback.TeachGate.requestedPack
        if (!req.isNullOrBlank()) {
            com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack = req
            com.hamyareman.ir.platform.feature.playback.TeachGate.requestedPack = null
        }
    }

    override fun onPause() {
        super.onPause()
        // خروج از اپ = شروع دوباره‌ی تایمر قفل خودکار (اگر کاربر فعالش کرده باشد).
        (application as HamyarApplication).container.lock.lock()
    }
}
