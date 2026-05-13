package com.deviceinfo.auto

import android.content.Context
import java.util.Locale

object DeviceInfoUiShared {
    object Section {
        const val BATTERY = "section_battery"
        const val DISPLAY = "section_display"
        const val STORAGE_MEMORY = "section_storage_memory"
        const val NETWORK = "section_network"
        const val DEVICE = "section_device"
        const val PROCESSOR = "section_processor"
        const val RAM = "section_ram"
    }

    object Row {
        const val LEVEL = "row_level"
        const val PLUGGED = "row_plugged"
        const val HEALTH = "row_health"
        const val TECHNOLOGY = "row_technology"
        const val TEMPERATURE = "row_temperature"
        const val VOLTAGE = "row_voltage"
        const val CURRENT = "row_current"
        const val POWER = "row_power"
        const val CHARGE_COUNTER = "row_charge_counter"
        const val CYCLE_COUNT = "row_cycle_count"
        const val RESOLUTION = "row_resolution"
        const val DENSITY = "row_density"
        const val REFRESH_RATE = "row_refresh_rate"
        const val STORAGE = "row_storage"
        const val RAM = "row_ram"
        const val CONNECTION = "row_connection"
        const val BLUETOOTH = "row_bluetooth"
        const val CELLULAR = "row_cellular"
        const val CARRIER = "row_carrier"
        const val SIM_STATE = "row_sim_state"
        const val SIM_COUNTRY = "row_sim_country"
        const val SIM_TYPE = "row_sim_type"
        const val VPN = "row_vpn"
        const val METERED = "row_metered"
        const val ROAMING = "row_roaming"
        const val BANDWIDTH = "row_bandwidth"
        const val DEVICE_NAME = "row_device_name"
        const val MODEL = "row_model"
        const val MANUFACTURER = "row_manufacturer"
        const val CODENAME = "row_codename"
        const val SYSTEM = "row_system"
        const val SECURITY_PATCH = "row_security_patch"
        const val BOOTLOADER = "row_bootloader"
        const val KERNEL = "row_kernel"
        const val BUILD = "row_build"
        const val CPU = "row_cpu"
        const val CPU_FREQ = "row_cpu_freq"
        const val CPU_FREQ_CURRENT = "row_cpu_freq_current"
        const val CPU_ABI = "row_cpu_abi"
        const val GPU = "row_gpu"
        const val OPENGL_ES = "row_opengl_es"
    }

    fun sectionTitle(context: Context, key: String): String =
        when (key) {
            Section.BATTERY -> context.getString(R.string.section_battery)
            Section.DISPLAY -> context.getString(R.string.section_display)
            Section.STORAGE_MEMORY -> context.getString(R.string.section_storage_memory)
            Section.NETWORK -> context.getString(R.string.section_network)
            Section.DEVICE -> context.getString(R.string.section_device)
            Section.PROCESSOR -> context.getString(R.string.section_processor)
            else -> key
        }

    fun rowTitle(context: Context, key: String): String =
        when (key) {
            Row.LEVEL -> context.getString(R.string.row_level)
            Row.PLUGGED -> context.getString(R.string.row_plugged)
            Row.HEALTH -> context.getString(R.string.row_health)
            Row.TECHNOLOGY -> context.getString(R.string.row_technology)
            Row.TEMPERATURE -> context.getString(R.string.row_temperature)
            Row.VOLTAGE -> context.getString(R.string.row_voltage)
            Row.CURRENT -> context.getString(R.string.row_current)
            Row.POWER -> context.getString(R.string.row_power)
            Row.CHARGE_COUNTER -> context.getString(R.string.row_charge_counter)
            Row.CYCLE_COUNT -> context.getString(R.string.row_cycle_count)
            Row.RESOLUTION -> context.getString(R.string.row_resolution)
            Row.DENSITY -> context.getString(R.string.row_density)
            Row.REFRESH_RATE -> context.getString(R.string.row_refresh_rate)
            Row.STORAGE -> context.getString(R.string.row_storage)
            Row.RAM -> "RAM"
            Row.CONNECTION -> context.getString(R.string.row_connection)
            Row.BLUETOOTH -> "Bluetooth"
            Row.CELLULAR -> context.getString(R.string.row_cellular)
            Row.CARRIER -> context.getString(R.string.row_carrier)
            Row.SIM_STATE -> context.getString(R.string.row_sim_state)
            Row.SIM_COUNTRY -> context.getString(R.string.row_sim_country)
            Row.SIM_TYPE -> context.getString(R.string.row_sim_type)
            Row.VPN -> "VPN"
            Row.METERED -> context.getString(R.string.row_metered)
            Row.ROAMING -> context.getString(R.string.row_roaming)
            Row.BANDWIDTH -> context.getString(R.string.row_bandwidth)
            Row.DEVICE_NAME -> context.getString(R.string.row_device_name)
            Row.MODEL -> context.getString(R.string.row_model)
            Row.MANUFACTURER -> context.getString(R.string.row_manufacturer)
            Row.CODENAME -> context.getString(R.string.row_codename)
            Row.SYSTEM -> context.getString(R.string.row_system)
            Row.SECURITY_PATCH -> context.getString(R.string.row_security_patch)
            Row.BOOTLOADER -> "Bootloader"
            Row.KERNEL -> "Kernel"
            Row.BUILD -> context.getString(R.string.row_build)
            Row.CPU -> "CPU"
            Row.CPU_FREQ -> context.getString(R.string.row_cpu_freq)
            Row.CPU_FREQ_CURRENT -> context.getString(R.string.row_cpu_freq_current)
            Row.CPU_ABI -> "CPU ABI"
            Row.GPU -> "GPU"
            Row.OPENGL_ES -> "OpenGL ES"
            else -> key
        }

