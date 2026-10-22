package com.deviceinfo.auto

import android.location.Location
import android.os.Build
import android.os.SystemClock

object GooglePlayServicesLocationStore {
    private val lock = Any()
    private var location: Location? = null
    private var status = GooglePlayServicesLocationStatus.WAITING

    fun update(location: Location) {
        synchronized(lock) {
            this.location = Location(location)
            status = GooglePlayServicesLocationStatus.AVAILABLE
        }
    }

    fun updateStatus(status: GooglePlayServicesLocationStatus) {
        synchronized(lock) {
            this.status = status
            if (status == GooglePlayServicesLocationStatus.UNAVAILABLE) location = null
        }
    }

    fun snapshot(): Pair<GooglePlayServicesLocationStatus, LocationFixSnapshot> =
        synchronized(lock) {
            status to (location?.toSnapshot() ?: LocationFixSnapshot())
        }

    fun clear() {
        synchronized(lock) {
            location = null
            status = GooglePlayServicesLocationStatus.WAITING
        }
    }

    private fun Location.toSnapshot(): LocationFixSnapshot = LocationFixSnapshot(
        latitude = latitude,
        longitude = longitude,
        speedMps = speed.takeIf { hasSpeed() },
        hasSpeed = hasSpeed(),
        bearingDegrees = bearing.takeIf { hasBearing() },
        hasBearing = hasBearing(),
        accuracyMeters = accuracy.takeIf { hasAccuracy() },
        hasAccuracy = hasAccuracy(),
        altitudeMeters = altitude.takeIf { hasAltitude() },
        hasAltitude = hasAltitude(),
        verticalAccuracyMeters = verticalAccuracyMeters.takeIf { hasVerticalAccuracy() },
        hasVerticalAccuracy = hasVerticalAccuracy(),
        speedAccuracyMps = speedAccuracyMetersPerSecond.takeIf { hasSpeedAccuracy() },
        hasSpeedAccuracy = hasSpeedAccuracy(),
        bearingAccuracyDegrees = bearingAccuracyDegrees.takeIf { hasBearingAccuracy() },
        hasBearingAccuracy = hasBearingAccuracy(),
        isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock
        else @Suppress("DEPRECATION") isFromMockProvider,
        provider = provider,
        fixAgeMs = if (elapsedRealtimeNanos > 0L) {
            ((SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L)
                .coerceAtLeast(0L)
        } else {
            (System.currentTimeMillis() - time).coerceAtLeast(0L)
        },
        hasFix = true,
    )
}
