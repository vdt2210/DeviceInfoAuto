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
            if (BatteryRowVisibility.batteryCycleCountRowVisible(info.cycleCount)) {
                add(DeviceInfoUiShared.Row.CYCLE_COUNT)
            }
            if (BatteryRowVisibility.batteryDesignCapacityRowVisible(info.batteryDesignCapacityMicroAh)) {
                add(DeviceInfoUiShared.Row.BATTERY_DESIGN_CAPACITY)
            }
            if (BatteryRowVisibility.batteryChargeFullRowVisible(
                    info.batteryDesignCapacityMicroAh,
                    info.batteryChargeFullMicroAh,
                )
            ) {
                add(DeviceInfoUiShared.Row.BATTERY_CHARGE_FULL)
                info.batteryChargeFullSource?.let {
                    add(DeviceInfoUiShared.Row.BATTERY_CHARGE_FULL_SOURCE)
                }
            }
            if (DeviceInfoUiShared.chargeTimeRemainingRowVisible(info)) {
                add(DeviceInfoUiShared.Row.CHARGE_TIME_REMAINING)
            }
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
        DeviceInfoUiShared.Section.CONNECTION to NetworkRowVisibility.connectionRows(info),
        DeviceInfoUiShared.Section.WIRELESS to NetworkRowVisibility.wirelessRows(info),
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
            DeviceInfoUiShared.Row.HW_THERMAL,
        ),
        DeviceInfoUiShared.Section.HARDWARE to listOf(
            DeviceInfoUiShared.Row.HW_CAMERA,
            DeviceInfoUiShared.Row.HW_AUDIO,
            DeviceInfoUiShared.Row.HW_BIOMETRIC,
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
        DeviceInfoUiShared.Section.LOCATION to LocationRowVisibility.locationRows(info),
        DeviceInfoUiShared.Section.VEHICLE to VehiclePowertrainRules.rowKeys(
            info.vehicleTab.powertrain,
            dataRowsVisible = info.vehicleTab.dataRowsVisible,
        ),
    )

    fun carSchema(): List<Pair<String, List<String>>> = listOf(
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
        DeviceInfoUiShared.Row.WIFI,
        DeviceInfoUiShared.Row.WIFI_FREQUENCY,
        DeviceInfoUiShared.Row.WIFI_STANDARD,
        DeviceInfoUiShared.Row.WIFI_BANDS,
        DeviceInfoUiShared.Row.WIFI_LINK_SPEED,
        DeviceInfoUiShared.Row.WIFI_SIGNAL,
        DeviceInfoUiShared.Row.WIFI_BANDWIDTH,
    )

    /** Row keys shown on phone when SIM detail rows are enabled. */
    val phoneSimRowKeys: Set<String> = setOf(
        DeviceInfoUiShared.Row.CELLULAR,
        DeviceInfoUiShared.Row.CELLULAR_OPERATOR,
        DeviceInfoUiShared.Row.CELLULAR_SIGNAL,
        DeviceInfoUiShared.Row.CELLULAR_BANDWIDTH,
        DeviceInfoUiShared.Row.CARRIER,
        DeviceInfoUiShared.Row.SIM_PHONE_PERMISSION,
        DeviceInfoUiShared.Row.SIM_STATE,
        DeviceInfoUiShared.Row.SIM_COUNTRY,
        DeviceInfoUiShared.Row.SIM_TYPE,
    )

    val phoneConnectionRowKeys: Set<String> = setOf(
        DeviceInfoUiShared.Row.BLUETOOTH,
        DeviceInfoUiShared.Row.BLUETOOTH_TYPE,
        DeviceInfoUiShared.Row.NFC,
        DeviceInfoUiShared.Row.USB,
        DeviceInfoUiShared.Row.WIFI_DIRECT,
        DeviceInfoUiShared.Row.UWB,
        DeviceInfoUiShared.Row.CAST,
        DeviceInfoUiShared.Row.AIRPLANE_MODE,
    )

    val phoneNetworkOverviewRowKeys: Set<String> = setOf(
        DeviceInfoUiShared.Row.CONNECTION,
        DeviceInfoUiShared.Row.INTERNET_VALIDATED,
        DeviceInfoUiShared.Row.CAPTIVE_PORTAL,
        DeviceInfoUiShared.Row.MULTI_NETWORK,
        DeviceInfoUiShared.Row.ACTIVE_BANDWIDTH,
        DeviceInfoUiShared.Row.VPN,
        DeviceInfoUiShared.Row.METERED,
        DeviceInfoUiShared.Row.ROAMING,
    )

    fun connectionSchemaSignature(info: DeviceInfo): String =
        connectionSchema(info).second.joinToString(",")

    fun wirelessSchemaSignature(info: DeviceInfo): String =
        wirelessSchema(info).second.joinToString(",")

    fun connectionSchema(info: DeviceInfo): Pair<String, List<String>> =
        DeviceInfoUiShared.Section.CONNECTION to NetworkRowVisibility.connectionRows(info)

    fun wirelessSchema(info: DeviceInfo): Pair<String, List<String>> =
        DeviceInfoUiShared.Section.WIRELESS to NetworkRowVisibility.wirelessRows(info)

    fun allRowKeys(schema: List<Pair<String, List<String>>>): List<String> =
        schema.flatMap { it.second }.filter { !DeviceInfoUiShared.isSubsectionKey(it) }

    fun allSectionKeys(schema: List<Pair<String, List<String>>>): List<String> =
        schema.map { it.first }

    /** Static row → section lookup (dynamic storage volume + live schema use dedicated rules). */
    val staticRowSectionMap: Map<String, String> by lazy { buildStaticSectionRowMap() }

    private fun buildStaticSectionRowMap(): Map<String, String> {
        val connectionRows = phoneConnectionRowKeys.toList()
        val wirelessRows = phoneNetworkOverviewRowKeys.toList() +
            phoneWifiRowKeys.toList() +
            phoneSimRowKeys.toList()
        val sections = listOf(
            DeviceInfoUiShared.Section.BATTERY to listOf(
                DeviceInfoUiShared.Row.LEVEL,
                DeviceInfoUiShared.Row.PLUGGED,
                DeviceInfoUiShared.Row.HEALTH,
                DeviceInfoUiShared.Row.TEMPERATURE,
                DeviceInfoUiShared.Row.VOLTAGE,
                DeviceInfoUiShared.Row.CURRENT,
                DeviceInfoUiShared.Row.POWER,
                DeviceInfoUiShared.Row.CHARGE_COUNTER,
                DeviceInfoUiShared.Row.CYCLE_COUNT,
                DeviceInfoUiShared.Row.BATTERY_DESIGN_CAPACITY,
                DeviceInfoUiShared.Row.BATTERY_CHARGE_FULL,
                DeviceInfoUiShared.Row.BATTERY_CHARGE_FULL_SOURCE,
                DeviceInfoUiShared.Row.CHARGE_TIME_REMAINING,
                DeviceInfoUiShared.Row.TECHNOLOGY,
            ),
            DeviceInfoUiShared.Section.DISPLAY to listOf(
                DeviceInfoUiShared.Row.RESOLUTION,
                DeviceInfoUiShared.Row.DISPLAY_DIAGONAL,
                DeviceInfoUiShared.Row.DENSITY,
                DeviceInfoUiShared.Row.REFRESH_RATE,
                DeviceInfoUiShared.Row.REFRESH_RATE_MODES,
                DeviceInfoUiShared.Row.HDR,
                DeviceInfoUiShared.Row.DISPLAY_ROTATION,
            ),
            DeviceInfoUiShared.Section.STORAGE_MEMORY to listOf(
                DeviceInfoUiShared.Row.STORAGE,
                DeviceInfoUiShared.Row.RAM,
            ),
            DeviceInfoUiShared.Section.CONNECTION to connectionRows,
            DeviceInfoUiShared.Section.WIRELESS to wirelessRows,
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
                DeviceInfoUiShared.Row.HW_THERMAL,
            ),
            DeviceInfoUiShared.Section.HARDWARE to listOf(
                DeviceInfoUiShared.Row.HW_CAMERA,
                DeviceInfoUiShared.Row.HW_AUDIO,
                DeviceInfoUiShared.Row.HW_BIOMETRIC,
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
            DeviceInfoUiShared.Section.LOCATION to listOf(
                DeviceInfoUiShared.Row.LOCATION_GPS,
                DeviceInfoUiShared.Row.LOCATION_NETWORK,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_STATUS,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_COORDINATES,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_ALTITUDE,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_SPEED,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_BEARING,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_FIX_AGE,
                DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING,
                DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING_SOURCE,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_SPEED,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_BEARING,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_COORDINATES,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_ALTITUDE,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_PROVIDER,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_FIX_AGE,
                DeviceInfoUiShared.Row.LOCATION_GPS_SPEED,
                DeviceInfoUiShared.Row.LOCATION_GPS_BEARING,
                DeviceInfoUiShared.Row.LOCATION_GPS_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GPS_COORDINATES,
                DeviceInfoUiShared.Row.LOCATION_GPS_ALTITUDE,
                DeviceInfoUiShared.Row.LOCATION_GPS_PROVIDER,
                DeviceInfoUiShared.Row.LOCATION_GPS_FIX_AGE,
                DeviceInfoUiShared.Row.LOCATION_GNSS_SATELLITES,
                DeviceInfoUiShared.Row.LOCATION_GNSS_FREQUENCY_BANDS,
                DeviceInfoUiShared.Row.LOCATION_GPS_VERTICAL_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GPS_SPEED_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GPS_BEARING_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GPS_MOCK,
            ),
            DeviceInfoUiShared.Section.VEHICLE to listOf(
                DeviceInfoUiShared.Row.VEHICLE_CONNECTION,
                DeviceInfoUiShared.Row.VEHICLE_IDENTITY,
                DeviceInfoUiShared.Row.VEHICLE_MAKE,
                DeviceInfoUiShared.Row.VEHICLE_MODEL,
                DeviceInfoUiShared.Row.VEHICLE_YEAR,
                DeviceInfoUiShared.Row.VEHICLE_SPEED,
                DeviceInfoUiShared.Row.VEHICLE_SPEED_RAW,
                DeviceInfoUiShared.Row.VEHICLE_FUEL_LEVEL,
                DeviceInfoUiShared.Row.VEHICLE_FUEL_TYPES,
                DeviceInfoUiShared.Row.VEHICLE_RANGE,
                DeviceInfoUiShared.Row.VEHICLE_ODOMETER,
                DeviceInfoUiShared.Row.VEHICLE_EV_BATTERY,
                DeviceInfoUiShared.Row.VEHICLE_EV_CONNECTORS,
                DeviceInfoUiShared.Row.VEHICLE_DATA_STATUS,
            ),
        )
        return sections.flatMap { (sectionKey, rows) ->
            rows.map { rowKey -> rowKey to sectionKey }
        }.toMap()
    }
}
