package com.deviceinfo.auto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkRowVisibilityTest {

    @Test
    fun connectionRows_hidesCastWhenNotCapable() {
        val rows = NetworkRowVisibility.connectionRows(emptyDeviceInfo(castCapable = false))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.CAST))
    }

    @Test
    fun connectionRows_keepsWifiDirectAndUwbCapabilityRows() {
        val rows = NetworkRowVisibility.connectionRows(emptyDeviceInfo())
        assertTrue(rows.contains(DeviceInfoUiShared.Row.WIFI_DIRECT))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.UWB))
    }

    @Test
    fun networkOverview_hidesVpnWhenNetworkStateUnreadable() {
        val rows = NetworkRowVisibility.wirelessRows(
            emptyDeviceInfo(
                wifiNetworkRowsVisible = true,
                internetValidated = null,
            ),
        )
        assertTrue(rows.contains(DeviceInfoUiShared.Row.CONNECTION))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.VPN))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.ROAMING))
    }

    @Test
    fun cellularRows_showsOperatorOnlyWhenPresent() {
        val withOperator = NetworkRowVisibility.wirelessRows(
            emptyDeviceInfo(
                telephonyCapable = true,
                simDetailRowsVisible = true,
                cellularOperatorSummary = "SIM 1: Viettel",
            ),
        )
        assertTrue(withOperator.contains(DeviceInfoUiShared.Row.CELLULAR_OPERATOR))

        val withoutOperator = NetworkRowVisibility.wirelessRows(
            emptyDeviceInfo(
                telephonyCapable = true,
                simDetailRowsVisible = true,
                cellularOperatorSummary = null,
            ),
        )
        assertFalse(withoutOperator.contains(DeviceInfoUiShared.Row.CELLULAR_OPERATOR))
    }

    @Test
    fun cellularRows_showsSimStateOnlyWhenNoSim() {
        val noSim = NetworkRowVisibility.wirelessRows(
            emptyDeviceInfo(
                telephonyCapable = true,
                simDetailRowsVisible = true,
                simInserted = false,
            ),
        )
        assertTrue(noSim.contains(DeviceInfoUiShared.Subsection.CELLULAR))
        assertTrue(noSim.contains(DeviceInfoUiShared.Row.SIM_STATE))
        assertFalse(noSim.contains(DeviceInfoUiShared.Row.CELLULAR))
        assertFalse(noSim.contains(DeviceInfoUiShared.Row.CARRIER))

        val permissionOnly = NetworkRowVisibility.wirelessRows(
            emptyDeviceInfo(
                telephonyCapable = true,
                simDetailRowsVisible = false,
            ),
        )
        assertTrue(permissionOnly.contains(DeviceInfoUiShared.Row.SIM_PHONE_PERMISSION))
        assertFalse(permissionOnly.contains(DeviceInfoUiShared.Row.SIM_STATE))
    }

    @Test
    fun wifiRows_includesWifiStateWhenNetworkRowsVisible() {
        val rows = NetworkRowVisibility.wirelessRows(
            emptyDeviceInfo(wifiNetworkRowsVisible = true),
        )
        assertTrue(rows.contains(DeviceInfoUiShared.Subsection.WIFI))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.WIFI))
        val wifiSubIdx = rows.indexOf(DeviceInfoUiShared.Subsection.WIFI)
        assertEquals(wifiSubIdx + 1, rows.indexOf(DeviceInfoUiShared.Row.WIFI))
    }

    @Test
    fun cellularRows_simStateFirstWhenSimPresent() {
        val rows = NetworkRowVisibility.wirelessRows(
            emptyDeviceInfo(
                telephonyCapable = true,
                simDetailRowsVisible = true,
                simInserted = true,
            ),
        )
        val cellularSubIdx = rows.indexOf(DeviceInfoUiShared.Subsection.CELLULAR)
        assertTrue(cellularSubIdx >= 0)
        assertEquals(DeviceInfoUiShared.Row.SIM_STATE, rows[cellularSubIdx + 1])
    }
}
