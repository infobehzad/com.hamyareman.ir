package com.hamyareman.admin

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.AppwriteAuthService
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.appwrite.AppwriteFunctionsService
import com.hamyareman.ir.platform.core.appwrite.AppwriteStorageService
import com.hamyareman.ir.platform.core.appwrite.BillingGateway
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.UserRole

class AdminContainer(context: Context) {
    val store = LocalStore(context)
    val appwrite = AppwriteClientProvider(
        context = context,
        endpoint = BuildConfig.APPWRITE_ENDPOINT,
        projectId = BuildConfig.APPWRITE_PROJECT_ID,
        databaseId = BuildConfig.APPWRITE_DATABASE_ID,
    )
    val functions = AppwriteFunctionsService(appwrite)
    val storage = AppwriteStorageService(appwrite)
    val billing = BillingGateway(functions)
    val auth = AppwriteAuthService(
        provider = appwrite,
        fallbackRole = UserRole.ZAHRA,
        store = store,
        functions = functions,
        gateContext = { null },
    )
}
