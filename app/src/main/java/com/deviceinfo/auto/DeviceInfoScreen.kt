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

        fun levelIcon(level: Int, isPowerSaveMode: Boolean): CarIcon =
            when {
                level < 13 -> icon(R.drawable.ic_row_battery_0)   // 0–12.5%
                level < 25 -> icon(R.drawable.ic_row_battery_1)   // 12.5–25%
                level < 38 -> icon(R.drawable.ic_row_battery_2)   // 25–37.5%
                level < 50 -> icon(R.drawable.ic_row_battery_3)   // 37.5–50%
                level < 63 -> icon(R.drawable.ic_row_battery_4)   // 50–62.5%
                level < 75 -> icon(R.drawable.ic_row_battery_5)   // 62.5–75%
                level < 88 -> icon(R.drawable.ic_row_battery_6)   // 75–87.5%
                else -> icon(R.drawable.ic_row_battery_full)      // 87.5–100%
            }

        fun healthIcon(health: String): CarIcon =
            when (health) {
                "Good" -> icon(R.drawable.ic_row_health)
                "Unknown" -> icon(R.drawable.ic_row_unknown)
                else -> icon(R.drawable.ic_row_error)
            }

        fun temperatureIcon(tempC: Float): CarIcon =
            when {
                tempC < 10f -> icon(R.drawable.ic_row_temp_cold)
                tempC >= 45f -> icon(R.drawable.ic_row_temp_hot)
                tempC >= 35f -> icon(R.drawable.ic_row_temp_warm)
                else -> icon(R.drawable.ic_row_temp_normal)
            }

        fun levelText(info: DeviceInfo): String {
            val base = "${info.batteryLevel}% • ${info.chargingStatus}"
            return if (info.isPowerSaveMode) "$base • Saver on" else base
        }

        val batteryList = ItemList.Builder()
            .addItem(row("Level", levelText(info), levelIcon(info.batteryLevel, info.isPowerSaveMode)))
            .addItem(row("Health", info.health, healthIcon(info.health)))
            .addItem(row("Temperature", String.format("%.1f °C", info.batteryTemperatureCelsius), temperatureIcon(info.batteryTemperatureCelsius)))
            .build()
        val memoryList = ItemList.Builder()
            .addItem(row("RAM", info.ramSummary, icon(R.drawable.ic_row_memory)))
            .build()

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
            .addSectionedList(SectionedItemList.create(batteryList, "Battery"))
            .addSectionedList(SectionedItemList.create(memoryList, "RAM"))
            .build()
    }

    companion object {
        private const val MIN_REFRESH_INTERVAL_MS = 3_000L
    }
}
