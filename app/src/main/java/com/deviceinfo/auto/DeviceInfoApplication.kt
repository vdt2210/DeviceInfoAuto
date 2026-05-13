package com.deviceinfo.auto

import android.app.Application

class DeviceInfoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPreferences(this).applyToDelegate()
    }
}
