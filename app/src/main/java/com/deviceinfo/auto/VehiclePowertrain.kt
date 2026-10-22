package com.deviceinfo.auto

/** Inferred from head-unit [EnergyProfile] / connector data. */
enum class VehiclePowertrain {
    UNKNOWN,
    FUEL,
    EV,
    HYBRID,
}

object VehiclePowertrainRules {

    /** [VehicleEnergyLabels.FuelType.ELECTRIC] */
    private const val FUEL_TYPE_ELECTRIC = VehicleEnergyLabels.FuelType.ELECTRIC

    fun fromHardware(hw: VehicleHardwareSnapshot): VehiclePowertrain {
        val fuelTypes = hw.fuelTypes.filter { it > 0 }
        val hasEvConnectors = hw.evConnectorTypes.isNotEmpty()
        val hasElectricFuel = fuelTypes.contains(FUEL_TYPE_ELECTRIC)
        val hasCombustionFuel = fuelTypes.any { it != FUEL_TYPE_ELECTRIC }

        return when {
            fuelTypes.isEmpty() && !hasEvConnectors -> VehiclePowertrain.UNKNOWN
            (hasElectricFuel || hasEvConnectors) && hasCombustionFuel -> VehiclePowertrain.HYBRID
            hasElectricFuel || hasEvConnectors -> VehiclePowertrain.EV
            hasCombustionFuel -> VehiclePowertrain.FUEL
            else -> VehiclePowertrain.UNKNOWN
        }
    }

    fun rowKeys(powertrain: VehiclePowertrain, dataRowsVisible: Boolean = false): List<String> {
        if (!dataRowsVisible) {
            return listOf(
                DeviceInfoUiShared.Row.VEHICLE_CONNECTION,
                DeviceInfoUiShared.Row.VEHICLE_DATA_STATUS,
            )
        }
        return buildList {
            add(DeviceInfoUiShared.Row.VEHICLE_CONNECTION)
            add(DeviceInfoUiShared.Row.VEHICLE_IDENTITY)
            add(DeviceInfoUiShared.Row.VEHICLE_SPEED)
            add(DeviceInfoUiShared.Row.VEHICLE_SPEED_RAW)
            when (powertrain) {
                VehiclePowertrain.FUEL,
                VehiclePowertrain.HYBRID,
                -> {
                    add(DeviceInfoUiShared.Row.VEHICLE_FUEL_LEVEL)
                }
                VehiclePowertrain.UNKNOWN,
                VehiclePowertrain.EV,
                -> Unit
            }
            add(DeviceInfoUiShared.Row.VEHICLE_RANGE)
            add(DeviceInfoUiShared.Row.VEHICLE_ODOMETER)
            when (powertrain) {
                VehiclePowertrain.EV,
                VehiclePowertrain.HYBRID,
                -> {
                    add(DeviceInfoUiShared.Row.VEHICLE_EV_BATTERY)
                }
                VehiclePowertrain.UNKNOWN,
                VehiclePowertrain.FUEL,
                -> Unit
            }
            add(DeviceInfoUiShared.Row.VEHICLE_DATA_STATUS)
        }
    }
}
