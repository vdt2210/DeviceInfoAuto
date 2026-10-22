package com.deviceinfo.auto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class VehicleInfoFormatsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var prefs: AppPreferences

    @Before
    fun setUp() {
        prefs = AppPreferences(context)
        prefs.resetToDefaults()
    }

    @Test
    fun percentWithEnergyLow_appendsLowLabelOnSecondLine() {
        val text = VehicleInfoFormats.percentWithEnergyLow(
            context,
            18f,
            true,
            R.string.vehicle_energy_low_fuel,
        )
        assertTrue(text.contains("18%"))
        assertTrue(text.contains(context.getString(R.string.vehicle_energy_low_fuel)))
    }

    @Test
    fun fuelTypeLabels_joinsMultilineWithoutBullets() {
        val text = VehicleInfoFormats.fuelTypeLabels(context, listOf(1, 10))
        assertTrue(text.contains(context.getString(R.string.vehicle_fuel_unleaded)))
        assertTrue(text.contains(DeviceInfoUiShared.LIST_LINE_SEP))
        assertTrue(!text.contains("•"))
    }

    @Test
    fun evConnectorLabels_mapsShortNames() {
        val text = VehicleInfoFormats.evConnectorLabels(context, listOf(3, 5))
        assertTrue(text.contains(context.getString(R.string.vehicle_ev_chademo)))
        assertTrue(text.contains(context.getString(R.string.vehicle_ev_ccs2)))
    }

    @Test
    fun fuelDisplay_formatsLevelWarningAndTypesForBothSurfaces() {
        val level = VehicleInfoFormats.percentWithEnergyLow(
            context,
            12f,
            true,
            R.string.vehicle_energy_low_fuel,
        )
        val types = VehicleInfoFormats.fuelTypeLabels(context, listOf(1, 5))

        val display = VehicleInfoFormats.fuelDisplay(context, level, types)

        assertEquals(
            "12% — ${context.getString(R.string.vehicle_energy_low_fuel)}",
            display.primary,
        )
        assertEquals(
            context.getString(
                R.string.vehicle_fuel_type_format,
                "${context.getString(R.string.vehicle_fuel_unleaded)}, " +
                    context.getString(R.string.vehicle_fuel_biodiesel),
            ),
            display.detail,
        )
        assertEquals(listOf(display.primary, display.detail), display.carLines())
        assertEquals(display.carLines().joinToString("\n"), display.phoneText())
    }

    @Test
    fun evBatteryDisplay_omitsUnavailableParts() {
        val display = VehicleInfoFormats.evBatteryDisplay(
            context,
            context.getString(R.string.not_available),
            VehicleInfoFormats.evConnectorLabels(context, listOf(5)),
        )

        assertEquals(null, display.primary)
        assertEquals(
            context.getString(R.string.vehicle_ev_connectors_format, "CCS2"),
            display.detail,
        )
    }
}
