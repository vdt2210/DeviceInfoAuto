package com.deviceinfo.auto

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import android.content.Context

/** Raw vehicle telemetry from [VehicleInfoCarCollector] while the car app session is active. */
data class VehicleHardwareSnapshot(
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val speedDisplayMps: Float? = null,
    val speedRawMps: Float? = null,
    val fuelVolumePercent: Float? = null,
    val evBatteryPercent: Float? = null,
    val fuelLevelLow: Boolean? = null,
    val rangeRemainingMeters: Int? = null,
    val odometerMeters: Long? = null,
    val fuelTypes: List<Int> = emptyList(),
    val evConnectorTypes: List<Int> = emptyList(),
    val lastUpdatedAtMs: Long = 0L,
    val hardwareActive: Boolean = false,
)

/**
 * In-memory bridge: [VehicleInfoCarCollector] (AA session) writes, phone vehicle tab reads.
 * Listeners push UI updates — no phone-side polling.
 */
object VehicleInfoStore {
    private val hardware = AtomicReference(VehicleHardwareSnapshot())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun update(transform: (VehicleHardwareSnapshot) -> VehicleHardwareSnapshot) {
        hardware.updateAndGet(transform)
        listeners.forEach { it.invoke() }
    }

    fun snapshot(): VehicleHardwareSnapshot = hardware.get()

    private const val PREFS_NAME = "vehicle_info_store"
    private const val KEY_CAR_SESSION_ACTIVE = "car_app_session_active"

    /** Persists car-app session across AA host process / phone UI (in-memory alone is not always shared). */
    fun setCarAppSessionActive(context: Context, active: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CAR_SESSION_ACTIVE, active)
            .apply()
        listeners.forEach { it.invoke() }
    }

    fun isCarAppSessionActive(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CAR_SESSION_ACTIVE, false)

    fun clear() {
        hardware.set(VehicleHardwareSnapshot())
        listeners.forEach { it.invoke() }
    }
}
