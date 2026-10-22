package com.deviceinfo.auto

/** Latest orientation-derived device heading, independent from GPS movement bearing. */
object DeviceHeadingStore {

    @Volatile
    private var headingDegrees: Float? = null
    @Volatile
    private var accuracy: Int? = null
    @Volatile
    private var source: DeviceHeadingSource? = null

    fun update(degrees: Float, headingSource: DeviceHeadingSource) {
        headingDegrees = degrees.takeIf { it.isFinite() }
        source = headingSource
    }

    fun snapshot(): Float? = headingDegrees
    fun accuracy(): Int? = accuracy
    fun source(): DeviceHeadingSource? = source

    fun updateAccuracy(sensorAccuracy: Int) {
        accuracy = sensorAccuracy
    }

    fun clear() {
        headingDegrees = null
        accuracy = null
        source = null
    }
}

enum class DeviceHeadingSource {
    ROTATION_VECTOR,
    ACCELEROMETER_MAGNETIC_FIELD,
}
