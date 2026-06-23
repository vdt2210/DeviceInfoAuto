package com.deviceinfo.auto

import android.hardware.Sensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SensorRowFormatTest {

    @Test
    fun rowKeyForSensorType_roundTripsWithSensorTypeForRowKey() {
        SensorRowFormat.streamSensorTypes.forEach { type ->
            val key = SensorRowFormat.rowKeyForSensorType(type) ?: return@forEach
            assertEquals(type, SensorRowFormat.sensorTypeForRowKey(key))
        }
    }

    @Test
    fun format_accelerometer_xyz() {
        val text = SensorRowFormat.format(
            Sensor.TYPE_ACCELEROMETER,
            floatArrayOf(1f, 2f, 3f),
        )
        assertEquals("X 1.00\nY 2.00\nZ 3.00", text)
    }

    @Test
    fun format_light_emptyValues_returnsDash() {
        assertEquals("—", SensorRowFormat.format(Sensor.TYPE_LIGHT, floatArrayOf()))
    }

    @Test
    fun format_unknownType_withoutContext_returnsDash() {
        assertEquals("—", SensorRowFormat.format(Sensor.TYPE_ALL, floatArrayOf(1f)))
    }

    @Test
    fun rowKeyForSensorType_unknown_returnsNull() {
        assertNull(SensorRowFormat.rowKeyForSensorType(Sensor.TYPE_ALL))
    }
}
