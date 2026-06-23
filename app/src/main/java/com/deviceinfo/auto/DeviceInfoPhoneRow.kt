package com.deviceinfo.auto

import android.content.Context

/** Title, value, and icon for one phone list row — shared by initial bind and live updates. */
data class PhoneRowUi(
    val title: String,
    val value: CharSequence,
    val iconResId: Int,
)

object DeviceInfoPhoneRow {

    fun shouldRefresh(
        key: String,
        updateBattery: Boolean,
        updateMemory: Boolean,
        updateProcessor: Boolean,
        updateNetwork: Boolean,
        updateDisplay: Boolean,
    ): Boolean = when (key) {
        DeviceInfoUiShared.Row.LEVEL,
        DeviceInfoUiShared.Row.PLUGGED,
        DeviceInfoUiShared.Row.HEALTH,
        DeviceInfoUiShared.Row.TECHNOLOGY,
        DeviceInfoUiShared.Row.TEMPERATURE,
        DeviceInfoUiShared.Row.VOLTAGE,
        DeviceInfoUiShared.Row.CURRENT,
        DeviceInfoUiShared.Row.POWER,
        DeviceInfoUiShared.Row.CHARGE_COUNTER,
        DeviceInfoUiShared.Row.CYCLE_COUNT,
        DeviceInfoUiShared.Row.BATTERY_DESIGN_CAPACITY,
        DeviceInfoUiShared.Row.CHARGE_TIME_REMAINING,
        -> updateBattery

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
        DeviceInfoUiShared.Row.WIFI_BANDS,
        DeviceInfoUiShared.Row.WIFI_LINK_SPEED,
        DeviceInfoUiShared.Row.WIFI_SIGNAL,
        DeviceInfoUiShared.Row.BLUETOOTH,
        DeviceInfoUiShared.Row.CELLULAR,
        DeviceInfoUiShared.Row.CELLULAR_SIGNAL,
        DeviceInfoUiShared.Row.CARRIER,
        DeviceInfoUiShared.Row.SIM_STATE,
        DeviceInfoUiShared.Row.SIM_COUNTRY,
        DeviceInfoUiShared.Row.SIM_TYPE,
        DeviceInfoUiShared.Row.VPN,
        DeviceInfoUiShared.Row.METERED,
        DeviceInfoUiShared.Row.ROAMING,
        DeviceInfoUiShared.Row.BANDWIDTH,
        -> updateNetwork

        DeviceInfoUiShared.Row.RESOLUTION,
        DeviceInfoUiShared.Row.DISPLAY_DIAGONAL,
        DeviceInfoUiShared.Row.DENSITY,
        DeviceInfoUiShared.Row.REFRESH_RATE,
        DeviceInfoUiShared.Row.REFRESH_RATE_MODES,
        DeviceInfoUiShared.Row.HDR,
        DeviceInfoUiShared.Row.DISPLAY_ROTATION,
        -> updateDisplay

        else -> updateMemory && DeviceInfoUiShared.Row.isStorageVolumeRowKey(key)
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
                DeviceInfoUiShared.temperatureText(info.batteryTemperatureCelsius),
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
            DeviceInfoUiShared.Row.CELLULAR -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.cellularNetworkLabelDisplay(context, info.cellularType),
                R.drawable.ic_row_cell_tower,
            )
            DeviceInfoUiShared.Row.CELLULAR_SIGNAL -> PhoneRowUi(
                title(key),
                info.cellularSignalSummary,
                R.drawable.ic_row_cell_tower,
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
            DeviceInfoUiShared.Row.BLUETOOTH -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.bluetoothState(context, info.bluetoothOn),
                R.drawable.ic_row_bluetooth,
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
            DeviceInfoUiShared.Row.BANDWIDTH -> PhoneRowUi(
                title(key),
                DeviceInfoUiShared.bandwidthDisplay(
                    context,
                    info.downstreamKbps,
                    info.upstreamKbps,
                    DeviceInfoUiShared.DisplaySurface.PHONE,
                ),
                R.drawable.ic_row_speed,
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
            DeviceInfoUiShared.Row.CPU_ABI -> PhoneRowUi(title(key), info.cpuAbi, R.drawable.ic_row_architecture)
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
            DeviceInfoUiShared.Row.HW_NFC -> PhoneRowUi(title(key), info.nfcSummary, R.drawable.ic_row_nfc)
            DeviceInfoUiShared.Row.HW_USB -> PhoneRowUi(
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
                DeviceInfoUiShared.thermalIconRes(info.thermalSummary),
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
}
