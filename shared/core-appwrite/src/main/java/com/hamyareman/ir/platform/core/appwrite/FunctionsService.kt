package com.hamyareman.ir.platform.core.appwrite

import io.appwrite.services.Functions
import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.AppResult

/** نتیجه‌ی اجرای یک تابع سرور. */
data class FunctionResult(val statusCode: Int, val body: String) {
    val isOk: Boolean get() = statusCode in 200..299
}

interface FunctionsService {
    val isConfigured: Boolean
    suspend fun call(functionId: String, body: String = "{}"): AppResult<FunctionResult>

    /** میان‌بر: فقط متن پاسخ، یا null در صورت خطا. */
    suspend fun callForBody(functionId: String, body: String = "{}"): String? =
        (call(functionId, body) as? AppResult.Ok)?.value?.body
}

class AppwriteFunctionsService(
    private val provider: AppwriteClientProvider,
) : FunctionsService {

    override val isConfigured: Boolean get() = provider.isConfigured

    override suspend fun call(functionId: String, body: String): AppResult<FunctionResult> {
        if (!provider.isConfigured) {
            return AppResult.Err(AppError.Local("تابع سرور در حالت محلی در دسترس نیست."))
        }
        return runCatching {
            val execution = Functions(provider.client).createExecution(
                functionId = functionId,
                body = body,
            )
            AppResult.Ok(
                FunctionResult(
                    statusCode = runCatching { execution.responseStatusCode.toInt() }.getOrDefault(200),
                    body = runCatching { execution.responseBody }.getOrDefault(""),
                ),
            )
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it, "اجرای تابع «$functionId» ناموفق بود.")) }
    }
}
