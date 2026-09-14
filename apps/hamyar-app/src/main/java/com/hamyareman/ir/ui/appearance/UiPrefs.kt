package com.hamyareman.ir.ui.appearance

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.designsystem.BrandTheme

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

    /**
     * لغزنده‌ی سایز متن: پایه ۱۳sp، از −۶ تا +۶، صفر = استاندارد.
     * روی کل اپ اعمال می‌شود (Density.fontScale).
     */
    var textSizeOffset by mutableIntStateOf(
        store.getString(KEY_SIZE, "0").toIntOrNull()?.coerceIn(-6, 6) ?: 0
    )
        private set

    private var themeUserSet: Boolean
        get() = store.getString(KEY_THEME_USER, "0") == "1"
        set(v) { store.putString(KEY_THEME_USER, if (v) "1" else "0") }

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
        themeUserSet = true
    }

    /** پیش‌فرض تم بر اساس جنسیت — فقط اگر کاربر هنوز تم را دستی عوض نکرده. */
    fun applyDefaultForGender(genderId: String) {
        if (themeUserSet) return
        theme = if (genderId == "girl") BrandTheme.DollStage else BrandTheme.Stitch
        store.putString(KEY_THEME, theme.name)
    }

    fun updateDarkMode(value: String) {
        darkMode = value
        store.putString(KEY_DARK, value)
    }

    fun updateFontKey(value: String) {
        fontKey = value
        store.putString(KEY_FONT, value)
    }

    fun updateTextSizeOffset(value: Int) {
        textSizeOffset = value.coerceIn(-6, 6)
        store.putString(KEY_SIZE, textSizeOffset.toString())
    }

    companion object {
        private const val KEY_THEME = "appearance_theme"
        private const val KEY_THEME_USER = "appearance_theme_user"
        private const val KEY_DARK = "appearance_dark"
        private const val KEY_FONT = "appearance_font"
        private const val KEY_SIZE = "appearance_text_offset"
    }
}
