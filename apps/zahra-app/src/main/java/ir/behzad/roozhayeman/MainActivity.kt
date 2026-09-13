package ir.behzad.roozhayeman

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
import ir.behzad.platform.core.common.AppResult
import ir.behzad.platform.core.designsystem.BrandTheme
import ir.behzad.platform.core.designsystem.PinLockGate
import ir.behzad.platform.core.designsystem.PlatformTheme
import ir.behzad.platform.core.notifications.NotificationPermissions
import ir.behzad.platform.core.security.AppLock
import ir.behzad.platform.core.security.BiometricPromptRunner
import ir.behzad.roozhayeman.di.AppContainer
import ir.behzad.roozhayeman.ui.appearance.FontLibrary
import ir.behzad.roozhayeman.ui.appearance.LocalUiPrefs
import ir.behzad.platform.feature.calls.IncomingCallsHost
import ir.behzad.roozhayeman.ui.auth.LoginScreen
import ir.behzad.roozhayeman.ui.navigation.ZahraNavHost
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
        val app = application as RoozhayeManApplication
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
        val container = (application as RoozhayeManApplication).container
        unlocked.value = !container.lock.isLockedNow()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureTeachIntent(intent)
    }

    /** extra درسِ اعلان پخش → [TeachLaunch] (nav به صفحه‌ی تدریس می‌پرد). */
    private fun captureTeachIntent(intent: android.content.Intent?) {
        val action = ir.behzad.platform.feature.playback.PlaybackService.TEACH_OPEN_ACTION
        val packId = intent?.getStringExtra(ir.behzad.platform.feature.playback.PlaybackService.TEACH_OPEN_EXTRA)
        if (intent?.action == action && !packId.isNullOrBlank()) {
            ir.behzad.roozhayeman.ui.study.TeachLaunch.pendingTeachPack = packId
            ir.behzad.platform.feature.playback.TeachGate.requestedPack = null
            return
        }
        // پلیِ اعلان وقتی اکتیویتی بدون extra بالا آمده (همان پروسه) — از گیتِ سرویس بخوان.
        val req = ir.behzad.platform.feature.playback.TeachGate.requestedPack
        if (!req.isNullOrBlank()) {
            ir.behzad.roozhayeman.ui.study.TeachLaunch.pendingTeachPack = req
            ir.behzad.platform.feature.playback.TeachGate.requestedPack = null
        }
    }

    override fun onPause() {
        super.onPause()
        // خروج از اپ = شروع دوباره‌ی تایمر قفل خودکار (اگر کاربر فعالش کرده باشد).
        (application as RoozhayeManApplication).container.lock.lock()
    }
}
