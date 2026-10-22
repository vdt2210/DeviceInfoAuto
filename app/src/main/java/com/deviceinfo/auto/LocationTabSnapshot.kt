package com.deviceinfo.auto

/** One provider's latest location fix. */
data class LocationFixSnapshot(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedMps: Float? = null,
    val hasSpeed: Boolean = false,
    val bearingDegrees: Float? = null,
    val hasBearing: Boolean = false,
    val accuracyMeters: Float? = null,
    val hasAccuracy: Boolean = false,
    val altitudeMeters: Double? = null,
    val hasAltitude: Boolean = false,
    val verticalAccuracyMeters: Float? = null,
    val hasVerticalAccuracy: Boolean = false,
    val speedAccuracyMps: Float? = null,
    val hasSpeedAccuracy: Boolean = false,
    val bearingAccuracyDegrees: Float? = null,
    val hasBearingAccuracy: Boolean = false,
    val isMock: Boolean = false,
    val provider: String? = null,
    val fixAgeMs: Long? = null,
    val hasFix: Boolean = false,
)

/** Live / system location fields for the phone location tab. */
data class LocationTabSnapshot(
    /** System location master switch; null when unavailable. */
    val locationEnabled: Boolean? = null,
    val gpsEnabled: Boolean? = null,
    val networkEnabled: Boolean? = null,
    val deviceHeadingDegrees: Float? = null,
    val hasDeviceHeading: Boolean = false,
    val deviceHeadingAccuracy: Int? = null,
    val deviceHeadingSource: DeviceHeadingSource? = null,
    val gnss: GnssInfoSnapshot = GnssInfoSnapshot(),
    val googlePlayServicesLocationStatus: GooglePlayServicesLocationStatus =
        GooglePlayServicesLocationStatus.WAITING,
    val googlePlayServicesFix: LocationFixSnapshot = LocationFixSnapshot(),
    val gpsFix: LocationFixSnapshot = LocationFixSnapshot(),
    val networkFix: LocationFixSnapshot = LocationFixSnapshot(),
) {
    val motionRowsVisible: Boolean
        get() = locationEnabled == true
}

enum class GooglePlayServicesLocationStatus {
    WAITING,
    AVAILABLE,
    UNAVAILABLE,
    ERROR,
}
