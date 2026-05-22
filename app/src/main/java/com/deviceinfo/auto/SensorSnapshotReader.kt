package com.deviceinfo.auto

import android.content.Context
import android.hardware.Sensor
import java.util.Locale

/** Formatting + row-key mapping for [SensorEventListener] updates. */
object SensorRowFormat {

    private const val EM_DASH = "—"

    fun rowKeyForSensorType(type: Int): String? = when (type) {
        Sensor.TYPE_ACCELEROMETER -> DeviceInfoUiShared.Row.SENSOR_ACCELEROMETER
        Sensor.TYPE_GYROSCOPE -> DeviceInfoUiShared.Row.SENSOR_GYROSCOPE
        Sensor.TYPE_MAGNETIC_FIELD -> DeviceInfoUiShared.Row.SENSOR_MAGNETIC_FIELD
        Sensor.TYPE_LIGHT -> DeviceInfoUiShared.Row.SENSOR_LIGHT
        Sensor.TYPE_PRESSURE -> DeviceInfoUiShared.Row.SENSOR_PRESSURE
        Sensor.TYPE_RELATIVE_HUMIDITY -> DeviceInfoUiShared.Row.SENSOR_HUMIDITY
        Sensor.TYPE_AMBIENT_TEMPERATURE -> DeviceInfoUiShared.Row.SENSOR_AMBIENT_TEMPERATURE
        Sensor.TYPE_PROXIMITY -> DeviceInfoUiShared.Row.SENSOR_PROXIMITY
        Sensor.TYPE_HINGE_ANGLE -> DeviceInfoUiShared.Row.SENSOR_HINGE_ANGLE
        else -> null
    }

    fun sensorTypeForRowKey(rowKey: String): Int? = when (rowKey) {
        DeviceInfoUiShared.Row.SENSOR_ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
        DeviceInfoUiShared.Row.SENSOR_GYROSCOPE -> Sensor.TYPE_GYROSCOPE
        DeviceInfoUiShared.Row.SENSOR_MAGNETIC_FIELD -> Sensor.TYPE_MAGNETIC_FIELD
        DeviceInfoUiShared.Row.SENSOR_LIGHT -> Sensor.TYPE_LIGHT
        DeviceInfoUiShared.Row.SENSOR_PRESSURE -> Sensor.TYPE_PRESSURE
        DeviceInfoUiShared.Row.SENSOR_HUMIDITY -> Sensor.TYPE_RELATIVE_HUMIDITY
        DeviceInfoUiShared.Row.SENSOR_AMBIENT_TEMPERATURE -> Sensor.TYPE_AMBIENT_TEMPERATURE
        DeviceInfoUiShared.Row.SENSOR_PROXIMITY -> Sensor.TYPE_PROXIMITY
        DeviceInfoUiShared.Row.SENSOR_HINGE_ANGLE -> Sensor.TYPE_HINGE_ANGLE
        else -> null
    }

    /** @param sensor Proximity needs [Sensor.getMaximumRange] for near/far. */
    fun format(type: Int, v: FloatArray, sensor: Sensor? = null, context: Context? = null): String = when (type) {
        Sensor.TYPE_ACCELEROMETER -> formatXyzLabeled(v)
        Sensor.TYPE_GYROSCOPE -> formatXyzLabeled(v)
        Sensor.TYPE_MAGNETIC_FIELD -> formatXyzLabeled(v)
        Sensor.TYPE_LIGHT -> if (v.isEmpty()) EM_DASH else String.format(Locale.US, "%.1f lx", v[0])
        Sensor.TYPE_PRESSURE -> if (v.isEmpty()) EM_DASH else String.format(Locale.US, "%.2f hPa", v[0])
        Sensor.TYPE_RELATIVE_HUMIDITY -> if (v.isEmpty()) EM_DASH else String.format(Locale.US, "%.1f %%", v[0])
        Sensor.TYPE_AMBIENT_TEMPERATURE -> if (v.isEmpty()) EM_DASH else String.format(Locale.US, "%.1f °C", v[0])
        Sensor.TYPE_PROXIMITY -> formatProximity(v, sensor, context)
        Sensor.TYPE_HINGE_ANGLE -> if (v.isEmpty()) EM_DASH else String.format(Locale.US, "%.1f°", v[0])
        else -> context?.notAvailableText() ?: EM_DASH
    }

    private fun formatXyzLabeled(v: FloatArray): String =
        if (v.size < 3) EM_DASH
        else DeviceInfoUiShared.joinCompound(
            listOf(
                String.format(Locale.US, "X %.2f", v[0]),
                String.format(Locale.US, "Y %.2f", v[1]),
                String.format(Locale.US, "Z %.2f", v[2]),
            ),
            DeviceInfoUiShared.DisplaySurface.PHONE,
        )

    private fun formatProximity(v: FloatArray, sensor: Sensor?, context: Context?): String {
        if (v.isEmpty()) return EM_DASH
        val raw = v[0]
        if (!raw.isFinite()) return EM_DASH
        val ctx = context ?: return String.format(Locale.US, "%.3g", raw)
        val maxR = sensor?.maximumRange
        val far = maxR != null && maxR.isFinite() && maxR > 0f && raw >= maxR
        return if (far) ctx.getString(R.string.sensor_proximity_far) else ctx.getString(R.string.sensor_proximity_near)
    }

    val streamSensorTypes: IntArray = intArrayOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_PROXIMITY,
        Sensor.TYPE_PRESSURE,
        Sensor.TYPE_RELATIVE_HUMIDITY,
        Sensor.TYPE_AMBIENT_TEMPERATURE,
        Sensor.TYPE_HINGE_ANGLE,
    )
}

fun Context.notAvailableText(): String = getString(R.string.not_available)
