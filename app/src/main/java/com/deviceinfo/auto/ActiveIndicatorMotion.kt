package com.deviceinfo.auto

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Pure geometry for the shared elastic active-tab indicator motion. */
object ActiveIndicatorMotion {

    data class AxisBounds(val start: Float, val end: Float)

    fun axisBounds(
        startCenter: Float,
        endCenter: Float,
        indicatorSize: Float,
        progress: Float,
        maxCenterDistance: Float,
    ): AxisBounds {
        val t = progress.coerceIn(0f, 1f)
        val maxTravel = maxCenterDistance.coerceAtLeast(0f)
        val travel = (endCenter - startCenter).coerceIn(-maxTravel, maxTravel)
        val cappedEndCenter = startCenter + travel
        val halfSize = indicatorSize.coerceAtLeast(0f) / 2f
        val lead = sin(t * PI.toFloat() / 2f)
        val trail = 1f - cos(t * PI.toFloat() / 2f)
        val startTop = startCenter - halfSize
        val startBottom = startCenter + halfSize
        val endTop = cappedEndCenter - halfSize
        val endBottom = cappedEndCenter + halfSize
        return if (travel >= 0f) {
            AxisBounds(
                start = lerp(startTop, endTop, trail),
                end = lerp(startBottom, endBottom, lead),
            )
        } else {
            AxisBounds(
                start = lerp(startTop, endTop, lead),
                end = lerp(startBottom, endBottom, trail),
            )
        }
    }

    private fun lerp(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress
}
