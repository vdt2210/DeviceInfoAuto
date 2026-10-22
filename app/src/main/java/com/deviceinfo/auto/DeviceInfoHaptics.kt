package com.deviceinfo.auto

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object DeviceInfoHaptics {
    fun lightTabTap(view: View) {
        val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(feedback)
    }
}
