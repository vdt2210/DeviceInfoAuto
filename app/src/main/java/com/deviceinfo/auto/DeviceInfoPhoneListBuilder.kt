package com.deviceinfo.auto

import android.content.Context

/** Builds the phone [InfoItem] list schema from [DeviceInfo]. */
object DeviceInfoPhoneListBuilder {

    fun build(
        context: Context,
        info: DeviceInfo,
        displayRotation: Int,
        sensorDisplayForRow: (String) -> String,
        sensorRowValueForUi: (String, String) -> String,
        notAvailableText: () -> CharSequence,
    ): List<InfoItem> {
        val phoneSchema = DeviceInfoSchema.phoneSchema(info)
        val items = mutableListOf<InfoItem>()
        for ((sectionKey, rows) in phoneSchema) {
            items += InfoItem.Section(sectionKey)
            items += sectionEntriesToItems(
                context,
                rows,
                info,
                displayRotation,
                sensorDisplayForRow,
                sensorRowValueForUi,
                notAvailableText,
            )
        }
        return items
    }

    fun buildSection(
        context: Context,
        sectionKey: String,
        info: DeviceInfo,
        displayRotation: Int,
        sensorDisplayForRow: (String) -> String,
        sensorRowValueForUi: (String, String) -> String,
        notAvailableText: () -> CharSequence,
    ): List<InfoItem> {
        val rows = DeviceInfoSchema.phoneSchema(info)
            .firstOrNull { it.first == sectionKey }
            ?.second
            ?: return emptyList()
        return sectionEntriesToItems(
            context,
            rows,
            info,
            displayRotation,
            sensorDisplayForRow,
            sensorRowValueForUi,
            notAvailableText,
        )
    }

    private fun sectionEntriesToItems(
        context: Context,
        entries: List<String>,
        info: DeviceInfo,
        displayRotation: Int,
        sensorDisplayForRow: (String) -> String,
        sensorRowValueForUi: (String, String) -> String,
        notAvailableText: () -> CharSequence,
    ): List<InfoItem> {
        val items = mutableListOf<InfoItem>()
        for (entry in entries) {
            if (DeviceInfoUiShared.isSubsectionKey(entry)) {
                items += InfoItem.Section(entry)
                continue
            }
            items += rowForKey(
                context,
                entry,
                info,
                displayRotation,
                sensorDisplayForRow,
                sensorRowValueForUi,
                notAvailableText,
            )
        }
        return items
    }

    private fun rowForKey(
        context: Context,
        key: String,
        info: DeviceInfo,
        displayRotation: Int,
        sensorDisplayForRow: (String) -> String,
        sensorRowValueForUi: (String, String) -> String,
        notAvailableText: () -> CharSequence,
    ): InfoItem.Row {
        val ui = DeviceInfoPhoneRow.uiState(
            context = context,
            key = key,
            info = info,
            displayRotation = displayRotation,
            sensorDisplayValue = sensorRowValueForUi(key, sensorDisplayForRow(key)),
            fallbackNotAvailable = notAvailableText(),
        ) ?: return InfoItem.Row(
            key,
            DeviceInfoUiShared.rowTitle(context, key),
            notAvailableText(),
            R.drawable.ic_row_unknown,
        )
        return InfoItem.Row(key, ui.title, ui.value, ui.iconResId)
    }
}
