package com.deviceinfo.auto

import android.os.BatteryManager
import android.os.Build
import java.io.File
import java.util.Locale
import kotlin.math.abs

/**
 * Normalizes [BatteryManager.BATTERY_PROPERTY_CURRENT_NOW] to microamperes (µA).
 *
 * Many OEMs (especially Samsung) return milliamperes while the API specifies µA.
 * Linux sysfs `current_now` is typically already in µA and is used to reconcile when readable.
 *
 * App [minSdk][android.os.Build.VERSION_CODES.Q] is 29 (Android 10+); unsupported properties
 * return [Long.MIN_VALUE], not 0.
 */
object BatteryCurrentNormalizer {

    private val PREFERRED_SUPPLIES = listOf(
        "battery",
        "Battery",
        "sec-battery",
        "samsung_battery",
        "bms",
        "max170xx_battery",
        "mtk-battery",
        "google,battery",
        "main-battery",
    )

    fun normalize(
        apiRawMicroA: Long,
        status: Int,
        plugged: Int,
        sysfsMicroA: Long? = null,
        manufacturer: String = Build.MANUFACTURER,
    ): Long {
        val api = sanitizeRaw(apiRawMicroA)
        val sysfs = sysfsMicroA?.let { sanitizeRaw(it) }
        if (api == null && sysfs == null) return 0L

        val scaled = when {
            api == null -> sysfs!!
            sysfs == null -> api
            else -> reconcileApiWithSysfs(api, sysfs)
        }
        return applySignForOem(scaled, api ?: sysfs!!, status, manufacturer)
    }

    fun readSysfsCurrentMicroA(): Long? {
        for (supply in PREFERRED_SUPPLIES) {
            readSysfsLongAt("/sys/class/power_supply/$supply/current_now")?.let { return it }
        }
        try {
            val root = File("/sys/class/power_supply")
            if (!root.isDirectory) return null
            for (child in root.listFiles() ?: emptyArray()) {
                readSysfsLongAt(File(child, "current_now").path)?.let { return it }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun sanitizeRaw(raw: Long): Long? {
        if (raw == 0L || raw == Long.MIN_VALUE) return null
        return raw
    }

    private fun isSamsungLike(manufacturer: String): Boolean =
        manufacturer.trim().lowercase(Locale.US) == "samsung"

    /**
     * Some Samsung fuel gauges report mA, but values above 8,000 are more safely interpreted
     * according to the Android API contract (ÂµA). Treating every sub-50,000 value as mA turns
     * legitimate idle values such as 12,000 ÂµA into impossible 12 A spikes.
     * Other OEMs: scale when plugged/charging (≥ 20 mA raw) or on discharge when raw looks like mA.
     */
    private fun reconcileApiWithSysfs(
        api: Long,
        sysfs: Long,
    ): Long {
        val apiAbs = abs(api)
        val sysfsAbs = abs(sysfs)
        if (apiAbs == 0L) return sysfs
        if (sysfsAbs == 0L) return api

        val ratio = sysfsAbs.toDouble() / apiAbs.toDouble()
        val magnitude = when {
            ratio in 0.7..1.43 -> sysfsAbs
            ratio in 700.0..1430.0 -> sysfsAbs
            ratio in 0.0007..0.00143 -> apiAbs
            else -> apiAbs
        }
        val signSource = if (ratio in 0.7..1.43 || ratio in 700.0..1430.0) sysfs else api
        return if (signSource < 0L) -magnitude else magnitude
    }

    /** Samsung often inverts the AOSP sign (positive = charging). */
    private fun applySignForOem(
        microA: Long,
        originalRaw: Long,
        status: Int,
        manufacturer: String,
    ): Long {
        val magnitude = abs(microA)
        if (magnitude == 0L) return 0L
        if (!isSamsungLike(manufacturer)) return microA

        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> magnitude
            BatteryManager.BATTERY_STATUS_DISCHARGING -> -magnitude
            BatteryManager.BATTERY_STATUS_FULL ->
                if (originalRaw > 0L) magnitude else if (originalRaw < 0L) -magnitude else 0L
            else -> microA
        }
    }

    private fun readSysfsLongAt(path: String): Long? {
        return try {
            val f = File(path)
            if (!f.exists() || !f.canRead()) return null
            val text = f.readText().trim()
            text.toLongOrNull() ?: return null
        } catch (_: Exception) {
            null
        }
    }
}
