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
class MeasurementUnitsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var prefs: AppPreferences

    @Before
    fun setUp() {
        prefs = AppPreferences(context)
        prefs.resetToDefaults()
    }

    @Test
    fun metricPreference_usesCelsius() {
        prefs.setUnitMode(AppPreferences.UnitMode.METRIC)
        assertEquals("25.0 °C", MeasurementUnits.temperature(context, 25f))
    }

    @Test
    fun imperialPreference_usesFahrenheit() {
        prefs.setUnitMode(AppPreferences.UnitMode.IMPERIAL)
        assertEquals("77.0 °F", MeasurementUnits.temperature(context, 25f))
    }

    @Test
    fun metricPreference_overridesAutoForSpeed() {
        prefs.setUnitMode(AppPreferences.UnitMode.METRIC)
        assertTrue(MeasurementUnits.usesMetric(context))
        assertEquals("36 km/h", MeasurementUnits.speed(context, 10f))
    }

    @Test
    fun imperialPreference_overridesAutoForSpeed() {
        prefs.setUnitMode(AppPreferences.UnitMode.IMPERIAL)
        assertTrue(MeasurementUnits.usesImperial(context))
        assertEquals("22 mph", MeasurementUnits.speed(context, 10f))
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun auto_usRegion_usesImperial() {
        prefs.setUnitMode(AppPreferences.UnitMode.AUTO)
        assertTrue(MeasurementUnits.deviceRegionUsesImperial())
        assertTrue(MeasurementUnits.usesImperial(context))
    }

    @Test
    fun imperialPreference_horizontalAccuracyUsesFeet() {
        prefs.setUnitMode(AppPreferences.UnitMode.IMPERIAL)
        assertEquals("±33 ft", MeasurementUnits.horizontalAccuracy(context, 10f))
    }

    @Test
    fun metricPreference_horizontalAccuracyUsesMeters() {
        prefs.setUnitMode(AppPreferences.UnitMode.METRIC)
        assertEquals("±10 m", MeasurementUnits.horizontalAccuracy(context, 10f))
    }

    @Test
    fun thermalStorage_roundTripsThroughDisplayPart() {
        prefs.setUnitMode(AppPreferences.UnitMode.METRIC)
        val stored = MeasurementUnits.encodeTempCelsius(42.5f)
        assertEquals("42.5 °C", MeasurementUnits.formatStoredPart(context, stored))
    }
}
