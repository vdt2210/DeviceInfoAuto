package com.deviceinfo.auto

import androidx.car.app.connection.CarConnection
import java.util.concurrent.atomic.AtomicInteger

/** Last-known AA head-unit connection type — updated on the main thread only. */
object VehicleConnectionState {
    private val connectionType = AtomicInteger(CarConnection.CONNECTION_TYPE_NOT_CONNECTED)

    fun update(type: Int) {
        connectionType.set(type)
    }

    fun connectionType(): Int = connectionType.get()
}
