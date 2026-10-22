package com.deviceinfo.auto

import android.content.Context
import android.location.LocationManager
import android.os.Build

object LocationInfoReader {

    fun read(context: Context): LocationTabSnapshot {
        val lm = context.getSystemService(LocationManager::class.java)
        val gpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val locationEnabled = lm?.let { isLocationEnabled(it) }
        val motion = LocationInfoStore.motionSnapshot()
        val deviceHeading = DeviceHeadingStore.snapshot()
        val (googleStatus, googleFix) = GooglePlayServicesLocationStore.snapshot()
        return motion.copy(
            locationEnabled = locationEnabled,
            gpsEnabled = gpsEnabled,
            networkEnabled = networkEnabled,
            deviceHeadingDegrees = deviceHeading,
            hasDeviceHeading = deviceHeading != null,
            deviceHeadingAccuracy = DeviceHeadingStore.accuracy(),
            deviceHeadingSource = DeviceHeadingStore.source(),
            gnss = GnssInfoStore.snapshot(),
            googlePlayServicesLocationStatus = googleStatus,
            googlePlayServicesFix = googleFix,
        )
    }

    private fun isLocationEnabled(lm: LocationManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
}
