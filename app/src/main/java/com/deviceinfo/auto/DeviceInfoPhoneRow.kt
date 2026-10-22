package com.deviceinfo.auto

import android.content.Context
import android.hardware.SensorManager

/** Title, value, and icon for one phone list row — shared by initial bind and live updates. */
data class PhoneRowUi(
    val title: String,
    val value: CharSequence,
    val iconResId: Int,
)

object DeviceInfoPhoneRow {

    fun shouldRefresh(
        key: String,
        updateBatteryBroadcast: Boolean,
        updateBatteryPoll: Boolean,
        updateMemory: Boolean,
        updateProcessor: Boolean,
        updateNetwork: Boolean,
        updateNetworkPoll: Boolean,
        updateDisplay: Boolean,
        updateVehicle: Boolean,
        updateLocation: Boolean,
        updateDisplayUnits: Boolean,
    ): Boolean {
        if (updateDisplayUnits) {
            return when (key) {
                DeviceInfoUiShared.Row.TEMPERATURE,
                DeviceInfoUiShared.Row.HW_THERMAL,
                DeviceInfoUiShared.Row.SENSOR_AMBIENT_TEMPERATURE,
                DeviceInfoUiShared.Row.VEHICLE_SPEED,
                DeviceInfoUiShared.Row.VEHICLE_SPEED_RAW,
                DeviceInfoUiShared.Row.VEHICLE_RANGE,
                DeviceInfoUiShared.Row.VEHICLE_ODOMETER,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_SPEED,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_ALTITUDE,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GPS_SPEED,
                DeviceInfoUiShared.Row.LOCATION_GPS_ALTITUDE,
                DeviceInfoUiShared.Row.LOCATION_GPS_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GPS_SPEED_ACCURACY,
                -> true
                else -> false
            }
        }
        return when (key) {
        DeviceInfoUiShared.Row.LEVEL,
        DeviceInfoUiShared.Row.PLUGGED,
        DeviceInfoUiShared.Row.HEALTH,
        DeviceInfoUiShared.Row.TECHNOLOGY,
        DeviceInfoUiShared.Row.TEMPERATURE,
        DeviceInfoUiShared.Row.CHARGE_COUNTER,
        -> updateBatteryBroadcast

        DeviceInfoUiShared.Row.VOLTAGE,
        DeviceInfoUiShared.Row.CURRENT,
        DeviceInfoUiShared.Row.POWER,
        DeviceInfoUiShared.Row.CHARGE_TIME_REMAINING,
        -> updateBatteryPoll

        DeviceInfoUiShared.Row.CYCLE_COUNT,
        DeviceInfoUiShared.Row.BATTERY_DESIGN_CAPACITY,
        DeviceInfoUiShared.Row.BATTERY_CHARGE_FULL,
        DeviceInfoUiShared.Row.BATTERY_CHARGE_FULL_SOURCE,
        -> false

        DeviceInfoUiShared.Row.STORAGE,
        DeviceInfoUiShared.Row.RAM,
        -> updateMemory

        DeviceInfoUiShared.Row.CPU_FREQ_CURRENT,
        DeviceInfoUiShared.Row.HW_THERMAL,
        -> updateProcessor

        DeviceInfoUiShared.Row.CONNECTION,
        DeviceInfoUiShared.Row.INTERNET_VALIDATED,
        DeviceInfoUiShared.Row.CAPTIVE_PORTAL,
        DeviceInfoUiShared.Row.MULTI_NETWORK,
        DeviceInfoUiShared.Row.ACTIVE_BANDWIDTH,
        DeviceInfoUiShared.Row.WIFI,
        DeviceInfoUiShared.Row.WIFI_BANDS,
        -> updateNetwork

        DeviceInfoUiShared.Row.WIFI_FREQUENCY,
        DeviceInfoUiShared.Row.WIFI_STANDARD,
        DeviceInfoUiShared.Row.WIFI_LINK_SPEED,
        DeviceInfoUiShared.Row.WIFI_SIGNAL,
        DeviceInfoUiShared.Row.WIFI_BANDWIDTH,
        DeviceInfoUiShared.Row.CELLULAR_SIGNAL,
        DeviceInfoUiShared.Row.CELLULAR_BANDWIDTH,
        DeviceInfoUiShared.Row.CELLULAR,
        DeviceInfoUiShared.Row.CELLULAR_OPERATOR,
        -> updateNetworkPoll

        DeviceInfoUiShared.Row.BLUETOOTH,
        DeviceInfoUiShared.Row.CAST,
        DeviceInfoUiShared.Row.AIRPLANE_MODE,
        DeviceInfoUiShared.Row.SIM_PHONE_PERMISSION,
        DeviceInfoUiShared.Row.CARRIER,
        DeviceInfoUiShared.Row.SIM_STATE,
        DeviceInfoUiShared.Row.SIM_COUNTRY,
        DeviceInfoUiShared.Row.SIM_TYPE,
        DeviceInfoUiShared.Row.VPN,
        DeviceInfoUiShared.Row.METERED,
        DeviceInfoUiShared.Row.ROAMING,
        -> updateNetwork

        DeviceInfoUiShared.Row.RESOLUTION,
        DeviceInfoUiShared.Row.DISPLAY_DIAGONAL,
        DeviceInfoUiShared.Row.DENSITY,
        DeviceInfoUiShared.Row.REFRESH_RATE,
        DeviceInfoUiShared.Row.REFRESH_RATE_MODES,
        DeviceInfoUiShared.Row.HDR,
        DeviceInfoUiShared.Row.DISPLAY_ROTATION,
        -> updateDisplay

        DeviceInfoUiShared.Row.VEHICLE_CONNECTION,
        DeviceInfoUiShared.Row.VEHICLE_IDENTITY,
        DeviceInfoUiShared.Row.VEHICLE_MAKE,
        DeviceInfoUiShared.Row.VEHICLE_MODEL,
        DeviceInfoUiShared.Row.VEHICLE_YEAR,
        DeviceInfoUiShared.Row.VEHICLE_SPEED,
        DeviceInfoUiShared.Row.VEHICLE_SPEED_RAW,
        DeviceInfoUiShared.Row.VEHICLE_FUEL_LEVEL,
        DeviceInfoUiShared.Row.VEHICLE_EV_BATTERY,
        DeviceInfoUiShared.Row.VEHICLE_RANGE,
        DeviceInfoUiShared.Row.VEHICLE_ODOMETER,
        DeviceInfoUiShared.Row.VEHICLE_FUEL_TYPES,
        DeviceInfoUiShared.Row.VEHICLE_EV_CONNECTORS,
        DeviceInfoUiShared.Row.VEHICLE_DATA_STATUS,
        -> updateVehicle

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
        -> updateLocation

        else -> updateMemory && DeviceInfoUiShared.Row.isStorageVolumeRowKey(key)
        }
    }

