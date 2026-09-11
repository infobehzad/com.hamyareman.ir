package ir.behzad.roozhayeman.ui.appearance

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import ir.behzad.platform.core.common.LocalStore
import ir.behzad.platform.core.designsystem.BrandTheme

/**
 * وضعیت ظاهر اپ (تم/حالت روشن‌وتاریک/فونت) — قابل‌مشاهده برای Compose
 * و پایدار در SharedPreferences. یک نمونه‌ی سراسری در AppContainer ساخته می‌شود.
 */
/** دسترسی سراسری به وضعیت ظاهر از داخل کامپوزابل‌ها. */
val LocalUiPrefs = staticCompositionLocalOf<UiPrefs> { error("UiPrefs missing") }

class UiPrefs(context: Context) {
    private val store = LocalStore(context, "hamyar_appearance")

    var theme by mutableStateOf(
        runCatching { BrandTheme.valueOf(store.getString(KEY_THEME, BrandTheme.Stitch.name)) }
            .getOrDefault(BrandTheme.Stitch)
    )
        private set

    /** system / light / dark */
    var darkMode by mutableStateOf(store.getString(KEY_DARK, "system"))
        private set

    /** کلید فونت انتخابی؛ خالی = فونت سیستم */
    var fontKey by mutableStateOf(store.getString(KEY_FONT, ""))
        private set

    val darkTheme: Boolean
        get() = when (darkMode) {
            "light" -> false
            "dark" -> true
            else -> android.content.res.Resources.getSystem().configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }

    fun updateTheme(value: BrandTheme) {
        theme = value
        store.putString(KEY_THEME, value.name)
    }

    fun updateDarkMode(value: String) {
        darkMode = value
        store.putString(KEY_DARK, value)
    }

    fun updateFontKey(value: String) {
        fontKey = value
        store.putString(KEY_FONT, value)
    }

    companion object {
        private const val KEY_THEME = "appearance_theme"
        private const val KEY_DARK = "appearance_dark"
        private const val KEY_FONT = "appearance_font"
    }
}
