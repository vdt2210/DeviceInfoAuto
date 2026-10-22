package com.deviceinfo.auto

import android.location.GnssStatus
import java.util.Locale

data class GnssInfoSnapshot(
    val satellitesVisible: Int? = null,
    val satellitesUsedInFix: Int? = null,
    val timeToFirstFixMs: Int? = null,
    val frequencyBands: List<String> = emptyList(),
)

/** Latest GNSS engine diagnostics while the phone Location tab is visible. */
object GnssInfoStore {

    @Volatile
    private var snapshot = GnssInfoSnapshot()

    fun updateStatus(status: GnssStatus) {
        var used = 0
        val bands = linkedSetOf<String>()
        for (index in 0 until status.satelliteCount) {
            if (status.usedInFix(index)) used++
            if (status.hasCarrierFrequencyHz(index)) {
                frequencyBand(status.getCarrierFrequencyHz(index))?.let(bands::add)
            }
        }
        snapshot = snapshot.copy(
            satellitesVisible = status.satelliteCount,
            satellitesUsedInFix = used,
            frequencyBands = bands.toList(),
        )
    }

    fun updateTimeToFirstFix(milliseconds: Int) {
        snapshot = snapshot.copy(timeToFirstFixMs = milliseconds.takeIf { it >= 0 })
    }

    fun snapshot(): GnssInfoSnapshot = snapshot

    fun clear() {
        snapshot = GnssInfoSnapshot()
    }

    internal fun frequencyBand(frequencyHz: Float): String? {
        if (!frequencyHz.isFinite() || frequencyHz <= 0f) return null
        val mhz = frequencyHz / 1_000_000f
        return when (mhz) {
            in 1559f..1610f -> "L1/E1/B1"
            in 1215f..1300f -> "L2"
            in 1164f..1215f -> "L5/E5/B2"
            else -> String.format(Locale.US, "%.1f MHz", mhz)
        }
    }
}
