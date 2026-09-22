package com.hamyareman.admin

import android.app.Application

class AdminApplication : Application() {
    lateinit var container: AdminContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AdminContainer(this)
    }
}
