package com.hamyareman.ir.platform.core.appwrite

import androidx.activity.ComponentActivity
import io.appwrite.ID
import io.appwrite.enums.OAuthProvider
import io.appwrite.services.Account
import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.FunctionIds
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.UserRole

/** کاربر جاری از دید اپ — نقش همیشه از Labels سرور می‌آید، نه از یک فیلد کلاینتی. */
data class AuthUser(
    val id: String,
    val name: String,
    val email: String,
    val labels: List<String>,
    val role: UserRole,
) {
    val isGuest: Boolean get() = role == UserRole.GUEST
}

interface AuthService {
    val isConfigured: Boolean

    suspend fun currentUser(): AuthUser?
    suspend fun currentUserId(): String?
    suspend fun currentLabels(): List<String>
    suspend fun currentRole(): UserRole

    suspend fun signUp(name: String, email: String, password: String): AppResult<AuthUser>
    suspend fun signIn(email: String, password: String): AppResult<AuthUser>
    suspend fun signInWithGoogle(activity: ComponentActivity): AppResult<AuthUser>

    /**
     * ورود native گوگل (Credential Manager) — idToken از اکانت‌های روی خود گوشی؛
     * صحت‌سنجی و ساخت سشن سمت سرور با تابع «google-auth». بدون مرورگر.
     */
    suspend fun signInWithGoogleToken(idToken: String, nonce: String): AppResult<AuthUser>
    suspend fun signInAsGuest(): AppResult<AuthUser>
    suspend fun logout(): AppResult<Unit>
}

/**
 * پیاده‌سازی Appwrite.
 *
 * نکات مهم:
 *  - نقش کاربر فقط با Label سمت سرور تعیین می‌شود (`zahra` / `father` / `guest`).
 *    تابع `user-bootstrap` برچسب را می‌گذارد و `pairing` پدر را ارتقا می‌دهد.
 *  - در حالت محلی (بدون projectId) یک کاربر محلی با [fallbackRole] برگردانده می‌شود
 *    تا UI و جریان‌ها قابل تست باشند.
 */
