package com.deviceinfo.auto

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
        DeviceInfoSchema.allRowKeys(DeviceInfoSchema.carSchema()).forEach { key ->
            val title = DeviceInfoUiShared.rowTitle(context, key)
            assertFalse("Missing row title for $key", title == key)
        }
    }

    @Test
    fun carSchema_isSubsetOfPhoneBatteryAndRamSections() {
        val carKeys = DeviceInfoSchema.allRowKeys(DeviceInfoSchema.carSchema()).toSet()
        val phoneKeys = DeviceInfoSchema.allRowKeys(
            DeviceInfoSchema.phoneSchema(emptyDeviceInfo()),
        ).toSet()
        assertTrue(phoneKeys.containsAll(carKeys))
    }

    @Test
    fun carSchema_remainsMinimalAndUnchanged() {
        assertEquals(
            listOf(
                DeviceInfoUiShared.Section.BATTERY to listOf(
                    DeviceInfoUiShared.Row.LEVEL,
                    DeviceInfoUiShared.Row.PLUGGED,
                    DeviceInfoUiShared.Row.HEALTH,
                    DeviceInfoUiShared.Row.TEMPERATURE,
                    DeviceInfoUiShared.Row.CURRENT,
                    DeviceInfoUiShared.Row.POWER,
                ),
                DeviceInfoUiShared.Section.RAM to listOf(DeviceInfoUiShared.Row.RAM),
            ),
            DeviceInfoSchema.carSchema(),
        )
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
    fun phoneSchema_chargeTimeRow_onlyWhenChargingWithEta() {
        val unplugged = DeviceInfoSchema.allRowKeys(
            DeviceInfoSchema.phoneSchema(
                emptyDeviceInfo(
                    chargeTimeRemainingMs = 3_600_000L,
                    chargingStatus = "Discharging",
                    plugged = "None",
                ),
            ),
        )
        assertFalse(unplugged.contains(DeviceInfoUiShared.Row.CHARGE_TIME_REMAINING))

        val charging = DeviceInfoSchema.allRowKeys(
            DeviceInfoSchema.phoneSchema(
                emptyDeviceInfo(
                    chargeTimeRemainingMs = 3_600_000L,
                    chargingStatus = "Charging",
                    plugged = "AC",
                ),
            ),
        )
        assertTrue(charging.contains(DeviceInfoUiShared.Row.CHARGE_TIME_REMAINING))
    }

    @Test
    fun phoneSchema_connectionTab_includesExpandedDeviceLinks() {
        val rows = DeviceInfoSchema.connectionSchema(
            emptyDeviceInfo(
                bluetoothCapable = true,
                bluetoothOn = true,
                bluetoothTypeSummary = "Classic",
            ),
        ).second
        assertTrue(rows.contains(DeviceInfoUiShared.Row.BLUETOOTH_TYPE))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.USB))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.CAST))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.WIFI_DIRECT))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.UWB))
    }

    @Test
    fun phoneSchema_connectionTab_onlyDeviceLinks_notNetworkOverview() {
        val connectionRows = DeviceInfoSchema.connectionSchema(
            emptyDeviceInfo(
                wifiNetworkRowsVisible = true,
                telephonyCapable = true,
                simDetailRowsVisible = true,
                wifiConnected = true,
                wifiStandardSummary = "Wi‑Fi 5",
                bluetoothCapable = true,
                bluetoothOn = true,
            ),
        ).second
        assertTrue(connectionRows.contains(DeviceInfoUiShared.Row.BLUETOOTH))
        assertTrue(connectionRows.contains(DeviceInfoUiShared.Row.NFC))
        assertFalse(connectionRows.contains(DeviceInfoUiShared.Row.CONNECTION))
        assertFalse(connectionRows.contains(DeviceInfoUiShared.Row.WIFI_STANDARD))
    }

    @Test
    fun phoneSchema_connectionTab_castWhenCapable() {
        val rows = DeviceInfoSchema.connectionSchema(
            emptyDeviceInfo(castCapable = true),
        ).second
        assertTrue(rows.contains(DeviceInfoUiShared.Row.CAST))
    }

    @Test
    fun phoneSchema_battery_hidesCycleCountWhenUnavailable() {
        val hidden = DeviceInfoSchema.phoneSchema(emptyDeviceInfo(cycleCount = null))
            .first { it.first == DeviceInfoUiShared.Section.BATTERY }.second
        assertFalse(hidden.contains(DeviceInfoUiShared.Row.CYCLE_COUNT))

        val shown = DeviceInfoSchema.phoneSchema(emptyDeviceInfo(cycleCount = 42))
            .first { it.first == DeviceInfoUiShared.Section.BATTERY }.second
        assertTrue(shown.contains(DeviceInfoUiShared.Row.CYCLE_COUNT))

        val zero = DeviceInfoSchema.phoneSchema(emptyDeviceInfo(cycleCount = 0))
            .first { it.first == DeviceInfoUiShared.Section.BATTERY }.second
        assertTrue(zero.contains(DeviceInfoUiShared.Row.CYCLE_COUNT))
    }

    @Test
    fun phoneSchema_cellular_withoutPhonePermission_showsPermissionRow() {
        val rows = DeviceInfoSchema.wirelessSchema(
            emptyDeviceInfo(telephonyCapable = true, simDetailRowsVisible = false),
        ).second
        assertTrue(rows.contains(DeviceInfoUiShared.Subsection.CELLULAR))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.SIM_PHONE_PERMISSION))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.CELLULAR))
    }

    @Test
    fun phoneSchema_networkOverview_vpnAndRoaming_whenNetworkReadable() {
        val rows = DeviceInfoSchema.wirelessSchema(
            emptyDeviceInfo(
                wifiNetworkRowsVisible = true,
            ),
        ).second
        assertTrue(rows.contains(DeviceInfoUiShared.Row.VPN))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.ROAMING))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.METERED))
    }

    @Test
    fun phoneSchema_wifiBands_visibleEvenWhenNotConnected() {
        val wirelessRows = DeviceInfoSchema.wirelessSchema(
            emptyDeviceInfo(
                wifiNetworkRowsVisible = true,
                wifiConnected = false,
            ),
        ).second
        assertTrue(wirelessRows.contains(DeviceInfoUiShared.Subsection.WIFI))
        assertTrue(wirelessRows.contains(DeviceInfoUiShared.Row.WIFI_BANDS))
        assertFalse(wirelessRows.contains(DeviceInfoUiShared.Row.WIFI_SSID))
    }

    @Test
    fun phoneSchema_wirelessTab_networkOverviewBeforeWifiSubsection() {
        val wirelessRows = DeviceInfoSchema.wirelessSchema(
            emptyDeviceInfo(
                wifiNetworkRowsVisible = true,
                wifiConnected = true,
                wifiStandardSummary = "Wi‑Fi 5",
            ),
        ).second
        val networkIdx = wirelessRows.indexOf(DeviceInfoUiShared.Subsection.NETWORK)
        val wifiIdx = wirelessRows.indexOf(DeviceInfoUiShared.Subsection.WIFI)
        assertTrue(networkIdx >= 0)
        assertTrue(wifiIdx > networkIdx)
        assertTrue(wirelessRows.contains(DeviceInfoUiShared.Row.CONNECTION))
        assertTrue(wirelessRows.contains(DeviceInfoUiShared.Row.WIFI_STANDARD))
    }

    @Test
    fun phoneSchema_withWifiAndSim_includesOptionalNetworkRows() {
        val rows = DeviceInfoSchema.allRowKeys(
            DeviceInfoSchema.phoneSchema(
                emptyDeviceInfo(
                    wifiNetworkRowsVisible = true,
                    telephonyCapable = true,
                    simDetailRowsVisible = true,
                    wifiConnected = true,
                    wifiSignalSummary = "-55 dBm",
                    cellularType = "LTE",
                    cellularSignalSummary = "-90 dBm",
                    simCarrier = "Carrier",
                    simState = "Ready",
                    simCountryIso = "US",
                    simTypeSummary = "Physical",
                ),
            ),
        )
        assertTrue(rows.contains(DeviceInfoUiShared.Row.WIFI_SIGNAL))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.SIM_TYPE))
        assertTrue(
            DeviceInfoSchema.phoneSchema(emptyDeviceInfo()).any {
                it.first == DeviceInfoUiShared.Section.CONNECTION
            },
        )
    }

    @Test
    fun phoneSections_haveLocalizedTitles() {
        DeviceInfoSchema.allSectionKeys(DeviceInfoSchema.phoneSchema(emptyDeviceInfo()))
            .forEach { sectionKey ->
                val title = DeviceInfoUiShared.sectionTitle(context, sectionKey)
                assertFalse("Missing section title for $sectionKey", title == sectionKey)
            }
    }

    @Test
    fun phoneSchema_vehicle_withoutLiveData_showsMinimalRows() {
        val rows = vehicleRows(
            emptyDeviceInfo(vehicleTab = VehicleTabSnapshot(dataRowsVisible = false)),
        )
        assertEquals(
            listOf(
                DeviceInfoUiShared.Row.VEHICLE_CONNECTION,
                DeviceInfoUiShared.Row.VEHICLE_DATA_STATUS,
            ),
            rows,
        )
        assertFalse(rows.contains(DeviceInfoUiShared.Row.VEHICLE_MAKE))
    }

    @Test
    fun phoneSchema_vehicle_unknown_omitsFuelAndEvRows() {
        val rows = vehicleRows(
            emptyDeviceInfo(
                vehicleTab = VehicleTabSnapshot(
                    dataRowsVisible = true,
                    powertrain = VehiclePowertrain.UNKNOWN,
                ),
            ),
        )
        assertFalse(rows.contains(DeviceInfoUiShared.Row.VEHICLE_FUEL_LEVEL))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.VEHICLE_EV_BATTERY))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.VEHICLE_CONNECTION))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.VEHICLE_DATA_STATUS))
    }

    @Test
    fun phoneSchema_vehicle_fuel_includesFuelRowsOnly() {
        val rows = vehicleRows(
            emptyDeviceInfo(
                vehicleTab = VehicleTabSnapshot(
                    dataRowsVisible = true,
                    powertrain = VehiclePowertrain.FUEL,
                ),
            ),
        )
        assertTrue(rows.contains(DeviceInfoUiShared.Row.VEHICLE_FUEL_LEVEL))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.VEHICLE_FUEL_TYPES))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.VEHICLE_EV_BATTERY))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.VEHICLE_EV_CONNECTORS))
    }

    @Test
    fun phoneSchema_vehicle_ev_includesEvRowsOnly() {
        val rows = vehicleRows(
            emptyDeviceInfo(
                vehicleTab = VehicleTabSnapshot(
                    dataRowsVisible = true,
                    powertrain = VehiclePowertrain.EV,
                ),
            ),
        )
        assertFalse(rows.contains(DeviceInfoUiShared.Row.VEHICLE_FUEL_LEVEL))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.VEHICLE_EV_BATTERY))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.VEHICLE_EV_CONNECTORS))
    }

    @Test
    fun phoneSchema_vehicle_hybrid_includesFuelAndEvRows() {
        val rows = vehicleRows(
            emptyDeviceInfo(
                vehicleTab = VehicleTabSnapshot(
                    dataRowsVisible = true,
                    powertrain = VehiclePowertrain.HYBRID,
                ),
            ),
        )
        assertTrue(rows.contains(DeviceInfoUiShared.Row.VEHICLE_FUEL_LEVEL))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.VEHICLE_EV_BATTERY))
    }

    @Test
    fun phoneSchema_socThermal_isInProcessorSection() {
        val section = DeviceInfoUiShared.sectionForRowKey(DeviceInfoUiShared.Row.HW_THERMAL)
        assertEquals(DeviceInfoUiShared.Section.PROCESSOR, section)
        val processorRows = DeviceInfoSchema.phoneSchema(emptyDeviceInfo())
            .first { it.first == DeviceInfoUiShared.Section.PROCESSOR }
            .second
        assertTrue(processorRows.contains(DeviceInfoUiShared.Row.HW_THERMAL))
        val hardwareRows = DeviceInfoSchema.phoneSchema(emptyDeviceInfo())
            .first { it.first == DeviceInfoUiShared.Section.HARDWARE }
            .second
        assertFalse(hardwareRows.contains(DeviceInfoUiShared.Row.HW_THERMAL))
    }

    @Test
    fun phoneSchema_locationSection_betweenSensorsAndVehicle() {
        val sections = DeviceInfoSchema.allSectionKeys(DeviceInfoSchema.phoneSchema(emptyDeviceInfo()))
        val sensorsIndex = sections.indexOf(DeviceInfoUiShared.Section.SENSORS)
        val locationIndex = sections.indexOf(DeviceInfoUiShared.Section.LOCATION)
        val vehicleIndex = sections.indexOf(DeviceInfoUiShared.Section.VEHICLE)
        assertTrue(locationIndex > sensorsIndex)
        assertTrue(locationIndex < vehicleIndex)
    }

    @Test
    fun phoneSchema_locationDisabled_keepsStableRows() {
        val disabledRows = DeviceInfoSchema.phoneSchema(
            emptyDeviceInfo(locationTab = LocationTabSnapshot(locationEnabled = false)),
        ).first { it.first == DeviceInfoUiShared.Section.LOCATION }.second
        val enabledRows = DeviceInfoSchema.phoneSchema(
            emptyDeviceInfo(locationTab = LocationTabSnapshot(locationEnabled = true)),
        ).first { it.first == DeviceInfoUiShared.Section.LOCATION }.second
        assertEquals(enabledRows, disabledRows)
    }

    @Test
    fun phoneSchema_locationEnabled_showsMotionRows() {
        val rows = DeviceInfoSchema.phoneSchema(
            emptyDeviceInfo(locationTab = LocationTabSnapshot(locationEnabled = true)),
        ).first { it.first == DeviceInfoUiShared.Section.LOCATION }.second
        assertTrue(rows.contains(DeviceInfoUiShared.Row.LOCATION_GOOGLE_SPEED))
        assertTrue(rows.contains(DeviceInfoUiShared.Row.LOCATION_GOOGLE_FIX_AGE))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.LOCATION_NETWORK_SPEED))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.LOCATION_NETWORK_BEARING))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.LOCATION_NETWORK_PROVIDER))
        assertFalse(rows.contains(DeviceInfoUiShared.Row.LOCATION_GPS_PROVIDER))
    }

    private fun vehicleRows(info: DeviceInfo): List<String> =
        DeviceInfoSchema.phoneSchema(info)
            .first { it.first == DeviceInfoUiShared.Section.VEHICLE }
            .second
}
