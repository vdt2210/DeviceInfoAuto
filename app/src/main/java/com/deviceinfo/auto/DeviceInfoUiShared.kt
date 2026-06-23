package com.deviceinfo.auto

import android.content.Context
import android.view.Surface
import java.util.Locale

object DeviceInfoUiShared {

    /** Where the formatted string is shown — list layout differs (phone multiline vs car single line). */
    enum class DisplaySurface {
        PHONE,
        CAR,
    }

    /** Stored list separator when building [DeviceInfo] (phone layout). */
    const val LIST_LINE_SEP = "\n"

    /** Canonical tokens for [DeviceInfo.multiNetworkSummary] — translated at display time. */
    object MultiNetworkToken {
        const val NONE = "__multi_none__"
        const val WIFI = "Wi-Fi"
        const val CELLULAR = "Cellular"
        const val ETHERNET = "Ethernet"
        const val BLUETOOTH = "Bluetooth"
    }

    /** Canonical storage separator for compound values (level + status, thermal + °C, …). */
    const val COMPOUND_SEP = ", "

    /** Android Auto: inline separator for lists and compound values. */
    const val BULLET_SEP = " • "

    /** Join separate list items when building [DeviceInfo] (always phone multiline storage). */
    fun joinList(items: List<String>): String = items.joinToString(LIST_LINE_SEP)

    /** Join parts of one combined value for display. */
    fun joinCompound(parts: List<String>, surface: DisplaySurface): String =
        when (surface) {
            DisplaySurface.PHONE -> parts.joinToString(LIST_LINE_SEP)
            DisplaySurface.CAR -> parts.joinToString(BULLET_SEP)
        }

    /** List summaries stored with [LIST_LINE_SEP]. */
    fun listSummaryForDisplay(context: Context, raw: String, surface: DisplaySurface): String {
        if (raw == DASH || raw.isBlank()) return valueNotAvailableLabel(context)
        return when (surface) {
            DisplaySurface.PHONE -> raw
            DisplaySurface.CAR -> raw.replace(LIST_LINE_SEP, BULLET_SEP)
        }
    }

    /** Compound summaries stored with [COMPOUND_SEP] (e.g. thermal status + °C). */
    fun compoundSummaryForDisplay(context: Context, raw: String, surface: DisplaySurface): String {
        if (raw == DASH || raw.isBlank()) return valueNotAvailableLabel(context)
        return when (surface) {
            DisplaySurface.PHONE -> raw.replace(COMPOUND_SEP, LIST_LINE_SEP)
            DisplaySurface.CAR -> raw.replace(COMPOUND_SEP, BULLET_SEP)
        }
    }

    fun temperatureText(celsius: Float): String =
        String.format(Locale.US, "%.1f °C", celsius)

    object Section {
        const val BATTERY = "section_battery"
        const val DISPLAY = "section_display"
        const val STORAGE_MEMORY = "section_storage_memory"
        const val NETWORK = "section_network"
        const val DEVICE = "section_device"
        const val PROCESSOR = "section_processor"
        const val RAM = "section_ram"
        const val HARDWARE = "section_hardware"
        const val SENSORS = "section_sensors"
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
        const val BATTERY_DESIGN_CAPACITY = "row_battery_design_capacity"
        const val CHARGE_TIME_REMAINING = "row_charge_time_remaining"
        const val RESOLUTION = "row_resolution"
        const val DISPLAY_DIAGONAL = "row_display_diagonal"
        const val DENSITY = "row_density"
        const val REFRESH_RATE = "row_refresh_rate"
        const val REFRESH_RATE_MODES = "row_refresh_rate_modes"
        const val HDR = "row_hdr"
        const val DISPLAY_ROTATION = "row_display_rotation"
        const val STORAGE = "row_storage"
        const val STORAGE_VOLUME_KEY_PREFIX = "row_storage_vol_"
        const val RAM = "row_ram"

        fun storageVolumeRowKey(volumeId: String): String {
            val safe = volumeId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            return STORAGE_VOLUME_KEY_PREFIX + safe
        }

