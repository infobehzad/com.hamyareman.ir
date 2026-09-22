package com.hamyareman.ir.platform.core.appwrite

import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.BillingActions
import com.hamyareman.ir.platform.core.common.FunctionIds
import org.json.JSONArray
import org.json.JSONObject

data class BillingOrder(
    val id: String = "",
    val userId: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val grade: String = "",
    val gender: String = "",
    val phone: String = "",
    val schoolName: String = "",
    val province: String = "",
    val city: String = "",
    val age: Int = 0,
    val planId: String = "",
    val planTitle: String = "",
    val amountToman: Int = 0,
    val payText: String = "",
    val receiptFileId: String = "",
    val payerName: String = "",
    val status: String = "",
    val createdAtMs: Long = 0L,
    val paidAtMs: Long = 0L,
    val refundShaba: String = "",
    val refundCard: String = "",
    val refundAccountName: String = "",
    val refundReason: String = "",
    val refundAtMs: Long = 0L,
    val farewell: String = "",
    val adminNote: String = "",
)

data class BillingProfile(
    val userId: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val grade: String = "",
    val gender: String = "",
    val phone: String = "",
    val schoolName: String = "",
    val province: String = "",
    val city: String = "",
    val county: String = "",
    val age: Int = 0,
    val birthDate: String = "",
    val subscription: String = "",
    val name: String = "",
    val labels: List<String> = emptyList(),
    val blocked: Boolean = false,
    val hamyarGrade: String = "",
    val deviceCount: Int = 0,
)

data class AdminDevice(
    val id: String = "",
    val label: String = "",
    val lastAt: Long = 0L,
)

data class AdminUser(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val labels: List<String> = emptyList(),
    val status: Boolean = true,
    val blocked: Boolean = false,
    val hamyarGrade: String = "",
    val devices: List<AdminDevice> = emptyList(),
    val sessionCount: Int = 0,
    val subscription: String = "",
    val profile: BillingProfile = BillingProfile(),
    val tempPassword: String = "",
)

data class AdminStats(
    val usersTotal: Int = 0,
    val pendingPay: Int = 0,
    val pendingRefund: Int = 0,
    val approved: Int = 0,
    val refunded: Int = 0,
    val paidProfiles: Int = 0,
)

class BillingGateway(private val functions: FunctionsService) {

    suspend fun call(action: String, extra: JSONObject = JSONObject()): AppResult<JSONObject> {
        extra.put("action", action)
        return when (val r = functions.call(FunctionIds.ADMIN_OPS, extra.toString())) {
            is AppResult.Err -> r
            is AppResult.Ok -> {
                val body = r.value.body.ifBlank { "{}" }
                val json = runCatching { JSONObject(body) }.getOrElse {
                    return AppResult.Err(AppError.Unknown("پاسخ سرور خوانده نشد."))
                }
                if (!json.optBoolean("ok", false)) {
                    val msg = json.optString("messageFa").ifBlank { "عملیات انجام نشد." }
                    AppResult.Err(AppError.Local(msg))
                } else {
                    AppResult.Ok(json)
                }
            }
        }
    }

    suspend fun myOrder(): AppResult<Pair<String, BillingOrder?>> =
        when (val r = call(BillingActions.MY_ORDER)) {
            is AppResult.Err -> r
            is AppResult.Ok -> AppResult.Ok(
                r.value.optString("subscription").ifBlank { "free" } to
                    r.value.optJSONObject("order")?.let { parseOrder(it) },
            )
        }

    suspend fun createOrder(
        planId: String,
        payText: String,
        receiptFileId: String,
        payerName: String,
    ): AppResult<Pair<String, BillingOrder?>> {
        val body = JSONObject()
            .put("planId", planId)
            .put("payText", payText)
            .put("receiptFileId", receiptFileId)
            .put("payerName", payerName)
        return when (val r = call(BillingActions.CREATE_ORDER, body)) {
            is AppResult.Err -> r
            is AppResult.Ok -> AppResult.Ok(
                r.value.optString("subscription") to r.value.optJSONObject("order")?.let { parseOrder(it) },
            )
        }
    }

    suspend fun requestRefund(
        shaba: String,
        card: String,
        accountName: String,
        reason: String,
    ): AppResult<Pair<String, BillingOrder?>> {
        val body = JSONObject()
            .put("refundShaba", shaba)
            .put("refundCard", card)
            .put("refundAccountName", accountName)
            .put("refundReason", reason)
        return when (val r = call(BillingActions.REQUEST_REFUND, body)) {
            is AppResult.Err -> r
            is AppResult.Ok -> AppResult.Ok(
                r.value.optString("subscription") to r.value.optJSONObject("order")?.let { parseOrder(it) },
            )
        }
    }

