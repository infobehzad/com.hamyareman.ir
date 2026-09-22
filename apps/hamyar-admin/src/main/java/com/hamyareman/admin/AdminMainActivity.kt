package com.hamyareman.admin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.hamyareman.admin.ui.AdminHomeScreen
import com.hamyareman.admin.ui.AdminLoginScreen
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.designsystem.BrandTheme
import com.hamyareman.ir.platform.core.designsystem.PlatformTheme
import kotlinx.coroutines.launch

val LocalAdmin = staticCompositionLocalOf<AdminContainer> { error("AdminContainer missing") }

class AdminMainActivity : AppCompatActivity() {

    private val loggedIn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AdminApplication
        enableEdgeToEdge()
        setContent {
            val container = app.container
            val scope = rememberCoroutineScope()
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                val lastCrash = app.consumeLastCrash()
                if (lastCrash != null) {
                    error = "اپ دفعهٔ قبل بسته شد. اگر تکرار شد این متن را بفرست:\n$lastCrash"
                }
                loading = true
                val outcome = runCatching {
                    val u = runCatching { container.auth.currentUser() }.getOrNull()
                    if (u == null) {
                        false to null
                    } else when (val p = adminIo { container.billing.adminPing() }) {
                        is AppResult.Ok -> true to null
                        is AppResult.Err -> {
                            runCatching { container.auth.logout() }
                            false to p.error.userMessage
                        }
                    }
                }
                outcome.fold(
                    onSuccess = { (ok, msg) ->
                        loggedIn.value = ok
                        if (msg != null) error = msg
                    },
                    onFailure = { t ->
                        loggedIn.value = false
                        error = t.message?.ifBlank { null } ?: "بررسی نشست ناموفق بود."
                    },
                )
                loading = false
            }

            PlatformTheme(brand = BrandTheme.Mint, darkTheme = false) {
                CompositionLocalProvider(LocalAdmin provides container) {
                    Surface(Modifier.fillMaxSize()) {
                        if (loggedIn.value) {
                            AdminHomeScreen(
                                onLogout = {
                                    scope.launch {
                                        runCatching { adminIo { container.auth.logout() } }
                                        loggedIn.value = false
                                    }
                                },
                            )
                        } else {
                            AdminLoginScreen(
                                loading = loading,
                                error = error,
                                onSignIn = { email, password ->
                                    loading = true
                                    error = null
                                    scope.launch {
                                        val outcome = runCatching {
                                            when (val r = adminIo { container.auth.signIn(email, password) }) {
                                                is AppResult.Err -> false to r.error.userMessage
                                                is AppResult.Ok -> when (val p = adminIo { container.billing.adminPing() }) {
                                                    is AppResult.Ok -> true to null
                                                    is AppResult.Err -> {
                                                        runCatching { container.auth.logout() }
                                                        false to p.error.userMessage
                                                    }
                                                }
                                            }
                                        }
                                        outcome.fold(
                                            onSuccess = { (ok, msg) ->
                                                loggedIn.value = ok
                                                error = msg
                                            },
                                            onFailure = { t ->
                                                loggedIn.value = false
                                                error = "ورود ناموفق: " + (t.message?.ifBlank { null } ?: t.javaClass.simpleName)
                                            },
                                        )
                                        loading = false
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