        fun isStorageVolumeRowKey(key: String): Boolean = key.startsWith(STORAGE_VOLUME_KEY_PREFIX)
        const val CONNECTION = "row_connection"
        const val INTERNET_VALIDATED = "row_internet_validated"
        const val CAPTIVE_PORTAL = "row_captive_portal"
        const val MULTI_NETWORK = "row_multi_network"
        const val WIFI_BANDS = "row_wifi_bands"
        const val WIFI_LINK_SPEED = "row_wifi_link_speed"
        const val WIFI_SIGNAL = "row_wifi_signal"
        const val BLUETOOTH = "row_bluetooth"
        const val CELLULAR = "row_cellular"
        const val CELLULAR_SIGNAL = "row_cellular_signal"
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

        const val HW_CAMERA = "row_hw_camera"
        const val HW_NFC = "row_hw_nfc"
        const val HW_USB = "row_hw_usb"
        const val HW_AUDIO = "row_hw_audio"
        const val HW_BIOMETRIC = "row_hw_biometric"
        const val HW_THERMAL = "row_hw_thermal"

        const val SENSOR_ACCELEROMETER = "row_sensor_accelerometer"
        const val SENSOR_GYROSCOPE = "row_sensor_gyroscope"
        const val SENSOR_MAGNETIC_FIELD = "row_sensor_magnetic_field"
        const val SENSOR_LIGHT = "row_sensor_light"
        const val SENSOR_PRESSURE = "row_sensor_pressure"
        const val SENSOR_HUMIDITY = "row_sensor_humidity"
        const val SENSOR_AMBIENT_TEMPERATURE = "row_sensor_ambient_temperature"
        const val SENSOR_PROXIMITY = "row_sensor_proximity"
        const val SENSOR_HINGE_ANGLE = "row_sensor_hinge_angle"
    }

    fun sectionTitle(context: Context, key: String): String =
        when (key) {
            Section.BATTERY -> context.getString(R.string.section_battery)
            Section.DISPLAY -> context.getString(R.string.section_display)
            Section.STORAGE_MEMORY -> context.getString(R.string.section_storage_memory)
            Section.NETWORK -> context.getString(R.string.section_network)
            Section.DEVICE -> context.getString(R.string.section_device)
            Section.PROCESSOR -> context.getString(R.string.section_processor)
            Section.HARDWARE -> context.getString(R.string.section_hardware)
            Section.SENSORS -> context.getString(R.string.section_sensors)
            Section.RAM -> context.getString(R.string.section_storage_memory)
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
            Row.BATTERY_DESIGN_CAPACITY -> context.getString(R.string.row_battery_design_capacity)
            Row.CHARGE_TIME_REMAINING -> context.getString(R.string.row_charge_time_remaining)
            Row.RESOLUTION -> context.getString(R.string.row_resolution)
            Row.DISPLAY_DIAGONAL -> context.getString(R.string.row_display_diagonal)
            Row.DENSITY -> context.getString(R.string.row_density)
            Row.REFRESH_RATE -> context.getString(R.string.row_refresh_rate)
            Row.REFRESH_RATE_MODES -> context.getString(R.string.row_refresh_modes)
            Row.HDR -> context.getString(R.string.row_hdr)
            Row.DISPLAY_ROTATION -> context.getString(R.string.row_display_orientation)
            Row.STORAGE -> context.getString(R.string.row_storage)
            Row.RAM -> "RAM"
            Row.CONNECTION -> context.getString(R.string.row_connection)
            Row.INTERNET_VALIDATED -> context.getString(R.string.row_internet_validated)
            Row.CAPTIVE_PORTAL -> context.getString(R.string.row_captive_portal)
            Row.MULTI_NETWORK -> context.getString(R.string.row_multi_network)
            Row.WIFI_BANDS -> context.getString(R.string.row_wifi_bands)
            Row.WIFI_LINK_SPEED -> context.getString(R.string.row_wifi_link_speed)
            Row.WIFI_SIGNAL -> context.getString(R.string.row_wifi_signal)
            Row.BLUETOOTH -> "Bluetooth"
            Row.CELLULAR -> context.getString(R.string.row_cellular)
            Row.CELLULAR_SIGNAL -> context.getString(R.string.row_cellular_signal)
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
            Row.HW_CAMERA -> context.getString(R.string.row_hw_camera)
            Row.HW_NFC -> context.getString(R.string.row_hw_nfc)
            Row.HW_USB -> context.getString(R.string.row_hw_usb)
            Row.HW_AUDIO -> context.getString(R.string.row_hw_audio)
            Row.HW_BIOMETRIC -> context.getString(R.string.row_hw_biometric)
            Row.HW_THERMAL -> context.getString(R.string.row_hw_thermal)
            Row.SENSOR_ACCELEROMETER -> context.getString(R.string.sensor_row_accelerometer)
            Row.SENSOR_GYROSCOPE -> context.getString(R.string.sensor_row_gyroscope)
            Row.SENSOR_MAGNETIC_FIELD -> context.getString(R.string.sensor_row_magnetic_field)
            Row.SENSOR_LIGHT -> context.getString(R.string.sensor_row_light)
            Row.SENSOR_PRESSURE -> context.getString(R.string.sensor_row_pressure)
            Row.SENSOR_HUMIDITY -> context.getString(R.string.sensor_row_humidity)
            Row.SENSOR_AMBIENT_TEMPERATURE -> context.getString(R.string.sensor_row_ambient_temperature)
            Row.SENSOR_PROXIMITY -> context.getString(R.string.sensor_row_proximity)
            Row.SENSOR_HINGE_ANGLE -> context.getString(R.string.sensor_row_hinge_angle)
            else -> key
        }

