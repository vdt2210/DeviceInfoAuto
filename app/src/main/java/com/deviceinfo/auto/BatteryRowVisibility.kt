package com.deviceinfo.auto

object BatteryRowVisibility {

    fun batteryCycleCountRowVisible(cycleCount: Int?): Boolean =
        cycleCount != null

    fun batteryDesignCapacityRowVisible(microAh: Long?): Boolean =
        microAh != null && microAh > 0L

    fun batteryChargeFullRowVisible(designMicroAh: Long?, chargeFullMicroAh: Long?): Boolean {
        if (chargeFullMicroAh == null || chargeFullMicroAh <= 0L) return false
        return true
    }
}
