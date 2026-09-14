package com.hamyareman.ir.ui.profile

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.RowPermissions
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.LocalStore

/** پایه‌های تحصیلی — دراپ‌داون ثبت‌نام و کلید فیلتر محتوا. */
enum class GradeLevel(val id: String, val fa: String) {
    G5("grade5", "پایه پنجم"),
    G6("grade6", "پایه ششم"),
    G7("grade7", "پایه هفتم"),
    G8("grade8", "پایه هشتم"),
    G9("grade9", "پایه نهم"),
    G10("grade10", "پایه دهم"),
    G11("grade11", "پایه یازدهم"),
    G12("grade12", "پایه دوازدهم");

    companion object {
        fun byId(id: String?): GradeLevel = entries.firstOrNull { it.id == id } ?: G9
    }
}

/**
 * نگاشت کتاب → پایه. **نگارش فعلی:** همه‌ی کتاب‌ها و دروس موجودِ جدول به پایه‌ی
 * نهم لینک شده‌اند (به محض ارائه‌ی فهرست پایه‌های دیگر، همین یک تابع گسترش می‌یابد
 * و هیچ‌جای دیگری از کد نباید تغییر کند).
 */
fun gradeOfBook(bookCode: String): GradeLevel = GradeLevel.G9

/**
 * وضعیت زنده‌ی پایه‌ی کاربر — آینه‌ی محلی (فوری) + تأیید ابری (سینک).
 * صفحه‌های فهرست کتاب با [GradeGate] می‌خوانند.
 */
object StudentProfileState {
    @Volatile var grade: GradeLevel = GradeLevel.G9
    @Volatile var hasProfile: Boolean = false

    /** نام کوچک برای خوش‌آمد داشبورد — state تا UI پس از ذخیره فوری تازه شود. */
    var firstName: String by androidx.compose.runtime.mutableStateOf("")
        private set

    var subscription: String by androidx.compose.runtime.mutableStateOf("free")
        private set

    fun loadMirror(ctx: Context) {
        val store = LocalStore(ctx, STORE)
        grade = GradeLevel.byId(store.getString(KEY_GRADE, "").ifBlank { null })
        hasProfile = store.getString(KEY_DONE, "0") == "1"
        firstName = store.getString(KEY_NAME, "").orEmpty()
        subscription = store.getString(KEY_SUB, "free").ifBlank { "free" }
    }

    fun writeMirror(ctx: Context, g: GradeLevel, done: Boolean) {
        writeMirror(ctx, g, done, firstName, subscription)
    }

    fun writeMirror(ctx: Context, g: GradeLevel, done: Boolean, name: String, sub: String) {
        val store = LocalStore(ctx, STORE)
        store.putString(KEY_GRADE, g.id)
        store.putString(KEY_DONE, if (done) "1" else "0")
        store.putString(KEY_NAME, name)
        store.putString(KEY_SUB, sub.ifBlank { "free" })
        grade = g
        hasProfile = done
        firstName = name
        subscription = sub.ifBlank { "free" }
    }

    /** آواتار محلی (مسیر فایل) — در سرور آپلود نمی‌شود. */
    @Volatile var avatarPath: String = ""

    fun loadAvatarMirror(ctx: Context) {
        avatarPath = LocalStore(ctx, STORE).getString(KEY_AVATAR, "").orEmpty()
    }

    fun saveAvatarMirror(ctx: Context, path: String) {
        LocalStore(ctx, STORE).putString(KEY_AVATAR, path)
        avatarPath = path
    }

    private const val STORE = "hamyar_profile"
    private const val KEY_GRADE = "grade"
    private const val KEY_DONE = "registered"
    private const val KEY_NAME = "firstName"
    private const val KEY_SUB = "subscription"
    private const val KEY_AVATAR = "avatarPath"
}

