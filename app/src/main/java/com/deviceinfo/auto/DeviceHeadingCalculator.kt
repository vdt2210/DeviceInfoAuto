package com.deviceinfo.auto

import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.PI

/** Converts Android orientation sensors into a screen-relative compass heading. */
object DeviceHeadingCalculator {

    fun fromRotationVector(values: FloatArray, displayRotation: Int): Float? =
        runCatching {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
            headingFromRotationMatrix(rotationMatrix, displayRotation)
        }.getOrNull()

    fun fromAccelerometerAndMagneticField(
        accelerometer: FloatArray,
        magneticField: FloatArray,
        displayRotation: Int,
    ): Float? {
        if (accelerometer.size < 3 || magneticField.size < 3) return null
        val rotationMatrix = FloatArray(9)
        if (!SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometer,
                magneticField,
            )
        ) {
            return null
        }
        return headingFromRotationMatrix(rotationMatrix, displayRotation)
    }

    internal fun normalizeDegrees(degrees: Float): Float? {
        if (!degrees.isFinite()) return null
        return ((degrees % 360f) + 360f) % 360f
    }

    private fun headingFromRotationMatrix(
        rotationMatrix: FloatArray,
        displayRotation: Int,
    ): Float? {
        val remapped = FloatArray(9)
        val (xAxis, yAxis) = when (displayRotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 ->
                SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        if (!SensorManager.remapCoordinateSystem(rotationMatrix, xAxis, yAxis, remapped)) {
            return null
        }
        val radians = SensorManager.getOrientation(remapped, FloatArray(3))[0]
        return normalizeDegrees((radians * 180f / PI).toFloat())
    }
}