    suspend fun adminPing(): AppResult<Unit> = when (val r = call(BillingActions.ADMIN_PING)) {
        is AppResult.Err -> r
        is AppResult.Ok -> AppResult.Ok(Unit)
    }

    suspend fun adminList(queue: String): AppResult<List<BillingOrder>> =
        when (val r = call(BillingActions.ADMIN_LIST, JSONObject().put("queue", queue))) {
            is AppResult.Err -> r
            is AppResult.Ok -> AppResult.Ok(parseOrders(r.value.optJSONArray("orders")))
        }

    suspend fun adminGet(orderId: String): AppResult<Triple<BillingOrder, BillingProfile, String>> =
        when (val r = call(BillingActions.ADMIN_GET, JSONObject().put("orderId", orderId))) {
            is AppResult.Err -> r
            is AppResult.Ok -> {
                val order = r.value.optJSONObject("order")?.let { parseOrder(it) }
                    ?: return AppResult.Err(AppError.Local("سفارش پیدا نشد."))
                val profile = parseProfile(r.value.optJSONObject("profile"))
                AppResult.Ok(Triple(order, profile, r.value.optString("avatarFileId")))
            }
        }

    suspend fun adminApprove(orderId: String): AppResult<Unit> =
        unit(call(BillingActions.ADMIN_APPROVE, JSONObject().put("orderId", orderId)))

    suspend fun adminReject(orderId: String, note: String = ""): AppResult<Unit> =
        unit(call(BillingActions.ADMIN_REJECT, JSONObject().put("orderId", orderId).put("adminNote", note)))

    suspend fun adminRefundOk(orderId: String): AppResult<Unit> =
        unit(call(BillingActions.ADMIN_REFUND, JSONObject().put("orderId", orderId)))

