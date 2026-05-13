package com.deviceinfo.auto

object DeviceInfoUiShared {
    object Section {
        const val BATTERY = "Battery"
        const val DISPLAY = "Display"
        const val STORAGE_MEMORY = "Storage & memory"
        const val NETWORK = "Network"
        const val DEVICE = "Device"
        const val PROCESSOR = "Processor"
        const val RAM = "RAM"
    }

    object Row {
        const val LEVEL = "Level"
        const val PLUGGED = "Plugged"
        const val HEALTH = "Health"
        const val TECHNOLOGY = "Technology"
        const val TEMPERATURE = "Temperature"
        const val VOLTAGE = "Voltage"
        const val CURRENT = "Current"
        const val CHARGE_COUNTER = "Charge counter"
        const val CYCLE_COUNT = "Cycle count"
        const val RESOLUTION = "Resolution"
        const val DENSITY = "Density"
        const val REFRESH_RATE = "Refresh rate"
        const val STORAGE = "Storage"
        const val RAM = "RAM"
        const val CONNECTION = "Connection"
        const val BLUETOOTH = "Bluetooth"
        const val CELLULAR = "Cellular"
        const val CARRIER = "Carrier"
        const val SIM_STATE = "SIM state"
        const val SIM_COUNTRY = "SIM country"
        const val SIM_TYPE = "SIM type"
        const val VPN = "VPN"
        const val METERED = "Metered"
        const val ROAMING = "Roaming"
        const val BANDWIDTH = "Bandwidth"
        const val DEVICE_NAME = "Device name"
        const val MODEL = "Model"
        const val MANUFACTURER = "Manufacturer"
        const val CODENAME = "Codename"
        const val SYSTEM = "System"
        const val SECURITY_PATCH = "Security patch"
        const val BOOTLOADER = "Bootloader"
        const val KERNEL = "Kernel"
        const val BUILD = "Build"
        const val CPU = "CPU"
        const val CPU_FREQ = "CPU frequency"
        const val CPU_CURRENT = "CPU current"
        const val CPU_ABI = "CPU ABI"
        const val GPU = "GPU"
        const val OPENGL_ES = "OpenGL ES"
    }

    fun levelText(info: DeviceInfo): String {
        val base = "${info.batteryLevel}% • ${info.chargingStatus}"
        return if (info.isPowerSaveMode) "$base • Saver on" else base
    }

    fun currentText(microA: Long): String {
        if (microA == 0L) return "—"
        val ma = microA / 1000.0
        return if (kotlin.math.abs(ma) >= 1000) {
            String.format("%.2f A", ma / 1000)
        } else {
            String.format("%.0f mA", ma)
        }
    }

    fun chargeCounterText(microAh: Long): String {
        if (microAh <= 0L) return "—"
        val mah = microAh / 1000.0
        return if (mah >= 1000) {
            String.format("%.2f Ah", mah / 1000)
        } else {
            String.format("%.0f mAh", mah)
        }
    }

    fun cycleCountText(c: Int?): String = c?.toString() ?: "—"

    fun healthIconRes(health: String): Int =
        when (health) {
            "Good" -> R.drawable.ic_row_health
            "Unknown" -> R.drawable.ic_row_unknown
            else -> R.drawable.ic_row_error
        }

    fun levelIconRes(level: Int, isPowerSaveMode: Boolean): Int =
        when {
            level < 13 -> R.drawable.ic_row_battery_0
            level < 25 -> R.drawable.ic_row_battery_1
            level < 38 -> R.drawable.ic_row_battery_2
            level < 50 -> R.drawable.ic_row_battery_3
            level < 63 -> R.drawable.ic_row_battery_4
            level < 75 -> R.drawable.ic_row_battery_5
            level < 88 -> R.drawable.ic_row_battery_6
            else -> R.drawable.ic_row_battery_full
        }

    fun temperatureIconRes(tempC: Float): Int =
        when {
            tempC < 10f -> R.drawable.ic_row_temp_cold
            tempC >= 45f -> R.drawable.ic_row_temp_hot
            tempC >= 35f -> R.drawable.ic_row_temp_warm
            else -> R.drawable.ic_row_temp_normal
        }

    fun pluggedIconRes(plugged: String): Int =
        when (plugged) {
            "AC" -> R.drawable.ic_row_plugged
            "USB" -> R.drawable.ic_row_plugged_usb
            "Wireless" -> R.drawable.ic_row_plugged_wireless
            else -> R.drawable.ic_row_plugged_none
        }

    fun connectionIconRes(connection: String): Int =
        when (connection) {
            "Wi‑Fi" -> R.drawable.ic_row_wifi
            "Cellular" -> R.drawable.ic_row_cellular
            "Offline" -> R.drawable.ic_row_offline
            else -> R.drawable.ic_row_offline
        }

    fun formatBandwidthKbps(kbps: Int): String {
        if (kbps <= 0) return "—"
        return if (kbps >= 1000) String.format("%.1f Mbps", kbps / 1000.0) else "$kbps kbps"
    }
}