    fun levelText(
        context: Context,
        info: DeviceInfo,
        surface: DisplaySurface = DisplaySurface.PHONE,
    ): String {
        val parts = buildList {
            add("${info.batteryLevel}%")
            add(chargingStatusLabel(context, info.chargingStatus))
            if (info.isPowerSaveMode) add(context.getString(R.string.saver_on))
        }
        return joinCompound(parts, surface)
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
            "Unknown" -> valueNotAvailableLabel(context)
            else -> raw
        }

    fun yesNo(context: Context, value: Boolean): String =
        if (value) context.getString(R.string.value_yes) else context.getString(R.string.value_no)

    fun yesNoOptional(context: Context, value: Boolean?): String =
        when (value) {
            null -> valueNotAvailableLabel(context)
            true -> context.getString(R.string.value_yes)
            false -> context.getString(R.string.value_no)
        }

    fun networkReadingOrNotAvailable(
        context: Context,
        raw: String,
        surface: DisplaySurface = DisplaySurface.PHONE,
    ): String = listSummaryForDisplay(context, raw, surface)

    fun multiNetworkSummaryDisplay(
        context: Context,
        raw: String,
        surface: DisplaySurface = DisplaySurface.PHONE,
    ): String {
        if (raw == DASH || raw.isBlank()) return valueNotAvailableLabel(context)
        if (raw == MultiNetworkToken.NONE ||
            raw.equals("No", ignoreCase = true) ||
            raw == context.getString(R.string.multi_network_none)
        ) {
            return context.getString(R.string.multi_network_none)
        }
        val sep = if (surface == DisplaySurface.PHONE) LIST_LINE_SEP else BULLET_SEP
        return raw.lineSequence()
            .map { line -> multiNetworkLabelSingle(context, line.trim()) }
            .joinToString(sep)
    }

    private fun multiNetworkLabelSingle(context: Context, label: String): String =
        when (label) {
            MultiNetworkToken.WIFI, "Wi-Fi", "Wi‑Fi" -> context.getString(R.string.network_wifi_short)
            MultiNetworkToken.CELLULAR, "Cellular" -> context.getString(R.string.network_cellular)
            MultiNetworkToken.ETHERNET, "Ethernet" -> context.getString(R.string.connection_ethernet)
            MultiNetworkToken.BLUETOOTH, "Bluetooth" -> context.getString(R.string.network_bluetooth)
            else -> label
        }

