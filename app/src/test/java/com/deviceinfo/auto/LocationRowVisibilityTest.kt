package com.deviceinfo.auto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationRowVisibilityTest {

    @Test
    fun locationRows_alwaysIncludesGpsAndNetwork() {
        val rows = LocationRowVisibility.locationRows(emptyDeviceInfo())
        assertTrue(rows.contains(DeviceInfoUiShared.Row.LOCATION_GPS))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.LOCATION_NETWORK))
    }

    @Test
    fun locationRows_keepsStableSchemaWhenLocationDisabled() {
        val disabledRows = LocationRowVisibility.locationRows(
            emptyDeviceInfo(
                locationTab = LocationTabSnapshot(locationEnabled = false),
            ),
        )
        val enabledRows = LocationRowVisibility.locationRows(
            emptyDeviceInfo(
                locationTab = LocationTabSnapshot(locationEnabled = true),
            ),
        )
        assertEquals(enabledRows, disabledRows)
    }

    @Test
    fun locationRows_includesMotionWhenLocationEnabled() {
        val rows = LocationRowVisibility.locationRows(
            emptyDeviceInfo(
                locationTab = LocationTabSnapshot(locationEnabled = true),
            ),
        )
        assertEquals(
            listOf(
                DeviceInfoUiShared.Subsection.LOCATION_COMPASS,
                DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING,
                DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING_SOURCE,
                DeviceInfoUiShared.Subsection.LOCATION_GOOGLE_PLAY_SERVICES,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_STATUS,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_COORDINATES,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_ALTITUDE,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_SPEED,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_BEARING,
                DeviceInfoUiShared.Row.LOCATION_GOOGLE_FIX_AGE,
                DeviceInfoUiShared.Subsection.LOCATION_NETWORK,
                DeviceInfoUiShared.Row.LOCATION_NETWORK,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_COORDINATES,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_ALTITUDE,
                DeviceInfoUiShared.Row.LOCATION_NETWORK_FIX_AGE,
                DeviceInfoUiShared.Subsection.LOCATION_GPS,
                DeviceInfoUiShared.Row.LOCATION_GPS,
                DeviceInfoUiShared.Row.LOCATION_GPS_COORDINATES,
                DeviceInfoUiShared.Row.LOCATION_GPS_SPEED,
                DeviceInfoUiShared.Row.LOCATION_GPS_BEARING,
                DeviceInfoUiShared.Row.LOCATION_GPS_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GPS_ALTITUDE,
                DeviceInfoUiShared.Row.LOCATION_GPS_FIX_AGE,
                DeviceInfoUiShared.Row.LOCATION_GPS_VERTICAL_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GPS_SPEED_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GPS_BEARING_ACCURACY,
                DeviceInfoUiShared.Row.LOCATION_GPS_MOCK,
                DeviceInfoUiShared.Row.LOCATION_GNSS_SATELLITES,
                DeviceInfoUiShared.Row.LOCATION_GNSS_FREQUENCY_BANDS,
            ),
            rows,
        )
    }
}