    suspend fun adminSearch(q: String): AppResult<List<BillingProfile>> =
        when (val r = call(BillingActions.ADMIN_SEARCH, JSONObject().put("q", q))) {
            is AppResult.Err -> r
            is AppResult.Ok -> {
                val arr = r.value.optJSONArray("hits") ?: JSONArray()
                val out = mutableListOf<BillingProfile>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val p = parseProfile(o.optJSONObject("profile"))
                    out += p.copy(
                        userId = o.optString("userId").ifBlank { p.userId },
                        email = o.optString("email").ifBlank { p.email },
                        name = o.optString("name").ifBlank { p.name },
                        labels = parseLabels(o.optJSONArray("labels")).ifEmpty { p.labels },
                        blocked = o.optBoolean("blocked", p.blocked),
                        hamyarGrade = o.optString("hamyarGrade").ifBlank { p.hamyarGrade.ifBlank { p.grade } },
                        deviceCount = o.optInt("deviceCount", p.deviceCount),
                        subscription = o.optString("subscription").ifBlank { p.subscription },
                    )
                }
                AppResult.Ok(out)
            }
        }

    suspend fun adminUser(userId: String): AppResult<AdminUser> =
        userOf(call(BillingActions.ADMIN_USER, JSONObject().put("targetUserId", userId)))

    suspend fun adminSetGrade(userId: String, grade: String): AppResult<AdminUser> =
        userOf(call(BillingActions.ADMIN_SET_GRADE, JSONObject().put("targetUserId", userId).put("grade", grade)))

    suspend fun adminSetPremium(userId: String, paid: Boolean): AppResult<AdminUser> =
        userOf(call(BillingActions.ADMIN_SET_PREMIUM, JSONObject().put("targetUserId", userId).put("paid", paid)))

    suspend fun adminRevokeDevice(userId: String, deviceId: String): AppResult<AdminUser> =
        userOf(call(BillingActions.ADMIN_REVOKE_DEVICE, JSONObject().put("targetUserId", userId).put("deviceId", deviceId)))

    suspend fun adminClearDevices(userId: String): AppResult<AdminUser> =
        userOf(call(BillingActions.ADMIN_CLEAR_DEVICES, JSONObject().put("targetUserId", userId)))

    suspend fun adminForceLogout(userId: String): AppResult<AdminUser> =
        userOf(call(BillingActions.ADMIN_LOGOUT, JSONObject().put("targetUserId", userId)))

    suspend fun adminBlock(userId: String, block: Boolean): AppResult<AdminUser> =
        userOf(
            call(
                if (block) BillingActions.ADMIN_BLOCK else BillingActions.ADMIN_UNBLOCK,
                JSONObject().put("targetUserId", userId),
            ),
        )

    suspend fun adminResetPassword(userId: String): AppResult<AdminUser> =
        userOf(call(BillingActions.ADMIN_RESET_PASSWORD, JSONObject().put("targetUserId", userId)))

    suspend fun adminStats(): AppResult<AdminStats> =
        when (val r = call(BillingActions.ADMIN_STATS)) {
            is AppResult.Err -> r
            is AppResult.Ok -> {
                val s = r.value.optJSONObject("stats") ?: JSONObject()
                AppResult.Ok(
                    AdminStats(
                        usersTotal = s.optInt("usersTotal"),
                        pendingPay = s.optInt("pendingPay"),
                        pendingRefund = s.optInt("pendingRefund"),
                        approved = s.optInt("approved"),
                        refunded = s.optInt("refunded"),
                        paidProfiles = s.optInt("paidProfiles"),
                    ),
                )
            }
        }

    private fun userOf(r: AppResult<JSONObject>): AppResult<AdminUser> = when (r) {
        is AppResult.Err -> r
        is AppResult.Ok -> {
            val u = r.value.optJSONObject("user")
                ?: return AppResult.Err(AppError.Local("کاربر برنگشت."))
            val parsed = parseAdminUser(u)
            val temp = r.value.optString("tempPassword").ifBlank { parsed.tempPassword }
            AppResult.Ok(parsed.copy(tempPassword = temp))
        }
    }

    private fun unit(r: AppResult<JSONObject>): AppResult<Unit> = when (r) {
        is AppResult.Err -> r
        is AppResult.Ok -> AppResult.Ok(Unit)
    }

    companion object {
        fun parseOrder(o: JSONObject): BillingOrder = BillingOrder(
            id = o.optString("id"),
            userId = o.optString("userId"),
            email = o.optString("email"),
            firstName = o.optString("firstName"),
            lastName = o.optString("lastName"),
            grade = o.optString("grade"),
            gender = o.optString("gender"),
            phone = o.optString("phone"),
            schoolName = o.optString("schoolName"),
            province = o.optString("province"),
            city = o.optString("city"),
            age = o.optInt("age"),
            planId = o.optString("planId"),
            planTitle = o.optString("planTitle"),
            amountToman = o.optInt("amountToman"),
            payText = o.optString("payText"),
            receiptFileId = o.optString("receiptFileId"),
            payerName = o.optString("payerName"),
            status = o.optString("status"),
            createdAtMs = o.optLong("createdAtMs"),
            paidAtMs = o.optLong("paidAtMs"),
            refundShaba = o.optString("refundShaba"),
            refundCard = o.optString("refundCard"),
            refundAccountName = o.optString("refundAccountName"),
            refundReason = o.optString("refundReason"),
            refundAtMs = o.optLong("refundAtMs"),
            farewell = o.optString("farewell"),
            adminNote = o.optString("adminNote"),
        )

        fun parseOrders(arr: JSONArray?): List<BillingOrder> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { parseOrder(it) }
            }
        }

        fun parseLabels(arr: JSONArray?): List<String> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        }

        fun parseProfile(o: JSONObject?): BillingProfile {
            if (o == null) return BillingProfile()
            return BillingProfile(
                userId = o.optString("userId"),
                email = o.optString("email"),
                firstName = o.optString("firstName"),
                lastName = o.optString("lastName"),
                grade = o.optString("grade"),
                gender = o.optString("gender"),
                phone = o.optString("phone"),
                schoolName = o.optString("schoolName"),
                province = o.optString("province"),
                city = o.optString("city"),
                county = o.optString("county"),
                age = o.optInt("age"),
                birthDate = o.optString("birthDate"),
                subscription = o.optString("subscription"),
                name = o.optString("name"),
                labels = parseLabels(o.optJSONArray("labels")),
                blocked = o.optBoolean("blocked", false),
                hamyarGrade = o.optString("hamyarGrade"),
                deviceCount = o.optInt("deviceCount"),
            )
        }

        fun parseAdminUser(o: JSONObject): AdminUser {
            val devicesArr = o.optJSONArray("devices")
            val devices = if (devicesArr == null) emptyList() else
                (0 until devicesArr.length()).mapNotNull { i ->
                    val d = devicesArr.optJSONObject(i) ?: return@mapNotNull null
                    AdminDevice(
                        id = d.optString("id"),
                        label = d.optString("label").ifBlank { "دستگاه" },
                        lastAt = d.optLong("lastAt"),
                    )
                }
            return AdminUser(
                userId = o.optString("userId"),
                email = o.optString("email"),
                name = o.optString("name"),
                labels = parseLabels(o.optJSONArray("labels")),
                status = o.optBoolean("status", true),
                blocked = o.optBoolean("blocked", false),
                hamyarGrade = o.optString("hamyarGrade"),
                devices = devices,
                sessionCount = o.optInt("sessionCount"),
                subscription = o.optString("subscription"),
                profile = parseProfile(o.optJSONObject("profile")),
                tempPassword = o.optString("tempPassword"),
            )
        }
    }
}
