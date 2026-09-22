package com.hamyareman.ir.platform.core.common

/**
 * قرارداد اشتراک کارت‌به‌کارت — مشترک بین اپ دانش‌آموز و اپ ادمین.
 *
 * هیچ کلید سروری این‌جا نیست. تأیید پرداخت فقط از تابع `user-bootstrap`
 * (actionهای billing) با کلید سرور انجام می‌شود.
 *
 * شماره کارت کسب‌وکار را قبل از انتشار واقعی در [ACCOUNT_HOLDER] / [CARD_NUMBER]
 * / [BANK_NAME] بگذار؛ تا آن موقع صفحه کارت را به‌صورت «اعلام می‌شود» نشان می‌دهد.
 */
object BillingConfig {
    const val ACCOUNT_HOLDER = "همیار من"
    const val CARD_NUMBER = "XXXX-XXXX-XXXX-XXXX"
    const val BANK_NAME = "کارت‌به‌کارت به حساب همیار"

    const val REFUND_DAYS = 7

    const val GUARANTEE_FA =
        "اگر تا هفت روز پس از فعال‌شدن اشتراک، حس کردی همیار آن‌طور که امید داشتی " +
            "کنارت نیست، بدون هیچ قید و پرسشی کارت‌به‌کارت را برمی‌گردانیم. " +
            "یادگیری باید با خیال راحت شروع شود؛ پشیمانی‌ات را هزینه نمی‌کنیم."

    const val FAREWELL_FA =
        "اشتراک برایت غیرفعال شد و مبلغ در مسیر بازگشت است. مسیر درس خواندن گاهی " +
            "پیچ می‌خورد؛ این پایان داستان تو نیست. هر وقت آماده بودی، همیار با آغوش " +
            "باز منتظر است. موفقیتت را از همین‌جا می‌بینیم — به امید دیدار."

    data class Plan(
        val id: String,
        val titleFa: String,
        val priceToman: Int,
        val periodFa: String,
        val blurbFa: String,
    )

    val MONTHLY = Plan(
        id = "monthly",
        titleFa = "ماهانه",
        priceToman = 49_000,
        periodFa = "یک ماه",
        blurbFa = "دسترسی یک‌ماهه به تدریس و تمرین",
    )
    val YEARLY = Plan(
        id = "yearly",
        titleFa = "سالانه",
        priceToman = 390_000,
        periodFa = "یک سال",
        blurbFa = "صرفه‌جویی نسبت به ماهانه · پیشنهاد اصلی",
    )

    val plans: List<Plan> = listOf(MONTHLY, YEARLY)

    fun plan(id: String): Plan =
        plans.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: YEARLY

    fun cardReady(): Boolean =
        CARD_NUMBER.any { it.isDigit() } && !CARD_NUMBER.contains("X", ignoreCase = true)
}

/** وضعیت اشتراک روی پروفایل دانش‌آموز (ستون `subscription`). */
object BillingStatus {
    const val FREE = "free"
    const val PENDING = "pending"
    const val YEARLY = "yearly"
    const val REFUND_PENDING = "refund_pending"
    const val REJECTED = "rejected"
    const val REFUNDED = "refunded"

    fun norm(raw: String?): String = raw.orEmpty().trim().lowercase().ifBlank { FREE }

    /** درس‌های کامل فقط با اشتراک تأییدشده (یا در انتظار استرداد، تا وقتی ادمین تأیید نکرده). */
    fun isPaid(raw: String?): Boolean = when (norm(raw)) {
        YEARLY, "paid", REFUND_PENDING -> true
        else -> false
    }

    fun chipFa(raw: String?): String = when (norm(raw)) {
        YEARLY, "paid" -> "اشتراک فعال"
        PENDING -> "انتظار تأیید پرداخت"
        REFUND_PENDING -> "انتظار بازگشت وجه"
        REFUNDED -> "بازگشت وجه انجام شد"
        REJECTED -> "پرداخت تأیید نشد"
        else -> "مهمان همیار من"
    }
}

/** actionهای JSON تابع سرور — شناسهٔ تابع همان [FunctionIds.USER_BOOTSTRAP] است (سقف پلن). */
object BillingActions {
    const val MY_ORDER = "billing_my"
    const val CREATE_ORDER = "billing_create"
    const val REQUEST_REFUND = "billing_refund"
    const val ADMIN_PING = "admin_ping"
    const val ADMIN_LIST = "admin_list"
    const val ADMIN_GET = "admin_get"
    const val ADMIN_APPROVE = "admin_approve"
    const val ADMIN_REJECT = "admin_reject"
    const val ADMIN_REFUND = "admin_refund_ok"
    const val ADMIN_SEARCH = "admin_search"
    const val ADMIN_USER = "admin_user"
    const val ADMIN_SET_GRADE = "admin_set_grade"
    const val ADMIN_SET_PREMIUM = "admin_set_premium"
    const val ADMIN_REVOKE_DEVICE = "admin_revoke_device"
    const val ADMIN_CLEAR_DEVICES = "admin_clear_devices"
    const val ADMIN_LOGOUT = "admin_logout"
    const val ADMIN_BLOCK = "admin_block"
    const val ADMIN_UNBLOCK = "admin_unblock"
    const val ADMIN_RESET_PASSWORD = "admin_reset_password"
    const val ADMIN_STATS = "admin_stats"
}
