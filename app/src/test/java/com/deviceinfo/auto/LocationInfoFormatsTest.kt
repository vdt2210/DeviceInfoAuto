package com.deviceinfo.auto

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationInfoFormatsTest {

    @Test
    fun coordinatesDisplay_usesStableMapFormat() {
        assertEquals(
            "10.77584, 106.70176",
            LocationInfoFormats.coordinatesDisplay(context, 10.775843, 106.701755),
        )
    }

    private val context =
        ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun bearingDisplay_includesCardinalAndDegrees() {
        val result = LocationInfoFormats.bearingDisplay(context, 90f)
        assertTrue(result.contains("90"))
        assertTrue(result.contains("("))
    }

    @Test
    fun bearingDisplay_normalizesNegativeDegrees() {
        val result = LocationInfoFormats.bearingDisplay(context, -90f)
        assertTrue(result.contains("270"))
    }

    @Test
    fun fixAgeDisplay_secondsAndMinutes() {
        assertEquals(
            context.getString(R.string.location_fix_age_now),
            LocationInfoFormats.fixAgeDisplay(context, 500L),
        )
        assertEquals(
            context.getString(R.string.location_fix_age_seconds, 5L),
            LocationInfoFormats.fixAgeDisplay(context, 5_000L),
        )
        assertEquals(
            context.getString(R.string.location_fix_age_minutes, 2L),
            LocationInfoFormats.fixAgeDisplay(context, 120_000L),
        )
    }

    @Test
    fun bearingDisplay_correctCardinals() {
        assertTrue(LocationInfoFormats.bearingDisplay(context, 0f).contains("N"))
        assertTrue(LocationInfoFormats.bearingDisplay(context, 10f).contains("N"))
        assertTrue(LocationInfoFormats.bearingDisplay(context, 350f).contains("N"))
        assertTrue(LocationInfoFormats.bearingDisplay(context, 45f).contains("NE"))
        assertTrue(LocationInfoFormats.bearingDisplay(context, 90f).contains("E"))
        assertTrue(LocationInfoFormats.bearingDisplay(context, 180f).contains("S"))
    }

    @Test
    fun providerLabel_mapsKnownProviders() {
        assertEquals(
            context.getString(R.string.location_provider_gps),
            LocationInfoFormats.providerLabel(context, android.location.LocationManager.GPS_PROVIDER),
        )
        assertEquals(
            context.getString(R.string.location_provider_fused),
            LocationInfoFormats.providerLabel(context, "fused"),
        )
    }
}
