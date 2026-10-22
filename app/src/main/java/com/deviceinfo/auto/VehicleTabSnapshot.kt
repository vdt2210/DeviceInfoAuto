package com.deviceinfo.auto

/** Display-ready vehicle / Android Auto head-unit fields for the phone vehicle tab. */
data class VehicleTabSnapshot(
    /** Telemetry rows (make, speed, fuel, …) — only when AA is connected and CarInfo is live. */
    val dataRowsVisible: Boolean = false,
    val connectionSummary: String = "—",
    val make: String = "—",
    val model: String = "—",
    val year: String = "—",
    val speedDisplay: String = "—",
    val speedRaw: String = "—",
    val fuelLevel: String = "—",
    val evBattery: String = "—",
    val rangeRemaining: String = "—",
    val odometer: String = "—",
    val fuelTypes: String = "—",
    val evConnectors: String = "—",
    val dataStatus: String = "—",
    val powertrain: VehiclePowertrain = VehiclePowertrain.UNKNOWN,
)
