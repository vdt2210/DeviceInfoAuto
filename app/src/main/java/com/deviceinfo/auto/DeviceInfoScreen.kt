package com.deviceinfo.auto

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Android Auto: battery + RAM. Live values refresh on a fixed poll only (no battery
 * broadcasts) so the host is not rebuilding [ListTemplate] continuously while scrolling.
 */
class DeviceInfoScreen(carContext: CarContext) : Screen(carContext) {

    private val appCtx: Context = carContext.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(carContext)
    private val handler = Handler(Looper.getMainLooper())

    /** Last displayed telemetry; skip [invalidate] when poll sees no change. */
    private var lastTelemetrySnapshot: String? = null

    private val telemetryPollRunnable = object : Runnable {
        override fun run() {
            val info = DeviceInfoProvider.get(appCtx)
            val snapshot = telemetrySnapshot(info)
            if (snapshot != lastTelemetrySnapshot) {
                lastTelemetrySnapshot = snapshot
                invalidate()
            }
            handler.postDelayed(this, DeviceInfoUiShared.CAR_TELEMETRY_POLL_INTERVAL_MS)
        }
    }

    private val updatesObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            handler.removeCallbacks(telemetryPollRunnable)
            handler.post(telemetryPollRunnable)
        }

        override fun onStop(owner: LifecycleOwner) {
            handler.removeCallbacks(telemetryPollRunnable)
            lastTelemetrySnapshot = null
        }
    }

    init {
        lifecycle.addObserver(updatesObserver)
    }

    /** Stable key of all dynamic row bodies (pin, dòng, công suất, RAM, …). */
    private fun telemetrySnapshot(info: DeviceInfo): String = buildString {
        append(info.batteryLevel).append('|')
        append(info.isPowerSaveMode).append('|')
        append(info.plugged).append('|')
        append(info.health).append('|')
        append(info.batteryTemperatureCelsius).append('|')
        append(info.currentNowMicroA).append('|')
        append(info.batteryVoltageV).append('|')
        append(info.ramSummary)
    }

    override fun onGetTemplate(): Template {
        val info = DeviceInfoProvider.get(appCtx)
        lastTelemetrySnapshot = telemetrySnapshot(info)
        val ctx = AppPreferences.localizedContext(carContext)
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
                row(
                    DeviceInfoUiShared.rowTitle(ctx, key),
                    DeviceInfoUiShared.levelText(ctx, info, DeviceInfoUiShared.DisplaySurface.CAR),
                    icon(DeviceInfoUiShared.levelIconRes(info.batteryLevel, info.isPowerSaveMode))
                )
            DeviceInfoUiShared.Row.PLUGGED ->
                row(
                    DeviceInfoUiShared.rowTitle(ctx, key),
                    DeviceInfoUiShared.pluggedLabel(ctx, info.plugged),
                    icon(DeviceInfoUiShared.pluggedIconRes(info.plugged))
                )
            DeviceInfoUiShared.Row.HEALTH ->
                row(
                    DeviceInfoUiShared.rowTitle(ctx, key),
                    DeviceInfoUiShared.healthLabel(ctx, info.health),
                    icon(DeviceInfoUiShared.healthIconRes(info.health))
                )
            DeviceInfoUiShared.Row.TEMPERATURE ->
                row(
                    DeviceInfoUiShared.rowTitle(ctx, key),
                    DeviceInfoUiShared.temperatureText(info.batteryTemperatureCelsius),
                    icon(DeviceInfoUiShared.temperatureIconRes(info.batteryTemperatureCelsius))
                )
            DeviceInfoUiShared.Row.CURRENT ->
                row(
                    DeviceInfoUiShared.rowTitle(ctx, key),
                    DeviceInfoUiShared.currentText(info.currentNowMicroA),
                    icon(R.drawable.ic_row_current)
                )
            DeviceInfoUiShared.Row.POWER ->
                row(
                    DeviceInfoUiShared.rowTitle(ctx, key),
                    DeviceInfoUiShared.instantPowerText(info.batteryVoltageV, info.currentNowMicroA),
                    icon(R.drawable.ic_row_current)
                )
            DeviceInfoUiShared.Row.RAM ->
                row(
                    DeviceInfoUiShared.rowTitle(ctx, key),
                    info.ramSummary,
                    icon(R.drawable.ic_row_memory)
                )
            else ->
                row(
                    "—",
                    DeviceInfoUiShared.valueNotAvailableLabel(ctx),
                    icon(R.drawable.ic_row_unknown)
                )
        }

        val refreshIcon = icon(R.drawable.ic_row_refresh)
        val refreshAction = Action.Builder()
            .setIcon(refreshIcon)
            .setOnClickListener {
                lastTelemetrySnapshot = null
                mainExecutor.execute { invalidate() }
            }
            .build()

        val actionStrip = ActionStrip.Builder()
            .addAction(refreshAction)
            .build()

        return ListTemplate.Builder()
            .setTitle(ctx.getString(R.string.app_name))
            .setActionStrip(actionStrip)
            .apply {
                val autoSchema = listOf(
                    DeviceInfoUiShared.Section.BATTERY to buildList {
                        add(DeviceInfoUiShared.Row.LEVEL)
                        add(DeviceInfoUiShared.Row.PLUGGED)
                        add(DeviceInfoUiShared.Row.HEALTH)
                        add(DeviceInfoUiShared.Row.TEMPERATURE)
                        add(DeviceInfoUiShared.Row.CURRENT)
                        add(DeviceInfoUiShared.Row.POWER)
                    },
                    DeviceInfoUiShared.Section.RAM to listOf(
                        DeviceInfoUiShared.Row.RAM
                    ),
                )
                autoSchema.forEach { (sectionKey, rows) ->
                    val sectionTitle = DeviceInfoUiShared.sectionTitle(ctx, sectionKey)
                    val list = ItemList.Builder().apply {
                        rows.forEach { addItem(rowForKey(it)) }
                    }.build()
                    addSectionedList(SectionedItemList.create(list, sectionTitle))
                }
            }
            .build()
    }
}