    fun cellularNetworkLabelDisplay(context: Context, raw: String): String =
        when (raw) {
            DASH, "" -> valueNotAvailableLabel(context)
            "5G NR" -> context.getString(R.string.cellular_network_5g)
            "5G NR (NSA)" -> context.getString(R.string.cellular_network_5g_nsa)
            "LTE" -> context.getString(R.string.cellular_network_lte)
            "HSPA+" -> context.getString(R.string.cellular_network_hspa_plus)
            "HSPA" -> context.getString(R.string.cellular_network_hspa)
            "HSDPA" -> context.getString(R.string.cellular_network_hsdpa)
            "HSUPA" -> context.getString(R.string.cellular_network_hsupa)
            "UMTS" -> context.getString(R.string.cellular_network_umts)
            "EDGE" -> context.getString(R.string.cellular_network_edge)
            "GPRS" -> context.getString(R.string.cellular_network_gprs)
            "GSM" -> context.getString(R.string.cellular_network_gsm)
            "IWLAN" -> context.getString(R.string.cellular_network_iwlan)
            "TD-SCDMA" -> context.getString(R.string.cellular_network_td_scdma)
            else -> raw
        }

    fun onOff(context: Context, on: Boolean): String =
        if (on) context.getString(R.string.value_on) else context.getString(R.string.value_off)

    fun valueNotAvailableLabel(context: Context): String =
        context.notAvailableText()

    fun bluetoothState(context: Context, on: Boolean?): String =
        when (on) {
            null -> valueNotAvailableLabel(context)
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
            valueNotAvailableLabel(context)
        } else {
            info.androidVersion
        }

    fun resolutionDisplay(context: Context, raw: String): String =
        if (raw.trim().equals("unknown", ignoreCase = true)) {
            valueNotAvailableLabel(context)
        } else {
            raw
        }

    private const val DASH = "—"

    /** sysfs/EGL paths only; not for SIM/counter dashes. */
    fun sysfsReadingOrNotAvailable(context: Context, raw: String): String =
        if (raw == DASH) valueNotAvailableLabel(context) else raw

    fun refreshRateDisplay(context: Context, hz: Float): String =
        if (hz > 0f) String.format(Locale.US, "%.0f Hz", hz) else valueNotAvailableLabel(context)

    fun displayRefreshModesText(
        context: Context,
        summary: String,
        surface: DisplaySurface = DisplaySurface.PHONE,
    ): String = listSummaryForDisplay(context, summary, surface)

    fun displayHdrSummaryText(
        context: Context,
        summary: String,
        surface: DisplaySurface = DisplaySurface.PHONE,
    ): String = listSummaryForDisplay(context, summary, surface)

