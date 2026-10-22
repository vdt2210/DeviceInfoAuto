package com.deviceinfo.auto

import android.location.Location
import android.location.LocationManager
import android.os.SystemClock

/** Latest motion fix from [android.location.LocationListener] (lat/long not exposed to UI). */
object LocationInfoStore {

    private val lock = Any()
    private var gpsLocation: Location? = null
    private var networkLocation: Location? = null

    fun updateFromLocation(location: Location) {
        val copy = Location(location)
        synchronized(lock) {
            when (copy.provider) {
                LocationManager.GPS_PROVIDER -> {
                    gpsLocation = newerOf(copy, gpsLocation)
                }
                LocationManager.NETWORK_PROVIDER -> {
                    networkLocation = newerOf(copy, networkLocation)
                }
            }
        }
    }

    fun motionSnapshot(): LocationTabSnapshot {
        val (gps, network) = synchronized(lock) {
            Pair(
                gpsLocation?.let(::Location),
                networkLocation?.let(::Location),
            )
        }
        return LocationTabSnapshot(
            gpsFix = gps.toSnapshot(),
            networkFix = network.toSnapshot(),
        )
    }

    fun clearMotion() {
        synchronized(lock) {
            gpsLocation = null
            networkLocation = null
        }
    }

    private fun newerOf(candidate: Location, current: Location?): Location =
        if (current == null || candidate.comparisonTimeMs() >= current.comparisonTimeMs()) {
            candidate
        } else {
            current
        }

    private fun Location.comparisonTimeMs(): Long =
        elapsedRealtimeNanos.takeIf { it > 0L }?.div(1_000_000L) ?: time

    private fun Location.fixAgeMs(): Long =
        if (elapsedRealtimeNanos > 0L) {
            ((SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L)
                .coerceAtLeast(0L)
        } else {
            (System.currentTimeMillis() - time).coerceAtLeast(0L)
        }

    private fun Location?.toSnapshot(): LocationFixSnapshot {
        val location = this ?: return LocationFixSnapshot()
        return LocationFixSnapshot(
            latitude = location.latitude,
            longitude = location.longitude,
            speedMps = location.speed.takeIf { location.hasSpeed() },
            hasSpeed = location.hasSpeed(),
            bearingDegrees = location.bearing.takeIf { location.hasBearing() },
            hasBearing = location.hasBearing(),
            accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
            hasAccuracy = location.hasAccuracy(),
            altitudeMeters = location.altitude.takeIf { location.hasAltitude() },
            hasAltitude = location.hasAltitude(),
            verticalAccuracyMeters =
                location.verticalAccuracyMeters.takeIf { location.hasVerticalAccuracy() },
            hasVerticalAccuracy = location.hasVerticalAccuracy(),
            speedAccuracyMps =
                location.speedAccuracyMetersPerSecond.takeIf { location.hasSpeedAccuracy() },
            hasSpeedAccuracy = location.hasSpeedAccuracy(),
            bearingAccuracyDegrees =
                location.bearingAccuracyDegrees.takeIf { location.hasBearingAccuracy() },
            hasBearingAccuracy = location.hasBearingAccuracy(),
            isMock = location.isMockLocation(),
            provider = location.provider,
            fixAgeMs = location.fixAgeMs(),
            hasFix = true,
        )
    }

    @Suppress("DEPRECATION")
    private fun Location.isMockLocation(): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            isMock
        } else {
            isFromMockProvider
        }
}