/** فیلتر مرکزی محتوای مدرسه بر اساس پایه‌ی کاربر. */
object GradeGate {
    fun canSeeBook(bookCode: String): Boolean = gradeOfBook(bookCode) == StudentProfileState.grade
    fun <T> filter(list: List<T>, bookCodeOf: (T) -> String): List<T> = list.filter { canSeeBook(bookCodeOf(it)) }
}

/**
 * پروفایل دانش‌آموز — جدول واحد `student_profiles`؛ هر کاربر دقیقاً یک ردیف با
 * `rowId = userId` گوگل (یک یوزرآی‌دی واحد برای همه‌ی دسترسی‌ها و سینک‌های بعدی).
 */
data class StudentProfile(
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val grade: GradeLevel,
    val phone: String, // ۱۰ رقم، بدون +98 (پیش‌شماره در UI ثابت است)
    val schoolName: String = "",
    val province: String = "",
    val city: String = "",
    val subscription: String = "free", // free | yearly — فقط از سمت پشتیبانی تغییر می‌کند
)

object StudentProfileRepo {
    private const val TABLE = TableIds.STUDENT_PROFILES

    /** ردیف کاربر؛ نبود/خطا → null (خطا هرگز نباید فرم را بی‌دلیل نشان دهد). */
    suspend fun fetch(tables: TablesDbService, userId: String): StudentProfile? {
        if (userId.isBlank()) return null
        val r = tables.get(TABLE, userId)
        return when (r) {
            is com.hamyareman.ir.platform.core.common.AppResult.Ok -> r.value?.let { row ->
                val d = row.data
                StudentProfile(
                    userId = userId,
                    email = d["email"]?.toString().orEmpty(),
                    firstName = d["firstName"]?.toString().orEmpty(),
                    lastName = d["lastName"]?.toString().orEmpty(),
                    age = d["age"]?.toString()?.toIntOrNull() ?: 0,
                    grade = GradeLevel.byId(d["grade"]?.toString()),
                    phone = d["phone"]?.toString().orEmpty(),
                    schoolName = d["schoolName"]?.toString().orEmpty(),
                    province = d["province"]?.toString().orEmpty(),
                    city = d["city"]?.toString().orEmpty(),
                    subscription = (d["subscription"]?.toString().ifBlank { null } ?: "free"),
                )
            }
            else -> null
        }
    }

    /** ذخیره (ساخت/بازنویسی) + پرمیشن فقط-خودِ-کاربر؛ true = در سرور ثبت شد. */
    suspend fun save(tables: TablesDbService, email: String, p: StudentProfile): Boolean {
        val data = mapOf(
            "userId" to p.userId,
            "email" to email,
            "firstName" to p.firstName,
            "lastName" to p.lastName,
            "age" to p.age,
            "grade" to p.grade.id,
            "phone" to p.phone,
            "schoolName" to p.schoolName,
            "province" to p.province,
            "city" to p.city,
            "subscription" to p.subscription.ifBlank { "free" },
            "updatedAtMs" to System.currentTimeMillis(),
        )
        return when (tables.upsert(TABLE, p.userId, data, RowPermissions.forUser(p.userId))) {
            is com.hamyareman.ir.platform.core.common.AppResult.Ok -> true
            else -> false
        }
    }
}

/** استان‌های ایران — دراپ‌داون پروفایل. */
val IRAN_PROVINCES = listOf(
    "آذربایجان شرقی", "آذربایجان غربی", "اردبیل", "اصفهان", "البرز", "ایلام", "بوشهر",
    "تهران", "چهارمحال و بختیاری", "خراسان جنوبی", "خراسان رضوی", "خراسان شمالی",
    "خوزستان", "زنجان", "سمنان", "سیستان و بلوچستان", "فارس", "قزوین", "قم", "کردستان",
    "کرمان", "کرمانشاه", "کهگیلویه و بویراحمد", "گلستان", "گیلان", "لرستان", "مازندران",
    "مرکزی", "هرمزگان", "همدان", "یزد",
)
