package com.deviceinfo.auto

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BatteryCurrentNormalizerTest {

    @Test
    fun samsungCharging_withoutCorroboration_trustsApiMicroAmpContract() {
        val out = BatteryCurrentNormalizer.normalize(
            apiRawMicroA = 1_500,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_AC,
            sysfsMicroA = null,
            manufacturer = "samsung",
        )
        assertEquals(1_500L, out)
    }

    @Test
    fun samsungCharging_negativeRaw_becomesPositiveMicroAmps() {
        val out = BatteryCurrentNormalizer.normalize(
            apiRawMicroA = -1_800,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_USB,
            sysfsMicroA = null,
            manufacturer = "samsung",
        )
        assertEquals(1_800L, out)
    }

    @Test
    fun samsungDischarging_negativeMicroAmps() {
        val out = BatteryCurrentNormalizer.normalize(
            apiRawMicroA = -320,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            plugged = 0,
            sysfsMicroA = null,
            manufacturer = "samsung",
        )
        assertEquals(-320L, out)
    }

    @Test
    fun samsungSmallMicroAmpReading_isNotInflatedIntoAmpSpike() {
        val out = BatteryCurrentNormalizer.normalize(
            apiRawMicroA = 12_000,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_USB,
            sysfsMicroA = null,
            manufacturer = "samsung",
        )
        assertEquals(12_000L, out)
    }

    @Test
    fun pixelLike_alreadyMicroAmps_unchanged() {
        val out = BatteryCurrentNormalizer.normalize(
            apiRawMicroA = -1_250_000,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            plugged = 0,
            sysfsMicroA = null,
            manufacturer = "Google",
        )
        assertEquals(-1_250_000L, out)
    }

    @Test
    fun genericOemCharging_doesNotGuessMilliAmpUnit() {
        val out = BatteryCurrentNormalizer.normalize(
            apiRawMicroA = 980,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_AC,
            sysfsMicroA = null,
            manufacturer = "realme",
        )
        assertEquals(980L, out)
    }

    @Test
    fun genericOemIdleDischarge_keepsPlausibleMicroAmps() {
        val out = BatteryCurrentNormalizer.normalize(
            apiRawMicroA = 18_000,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            plugged = 0,
            sysfsMicroA = null,
            manufacturer = "Google",
        )
        assertEquals(18_000L, out)
    }

    @Test
    fun sysfsReconcilesApiInMilliAmps() {
        val out = BatteryCurrentNormalizer.normalize(
            apiRawMicroA = 1_200,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_AC,
            sysfsMicroA = 1_150_000,
            manufacturer = "Google",
        )
        assertEquals(1_150_000L, out)
    }

    @Test
    fun apiMicroAmps_areKeptWhenSysfsLooksLikeMilliAmps() {
        val out = BatteryCurrentNormalizer.normalize(
            apiRawMicroA = 1_200_000,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_USB,
            sysfsMicroA = 1_200,
            manufacturer = "Google",
        )
        assertEquals(1_200_000L, out)
    }

    @Test
    fun unsupportedApi_usesSysfs() {
        val out = BatteryCurrentNormalizer.normalize(
            apiRawMicroA = Long.MIN_VALUE,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_AC,
            sysfsMicroA = 2_000_000,
            manufacturer = "samsung",
        )
        assertEquals(2_000_000L, out)
    }

    @Test
    fun zeroAndUnsupported_returnsZero() {
        assertEquals(
            0L,
            BatteryCurrentNormalizer.normalize(
                apiRawMicroA = Long.MIN_VALUE,
                status = BatteryManager.BATTERY_STATUS_UNKNOWN,
                plugged = 0,
                sysfsMicroA = null,
                manufacturer = "samsung",
            ),
        )
    }
}
