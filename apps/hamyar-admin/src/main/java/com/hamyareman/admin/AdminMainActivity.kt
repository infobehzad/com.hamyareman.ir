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

    private val loggedIn = mutableStateOf<Boolean?>(null)

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
                if (loggedIn.value == null) {
                    val u = runCatching { container.auth.currentUser() }.getOrNull()
                    if (u == null) {
                        loggedIn.value = false
                    } else {
                        when (val p = container.billing.adminPing()) {
                            is AppResult.Ok -> loggedIn.value = true
                            is AppResult.Err -> {
                                runCatching { container.auth.logout() }
                                error = p.error.userMessage
                                loggedIn.value = false
                            }
                        }
                    }
                }
            }

            PlatformTheme(brand = BrandTheme.Mint, darkTheme = false) {
                CompositionLocalProvider(LocalAdmin provides container) {
                    Surface(Modifier.fillMaxSize()) {
                        when (loggedIn.value) {
                            null -> Unit
                            false -> AdminLoginScreen(
                                loading = loading,
                                error = error,
                                onSignIn = { email, password ->
                                    loading = true
                                    error = null
                                    scope.launch {
                                        when (val r = container.auth.signIn(email, password)) {
                                            is AppResult.Err -> {
                                                error = r.error.userMessage
                                                loading = false
                                            }
                                            is AppResult.Ok -> when (val p = container.billing.adminPing()) {
                                                is AppResult.Ok -> {
                                                    loading = false
                                                    loggedIn.value = true
                                                }
                                                is AppResult.Err -> {
                                                    runCatching { container.auth.logout() }
                                                    error = p.error.userMessage
                                                    loading = false
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                            true -> AdminHomeScreen(
                                onLogout = {
                                    scope.launch {
                                        container.auth.logout()
                                        loggedIn.value = false
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
