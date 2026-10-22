package com.deviceinfo.auto

import android.location.Location
import android.os.SystemClock
import java.util.concurrent.CopyOnWriteArrayList

data class CarLocationSnapshot(
    val vehicle: Location? = null,
    val device: Location? = null,
)

/** Location bridge owned by the active car-app session. Vehicle and device sources stay distinct. */
object CarLocationStore {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    @Volatile private var snapshot = CarLocationSnapshot()

    fun addListener(listener: () -> Unit) = listeners.add(listener)
    fun removeListener(listener: () -> Unit) = listeners.remove(listener)
    fun snapshot(): CarLocationSnapshot = snapshot.copy(
        vehicle = snapshot.vehicle?.let(::Location),
        device = snapshot.device?.let(::Location),
    )

    fun updateVehicle(location: Location?) = update(vehicle = location, device = snapshot.device)
    fun updateDevice(location: Location?) = update(vehicle = snapshot.vehicle, device = location)

    fun clear() {
        snapshot = CarLocationSnapshot()
        listeners.forEach { it() }
    }

    private fun update(vehicle: Location?, device: Location?) {
        snapshot = CarLocationSnapshot(
            vehicle = vehicle?.let(::Location),
            device = device?.let(::Location),
        )
        listeners.forEach { it() }
    }

    fun ageMs(location: Location): Long =
        if (location.elapsedRealtimeNanos > 0L) {
            ((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L)
                .coerceAtLeast(0L)
        } else {
            (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
        }
}
