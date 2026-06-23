package com.deviceinfo.auto

/**
 * Canonical phone / Android Auto row schemas. Used by list builders and unit tests
 * so phone and car UIs stay aligned when rows are added or removed.
 */
object DeviceInfoSchema {

    fun phoneSchema(info: DeviceInfo): List<Pair<String, List<String>>> = listOf(
        DeviceInfoUiShared.Section.BATTERY to buildList {
            add(DeviceInfoUiShared.Row.LEVEL)
            add(DeviceInfoUiShared.Row.PLUGGED)
            add(DeviceInfoUiShared.Row.HEALTH)
            add(DeviceInfoUiShared.Row.TEMPERATURE)
            add(DeviceInfoUiShared.Row.VOLTAGE)
            add(DeviceInfoUiShared.Row.CURRENT)
            add(DeviceInfoUiShared.Row.POWER)
            add(DeviceInfoUiShared.Row.CHARGE_COUNTER)
            add(DeviceInfoUiShared.Row.CYCLE_COUNT)
            add(DeviceInfoUiShared.Row.BATTERY_DESIGN_CAPACITY)
            add(DeviceInfoUiShared.Row.CHARGE_TIME_REMAINING)
            add(DeviceInfoUiShared.Row.TECHNOLOGY)
        },
        DeviceInfoUiShared.Section.DISPLAY to listOf(
            DeviceInfoUiShared.Row.RESOLUTION,
            DeviceInfoUiShared.Row.DISPLAY_DIAGONAL,
            DeviceInfoUiShared.Row.DENSITY,
            DeviceInfoUiShared.Row.REFRESH_RATE,
            DeviceInfoUiShared.Row.REFRESH_RATE_MODES,
            DeviceInfoUiShared.Row.HDR,
            DeviceInfoUiShared.Row.DISPLAY_ROTATION,
        ),
        DeviceInfoUiShared.Section.STORAGE_MEMORY to buildList {
            add(DeviceInfoUiShared.Row.STORAGE)
            info.externalStorageVolumes.forEach { add(it.rowKey) }
            add(DeviceInfoUiShared.Row.RAM)
        },
        DeviceInfoUiShared.Section.NETWORK to buildList {
            add(DeviceInfoUiShared.Row.CONNECTION)
            add(DeviceInfoUiShared.Row.INTERNET_VALIDATED)
            add(DeviceInfoUiShared.Row.CAPTIVE_PORTAL)
            add(DeviceInfoUiShared.Row.MULTI_NETWORK)
            if (info.wifiNetworkRowsVisible) {
                add(DeviceInfoUiShared.Row.WIFI_BANDS)
                add(DeviceInfoUiShared.Row.WIFI_LINK_SPEED)
                add(DeviceInfoUiShared.Row.WIFI_SIGNAL)
            }
            if (info.simDetailRowsVisible) {
                add(DeviceInfoUiShared.Row.CELLULAR)
                add(DeviceInfoUiShared.Row.CELLULAR_SIGNAL)
                add(DeviceInfoUiShared.Row.CARRIER)
                add(DeviceInfoUiShared.Row.SIM_STATE)
                add(DeviceInfoUiShared.Row.SIM_COUNTRY)
                add(DeviceInfoUiShared.Row.SIM_TYPE)
            }
            add(DeviceInfoUiShared.Row.BLUETOOTH)
            add(DeviceInfoUiShared.Row.VPN)
            add(DeviceInfoUiShared.Row.METERED)
            add(DeviceInfoUiShared.Row.ROAMING)
            add(DeviceInfoUiShared.Row.BANDWIDTH)
        },
        DeviceInfoUiShared.Section.DEVICE to listOf(
            DeviceInfoUiShared.Row.DEVICE_NAME,
            DeviceInfoUiShared.Row.MODEL,
            DeviceInfoUiShared.Row.MANUFACTURER,
            DeviceInfoUiShared.Row.CODENAME,
            DeviceInfoUiShared.Row.SYSTEM,
            DeviceInfoUiShared.Row.SECURITY_PATCH,
            DeviceInfoUiShared.Row.BOOTLOADER,
            DeviceInfoUiShared.Row.KERNEL,
            DeviceInfoUiShared.Row.BUILD,
        ),
        DeviceInfoUiShared.Section.PROCESSOR to listOf(
            DeviceInfoUiShared.Row.CPU,
            DeviceInfoUiShared.Row.CPU_FREQ,
            DeviceInfoUiShared.Row.CPU_FREQ_CURRENT,
            DeviceInfoUiShared.Row.CPU_ABI,
            DeviceInfoUiShared.Row.GPU,
            DeviceInfoUiShared.Row.OPENGL_ES,
        ),
        DeviceInfoUiShared.Section.HARDWARE to listOf(
            DeviceInfoUiShared.Row.HW_CAMERA,
            DeviceInfoUiShared.Row.HW_NFC,
            DeviceInfoUiShared.Row.HW_USB,
            DeviceInfoUiShared.Row.HW_AUDIO,
            DeviceInfoUiShared.Row.HW_BIOMETRIC,
            DeviceInfoUiShared.Row.HW_THERMAL,
        ),
        DeviceInfoUiShared.Section.SENSORS to listOf(
            DeviceInfoUiShared.Row.SENSOR_ACCELEROMETER,
            DeviceInfoUiShared.Row.SENSOR_GYROSCOPE,
            DeviceInfoUiShared.Row.SENSOR_MAGNETIC_FIELD,
            DeviceInfoUiShared.Row.SENSOR_LIGHT,
            DeviceInfoUiShared.Row.SENSOR_PROXIMITY,
            DeviceInfoUiShared.Row.SENSOR_PRESSURE,
            DeviceInfoUiShared.Row.SENSOR_HUMIDITY,
            DeviceInfoUiShared.Row.SENSOR_AMBIENT_TEMPERATURE,
            DeviceInfoUiShared.Row.SENSOR_HINGE_ANGLE,
        ),
    )