    fun levelText(context: Context, info: DeviceInfo): String {
        val charging = chargingStatusLabel(context, info.chargingStatus)
        val base = "${info.batteryLevel}% • $charging"
        return if (info.isPowerSaveMode) {
            "$base • ${context.getString(R.string.saver_on)}"
        } else {
            base
        }
    }

    fun chargingStatusLabel(context: Context, statusEn: String): String =
        when (statusEn) {
            "Charging" -> context.getString(R.string.battery_status_charging)
            "Discharging" -> context.getString(R.string.battery_status_discharging)
            "Full" -> context.getString(R.string.battery_status_full)
            "Not charging" -> context.getString(R.string.battery_status_not_charging)
            "Unknown" -> context.getString(R.string.battery_status_unknown)
            else -> statusEn
        }

    fun pluggedLabel(context: Context, raw: String): String =
        when (raw) {
            "Wireless" -> context.getString(R.string.plugged_wireless)
            "None" -> context.getString(R.string.plugged_none)
            else -> raw
        }

    fun healthLabel(context: Context, raw: String): String =
        when (raw) {
            "Good" -> context.getString(R.string.health_good)
            "Unknown" -> context.getString(R.string.health_unknown)
            "Cold" -> context.getString(R.string.health_cold)
            "Dead" -> context.getString(R.string.health_dead)
            "Overheat" -> context.getString(R.string.health_overheat)
            "Over voltage" -> context.getString(R.string.health_over_voltage)
            "Unspecified failure" -> context.getString(R.string.health_unspecified_failure)
            else -> raw
        }

    fun connectionLabel(context: Context, raw: String): String =
        when (raw) {
            "Cellular" -> context.getString(R.string.connection_cellular)
            "Ethernet" -> context.getString(R.string.connection_ethernet)
            "Online" -> context.getString(R.string.connection_online)
            "Offline" -> context.getString(R.string.connection_offline)
            "Unknown" -> context.getString(R.string.connection_unknown)
            else -> raw
        }

    fun yesNo(context: Context, value: Boolean): String =
        if (value) context.getString(R.string.value_yes) else context.getString(R.string.value_no)

    fun onOff(context: Context, on: Boolean): String =
        if (on) context.getString(R.string.value_on) else context.getString(R.string.value_off)

    fun bluetoothState(context: Context, on: Boolean?): String =
        when (on) {
            null -> "—"
            true -> onOff(context, true)
            false -> onOff(context, false)
        }

    fun densityLabel(dpi: Int): String =
        if (dpi > 0) String.format(Locale.US, "%d dpi", dpi) else "—"

    fun unknownWord(context: Context, raw: String): String =
        if (raw.trim().equals("unknown", ignoreCase = true)) {
            context.getString(R.string.word_unknown)
        } else {
            raw
        }

    fun systemVersionDisplay(context: Context, info: DeviceInfo): String =
        if (info.androidVersion.trim().equals("unknown", ignoreCase = true)) {
            context.getString(R.string.word_unknown)
        } else {
            info.androidVersion
        }

    fun resolutionDisplay(context: Context, raw: String): String =
        if (raw.trim().equals("unknown", ignoreCase = true)) {
            context.getString(R.string.word_unknown)
        } else {
            raw
        }

