package com.deviceinfo.auto

import android.content.Context
import androidx.annotation.StringRes

/**
 * Car API [androidx.car.app.hardware.info.EnergyProfile] fuel / EV connector codes
 * mapped to short, widely recognized display names.
 */
object VehicleEnergyLabels {

    object FuelType {
        const val UNLEADED = 1
        const val LEADED = 2
        const val DIESEL_1 = 3
        const val DIESEL_2 = 4
        const val BIODIESEL = 5
        const val E85 = 6
        const val LPG = 7
        const val CNG = 8
        const val LNG = 9
        const val ELECTRIC = 10
        const val HYDROGEN = 11
        const val OTHER = 12
    }

    object EvConnector {
        const val J1772 = 1
        const val TYPE_2 = 2
        const val CHADEMO = 3
        const val CCS_COMBO_1 = 4
        const val CCS_COMBO_2 = 5
        const val TESLA_ROADSTER = 6
        const val TESLA_HPWC = 7
        const val TESLA_SUPERCHARGER = 8
        const val GBT_AC = 9
        const val GBT_DC = 10
        const val SCAME = 11
    }

    private val fuelTypeLabels: Map<Int, Int> = mapOf(
        FuelType.UNLEADED to R.string.vehicle_fuel_unleaded,
        FuelType.LEADED to R.string.vehicle_fuel_leaded,
        FuelType.DIESEL_1 to R.string.vehicle_fuel_diesel,
        FuelType.DIESEL_2 to R.string.vehicle_fuel_diesel,
        FuelType.BIODIESEL to R.string.vehicle_fuel_biodiesel,
        FuelType.E85 to R.string.vehicle_fuel_e85,
        FuelType.LPG to R.string.vehicle_fuel_lpg,
        FuelType.CNG to R.string.vehicle_fuel_cng,
        FuelType.LNG to R.string.vehicle_fuel_lng,
        FuelType.ELECTRIC to R.string.vehicle_fuel_electric,
        FuelType.HYDROGEN to R.string.vehicle_fuel_hydrogen,
        FuelType.OTHER to R.string.vehicle_fuel_other,
    )

    private val evConnectorLabels: Map<Int, Int> = mapOf(
        EvConnector.J1772 to R.string.vehicle_ev_type_1,
        EvConnector.TYPE_2 to R.string.vehicle_ev_type_2,
        EvConnector.CHADEMO to R.string.vehicle_ev_chademo,
        EvConnector.CCS_COMBO_1 to R.string.vehicle_ev_ccs1,
        EvConnector.CCS_COMBO_2 to R.string.vehicle_ev_ccs2,
        EvConnector.TESLA_ROADSTER to R.string.vehicle_ev_tesla_legacy,
        EvConnector.TESLA_HPWC to R.string.vehicle_ev_tesla_wall,
        EvConnector.TESLA_SUPERCHARGER to R.string.vehicle_ev_supercharger,
        EvConnector.GBT_AC to R.string.vehicle_ev_gbt_ac,
        EvConnector.GBT_DC to R.string.vehicle_ev_gbt_dc,
        EvConnector.SCAME to R.string.vehicle_ev_scame,
    )

    fun fuelTypeLabel(context: Context, type: Int): String =
        label(context, type, fuelTypeLabels, R.string.vehicle_fuel_unknown)

    fun evConnectorLabel(context: Context, type: Int): String =
        label(context, type, evConnectorLabels, R.string.vehicle_ev_unknown)

    private fun label(
        context: Context,
        code: Int,
        mapping: Map<Int, Int>,
        @StringRes unknownRes: Int,
    ): String {
        val resId = mapping[code] ?: return context.getString(unknownRes, code)
        return context.getString(resId)
    }
}