    fun displayRotationLabel(context: Context, rotation: Int): String =
        when (rotation) {
            Surface.ROTATION_0 -> context.getString(R.string.display_rotation_0)
            Surface.ROTATION_90 -> context.getString(R.string.display_rotation_90)
            Surface.ROTATION_180 -> context.getString(R.string.display_rotation_180)
            Surface.ROTATION_270 -> context.getString(R.string.display_rotation_270)
            else ->
                if (rotation < 0) context.getString(R.string.not_available)
                else context.getString(R.string.display_rotation_unknown, rotation)
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
            "eSIM" -> context.getString(R.string.sim_kind_esim)
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

    fun bandwidthDisplay(
        context: Context,
        downstreamKbps: Int,
        upstreamKbps: Int,
        surface: DisplaySurface = DisplaySurface.PHONE,
    ): String {
        val down = formatBandwidthKbps(downstreamKbps)
        val up = formatBandwidthKbps(upstreamKbps)
        if (down == DASH && up == DASH) return DASH
        val parts = buildList {
            if (up != DASH) add("▲ $up")
            if (down != DASH) add("▼ $down")
        }
        return joinCompound(parts, surface)
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

    fun chargeCounterText(microAh: Long): String =
        formatBatteryMah(microAh) ?: "—"

    fun cycleCountText(context: Context, c: Int?): String =
        when {
            c == null -> context.getString(R.string.not_available)
            c <= 0 -> "—"
            else -> c.toString()
        }

    fun batteryDesignCapacityText(context: Context, microAh: Long?): String {
        if (microAh == null) return context.getString(R.string.not_available)
        val mah = kotlin.math.round(microAh / 1000.0).toLong()
        if (mah !in 500L..50_000L) return context.getString(R.string.not_available)
        return formatBatteryMah(microAh) ?: context.getString(R.string.not_available)
    }

    /** Phone batteries are usually labeled in mAh (e.g. 4500 mAh), not Ah. */
    private fun formatBatteryMah(microAh: Long): String? {
        if (microAh < 100_000L) return null
        val mah = kotlin.math.round(microAh / 1000.0).toLong()
        if (mah < 1L) return null
        return String.format(Locale.getDefault(), "%,d mAh", mah)
    }

    fun internetValidatedDisplay(
        context: Context,
        validated: Boolean?,
        activeConnection: String,
    ): String {
        val base = yesNoOptional(context, validated)
        if (validated == null) return base
        val via = when (activeConnection) {
            "Wi-Fi" -> context.getString(R.string.network_via_wifi)
            "Cellular" -> context.getString(R.string.network_via_cellular)
            "Ethernet" -> context.getString(R.string.network_via_ethernet)
            else -> null
        }
        return if (via != null) "$base ($via)" else base
    }

    fun chargeTimeRemainingText(context: Context, ms: Long?): String {
        val remainingMs = ms ?: return "—"
        if (remainingMs <= 0L) return "—"
        val totalMinutes = (remainingMs + 59_999L) / 60_000L
        if (totalMinutes <= 0L) return "—"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0L && minutes > 0L ->
                context.getString(R.string.charge_time_hours_minutes, hours, minutes)
            hours > 0L -> context.getString(R.string.charge_time_hours_only, hours)
            else -> context.getString(R.string.charge_time_minutes_only, minutes)
        }
    }

    fun displayDiagonalText(context: Context, inches: Float): String =
        if (inches <= 0f || !inches.isFinite()) {
            context.getString(R.string.not_available)
        } else {
            String.format(Locale.US, "%.1f\"", inches)
        }

    fun healthIconRes(health: String): Int =
        when (health) {
            "Good" -> R.drawable.ic_row_health
            "Unknown" -> R.drawable.ic_row_unknown
            else -> R.drawable.ic_row_error
        }

    fun levelIconRes(level: Int, isPowerSaveMode: Boolean): Int {
        val effective = if (isPowerSaveMode) minOf(level, 62) else level
        return when {
            effective < 13 -> R.drawable.ic_row_battery_0
            effective < 25 -> R.drawable.ic_row_battery_1
            effective < 38 -> R.drawable.ic_row_battery_2
            effective < 50 -> R.drawable.ic_row_battery_3
            effective < 63 -> R.drawable.ic_row_battery_4
            effective < 75 -> R.drawable.ic_row_battery_5
            effective < 88 -> R.drawable.ic_row_battery_6
            else -> R.drawable.ic_row_battery_full
        }
    }

    fun temperatureIconRes(tempC: Float): Int =
        when {
            tempC < 10f -> R.drawable.ic_row_temp_cold
            tempC >= 45f -> R.drawable.ic_row_temp_hot
            tempC >= 35f -> R.drawable.ic_row_temp_warm
            else -> R.drawable.ic_row_temp_normal
        }

    fun thermalIconRes(thermalSummary: String): Int {
        val temp = Regex("""([\d.]+)\s*°C""").find(thermalSummary)?.groupValues?.get(1)?.toFloatOrNull()
        if (temp != null) return temperatureIconRes(temp)
        return R.drawable.ic_row_temp_normal
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

    /** Phone: RAM, CPU freq, display rotation (battery updates via sticky broadcast, not this poll). */
    const val PHONE_LIVE_POLL_INTERVAL_MS = 5_000L

    /** Android Auto: only cadence for live rows (pin, dòng, công suất, RAM); no broadcast rebuilds. */
    const val CAR_TELEMETRY_POLL_INTERVAL_MS = 5_000L
}
