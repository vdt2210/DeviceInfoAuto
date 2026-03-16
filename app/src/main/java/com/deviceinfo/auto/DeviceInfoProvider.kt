package com.deviceinfo.auto

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs

data class DeviceInfo(
    val batteryLevel: Int,
    val batteryTemperatureCelsius: Float,
    val batteryVoltageV: Float,
    val chargingStatus: String,
    val plugged: String,
    val health: String,
    val technology: String,
    val currentNowMicroA: Long,
    val chargeCounterMicroAh: Long,
    val cycleCount: Int?,
    val storageSummary: String,
    val ramSummary: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val securityPatch: String,
    val buildId: String,
    val isPowerSaveMode: Boolean
)

object DeviceInfoProvider {

    fun get(context: Context): DeviceInfo {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return createEmpty()

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val batteryPct = if (scale > 0) (level * 100 / scale) else 0

        val tempRaw = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempCelsius = tempRaw / 10f

        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val chargingStatus = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }

        val health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
            else -> "Unknown"
        }

        val voltageMv = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageV = voltageMv / 1000f

        val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val pluggedStr = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "None"
        }

        val technology = batteryIntent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.ifEmpty { null } ?: "Unknown"

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val currentNowMicroA = if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        } else 0L
        val chargeCounterMicroAh = if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        } else 0L

        val cycleCount = if (Build.VERSION.SDK_INT >= 34) {
            val v = batteryIntent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
            if (v >= 0) v else null
        } else null

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0)
        val freeGB = freeBytes / (1024.0 * 1024.0 * 1024.0)

        val memInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(memInfo)
        val ramTotalGB = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val ramAvailGB = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)

        val storageSummary = formatFreeOf(freeGB, totalGB)
        val ramSummary = formatFreeOf(ramAvailGB, ramTotalGB)

        val manufacturer = Build.MANUFACTURER.ifEmpty { "Unknown" }
        val model = Build.MODEL.ifEmpty { "Unknown" }
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val securityPatch = Build.VERSION.SECURITY_PATCH.ifEmpty { "Unknown" }
        val buildId = Build.DISPLAY.ifEmpty { Build.ID }.ifEmpty { "Unknown" }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaveMode = powerManager?.isPowerSaveMode == true

        return DeviceInfo(
            batteryLevel = batteryPct,
            batteryTemperatureCelsius = tempCelsius,
            batteryVoltageV = voltageV,
            chargingStatus = chargingStatus,
            plugged = pluggedStr,
            health = healthStr,
            technology = technology,
            currentNowMicroA = currentNowMicroA,
            chargeCounterMicroAh = chargeCounterMicroAh,
            cycleCount = cycleCount,
            storageSummary = storageSummary,
            ramSummary = ramSummary,
            manufacturer = manufacturer,
            model = model,
            androidVersion = androidVersion,
            securityPatch = securityPatch,
            buildId = buildId,
            isPowerSaveMode = isPowerSaveMode
        )
    }

    private fun formatSize(gb: Double): String =
        when {
            gb >= 0.1 -> String.format("%.1f GB", gb)
            gb >= 1.0 / 1024 -> String.format("%.0f MB", gb * 1024)
            gb >= 1.0 / (1024 * 1024) -> String.format("%.0f KB", gb * 1024 * 1024)
            else -> String.format("%.0f B", gb * 1024 * 1024 * 1024)
        }

    private fun formatFreeOf(freeGb: Double, totalGb: Double): String =
        "${formatSize(freeGb)} free of ${formatSize(totalGb)}"

    private fun createEmpty(): DeviceInfo = DeviceInfo(
        batteryLevel = 0,
        batteryTemperatureCelsius = 0f,
        batteryVoltageV = 0f,
        chargingStatus = "Unknown",
        plugged = "None",
        health = "Unknown",
        technology = "Unknown",
        currentNowMicroA = 0L,
        chargeCounterMicroAh = 0L,
        cycleCount = null,
        storageSummary = "—",
        ramSummary = "—",
        manufacturer = "Unknown",
        model = "Unknown",
        androidVersion = "Unknown",
        securityPatch = "Unknown",
        buildId = "Unknown",
        isPowerSaveMode = false
    )
}
