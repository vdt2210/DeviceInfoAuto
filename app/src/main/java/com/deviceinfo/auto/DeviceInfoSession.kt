package com.deviceinfo.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class DeviceInfoSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = DeviceInfoScreen(carContext)
}
