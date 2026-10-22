package com.deviceinfo.auto

/** When each connection / wireless row is included in the tab schema. */
object NetworkRowVisibility {

    /** Device radios / short-range links (Bluetooth, NFC, …). */
    fun connectionRows(info: DeviceInfo): List<String> = buildList {
        if (info.bluetoothCapable) {
            add(DeviceInfoUiShared.Row.BLUETOOTH_TYPE)
            add(DeviceInfoUiShared.Row.BLUETOOTH)
        }
        add(DeviceInfoUiShared.Row.NFC)
        add(DeviceInfoUiShared.Row.USB)
        add(DeviceInfoUiShared.Row.WIFI_DIRECT)
        add(DeviceInfoUiShared.Row.UWB)
        if (info.castCapable) {
            add(DeviceInfoUiShared.Row.CAST)
        }
        if (info.airplaneModeOn != null) {
            add(DeviceInfoUiShared.Row.AIRPLANE_MODE)
        }
    }

    fun wirelessRows(info: DeviceInfo): List<String> = buildList {
        val overviewRows = networkOverviewRows(info)
        if (overviewRows.isNotEmpty()) {
            add(DeviceInfoUiShared.Subsection.NETWORK)
            addAll(overviewRows)
        }
        val wifiRows = wifiRows(info)
        if (wifiRows.isNotEmpty()) {
            add(DeviceInfoUiShared.Subsection.WIFI)
            addAll(wifiRows)
        }
        val cellularRows = cellularRows(info)
        if (cellularRows.isNotEmpty()) {
            add(DeviceInfoUiShared.Subsection.CELLULAR)
            addAll(cellularRows)
        }
    }

    private fun networkOverviewRows(info: DeviceInfo): List<String> = buildList {
        add(DeviceInfoUiShared.Row.CONNECTION)
        if (info.connectionType != "Offline" && info.internetValidated != null) {
            add(DeviceInfoUiShared.Row.INTERNET_VALIDATED)
            add(DeviceInfoUiShared.Row.VPN)
            add(DeviceInfoUiShared.Row.METERED)
            add(DeviceInfoUiShared.Row.ROAMING)
        }
        if (info.captivePortal == true) {
            add(DeviceInfoUiShared.Row.CAPTIVE_PORTAL)
        }
        if (info.multiNetworkSummary != DeviceInfoUiShared.MultiNetworkToken.NONE) {
            add(DeviceInfoUiShared.Row.MULTI_NETWORK)
        }
        if (info.connectionType != "Offline" &&
            info.internetValidated != null &&
            (info.downstreamKbps > 0 || info.upstreamKbps > 0)
        ) {
            add(DeviceInfoUiShared.Row.ACTIVE_BANDWIDTH)
        }
    }

    private fun wifiRows(info: DeviceInfo): List<String> = buildList {
        if (!info.wifiNetworkRowsVisible) return@buildList
        add(DeviceInfoUiShared.Row.WIFI)
        add(DeviceInfoUiShared.Row.WIFI_BANDS)
        if (!info.wifiConnected) return@buildList
        add(DeviceInfoUiShared.Row.WIFI_FREQUENCY)
        add(DeviceInfoUiShared.Row.WIFI_STANDARD)
        add(DeviceInfoUiShared.Row.WIFI_LINK_SPEED)
        add(DeviceInfoUiShared.Row.WIFI_SIGNAL)
        if (info.wifiDownstreamKbps > 0 || info.wifiUpstreamKbps > 0) {
            add(DeviceInfoUiShared.Row.WIFI_BANDWIDTH)
        }
    }

    private fun cellularRows(info: DeviceInfo): List<String> = buildList {
        if (!info.telephonyCapable) return@buildList
        if (!info.simDetailRowsVisible) {
            add(DeviceInfoUiShared.Row.SIM_PHONE_PERMISSION)
            return@buildList
        }
        add(DeviceInfoUiShared.Row.SIM_STATE)
        if (!info.simInserted) return@buildList
        add(DeviceInfoUiShared.Row.CARRIER)
        add(DeviceInfoUiShared.Row.SIM_COUNTRY)
        add(DeviceInfoUiShared.Row.SIM_TYPE)
        add(DeviceInfoUiShared.Row.CELLULAR)
        if (info.cellularOperatorSummary != null) {
            add(DeviceInfoUiShared.Row.CELLULAR_OPERATOR)
        }
        add(DeviceInfoUiShared.Row.CELLULAR_SIGNAL)
        if (info.cellularDownstreamKbps > 0 || info.cellularUpstreamKbps > 0) {
            add(DeviceInfoUiShared.Row.CELLULAR_BANDWIDTH)
        }
    }
}