    private fun mapLinesByColonSuffix(
        context: Context,
        raw: String,
        translateSuffix: (Context, String) -> String
    ): String =
        raw.lineSequence().map { line ->
            val idx = line.indexOf(':')
            if (idx < 0) {
                line
            } else {
                val prefix = line.substring(0, idx).trimEnd()
                val suffix = line.substring(idx + 1).trim()
                "$prefix: ${translateSuffix(context, suffix)}"
            }
        }.joinToString("\n")

    private fun simStateSingleDisplay(context: Context, en: String): String =
        when (en) {
            "Ready" -> context.getString(R.string.sim_state_ready)
            "Absent" -> context.getString(R.string.sim_state_absent)
            "PIN required" -> context.getString(R.string.sim_state_pin_required)
            "PUK required" -> context.getString(R.string.sim_state_puk_required)
            "Network locked" -> context.getString(R.string.sim_state_network_locked)
            "Not ready" -> context.getString(R.string.sim_state_not_ready)
            "Disabled" -> context.getString(R.string.sim_state_perm_disabled)
            "Loaded" -> context.getString(R.string.sim_state_loaded)
            "Unknown" -> context.getString(R.string.word_unknown)
            else -> en
        }

    private fun simTypeSingleDisplay(context: Context, en: String): String =
        when (en) {
            "Physical" -> context.getString(R.string.sim_kind_physical)
            else -> en
        }

    fun simStateSummaryDisplay(context: Context, raw: String): String {
        if (raw == "—" || raw.isBlank()) return raw
        return if (':' !in raw) {
            simStateSingleDisplay(context, raw.trim())
        } else {
            mapLinesByColonSuffix(context, raw, ::simStateSingleDisplay)
        }
    }

    fun simTypeSummaryDisplay(context: Context, raw: String): String {
        if (raw == "—" || raw.isBlank()) return raw
        return if (':' !in raw) {
            simTypeSingleDisplay(context, raw.trim())
        } else {
            mapLinesByColonSuffix(context, raw, ::simTypeSingleDisplay)
        }
    }

    fun technologyLabel(context: Context, raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.equals("Unknown", ignoreCase = true) || trimmed.isEmpty()) {
            context.getString(R.string.word_unknown)
        } else {
            raw
        }
    }

    fun formatBandwidthKbps(kbps: Int): String {
        if (kbps <= 0) return "—"
        return if (kbps >= 1000) {
            String.format(Locale.US, "%.1f Mbps", kbps / 1000.0)
        } else {
            String.format(Locale.US, "%d kbps", kbps)
        }
    }

    fun currentText(microA: Long): String {
        if (microA == 0L) return "—"
        val ma = microA / 1000.0
        return if (kotlin.math.abs(ma) >= 1000) {
            String.format(Locale.US, "%.2f A", ma / 1000)
        } else {
            String.format(Locale.US, "%.0f mA", ma)
        }
    }

    fun instantPowerText(voltageV: Float, currentMicroA: Long): String {
        if (voltageV <= 0f) return "—"
        if (currentMicroA == 0L || currentMicroA == Long.MIN_VALUE) return "—"
        val watts = voltageV * (currentMicroA / 1_000_000.0)
        if (!watts.isFinite()) return "—"
        val magnitude = kotlin.math.abs(watts)
        if (magnitude < 0.0005) return "0 mW"
        if (magnitude < 1.0) {
            val mw = watts * 1000.0
            val mAbs = kotlin.math.abs(mw)
            return when {
                mAbs >= 100.0 -> String.format(Locale.US, "%.0f mW", mw)
                mAbs >= 10.0 -> String.format(Locale.US, "%.1f mW", mw)
                else -> String.format(Locale.US, "%.2f mW", mw)
            }
        }
        return if (magnitude >= 100.0) {
            String.format(Locale.US, "%.1f W", watts)
        } else {
            String.format(Locale.US, "%.2f W", watts)
        }
    }

    fun chargeCounterText(microAh: Long): String {
        if (microAh <= 0L) return "—"
        val mah = microAh / 1000.0
        return if (mah >= 1000) {
            String.format(Locale.US, "%.2f Ah", mah / 1000)
        } else {
            String.format(Locale.US, "%.0f mAh", mah)
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
            "Wi-Fi" -> R.drawable.ic_row_wifi
            "Cellular" -> R.drawable.ic_row_cellular
            "Ethernet" -> R.drawable.ic_row_ethernet
            "Bluetooth" -> R.drawable.ic_row_bluetooth
            "Online" -> R.drawable.ic_row_wifi
            "Offline" -> R.drawable.ic_row_offline
            else -> R.drawable.ic_row_unknown
        }
}
