package com.deviceinfo.auto

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DeviceInfoSchemaTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun carRowKeys_areMappedInUiShared() {
        DeviceInfoSchema.allRowKeys(DeviceInfoSchema.carSchema).forEach { key ->
            val title = DeviceInfoUiShared.rowTitle(context, key)
            assertFalse("Missing row title for $key", title == key)
        }
    }

    @Test
    fun carSchema_isSubsetOfPhoneBatteryAndRamSections() {
        val carKeys = DeviceInfoSchema.allRowKeys(DeviceInfoSchema.carSchema).toSet()
        val phoneKeys = DeviceInfoSchema.allRowKeys(
            DeviceInfoSchema.phoneSchema(emptyDeviceInfo()),
        ).toSet()
        assertTrue(phoneKeys.containsAll(carKeys))
    }

    @Test
    fun phoneSchema_withoutWifiOrSim_omitsOptionalNetworkRows() {
        val rows = DeviceInfoSchema.allRowKeys(
            DeviceInfoSchema.phoneSchema(
                emptyDeviceInfo(
                    wifiNetworkRowsVisible = false,
                    simDetailRowsVisible = false,
                ),
            ),
        )
        assertFalse(rows.contains(DeviceInfoUiShared.Row.WIFI_SIGNAL))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.CELLULAR))
    }

    @Test
    fun phoneSchema_withWifiAndSim_includesOptionalNetworkRows() {
        val rows = DeviceInfoSchema.allRowKeys(
            DeviceInfoSchema.phoneSchema(
                emptyDeviceInfo(
                    wifiNetworkRowsVisible = true,
                    simDetailRowsVisible = true,
                ),
            ),
        )
        assertTrue(rows.contains(DeviceInfoUiShared.Row.WIFI_SIGNAL))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.SIM_TYPE))
    }

    @Test
    fun phoneSections_haveLocalizedTitles() {
        DeviceInfoSchema.allSectionKeys(DeviceInfoSchema.phoneSchema(emptyDeviceInfo()))
            .forEach { sectionKey ->
                val title = DeviceInfoUiShared.sectionTitle(context, sectionKey)
                assertFalse("Missing section title for $sectionKey", title == sectionKey)
            }
    }
}