class AppwriteAuthService(
    private val provider: AppwriteClientProvider,
    private val fallbackRole: UserRole,
    private val store: LocalStore? = null,
    private val functions: FunctionsService? = null,
) : AuthService {

    override val isConfigured: Boolean get() = provider.isConfigured

    private val account get() = Account(provider.client)

    private val localUser = AuthUser(
        id = "local-${fallbackRole.label}",
        name = fallbackRole.label,
        email = "",
        labels = listOf(fallbackRole.label),
        role = fallbackRole,
    )

    override suspend fun currentUser(): AuthUser? {
        if (!provider.isConfigured) return localUser
        return runCatching {
            val user = account.get()
            val labels = user.labels
            AuthUser(
                id = user.id,
                name = user.name,
                email = user.email,
                labels = labels,
                role = UserRole.fromLabels(labels),
            ).also { remember(it) }
        }.getOrNull()
    }

    override suspend fun currentUserId(): String? = currentUser()?.id

    override suspend fun currentLabels(): List<String> = currentUser()?.labels ?: emptyList()

    override suspend fun currentRole(): UserRole = currentUser()?.role ?: UserRole.GUEST

    override suspend fun signUp(name: String, email: String, password: String): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        return runCatching {
            account.create(userId = ID.unique(), email = email, password = password, name = name)
            account.createEmailPasswordSession(email = email, password = password)
            bootstrap()
            requireUser()
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it, "ساخت حساب ناموفق بود.")) }
    }

    override suspend fun signIn(email: String, password: String): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        return runCatching {
            account.createEmailPasswordSession(email = email, password = password)
            bootstrap()
            requireUser()
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it, "ورود ناموفق بود.")) }
    }

    override suspend fun signInAsGuest(): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        return runCatching {
            account.createAnonymousSession()
            bootstrap()
            requireUser()
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it, "ورود مهمان ناموفق بود.")) }
    }

    /**
     * ورود با گوگل (OAuth2): مرورگر/کروم‌تب باز می‌شود و برگشت با اسکیم
     * `appwrite-callback-<PROJECT_ID>` به همین اپ می‌آید (intent-filter در Manifest).
     * برگشت توسط SDK روی همان activity شنود می‌شود، پس [activity] باید همان
     * اکتیویتیِ زنده‌ی فعلی باشد.
     */
    override suspend fun signInWithGoogle(activity: ComponentActivity): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        return runCatching {
            // مهم: اگر سشن فعلی (مهمان یا لاگین قبلی) زنده باشد، Appwrite اکانت گوگل را
            // به همان کاربرِ فعلی می‌چسباند و کاربر جدیدی با ایمیل جدید ساخته نمی‌شود
            // (همان باگ «فقط ایمیل اولین ورود ذخیره می‌شود»). پس اول سشن فعلی را
            // تمام می‌کنیم تا OAuth همیشه از صفر شروع کند و هر اکانت گوگل، کاربر خودش را بسازد.
            runCatching { account.deleteSession("current") }
            account.createOAuth2Session(activity = activity, provider = OAuthProvider.GOOGLE)
            bootstrap()
            requireUser()
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it, "ورود با گوگل ناموفق بود.")) }
    }

    override suspend fun signInWithGoogleToken(idToken: String, nonce: String): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        return runCatching {
            val svc = functions ?: error("توابع سرور در دسترس نیست.")
            val payload = """{"mode":"google-auth","idToken":"$idToken","nonce":"$nonce"}"""
            val body = svc.callForBody(FunctionIds.GOOGLE_AUTH, payload)
                ?: error("پاسخ سرور دریافت نشد.")
            val o = org.json.JSONObject(body)
            require(o.optBoolean("ok")) { o.optString("error", "توکن گوگل پذیرفته نشد.") }
            account.createSession(o.optString("userId"), o.optString("secret"))
            bootstrap()
            requireUser()
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it, "ورود با گوگل ناموفق بود.")) }
    }

    override suspend fun logout(): AppResult<Unit> {
        forgetRole()
        if (!provider.isConfigured) return AppResult.Ok(Unit)
        return runCatching {
            account.deleteSession("current")
            AppResult.Ok(Unit)
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it)) }
    }

    /**
     * صدا زدن `user-bootstrap`: ساخت سطر profiles و گذاشتن Label مناسب.
     * اگر تابع سرور موجود نبود، خطا را قورت می‌دهیم تا ورود کاربر شکست نخورد
     * (در این حالت نقش `guest` می‌ماند و باید در کنسول بررسی شود).
     */
    private suspend fun bootstrap() {
        val svc = functions ?: return
        runCatching {
            svc.call(FunctionIds.USER_BOOTSTRAP, """{"app":"${fallbackRole.label}"}""")
        }
    }

    private suspend fun requireUser(): AppResult<AuthUser> =
        currentUser()?.let { AppResult.Ok(it) } ?: AppResult.Err(AppError.Auth())

    /**
     * کش سبک هویت: برای ساخت دسترسی سطرها (`Role.user(id)`) و نمایش فوری نقش،
     * پیش از آنکه پاسخ سرور برسد.
     */
    private fun remember(user: AuthUser) {
        store?.putString(KEY_ROLE, user.role.label)
        store?.putString(KEY_USER_ID, user.id)
    }

    private fun forgetRole() {
        store?.remove(KEY_ROLE, KEY_USER_ID)
    }

    /** نقش کش‌شده برای نمایش فوری UI قبل از رسیدن پاسخ سرور. */
    fun cachedRole(): UserRole =
        store?.getString(KEY_ROLE)?.takeIf { it.isNotBlank() }
            ?.let { label -> UserRole.entries.firstOrNull { it.label == label } }
            ?: fallbackRole

    /** شناسه‌ی کاربر جاری از کش محلی (بدون نیاز به suspend). */
    fun cachedUserId(): String? = store?.getString(KEY_USER_ID)?.takeIf { it.isNotBlank() }

    companion object {
        /** همین کلیدها را ماژول‌های دیگر (حرف دل، تماس) برای دسترسی سطرها می‌خوانند. */
        const val KEY_ROLE = "auth_role"
        const val KEY_USER_ID = "auth_user_id"
    }
}
