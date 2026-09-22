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
                    )
                }
                AppResult.Ok(out)
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

        fun parseProfile(o: JSONObject?): BillingProfile {
            if (o == null) return BillingProfile()
            val labelsArr = o.optJSONArray("labels")
            val labels = if (labelsArr == null) emptyList() else
                (0 until labelsArr.length()).map { labelsArr.optString(it) }
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
                labels = labels,
            )
        }
    }
}
