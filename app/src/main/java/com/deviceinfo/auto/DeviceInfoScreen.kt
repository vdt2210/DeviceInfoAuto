package com.deviceinfo.auto

import android.graphics.Color
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat

class DeviceInfoScreen(carContext: CarContext) : Screen(carContext) {

    private var lastRefreshTimeMs: Long = 0L

    override fun onGetTemplate(): Template {
        val info = DeviceInfoProvider.get(carContext.applicationContext)
        val ctx = carContext.applicationContext
        fun icon(resId: Int): CarIcon {
            val white = Color.WHITE
            val tint = CarColor.createCustom(white, white)
            return CarIcon.Builder(IconCompat.createWithResource(ctx, resId))
                .setTint(tint)
                .build()
        }
        fun row(title: String, text: String, carIcon: CarIcon) =
            Row.Builder()
                .setTitle(title)
                .setImage(carIcon, Row.IMAGE_TYPE_LARGE)
                .addText(text)
                .build()

        fun rowForKey(key: String): Row = when (key) {
            DeviceInfoUiShared.Row.LEVEL ->
                row(key, DeviceInfoUiShared.levelText(info), icon(DeviceInfoUiShared.levelIconRes(info.batteryLevel, info.isPowerSaveMode)))
            DeviceInfoUiShared.Row.PLUGGED ->
                row(key, info.plugged, icon(DeviceInfoUiShared.pluggedIconRes(info.plugged)))
            DeviceInfoUiShared.Row.HEALTH ->
                row(key, info.health, icon(DeviceInfoUiShared.healthIconRes(info.health)))
            DeviceInfoUiShared.Row.TEMPERATURE ->
                row(key, String.format("%.1f °C", info.batteryTemperatureCelsius), icon(DeviceInfoUiShared.temperatureIconRes(info.batteryTemperatureCelsius)))
            DeviceInfoUiShared.Row.CURRENT ->
                row(key, DeviceInfoUiShared.currentText(info.currentNowMicroA), icon(R.drawable.ic_row_current))
            DeviceInfoUiShared.Row.RAM ->
                row(key, info.ramSummary, icon(R.drawable.ic_row_memory))
            else ->
                row(key, "—", icon(R.drawable.ic_row_unknown))
        }

        val refreshIcon = icon(R.drawable.ic_row_refresh)
        val refreshAction = Action.Builder()
            .setIcon(refreshIcon)
            .setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastRefreshTimeMs < MIN_REFRESH_INTERVAL_MS) {
                    CarToast.makeText(
                        carContext,
                        "Please wait before refreshing again",
                        CarToast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                lastRefreshTimeMs = now
                invalidate()
                CarToast.makeText(
                    carContext,
                    "Refreshing device info…",
                    CarToast.LENGTH_SHORT
                ).show()
            }
            .build()

        val actionStrip = ActionStrip.Builder()
            .addAction(refreshAction)
            .build()

        return ListTemplate.Builder()
            .setTitle("Device information")
            .setActionStrip(actionStrip)
            .apply {
                val autoSchema = listOf(
                    DeviceInfoUiShared.Section.BATTERY to listOf(
                        DeviceInfoUiShared.Row.LEVEL,
                        DeviceInfoUiShared.Row.PLUGGED,
                        DeviceInfoUiShared.Row.HEALTH,
                        DeviceInfoUiShared.Row.TEMPERATURE,
                        DeviceInfoUiShared.Row.CURRENT
                    ),
                    DeviceInfoUiShared.Section.RAM to listOf(
                        DeviceInfoUiShared.Row.RAM
                    )
                )
                autoSchema.forEach { (sectionTitle, rows) ->
                    val list = ItemList.Builder().apply {
                        rows.forEach { addItem(rowForKey(it)) }
                    }.build()
                    addSectionedList(SectionedItemList.create(list, sectionTitle))
                }
            }
            .build()
    }

    companion object {
        private const val MIN_REFRESH_INTERVAL_MS = 3_000L
    }
}