    fun uiState(
        context: Context,
        key: String,
        info: DeviceInfo,
        displayRotation: Int,
        sensorDisplayValue: CharSequence,
        fallbackNotAvailable: CharSequence,
    ): PhoneRowUi? {
        val title = { rowKey: String -> DeviceInfoUiShared.rowTitle(context, rowKey) }
        return when (key) {
            DeviceInfoUiShared.Row.LEVEL -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.levelText(context, info, DeviceInfoUiShared.DisplaySurface.PHONE),
                DeviceInfoUiShared.levelIconRes(info.batteryLevel, info.isPowerSaveMode),
            )
            DeviceInfoUiShared.Row.PLUGGED -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.pluggedLabel(context, info.plugged),
                DeviceInfoUiShared.pluggedIconRes(info.plugged),
            )
            DeviceInfoUiShared.Row.HEALTH -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.healthLabel(context, info.health),
                DeviceInfoUiShared.healthIconRes(info.health),
            )
            DeviceInfoUiShared.Row.TECHNOLOGY -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.technologyLabel(context, info.technology),
                R.drawable.ic_row_battery_full,
            )
            DeviceInfoUiShared.Row.TEMPERATURE -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.temperatureText(context, info.batteryTemperatureCelsius),
                DeviceInfoUiShared.temperatureIconRes(info.batteryTemperatureCelsius),
            )
            DeviceInfoUiShared.Row.VOLTAGE -> PhoneRowUi(
                title(key),
                String.format("%.2f V", info.batteryVoltageV),
                R.drawable.ic_row_voltage,
            )
            DeviceInfoUiShared.Row.CURRENT -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.currentText(info.currentNowMicroA),
                R.drawable.ic_row_current,
            )
            DeviceInfoUiShared.Row.POWER -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.instantPowerText(info.batteryVoltageV, info.currentNowMicroA),
                R.drawable.ic_row_current,
            )
            DeviceInfoUiShared.Row.CHARGE_COUNTER -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.chargeCounterText(info.chargeCounterMicroAh),
                R.drawable.ic_row_charge_counter,
            )
            DeviceInfoUiShared.Row.CYCLE_COUNT -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.cycleCountText(context, info.cycleCount),
                R.drawable.ic_row_cycle_count,
            )
            DeviceInfoUiShared.Row.BATTERY_DESIGN_CAPACITY -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.batteryDesignCapacityText(context, info.batteryDesignCapacityMicroAh),
                R.drawable.ic_row_battery_full,
            )
            DeviceInfoUiShared.Row.BATTERY_CHARGE_FULL -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.batteryDesignCapacityText(context, info.batteryChargeFullMicroAh),
                R.drawable.ic_row_cycle_count,
            )
            DeviceInfoUiShared.Row.BATTERY_CHARGE_FULL_SOURCE -> PhoneRowUi(
                title(key),
                when (info.batteryChargeFullSource) {
                    BatteryCapacitySource.BATTERY_MANAGER ->
                        context.getString(R.string.battery_capacity_source_battery_manager)
                    BatteryCapacitySource.BATTERY_BROADCAST ->
                        context.getString(R.string.battery_capacity_source_broadcast)
                    BatteryCapacitySource.SYSFS ->
                        context.getString(R.string.battery_capacity_source_sysfs)
                    null -> fallbackNotAvailable
                },
                R.drawable.ic_row_terminal,
            )
            DeviceInfoUiShared.Row.CHARGE_TIME_REMAINING -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.chargeTimeRemainingText(context, info.chargeTimeRemainingMs),
                R.drawable.ic_row_plugged,
            )
            DeviceInfoUiShared.Row.RESOLUTION -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.resolutionDisplay(context, info.displayResolution),
                R.drawable.ic_row_model,
            )
            DeviceInfoUiShared.Row.DISPLAY_DIAGONAL -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.displayDiagonalText(context, info.displayDiagonalInches),
                R.drawable.ic_row_density,
            )
            DeviceInfoUiShared.Row.DENSITY -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.densityLabel(info.screenDensityDpi),
                R.drawable.ic_row_density,
            )
            DeviceInfoUiShared.Row.REFRESH_RATE -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.refreshRateDisplay(context, info.displayRefreshRateHz),
                R.drawable.ic_row_refresh_rate,
            )
            DeviceInfoUiShared.Row.REFRESH_RATE_MODES -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.displayRefreshModesText(context, info.displayRefreshModesSummary),
                R.drawable.ic_row_refresh_rate,
            )
            DeviceInfoUiShared.Row.HDR -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.displayHdrSummaryText(context, info.displayHdrSummary),
                R.drawable.ic_row_extension,
            )
            DeviceInfoUiShared.Row.DISPLAY_ROTATION -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.displayRotationLabel(context, displayRotation),
                R.drawable.ic_row_model,
            )
            DeviceInfoUiShared.Row.STORAGE -> PhoneRowUi(
                title(key),
                info.storageSummary,
                R.drawable.ic_row_storage,
            )
            DeviceInfoUiShared.Row.RAM -> PhoneRowUi(
                title(key),
                info.ramSummary,
                R.drawable.ic_row_memory,
            )
            DeviceInfoUiShared.Row.CONNECTION -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.connectionLabel(context, info.connectionType),
                DeviceInfoUiShared.connectionIconRes(info.connectionType),
            )
            DeviceInfoUiShared.Row.INTERNET_VALIDATED -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.internetValidatedDisplay(context, info.internetValidated, info.connectionType),
                DeviceInfoUiShared.connectionIconRes(info.connectionType),
            )
            DeviceInfoUiShared.Row.CAPTIVE_PORTAL -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.yesNoOptional(context, info.captivePortal),
                R.drawable.ic_row_wifi,
            )
            DeviceInfoUiShared.Row.MULTI_NETWORK -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.multiNetworkSummaryDisplay(context, info.multiNetworkSummary),
                R.drawable.ic_row_multi_network,
            )
            DeviceInfoUiShared.Row.ACTIVE_BANDWIDTH -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.bandwidthDisplay(
                    context,
                    info.downstreamKbps,
                    info.upstreamKbps,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                R.drawable.ic_row_speed,
            )
            DeviceInfoUiShared.Row.WIFI -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.wifiState(
                    context,
                    info.wifiOn,
                    permissionRequired = info.wifiNetworkRowsVisible && info.wifiOn == null,
                ),
                R.drawable.ic_row_wifi,
            )
            DeviceInfoUiShared.Row.WIFI_FREQUENCY -> PhoneRowUi(
                title(key),
                info.wifiFrequencySummary ?: fallbackNotAvailable,
                R.drawable.ic_row_wifi,
            )
            DeviceInfoUiShared.Row.WIFI_STANDARD -> PhoneRowUi(
                title(key),
                info.wifiStandardSummary ?: fallbackNotAvailable,
                R.drawable.ic_row_wifi,
            )
            DeviceInfoUiShared.Row.WIFI_BANDS -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.networkReadingOrNotAvailable(context, info.wifiSupportedBandsSummary),
                R.drawable.ic_row_wifi,
            )
            DeviceInfoUiShared.Row.WIFI_LINK_SPEED -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.networkReadingOrNotAvailable(context, info.wifiLinkSpeedSummary),
                R.drawable.ic_row_wifi,
            )
            DeviceInfoUiShared.Row.WIFI_SIGNAL -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.networkReadingOrNotAvailable(context, info.wifiSignalSummary),
                R.drawable.ic_row_wifi,
            )
            DeviceInfoUiShared.Row.WIFI_BANDWIDTH -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.bandwidthDisplay(
                    context,
                    info.wifiDownstreamKbps,
                    info.wifiUpstreamKbps,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                R.drawable.ic_row_speed,
            )
            DeviceInfoUiShared.Row.CELLULAR -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.cellularNetworkLabelDisplay(context, info.cellularType),
                R.drawable.ic_row_cell_tower,
            )
            DeviceInfoUiShared.Row.CELLULAR_OPERATOR -> PhoneRowUi(
                title(key),
                info.cellularOperatorSummary ?: fallbackNotAvailable,
                R.drawable.ic_row_cell_tower,
            )
            DeviceInfoUiShared.Row.CELLULAR_SIGNAL -> PhoneRowUi(
                title(key),
                info.cellularSignalSummary,
                R.drawable.ic_row_cell_tower,
            )
            DeviceInfoUiShared.Row.CELLULAR_BANDWIDTH -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.bandwidthDisplay(
                    context,
                    info.cellularDownstreamKbps,
                    info.cellularUpstreamKbps,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                R.drawable.ic_row_speed,
            )
            DeviceInfoUiShared.Row.CARRIER -> PhoneRowUi(
                title(key),
                info.simCarrier,
                R.drawable.ic_row_sim_card,
            )
            DeviceInfoUiShared.Row.SIM_STATE -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.simStateSummaryDisplay(context, info.simState),
                R.drawable.ic_row_sim_card,
            )
            DeviceInfoUiShared.Row.SIM_COUNTRY -> PhoneRowUi(
                title(key),
                info.simCountryIso,
                R.drawable.ic_row_language,
            )
            DeviceInfoUiShared.Row.SIM_TYPE -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.simTypeSummaryDisplay(context, info.simTypeSummary),
                R.drawable.ic_row_sim_card,
            )
            DeviceInfoUiShared.Row.SIM_PHONE_PERMISSION -> PhoneRowUi(
                title(key),
                context.getString(R.string.value_permission_required),
                R.drawable.ic_row_sim_card,
            )
            DeviceInfoUiShared.Row.BLUETOOTH -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.bluetoothState(
                    context,
                    info.bluetoothOn,
                    permissionRequired = info.bluetoothCapable && info.bluetoothOn == null,
                ),
                R.drawable.ic_row_bluetooth,
            )
            DeviceInfoUiShared.Row.BLUETOOTH_TYPE -> PhoneRowUi(
                title(key),
                info.bluetoothTypeSummary,
                R.drawable.ic_row_bluetooth,
            )
            DeviceInfoUiShared.Row.CAST -> PhoneRowUi(
                title(key),
                info.castSummary,
                R.drawable.ic_row_cast,
            )
            DeviceInfoUiShared.Row.WIFI_DIRECT -> PhoneRowUi(
                title(key),
                info.wifiDirectSummary,
                R.drawable.ic_row_wifi_direct,
            )
            DeviceInfoUiShared.Row.UWB -> PhoneRowUi(
                title(key),
                info.uwbSummary,
                R.drawable.ic_row_uwb,
            )
            DeviceInfoUiShared.Row.AIRPLANE_MODE -> PhoneRowUi(
                title(key),
                info.airplaneModeOn?.let { DeviceInfoUiShared.onOff(context, it) }
                    ?: fallbackNotAvailable,
                R.drawable.ic_row_airplane_mode,
            )
            DeviceInfoUiShared.Row.VPN -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.yesNo(context, info.isVpn),
                R.drawable.ic_row_vpn,
            )
            DeviceInfoUiShared.Row.METERED -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.yesNo(context, info.isMeteredConnection),
                R.drawable.ic_row_data_usage,
            )
            DeviceInfoUiShared.Row.ROAMING -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.yesNo(context, info.isRoaming),
                R.drawable.ic_row_roaming,
            )
            DeviceInfoUiShared.Row.DEVICE_NAME -> PhoneRowUi(title(key), info.deviceName, R.drawable.ic_row_model)
            DeviceInfoUiShared.Row.MODEL -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.unknownWord(context, info.model),
                R.drawable.ic_row_model,
            )
            DeviceInfoUiShared.Row.MANUFACTURER -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.unknownWord(context, info.manufacturer),
                R.drawable.ic_row_manufacturer,
            )
            DeviceInfoUiShared.Row.CODENAME -> PhoneRowUi(title(key), info.deviceCodename, R.drawable.ic_row_codename)
            DeviceInfoUiShared.Row.SYSTEM -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.systemVersionDisplay(context, info),
                R.drawable.ic_row_android,
            )
            DeviceInfoUiShared.Row.SECURITY_PATCH -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.unknownWord(context, info.securityPatch),
                R.drawable.ic_row_shield,
            )
            DeviceInfoUiShared.Row.BOOTLOADER -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.unknownWord(context, info.bootloader),
                R.drawable.ic_row_bootloader,
            )
            DeviceInfoUiShared.Row.KERNEL -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.sysfsReadingOrNotAvailable(context, info.kernelVersion),
                R.drawable.ic_row_terminal,
            )
            DeviceInfoUiShared.Row.BUILD -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.unknownWord(context, info.buildId),
                R.drawable.ic_row_build,
            )
            DeviceInfoUiShared.Row.CPU -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.sysfsReadingOrNotAvailable(context, info.cpuInfo),
                R.drawable.ic_row_memory,
            )
            DeviceInfoUiShared.Row.CPU_FREQ -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.sysfsReadingOrNotAvailable(context, info.cpuFreq),
                R.drawable.ic_row_speed,
            )
            DeviceInfoUiShared.Row.CPU_FREQ_CURRENT -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.sysfsReadingOrNotAvailable(context, info.cpuCurrentFreq),
                R.drawable.ic_row_speed,
            )
            DeviceInfoUiShared.Row.CPU_ABI -> PhoneRowUi(title(key), info.cpuAbi, R.drawable.ic_row_memory)
            DeviceInfoUiShared.Row.GPU -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.sysfsReadingOrNotAvailable(context, info.gpuRenderer),
                R.drawable.ic_row_memory,
            )
            DeviceInfoUiShared.Row.OPENGL_ES -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.sysfsReadingOrNotAvailable(context, info.gpuGlVersion),
                R.drawable.ic_row_extension,
            )
            DeviceInfoUiShared.Row.HW_CAMERA -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.listSummaryForDisplay(
                    context,
                    info.cameraSummary,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                R.drawable.ic_row_camera,
            )
            DeviceInfoUiShared.Row.NFC -> PhoneRowUi(title(key), info.nfcSummary, R.drawable.ic_row_nfc)
            DeviceInfoUiShared.Row.USB -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.listSummaryForDisplay(
                    context,
                    info.usbSummary,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                R.drawable.ic_row_usb,
            )
            DeviceInfoUiShared.Row.HW_AUDIO -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.listSummaryForDisplay(
                    context,
                    info.audioSummary,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                R.drawable.ic_row_speaker,
            )
            DeviceInfoUiShared.Row.HW_BIOMETRIC -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.listSummaryForDisplay(
                    context,
                    info.biometricSummary,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                R.drawable.ic_row_fingerprint,
            )
            DeviceInfoUiShared.Row.HW_THERMAL -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.compoundSummaryForDisplay(
                    context,
                    info.thermalSummary,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                R.drawable.ic_row_memory,
            )
            DeviceInfoUiShared.Row.VEHICLE_CONNECTION -> PhoneRowUi(
                title(key),
                info.vehicleTab.connectionSummary,
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_IDENTITY -> PhoneRowUi(
                title(key),
                listOf(
                    info.vehicleTab.make,
                    info.vehicleTab.model,
                    info.vehicleTab.year,
                ).filter {
                    it.isNotBlank() &&
                        it != "—" &&
                        it != fallbackNotAvailable.toString()
                }
                    .joinToString(DeviceInfoUiShared.BULLET_SEP)
                    .ifBlank { fallbackNotAvailable.toString() },
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_MAKE -> PhoneRowUi(
                title(key),
                info.vehicleTab.make,
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_MODEL -> PhoneRowUi(
                title(key),
                info.vehicleTab.model,
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_YEAR -> PhoneRowUi(
                title(key),
                info.vehicleTab.year,
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_SPEED -> PhoneRowUi(
                title(key),
                info.vehicleTab.speedDisplay,
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_SPEED_RAW -> PhoneRowUi(
                title(key),
                info.vehicleTab.speedRaw,
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_FUEL_LEVEL -> PhoneRowUi(
                title(key),
                VehicleInfoFormats.fuelDisplay(
                    context,
                    info.vehicleTab.fuelLevel,
                    info.vehicleTab.fuelTypes,
                ).phoneText().ifBlank { fallbackNotAvailable.toString() },
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_EV_BATTERY -> PhoneRowUi(
                title(key),
                VehicleInfoFormats.evBatteryDisplay(
                    context,
                    info.vehicleTab.evBattery,
                    info.vehicleTab.evConnectors,
                ).phoneText().ifBlank { fallbackNotAvailable.toString() },
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_RANGE -> PhoneRowUi(
                title(key),
                info.vehicleTab.rangeRemaining,
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_ODOMETER -> PhoneRowUi(
                title(key),
                info.vehicleTab.odometer,
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_FUEL_TYPES -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.listSummaryForDisplay(
                    context,
                    info.vehicleTab.fuelTypes,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_EV_CONNECTORS -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.listSummaryForDisplay(
                    context,
                    info.vehicleTab.evConnectors,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.VEHICLE_DATA_STATUS -> PhoneRowUi(
                title(key),
                info.vehicleTab.dataStatus,
                DeviceInfoUiShared.vehicleRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_GPS -> PhoneRowUi(
                title(key),
                locationProviderValue(context, info.locationTab.gpsEnabled, fallbackNotAvailable),
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_GPS_COORDINATES -> locationCoordinatesUi(
                context,
                key,
                info.locationTab.gpsFix,
                fallbackNotAvailable,
            )
            DeviceInfoUiShared.Row.LOCATION_NETWORK -> PhoneRowUi(
                title(key),
                locationProviderValue(context, info.locationTab.networkEnabled, fallbackNotAvailable),
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_NETWORK_COORDINATES -> locationCoordinatesUi(
                context,
                key,
                info.locationTab.networkFix,
                fallbackNotAvailable,
            )
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_STATUS -> PhoneRowUi(
                title(key),
                when (info.locationTab.googlePlayServicesLocationStatus) {
                    GooglePlayServicesLocationStatus.WAITING ->
                        context.getString(R.string.location_google_status_waiting)
                    GooglePlayServicesLocationStatus.AVAILABLE ->
                        context.getString(R.string.location_google_status_available)
                    GooglePlayServicesLocationStatus.UNAVAILABLE ->
                        context.getString(R.string.location_google_status_unavailable)
                    GooglePlayServicesLocationStatus.ERROR ->
                        context.getString(R.string.location_google_status_error)
                },
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_COORDINATES -> PhoneRowUi(
                title(key),
                locationFixValue(
                    context,
                    info.locationTab.googlePlayServicesFix,
                    true,
                    fallbackNotAvailable,
                ) {
                    LocationInfoFormats.coordinatesDisplay(
                        context,
                        info.locationTab.googlePlayServicesFix.latitude,
                        info.locationTab.googlePlayServicesFix.longitude,
                    )
                },
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_ACCURACY,
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_ALTITUDE,
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_SPEED,
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_BEARING,
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_FIX_AGE,
            -> locationFixUi(
                context,
                key,
                info.locationTab.googlePlayServicesFix,
                fallbackNotAvailable,
            )
            DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING -> PhoneRowUi(
                title(key),
                if (info.locationTab.hasDeviceHeading) {
                    LocationInfoFormats.bearingDisplay(
                        context,
                        info.locationTab.deviceHeadingDegrees,
                    )
                } else {
                    fallbackNotAvailable
                },
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING_ACCURACY -> PhoneRowUi(
                title(key),
                headingAccuracyLabel(
                    context,
                    info.locationTab.deviceHeadingAccuracy,
                    fallbackNotAvailable,
                ),
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING_SOURCE -> PhoneRowUi(
                title(key),
                when (info.locationTab.deviceHeadingSource) {
                    DeviceHeadingSource.ROTATION_VECTOR ->
                        context.getString(R.string.location_heading_source_rotation_vector)
                    DeviceHeadingSource.ACCELEROMETER_MAGNETIC_FIELD ->
                        context.getString(R.string.location_heading_source_fallback)
                    null -> fallbackNotAvailable
                },
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_NETWORK_SPEED ->
                locationFixUi(context, key, info.locationTab.networkFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_NETWORK_BEARING ->
                locationFixUi(context, key, info.locationTab.networkFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_NETWORK_ACCURACY ->
                locationFixUi(context, key, info.locationTab.networkFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_NETWORK_ALTITUDE ->
                locationFixUi(context, key, info.locationTab.networkFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_NETWORK_PROVIDER ->
                locationFixUi(context, key, info.locationTab.networkFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_NETWORK_FIX_AGE ->
                locationFixUi(context, key, info.locationTab.networkFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_GPS_SPEED ->
                locationFixUi(context, key, info.locationTab.gpsFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_GPS_BEARING ->
                locationFixUi(context, key, info.locationTab.gpsFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_GPS_ACCURACY ->
                locationFixUi(context, key, info.locationTab.gpsFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_GPS_ALTITUDE ->
                locationFixUi(context, key, info.locationTab.gpsFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_GPS_PROVIDER ->
                locationFixUi(context, key, info.locationTab.gpsFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_GPS_FIX_AGE ->
                locationFixUi(context, key, info.locationTab.gpsFix, fallbackNotAvailable)
            DeviceInfoUiShared.Row.LOCATION_GPS_VERTICAL_ACCURACY -> PhoneRowUi(
                title(key),
                locationFixValue(
                    context,
                    info.locationTab.gpsFix,
                    info.locationTab.gpsFix.hasVerticalAccuracy,
                    fallbackNotAvailable,
                ) {
                    LocationInfoFormats.accuracyDisplay(
                        context,
                        info.locationTab.gpsFix.verticalAccuracyMeters,
                    )
                },
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_GPS_SPEED_ACCURACY -> PhoneRowUi(
                title(key),
                locationFixValue(
                    context,
                    info.locationTab.gpsFix,
                    info.locationTab.gpsFix.hasSpeedAccuracy,
                    fallbackNotAvailable,
                ) {
                    context.getString(
                        R.string.location_speed_accuracy_format,
                        MeasurementUnits.speed(
                            context,
                            info.locationTab.gpsFix.speedAccuracyMps,
                        ),
                    )
                },
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_GPS_BEARING_ACCURACY -> PhoneRowUi(
                title(key),
                locationFixValue(
                    context,
                    info.locationTab.gpsFix,
                    info.locationTab.gpsFix.hasBearingAccuracy,
                    fallbackNotAvailable,
                ) {
                    context.getString(
                        R.string.location_bearing_accuracy_format,
                        info.locationTab.gpsFix.bearingAccuracyDegrees,
                    )
                },
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_GPS_MOCK -> PhoneRowUi(
                title(key),
                locationFixValue(
                    context,
                    info.locationTab.gpsFix,
                    true,
                    fallbackNotAvailable,
                ) {
                    DeviceInfoUiShared.yesNo(context, info.locationTab.gpsFix.isMock)
                },
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_GNSS_SATELLITES -> PhoneRowUi(
                title(key),
                gnssValue(context, fallbackNotAvailable) {
                    val used = info.locationTab.gnss.satellitesUsedInFix
                    val visible = info.locationTab.gnss.satellitesVisible
                    if (used == null || visible == null) {
                        fallbackNotAvailable
                    } else {
                        context.getString(
                            R.string.location_gnss_satellites_format,
                            used,
                            visible,
                        )
                    }
                },
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.LOCATION_GNSS_FREQUENCY_BANDS -> PhoneRowUi(
                title(key),
                gnssValue(context, fallbackNotAvailable) {
                    info.locationTab.gnss.frequencyBands
                        .takeIf { it.isNotEmpty() }
                        ?.let(DeviceInfoUiShared::joinList)
                        ?: fallbackNotAvailable
                },
                DeviceInfoUiShared.locationRowIconRes(key),
            )
            DeviceInfoUiShared.Row.SENSOR_ACCELEROMETER,
            DeviceInfoUiShared.Row.SENSOR_GYROSCOPE,
            DeviceInfoUiShared.Row.SENSOR_MAGNETIC_FIELD,
            DeviceInfoUiShared.Row.SENSOR_LIGHT,
            DeviceInfoUiShared.Row.SENSOR_PROXIMITY,
            DeviceInfoUiShared.Row.SENSOR_PRESSURE,
            DeviceInfoUiShared.Row.SENSOR_HUMIDITY,
            DeviceInfoUiShared.Row.SENSOR_AMBIENT_TEMPERATURE,
            DeviceInfoUiShared.Row.SENSOR_HINGE_ANGLE,
            -> PhoneRowUi(title(key), sensorDisplayValue, R.drawable.ic_row_sensor)

            else -> {
                val vol = info.externalStorageVolumes.find { it.rowKey == key }
                if (vol != null) {
                    PhoneRowUi(vol.title, vol.summary, R.drawable.ic_row_storage)
                } else {
                    PhoneRowUi(title(key), fallbackNotAvailable, R.drawable.ic_row_unknown)
                }
            }
        }
    }

    private fun locationProviderValue(
        context: Context,
        enabled: Boolean?,
        fallbackNotAvailable: CharSequence,
    ): CharSequence = when (enabled) {
        true -> DeviceInfoUiShared.onOff(context, true)
        false -> DeviceInfoUiShared.onOff(context, false)
        null -> fallbackNotAvailable
    }

    private fun headingAccuracyLabel(
        context: Context,
        accuracy: Int?,
        fallbackNotAvailable: CharSequence,
    ): CharSequence = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH ->
            context.getString(R.string.location_heading_accuracy_high)
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ->
            context.getString(R.string.location_heading_accuracy_medium)
        SensorManager.SENSOR_STATUS_ACCURACY_LOW ->
            context.getString(R.string.location_heading_accuracy_low)
        SensorManager.SENSOR_STATUS_UNRELIABLE ->
            context.getString(R.string.location_heading_accuracy_unreliable)
        else -> fallbackNotAvailable
    }

    private fun gnssValue(
        context: Context,
        fallbackNotAvailable: CharSequence,
        display: () -> CharSequence,
    ): CharSequence {
        if (!SensitivePermissions.hasFineLocationPermission(context)) {
            return context.getString(R.string.value_permission_required)
        }
        return display().takeIf { it.isNotEmpty() } ?: fallbackNotAvailable
    }

    private fun locationFixUi(
        context: Context,
        key: String,
        fix: LocationFixSnapshot,
        fallbackNotAvailable: CharSequence,
    ): PhoneRowUi {
        val value = when (key) {
            DeviceInfoUiShared.Row.LOCATION_NETWORK_SPEED,
            DeviceInfoUiShared.Row.LOCATION_GPS_SPEED,
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_SPEED,
            -> locationFixValue(context, fix, fix.hasSpeed, fallbackNotAvailable) {
                MeasurementUnits.speed(context, fix.speedMps)
            }
            DeviceInfoUiShared.Row.LOCATION_NETWORK_BEARING,
            DeviceInfoUiShared.Row.LOCATION_GPS_BEARING,
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_BEARING,
            -> locationFixValue(context, fix, fix.hasBearing, fallbackNotAvailable) {
                LocationInfoFormats.bearingDisplay(context, fix.bearingDegrees)
            }
            DeviceInfoUiShared.Row.LOCATION_NETWORK_ACCURACY,
            DeviceInfoUiShared.Row.LOCATION_GPS_ACCURACY,
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_ACCURACY,
            -> locationFixValue(context, fix, fix.hasAccuracy, fallbackNotAvailable) {
                LocationInfoFormats.accuracyDisplay(context, fix.accuracyMeters)
            }
            DeviceInfoUiShared.Row.LOCATION_NETWORK_ALTITUDE,
            DeviceInfoUiShared.Row.LOCATION_GPS_ALTITUDE,
            DeviceInfoUiShared.Row.LOCATION_GOOGLE_ALTITUDE,
            -> locationFixValue(context, fix, fix.hasAltitude, fallbackNotAvailable) {
                MeasurementUnits.altitude(context, fix.altitudeMeters)
            }
            DeviceInfoUiShared.Row.LOCATION_NETWORK_PROVIDER,
            DeviceInfoUiShared.Row.LOCATION_GPS_PROVIDER,
            -> locationFixValue(context, fix, true, fallbackNotAvailable) {
                LocationInfoFormats.providerLabel(context, fix.provider)
            }
            else -> locationFixValue(context, fix, true, fallbackNotAvailable) {
                LocationInfoFormats.fixAgeDisplay(context, fix.fixAgeMs)
            }
        }
        return PhoneRowUi(
            DeviceInfoUiShared.rowTitle(context, key),
            value,
            DeviceInfoUiShared.locationRowIconRes(key),
        )
    }

    private fun locationCoordinatesUi(
        context: Context,
        key: String,
        fix: LocationFixSnapshot,
        fallbackNotAvailable: CharSequence,
    ): PhoneRowUi = PhoneRowUi(
        DeviceInfoUiShared.rowTitle(context, key),
        locationFixValue(context, fix, true, fallbackNotAvailable) {
            LocationInfoFormats.coordinatesDisplay(context, fix.latitude, fix.longitude)
        },
        DeviceInfoUiShared.locationRowIconRes(key),
    )

    private fun locationFixValue(
        context: Context,
        fix: LocationFixSnapshot,
        fieldAvailable: Boolean,
        fallbackNotAvailable: CharSequence,
        display: () -> CharSequence,
    ): CharSequence {
        if (!SensitivePermissions.hasLocationPermission(context)) {
            return context.getString(R.string.value_permission_required)
        }
        if (!fix.hasFix || !fieldAvailable) return fallbackNotAvailable
        return display()
    }
}
