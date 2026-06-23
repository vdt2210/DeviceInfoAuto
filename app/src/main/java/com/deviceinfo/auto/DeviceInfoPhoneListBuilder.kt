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
        fun rowForKey(key: String): InfoItem.Row {
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

        val phoneSchema = DeviceInfoSchema.phoneSchema(info)

        val items = mutableListOf<InfoItem>()
        for ((sectionKey, rows) in phoneSchema) {
            items += InfoItem.Section(sectionKey)
            rows.forEach { rowKey -> items += rowForKey(rowKey) }
        }
        return items
    }
}
