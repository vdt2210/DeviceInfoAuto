package com.deviceinfo.auto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceHeadingCalculatorTest {

    @Test
    fun normalizeDegrees_wrapsIntoCompassRange() {
        assertEquals(350f, DeviceHeadingCalculator.normalizeDegrees(-10f))
        assertEquals(10f, DeviceHeadingCalculator.normalizeDegrees(370f))
    }

    @Test
    fun normalizeDegrees_rejectsNonFiniteInput() {
        assertNull(DeviceHeadingCalculator.normalizeDegrees(Float.NaN))
        assertNull(DeviceHeadingCalculator.normalizeDegrees(Float.POSITIVE_INFINITY))
    }
}
