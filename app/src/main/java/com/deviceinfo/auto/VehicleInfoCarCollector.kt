package com.deviceinfo.auto

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.GnssStatus
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.connection.CarConnection
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.CarHardwareLocation
import androidx.car.app.hardware.info.CarSensors
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.EnergyProfile
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Model
import androidx.car.app.hardware.info.Speed
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

/**
 * Subscribes to [CarInfo] while an Android Auto / AAOS car-app session is active and mirrors
 * telemetry into [VehicleInfoStore] for the phone vehicle tab. Does not touch [DeviceInfoScreen].
 */
class VehicleInfoCarCollector private constructor(
    private val carContext: CarContext,
) {
    private val executor: Executor = Executor { runnable -> carContext.mainExecutor.execute(runnable) }
    private var carInfo: CarInfo? = null
    private var carSensors: CarSensors? = null
    private val locationManager = carContext.getSystemService(LocationManager::class.java)
    private var gnssCallbackRegistered = false
    private var started = false

    private val speedListener = OnCarDataAvailableListener { value: Speed -> applySpeed(value) }
    private val energyListener = OnCarDataAvailableListener { value: EnergyLevel -> applyEnergy(value) }
    private val mileageListener = OnCarDataAvailableListener { value: Mileage -> applyMileage(value) }
    private val carLocationListener =
        OnCarDataAvailableListener { value: CarHardwareLocation ->
            val location = value.location
            CarLocationStore.updateVehicle(
                if (location.status == CarValue.STATUS_SUCCESS) location.value else null,
            )
        }
    private val deviceLocationListener = LocationListener { location: Location ->
        val current = CarLocationStore.snapshot().device
        if (current == null || location.elapsedRealtimeNanos >= current.elapsedRealtimeNanos) {
            CarLocationStore.updateDevice(location)
        }
    }
    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onFirstFix(ttffMillis: Int) {
            GnssInfoStore.updateTimeToFirstFix(ttffMillis)
        }

        override fun onSatelliteStatusChanged(status: GnssStatus) {
            GnssInfoStore.updateStatus(status)
        }
    }

    fun start() {
        if (started) return
        started = true
        VehicleInfoStore.setCarAppSessionActive(carContext, true)
        VehicleInfoStore.update { it.copy(hardwareActive = true, lastUpdatedAtMs = now()) }
        if (carContext.carAppApiLevel < 3) return
        SensitivePermissions.requestVehiclePermissions(carContext) {
            SensitivePermissions.requestLocationPermissions(carContext) {
                startDeviceLocation()
                startHardwareListeners()
            }
        }
    }

    private fun startHardwareListeners() {
        if (carInfo != null) return
        runCatching {
            val info = carContext.getCarService(CarHardwareManager::class.java).carInfo
            val sensors = carContext.getCarService(CarHardwareManager::class.java).carSensors
            carInfo = info
            carSensors = sensors
            info.fetchModel(executor) { model -> applyModel(model) }
            info.fetchEnergyProfile(executor) { profile -> applyEnergyProfile(profile) }
            info.addSpeedListener(executor, speedListener)
            info.addEnergyLevelListener(executor, energyListener)
            info.addMileageListener(executor, mileageListener)
            sensors.addCarHardwareLocationListener(
                CarSensors.UPDATE_RATE_UI,
                executor,
                carLocationListener,
            )
        }
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching {
            carInfo?.removeSpeedListener(speedListener)
            carInfo?.removeEnergyLevelListener(energyListener)
            carInfo?.removeMileageListener(mileageListener)
            carSensors?.removeCarHardwareLocationListener(carLocationListener)
            locationManager?.removeUpdates(deviceLocationListener)
            if (gnssCallbackRegistered) {
                locationManager?.unregisterGnssStatusCallback(gnssStatusCallback)
            }
        }
        gnssCallbackRegistered = false
        GnssInfoStore.clear()
        carInfo = null
        carSensors = null
        VehicleInfoStore.setCarAppSessionActive(carContext, false)
        VehicleInfoStore.clear()
        CarLocationStore.clear()
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun startDeviceLocation() {
        if (!SensitivePermissions.hasLocationPermission(carContext)) return
        val provider = LocationManager.GPS_PROVIDER
        runCatching {
            if (!locationManager.isProviderEnabled(provider)) return@runCatching
            locationManager.getLastKnownLocation(provider)
                ?.let(deviceLocationListener::onLocationChanged)
            locationManager.requestLocationUpdates(
                provider,
                1_000L,
                0f,
                deviceLocationListener,
                Looper.getMainLooper(),
            )
            if (!gnssCallbackRegistered) {
                gnssCallbackRegistered = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    locationManager.registerGnssStatusCallback(executor, gnssStatusCallback)
                } else {
                    locationManager.registerGnssStatusCallback(
                        gnssStatusCallback,
                        Handler(Looper.getMainLooper()),
                    )
                }
            }
        }
    }

    private fun applyModel(model: Model) {
        VehicleInfoStore.update {
            it.copy(
                make = unwrapString(model.manufacturer),
                model = unwrapString(model.name),
                year = unwrapInt(model.year),
                lastUpdatedAtMs = now(),
                hardwareActive = true,
            )
        }
    }

    private fun applyEnergyProfile(profile: EnergyProfile) {
        VehicleInfoStore.update {
            it.copy(
                fuelTypes = unwrapIntList(profile.fuelTypes),
                evConnectorTypes = unwrapIntList(profile.evConnectorTypes),
                lastUpdatedAtMs = now(),
                hardwareActive = true,
            )
        }
    }

    private fun applySpeed(speed: Speed) {
        VehicleInfoStore.update {
            it.copy(
                speedDisplayMps = unwrapFloat(speed.displaySpeedMetersPerSecond),
                speedRawMps = unwrapFloat(speed.rawSpeedMetersPerSecond),
                lastUpdatedAtMs = now(),
                hardwareActive = true,
            )
        }
    }

    private fun applyEnergy(energy: EnergyLevel) {
        VehicleInfoStore.update {
            it.copy(
                fuelVolumePercent = unwrapFloat(energy.fuelPercent),
                evBatteryPercent = unwrapFloat(energy.batteryPercent),
                fuelLevelLow = unwrapBoolean(energy.energyIsLow),
                rangeRemainingMeters = unwrapFloat(energy.rangeRemainingMeters)?.toInt(),
                lastUpdatedAtMs = now(),
                hardwareActive = true,
            )
        }
    }

    private fun applyMileage(mileage: Mileage) {
        VehicleInfoStore.update {
            it.copy(
                odometerMeters = unwrapFloat(mileage.odometerMeters)?.toLong(),
                lastUpdatedAtMs = now(),
                hardwareActive = true,
            )
        }
    }

    private fun unwrapString(value: CarValue<String>?): String? {
        if (value == null || !value.isSuccess()) return null
        return value.value?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun unwrapInt(value: CarValue<Int>?): Int? {
        if (value == null || !value.isSuccess()) return null
        val v = value.value ?: return null
        return v.takeIf { it > 0 }
    }

    private fun unwrapFloat(value: CarValue<Float>?): Float? {
        if (value == null || !value.isSuccess()) return null
        val v = value.value ?: return null
        return v.takeIf { it >= 0f }
    }

    private fun unwrapBoolean(value: CarValue<Boolean>?): Boolean? {
        if (value == null || !value.isSuccess()) return null
        return value.value
    }

    private fun unwrapIntList(value: CarValue<List<Int>>?): List<Int> {
        if (value == null || !value.isSuccess()) return emptyList()
        return value.value.orEmpty()
    }

    private fun CarValue<*>.isSuccess(): Boolean = status == CarValue.STATUS_SUCCESS

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        private val collectors = mutableMapOf<CarContext, VehicleInfoCarCollector>()

        fun attach(carContext: CarContext) {
            val collector = collectors.getOrPut(carContext) { VehicleInfoCarCollector(carContext) }
            collector.start()
            if (carContext is LifecycleOwner) {
                carContext.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        collectors.remove(carContext)?.stop()
                        owner.lifecycle.removeObserver(this)
                    }
                })
            }
        }
    }
}

