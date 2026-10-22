package com.deviceinfo.auto

import android.app.Application
import com.google.android.material.color.DynamicColors

class DeviceInfoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Requires Theme.Material3.* — falls back to theme colors when unavailable (API < 31).
        DynamicColors.applyToActivitiesIfAvailable(this)
        AppPreferences(this).applyToDelegate()
    }
}
