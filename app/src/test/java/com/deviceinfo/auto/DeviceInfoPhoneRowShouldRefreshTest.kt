package com.deviceinfo.auto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoPhoneRowShouldRefreshTest {

    @Test
    fun cellularTypeAndOperator_refreshOnNetworkPoll() {
        assertTrue(
            DeviceInfoPhoneRow.shouldRefresh(
                DeviceInfoUiShared.Row.CELLULAR,
                updateBatteryBroadcast = false,
                updateBatteryPoll = false,
                updateMemory = false,
                updateProcessor = false,
                updateNetwork = false,
                updateNetworkPoll = true,
                updateDisplay = false,
                updateVehicle = false,
                updateLocation = false,
                updateDisplayUnits = false,
            ),
        )
        assertTrue(
            DeviceInfoPhoneRow.shouldRefresh(
                DeviceInfoUiShared.Row.CELLULAR_OPERATOR,
                updateBatteryBroadcast = false,
                updateBatteryPoll = false,
                updateMemory = false,
                updateProcessor = false,
                updateNetwork = false,
                updateNetworkPoll = true,
                updateDisplay = false,
                updateVehicle = false,
                updateLocation = false,
                updateDisplayUnits = false,
            ),
        )
        assertFalse(
            DeviceInfoPhoneRow.shouldRefresh(
                DeviceInfoUiShared.Row.CELLULAR,
                updateBatteryBroadcast = false,
                updateBatteryPoll = false,
                updateMemory = false,
                updateProcessor = false,
                updateNetwork = true,
                updateNetworkPoll = false,
                updateDisplay = false,
                updateVehicle = false,
                updateLocation = false,
                updateDisplayUnits = false,
            ),
        )
    }
}