object VehicleInfoReader {

    fun read(context: Context): VehicleTabSnapshot {
        val connectionType = VehicleConnectionState.connectionType()
        val connectionSummary = connectionLabel(context, connectionType)
        val connected = connectionType != CarConnection.CONNECTION_TYPE_NOT_CONNECTED
        if (!connected) {
            return VehicleTabSnapshot(
                connectionSummary = connectionSummary,
                dataStatus = context.getString(R.string.vehicle_data_connect_aa),
            )
        }
        val hw = VehicleInfoStore.snapshot()
        val sessionActive = hw.hardwareActive || VehicleInfoStore.isCarAppSessionActive(context)
        if (!sessionActive) {
            return VehicleTabSnapshot(
                connectionSummary = connectionSummary,
                dataStatus = context.getString(R.string.vehicle_data_open_aa_app),
            )
        }
        val powertrain = VehiclePowertrainRules.fromHardware(hw)
        val dataStatus = if (hw.lastUpdatedAtMs > 0L) {
            context.getString(R.string.vehicle_data_live)
        } else {
            context.getString(R.string.vehicle_data_waiting)
        }
        return VehicleTabSnapshot(
            dataRowsVisible = true,
            connectionSummary = connectionSummary,
            make = VehicleInfoFormats.textOrDash(context, hw.make),
            model = VehicleInfoFormats.textOrDash(context, hw.model),
            year = VehicleInfoFormats.yearLabel(context, hw.year),
            speedDisplay = MeasurementUnits.speed(context, hw.speedDisplayMps),
            speedRaw = MeasurementUnits.speed(context, hw.speedRawMps),
            fuelLevel = VehicleInfoFormats.percentWithEnergyLow(
                context,
                hw.fuelVolumePercent,
                hw.fuelLevelLow.takeIf { powertrain == VehiclePowertrain.FUEL || powertrain == VehiclePowertrain.HYBRID },
                R.string.vehicle_energy_low_fuel,
            ),
            evBattery = VehicleInfoFormats.percentWithEnergyLow(
                context,
                hw.evBatteryPercent,
                hw.fuelLevelLow.takeIf { powertrain == VehiclePowertrain.EV || powertrain == VehiclePowertrain.HYBRID },
                R.string.vehicle_energy_low_battery,
            ),
            rangeRemaining = MeasurementUnits.range(context, hw.rangeRemainingMeters),
            odometer = MeasurementUnits.odometer(context, hw.odometerMeters),
            fuelTypes = VehicleInfoFormats.fuelTypeLabels(context, hw.fuelTypes),
            evConnectors = VehicleInfoFormats.evConnectorLabels(context, hw.evConnectorTypes),
            dataStatus = dataStatus,
            powertrain = powertrain,
        )
    }

    fun connectionLabel(context: Context, connectionType: Int): String =
        when (connectionType) {
            CarConnection.CONNECTION_TYPE_PROJECTION ->
                context.getString(R.string.vehicle_connection_projection)
            CarConnection.CONNECTION_TYPE_NATIVE ->
                context.getString(R.string.vehicle_connection_native)
            CarConnection.CONNECTION_TYPE_NOT_CONNECTED ->
                context.getString(R.string.vehicle_connection_none)
            else -> context.getString(R.string.vehicle_connection_unknown)
        }
}
