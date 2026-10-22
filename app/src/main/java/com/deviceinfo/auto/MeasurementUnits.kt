package com.deviceinfo.auto

import android.content.Context
import android.os.LocaleList
import java.util.Locale

/**
 * App-wide measurement display (vehicle distance/speed, temperatures, …).
 * [AppPreferences.UnitMode.AUTO] follows **device region**, not app UI language.
 */
object MeasurementUnits {

    private const val TEMP_C_STORAGE_PREFIX = "temp_c:"
    private const val METERS_PER_MILE = 1609.344
    private const val MPS_TO_MPH = 2.23694f

    /** ISO 3166-1 alpha-2: regions that commonly use mph, miles, and °F on roads / everyday use. */
    private val IMPERIAL_REGION_COUNTRIES = setOf("US", "GB", "LR", "MM")

    fun activeLocale(context: Context): Locale =
        context.resources.configuration.locales[0] ?: Locale.getDefault()

    fun usesImperial(context: Context): Boolean =
        when (AppPreferences(context.applicationContext).getUnitMode()) {
            AppPreferences.UnitMode.METRIC -> false
            AppPreferences.UnitMode.IMPERIAL -> true
            AppPreferences.UnitMode.AUTO -> deviceRegionUsesImperial()
        }

    fun usesMetric(context: Context): Boolean = !usesImperial(context)

    fun deviceRegionUsesImperial(): Boolean {
        val country = deviceRegionCountry()
        return country.isNotEmpty() && country in IMPERIAL_REGION_COUNTRIES
    }

    fun deviceRegionCountry(): String =
        LocaleList.getAdjustedDefault()[0]?.country?.uppercase(Locale.ROOT).orEmpty()

    fun temperature(context: Context, celsius: Float): String {
        val locale = activeLocale(context)
        return if (usesImperial(context)) {
            String.format(locale, "%.1f °F", celsius * 9f / 5f + 32f)
        } else {
            String.format(locale, "%.1f °C", celsius)
        }
    }

    /** Store Celsius in compound summaries; format at display time. */
    fun encodeTempCelsius(celsius: Float): String = "$TEMP_C_STORAGE_PREFIX$celsius"

    fun formatStoredPart(context: Context, part: String): String {
        decodeTempCelsiusToken(part)?.let { return temperature(context, it) }
        parseLegacyCelsiusPart(part)?.let { return temperature(context, it) }
        parseLegacyFahrenheitPart(part)?.let { return temperature(context, it) }
        return part
    }

    fun thermalTempCelsius(summary: String): Float? {
        for (part in summary.split(DeviceInfoUiShared.COMPOUND_SEP)) {
            decodeTempCelsiusToken(part)?.let { return it }
            parseLegacyCelsiusPart(part)?.let { return it }
            parseLegacyFahrenheitPart(part)?.let { return it }
        }
        return null
    }

    fun speed(context: Context, metersPerSecond: Float?): String {
        if (metersPerSecond == null || metersPerSecond < 0f) {
            return context.getString(R.string.not_available)
        }
        val locale = activeLocale(context)
        return if (usesImperial(context)) {
            String.format(locale, "%.0f mph", metersPerSecond * MPS_TO_MPH)
        } else {
            String.format(locale, "%.0f km/h", metersPerSecond * 3.6f)
        }
    }

    fun range(context: Context, meters: Int?): String {
        if (meters == null || meters < 0) return context.getString(R.string.not_available)
        val locale = activeLocale(context)
        return if (usesImperial(context)) {
            String.format(locale, "%.0f mi", meters / METERS_PER_MILE)
        } else {
            String.format(locale, "%.0f km", meters / 1000.0)
        }
    }

    fun odometer(context: Context, meters: Long?): String {
        if (meters == null || meters < 0L) return context.getString(R.string.not_available)
        val locale = activeLocale(context)
        val symbols = java.text.DecimalFormatSymbols.getInstance(locale)
        val fmt = java.text.DecimalFormat("#,##0.0", symbols)
        return if (usesImperial(context)) {
            "${fmt.format(meters / METERS_PER_MILE)} mi"
        } else {
            "${fmt.format(meters / 1000.0)} km"
        }
    }

    fun altitude(context: Context, meters: Double?): String {
        if (meters == null || !meters.isFinite()) {
            return context.getString(R.string.not_available)
        }
        val locale = activeLocale(context)
        return if (usesImperial(context)) {
            String.format(locale, "%.0f ft", meters * 3.28084)
        } else {
            String.format(locale, "%.0f m", meters)
        }
    }

    fun horizontalAccuracy(context: Context, meters: Float?): String {
        if (meters == null || meters < 0f) {
            return context.getString(R.string.not_available)
        }
        val locale = activeLocale(context)
        return if (usesImperial(context)) {
            String.format(locale, "±%.0f ft", meters * 3.28084f)
        } else {
            String.format(locale, "±%.0f m", meters)
        }
    }

    private fun decodeTempCelsiusToken(part: String): Float? =
        part.trim()
            .takeIf { it.startsWith(TEMP_C_STORAGE_PREFIX) }
            ?.removePrefix(TEMP_C_STORAGE_PREFIX)
            ?.toFloatOrNull()

    private fun parseLegacyCelsiusPart(part: String): Float? =
        Regex("""^([\d.]+)\s*°C$""").find(part.trim())?.groupValues?.get(1)?.toFloatOrNull()

    private fun parseLegacyFahrenheitPart(part: String): Float? =
        Regex("""^([\d.]+)\s*°F$""").find(part.trim())?.groupValues?.get(1)?.toFloatOrNull()
            ?.let { f -> (f - 32f) * 5f / 9f }
}
