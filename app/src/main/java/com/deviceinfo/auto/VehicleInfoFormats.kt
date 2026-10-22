package com.deviceinfo.auto

import android.content.Context

object VehicleInfoFormats {

    data class EnergyDisplay(
        val primary: String?,
        val detail: String?,
    ) {
        fun phoneText(): String = listOfNotNull(primary, detail).joinToString("\n")
        fun carLines(): List<String> = listOfNotNull(primary, detail)
    }

    fun percent(context: Context, value: Float?): String {
        if (value == null || value < 0f) return context.getString(R.string.not_available)
        return String.format(MeasurementUnits.activeLocale(context), "%.0f%%", value)
    }

    fun percentWithEnergyLow(
        context: Context,
        value: Float?,
        energyLow: Boolean?,
        lowLabelResId: Int,
    ): String {
        val pct = percent(context, value)
        val na = context.getString(R.string.not_available)
        if (energyLow != true || pct == na) return pct
        return listOf(pct, context.getString(lowLabelResId))
            .joinToString(DeviceInfoUiShared.COMPOUND_SEP)
    }

    fun fuelTypeLabels(context: Context, types: List<Int>): String {
        val labels = types
            .filter { it > 0 }
            .distinct()
            .map { VehicleEnergyLabels.fuelTypeLabel(context, it) }
        if (labels.isEmpty()) return context.getString(R.string.not_available)
        return DeviceInfoUiShared.joinList(labels)
    }

    fun evConnectorLabels(context: Context, types: List<Int>): String {
        val labels = types
            .distinct()
            .map { VehicleEnergyLabels.evConnectorLabel(context, it) }
        if (labels.isEmpty()) return context.getString(R.string.not_available)
        return DeviceInfoUiShared.joinList(labels)
    }

    fun fuelDisplay(
        context: Context,
        level: String,
        fuelTypes: String,
    ): EnergyDisplay = energyDisplay(
        context = context,
        level = level,
        details = fuelTypes,
        detailFormatResId = R.string.vehicle_fuel_type_format,
    )

    fun evBatteryDisplay(
        context: Context,
        level: String,
        connectors: String,
    ): EnergyDisplay = energyDisplay(
        context = context,
        level = level,
        details = connectors,
        detailFormatResId = R.string.vehicle_ev_connectors_format,
    )

    private fun energyDisplay(
        context: Context,
        level: String,
        details: String,
        detailFormatResId: Int,
    ): EnergyDisplay {
        val unavailable = context.getString(R.string.not_available)
        fun available(value: String): Boolean =
            value.isNotBlank() && value != unavailable && value != "—"

        val primary = level
            .takeIf(::available)
            ?.split(DeviceInfoUiShared.COMPOUND_SEP)
            ?.filter(String::isNotBlank)
            ?.joinToString(" — ")
            ?.takeIf(String::isNotBlank)
        val detail = details
            .takeIf(::available)
            ?.replace(DeviceInfoUiShared.LIST_LINE_SEP, ", ")
            ?.let { context.getString(detailFormatResId, it) }
        return EnergyDisplay(primary = primary, detail = detail)
    }

    fun yearLabel(context: Context, year: Int?): String =
        if (year == null || year <= 0) {
            context.getString(R.string.not_available)
        } else {
            year.toString()
        }

    fun textOrDash(context: Context, raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        return trimmed.ifEmpty { context.getString(R.string.not_available) }
    }
}
