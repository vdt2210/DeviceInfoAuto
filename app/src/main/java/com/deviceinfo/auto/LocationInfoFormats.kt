package com.deviceinfo.auto

import android.content.Context
import android.location.LocationManager
import java.util.Locale
import kotlin.math.roundToInt

object LocationInfoFormats {

    fun coordinatesDisplay(context: Context, latitude: Double?, longitude: Double?): String {
        if (latitude == null || longitude == null ||
            !latitude.isFinite() || !longitude.isFinite()
        ) {
            return context.getString(R.string.not_available)
        }
        return String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
    }

    fun bearingDisplay(context: Context, degrees: Float?): String {
        if (degrees == null || !degrees.isFinite()) {
            return context.getString(R.string.not_available)
        }
        val normalized = ((degrees % 360f) + 360f) % 360f
        val cardinal = cardinalLabel(context, normalized)
        return context.getString(
            R.string.location_bearing_format,
            cardinal,
            normalized.roundToInt(),
        )
    }

    fun fixAgeDisplay(context: Context, ageMs: Long?): String {
        if (ageMs == null || ageMs < 0L) {
            return context.getString(R.string.not_available)
        }
        val seconds = ageMs / 1000L
        return when {
            seconds < 1L -> context.getString(R.string.location_fix_age_now)
            seconds < 60L -> context.getString(R.string.location_fix_age_seconds, seconds)
            else -> context.getString(R.string.location_fix_age_minutes, seconds / 60L)
        }
    }

    fun providerLabel(context: Context, provider: String?): String {
        if (provider.isNullOrBlank()) return context.getString(R.string.not_available)
        return when (provider) {
            LocationManager.GPS_PROVIDER -> context.getString(R.string.location_provider_gps)
            LocationManager.NETWORK_PROVIDER -> context.getString(R.string.location_provider_network)
            LocationManager.PASSIVE_PROVIDER -> context.getString(R.string.location_provider_passive)
            "fused" -> context.getString(R.string.location_provider_fused)
            else -> provider
        }
    }

    fun accuracyDisplay(context: Context, meters: Float?): String =
        MeasurementUnits.horizontalAccuracy(context, meters)

    private fun cardinalLabel(context: Context, degrees: Float): String {
        val index = ((degrees + 22.5f) / 45f).toInt() % 8
        val labels = context.resources.getStringArray(R.array.location_cardinals)
        return labels.getOrElse(index) { "—" }
    }
}