    val carSchema: List<Pair<String, List<String>>> = listOf(
        DeviceInfoUiShared.Section.BATTERY to listOf(
            DeviceInfoUiShared.Row.LEVEL,
            DeviceInfoUiShared.Row.PLUGGED,
            DeviceInfoUiShared.Row.HEALTH,
            DeviceInfoUiShared.Row.TEMPERATURE,
            DeviceInfoUiShared.Row.CURRENT,
            DeviceInfoUiShared.Row.POWER,
        ),
        DeviceInfoUiShared.Section.RAM to listOf(
            DeviceInfoUiShared.Row.RAM,
        ),
    )

    /** Row keys shown on phone when Wi‑Fi detail rows are enabled. */
    val phoneWifiRowKeys: Set<String> = setOf(
        DeviceInfoUiShared.Row.WIFI_BANDS,
        DeviceInfoUiShared.Row.WIFI_LINK_SPEED,
        DeviceInfoUiShared.Row.WIFI_SIGNAL,
    )

    /** Row keys shown on phone when SIM detail rows are enabled. */
    val phoneSimRowKeys: Set<String> = setOf(
        DeviceInfoUiShared.Row.CELLULAR,
        DeviceInfoUiShared.Row.CELLULAR_SIGNAL,
        DeviceInfoUiShared.Row.CARRIER,
        DeviceInfoUiShared.Row.SIM_STATE,
        DeviceInfoUiShared.Row.SIM_COUNTRY,
        DeviceInfoUiShared.Row.SIM_TYPE,
    )

    fun allRowKeys(schema: List<Pair<String, List<String>>>): List<String> =
        schema.flatMap { it.second }

    fun allSectionKeys(schema: List<Pair<String, List<String>>>): List<String> =
        schema.map { it.first }
}
