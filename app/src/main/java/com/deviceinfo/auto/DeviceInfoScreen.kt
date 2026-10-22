package com.deviceinfo.auto

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.location.Location
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

/**
 * Android Auto telemetry. The visible row structure stays fixed and all updates pass through
 * one rendered-model scheduler so the host is not asked to rebuild the list unnecessarily.
 */
class DeviceInfoScreen(carContext: CarContext) : Screen(carContext) {

    private val appCtx: Context = carContext.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(carContext)
    private val handler = Handler(Looper.getMainLooper())
    private val telemetryExecutor = Executors.newSingleThreadExecutor()

    /** The model actually delivered to the host, not raw sensor values. */
    private var displayedModel: CarScreenRenderModel? = null
    private var pendingModel: CarScreenRenderModel? = null
    private var lastInvalidateAtMs = 0L
    private var refreshDirty = false
    private var forceRefresh = false
    private var phoneInfoCache: DeviceInfo? = null
    private var lifecycleGeneration = 0
    private var screenStarted = false
    private var telemetryReadInFlight = false
    private var forceInFlightRefresh = false
    private val iconCache = mutableMapOf<Int, CarIcon>()

    private val coalescedInvalidateRunnable = Runnable {
        if (!refreshDirty) return@Runnable
        refreshDirty = false
        val candidate = buildRenderModel()
        val shouldRefresh = CarScreenRefresh.shouldRefresh(displayedModel, candidate, forceRefresh)
        forceRefresh = false
        if (shouldRefresh) {
            pendingModel = candidate
            lastInvalidateAtMs = SystemClock.elapsedRealtime()
            invalidate()
        }
    }

    private val telemetryPollRunnable = object : Runnable {
        override fun run() {
            // Battery, RAM and connectivity have no complete event stream on all supported devices.
            requestPhoneTelemetryRefresh()
            handler.postDelayed(this, DeviceInfoUiShared.CAR_TELEMETRY_POLL_INTERVAL_MS)
        }
    }

    private val updatesObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            screenStarted = true
            lifecycleGeneration++
            VehicleInfoStore.addListener(storeListener)
            CarLocationStore.addListener(storeListener)
            handler.removeCallbacks(telemetryPollRunnable)
            handler.post(telemetryPollRunnable)
        }

        override fun onStop(owner: LifecycleOwner) {
            screenStarted = false
            lifecycleGeneration++
            VehicleInfoStore.removeListener(storeListener)
            CarLocationStore.removeListener(storeListener)
            handler.removeCallbacks(telemetryPollRunnable)
            handler.removeCallbacks(coalescedInvalidateRunnable)
            refreshDirty = false
            forceRefresh = false
            pendingModel = null
            forceInFlightRefresh = false
        }

        override fun onDestroy(owner: LifecycleOwner) {
            screenStarted = false
            lifecycleGeneration++
            telemetryExecutor.shutdownNow()
        }
    }
    private val storeListener: () -> Unit = {
        // Store callbacks can be frequent; they only mark the shared scheduler dirty.
        mainExecutor.execute(::markRefreshDirty)
    }

    init {
        lifecycle.addObserver(updatesObserver)
    }

    private fun markRefreshDirty(force: Boolean = false) {
        // A store callback may already be queued on the main executor when onStop removes
        // its listener. Do not leave refresh work behind for an inactive screen.
        if (!screenStarted) return
        refreshDirty = true
        forceRefresh = forceRefresh || force
        handler.removeCallbacks(coalescedInvalidateRunnable)
        val delay = CarScreenRefresh.delayUntilAllowed(
            nowMs = SystemClock.elapsedRealtime(),
            lastRefreshAtMs = lastInvalidateAtMs,
            minimumIntervalMs = CAR_MIN_INVALIDATE_INTERVAL_MS,
        )
        handler.postDelayed(coalescedInvalidateRunnable, delay)
    }

    override fun onGetTemplate(): Template {
        val model = pendingModel ?: buildRenderModel()
        pendingModel = null
        displayedModel = model
        lastInvalidateAtMs = SystemClock.elapsedRealtime()
        val refreshAction = Action.Builder()
            .setIcon(icon(R.drawable.ic_row_refresh))
            .setOnClickListener {
                mainExecutor.execute {
                    requestPhoneTelemetryRefresh(force = true)
                }
            }
            .build()
        val header = Header.Builder()
            .setTitle(model.title)
            .addEndHeaderAction(refreshAction)
            .build()

        return ListTemplate.Builder()
            .setHeader(header)
            .apply {
                model.sections.forEach { section ->
                    val list = ItemList.Builder().apply {
                        section.rows.forEach { row ->
                            addItem(
                                Row.Builder()
                                    .setTitle(row.title)
                                    .setImage(icon(row.iconResId), Row.IMAGE_TYPE_LARGE)
                                    .apply { row.texts.take(MAX_CAR_ROW_TEXT_LINES).forEach(::addText) }
                                    .build(),
                            )
                        }
                    }.build()
                    addSectionedList(SectionedItemList.create(list, section.title))
                }
            }
            .build()
    }

    private fun buildRenderModel(): CarScreenRenderModel {
        val ctx = AppPreferences.localizedContext(carContext)
        val info = phoneInfoCache
        val missing = "-"
        val unavailable = DeviceInfoUiShared.valueNotAvailableLabel(ctx)
        fun stable(value: String?): String = value
            ?.takeIf { it.isNotBlank() && it != unavailable && it != "—" }
            ?: missing
        fun row(title: String, text: String?, iconResId: Int) = CarScreenRowModel(
            title = title,
            texts = listOf(stable(text)),
            iconResId = iconResId,
        )
        fun row(title: String, texts: List<String>, iconResId: Int) = CarScreenRowModel(
            title = title,
            // Keep every row one line tall so values becoming available cannot move the list.
            texts = listOf(
                texts.map(::stable)
                    .filterNot { it == missing }
                    .take(MAX_CAR_ROW_TEXT_LINES)
                    .joinToString(DeviceInfoUiShared.BULLET_SEP)
                    .ifBlank { missing },
            ),
            iconResId = iconResId,
        )
        fun schemaRow(key: String): CarScreenRowModel = when (key) {
            DeviceInfoUiShared.Row.LEVEL -> row(DeviceInfoUiShared.rowTitle(ctx, key), info?.let { DeviceInfoUiShared.levelText(ctx, it, DeviceInfoUiShared.DisplaySurface.CAR) }, info?.let { DeviceInfoUiShared.levelIconRes(it.batteryLevel, it.isPowerSaveMode) } ?: R.drawable.ic_row_unknown)
            DeviceInfoUiShared.Row.PLUGGED -> row(DeviceInfoUiShared.rowTitle(ctx, key), info?.let { DeviceInfoUiShared.pluggedLabel(ctx, it.plugged) }, info?.let { DeviceInfoUiShared.pluggedIconRes(it.plugged) } ?: R.drawable.ic_row_unknown)
            DeviceInfoUiShared.Row.HEALTH -> row(DeviceInfoUiShared.rowTitle(ctx, key), info?.let { DeviceInfoUiShared.healthLabel(ctx, it.health) }, info?.let { DeviceInfoUiShared.healthIconRes(it.health) } ?: R.drawable.ic_row_unknown)
            DeviceInfoUiShared.Row.TEMPERATURE -> row(DeviceInfoUiShared.rowTitle(ctx, key), info?.let { DeviceInfoUiShared.temperatureText(ctx, it.batteryTemperatureCelsius) }, info?.let { DeviceInfoUiShared.temperatureIconRes(it.batteryTemperatureCelsius) } ?: R.drawable.ic_row_unknown)
            DeviceInfoUiShared.Row.CURRENT -> row(DeviceInfoUiShared.rowTitle(ctx, key), info?.let { DeviceInfoUiShared.currentText(it.currentNowMicroA) }, R.drawable.ic_row_current)
            DeviceInfoUiShared.Row.POWER -> row(DeviceInfoUiShared.rowTitle(ctx, key), info?.let { DeviceInfoUiShared.instantPowerText(it.batteryVoltageV, it.currentNowMicroA) }, R.drawable.ic_row_current)
            DeviceInfoUiShared.Row.RAM -> row(DeviceInfoUiShared.rowTitle(ctx, key), info?.ramSummary, R.drawable.ic_row_memory)
            else -> row(key, missing, R.drawable.ic_row_unknown)
        }

        val sections = DeviceInfoSchema.carSchema().map { (sectionKey, keys) ->
            CarScreenSectionModel(
                title = DeviceInfoUiShared.sectionTitle(ctx, sectionKey),
                rows = keys.map(::schemaRow),
            )
        }.toMutableList()

        sections += CarScreenSectionModel(
            title = DeviceInfoUiShared.sectionTitle(ctx, DeviceInfoUiShared.Section.CONNECTION),
            rows = listOf(
                row(DeviceInfoUiShared.rowTitle(ctx, DeviceInfoUiShared.Row.CONNECTION), info?.let { DeviceInfoUiShared.connectionLabel(ctx, it.connectionType) }, info?.let { DeviceInfoUiShared.connectionIconRes(it.connectionType) } ?: R.drawable.ic_row_unknown),
                row(DeviceInfoUiShared.rowTitle(ctx, DeviceInfoUiShared.Row.INTERNET_VALIDATED), info?.let { DeviceInfoUiShared.internetValidatedDisplay(ctx, it.internetValidated, it.connectionType) }, info?.let { DeviceInfoUiShared.connectionIconRes(it.connectionType) } ?: R.drawable.ic_row_unknown),
            ),
        )

        val vehicle = VehicleInfoReader.read(ctx)
        val identity = listOf(vehicle.make, vehicle.model, vehicle.year)
            .map(::stable)
            .filterNot { it == missing }
            .joinToString(DeviceInfoUiShared.BULLET_SEP)
            .ifBlank { missing }
        sections += CarScreenSectionModel(
            title = ctx.getString(R.string.section_vehicle),
            rows = listOf(
                row(ctx.getString(R.string.row_vehicle_identity), identity, R.drawable.ic_row_vehicle_car),
                row(ctx.getString(R.string.row_vehicle_speed), vehicle.speedDisplay, R.drawable.ic_row_speed),
                row(ctx.getString(R.string.row_vehicle_speed_raw), vehicle.speedRaw, R.drawable.ic_row_speed),
                row(ctx.getString(R.string.row_vehicle_fuel_level), VehicleInfoFormats.fuelDisplay(ctx, vehicle.fuelLevel, vehicle.fuelTypes).carLines(), R.drawable.ic_row_vehicle_fuel),
                row(ctx.getString(R.string.row_vehicle_ev_battery), VehicleInfoFormats.evBatteryDisplay(ctx, vehicle.evBattery, vehicle.evConnectors).carLines(), R.drawable.ic_row_vehicle_ev_battery),
                row(ctx.getString(R.string.row_vehicle_range), vehicle.rangeRemaining, R.drawable.ic_row_vehicle_route),
                row(ctx.getString(R.string.row_vehicle_odometer), vehicle.odometer, R.drawable.ic_row_vehicle_route),
            ),
        )

        val locations = CarLocationStore.snapshot()
        sections += CarScreenSectionModel(
            title = ctx.getString(R.string.section_vehicle_location),
            rows = locationRows(ctx, locations.vehicle, includeGpsDetails = false, missing = missing, unavailable = unavailable),
        )
        sections += CarScreenSectionModel(
            title = ctx.getString(R.string.section_device_location),
            rows = locationRows(ctx, locations.device, includeGpsDetails = true, missing = missing, unavailable = unavailable),
        )
        return CarScreenRenderModel(ctx.getString(R.string.app_name), sections)
    }

    private fun readPhoneTelemetry(): DeviceInfo {
        DeviceInfoProvider.get(appCtx, DeviceInfoProvider.GetOptions.CarTelemetry)
        return DeviceInfoProvider.get(appCtx, DeviceInfoProvider.GetOptions.Network)
    }

    /** Serializes provider reads off-main and rejects results from a stopped lifecycle generation. */
    private fun requestPhoneTelemetryRefresh(force: Boolean = false) {
        if (!screenStarted) return
        if (telemetryReadInFlight) {
            // A manual tap promotes the current read instead of starting a second refresh.
            forceInFlightRefresh = forceInFlightRefresh || force
            return
        }
        telemetryReadInFlight = true
        forceInFlightRefresh = force
        val generation = lifecycleGeneration
        telemetryExecutor.execute {
            val result = runCatching(::readPhoneTelemetry).getOrNull()
            mainExecutor.execute {
                telemetryReadInFlight = false
                val validResult = screenStarted && lifecycleGeneration == generation
                val applyForce = forceInFlightRefresh
                forceInFlightRefresh = false
                if (validResult) {
                    if (result != null) phoneInfoCache = result
                    markRefreshDirty(force = applyForce)
                }
                // If the screen restarted while the old generation was reading, fetch fresh data.
                if (!validResult && screenStarted) requestPhoneTelemetryRefresh(force = applyForce)
            }
        }
    }

    private fun locationRows(
        ctx: Context,
        location: Location?,
        includeGpsDetails: Boolean,
        missing: String,
        unavailable: String,
    ): List<CarScreenRowModel> {
        fun stable(value: String?): String = value
            ?.takeIf { it.isNotBlank() && it != unavailable && it != "—" }
            ?: missing
        fun row(titleRes: Int, text: String?, iconRes: Int) =
            CarScreenRowModel(ctx.getString(titleRes), listOf(stable(text)), iconRes)
        return buildList {
            add(row(R.string.row_location_coordinates, LocationInfoFormats.coordinatesDisplay(ctx, location?.latitude, location?.longitude), R.drawable.ic_row_location))
            add(row(R.string.row_location_accuracy, LocationInfoFormats.accuracyDisplay(ctx, location?.accuracy?.takeIf { location.hasAccuracy() }), R.drawable.ic_row_location))
            add(row(R.string.row_location_altitude, MeasurementUnits.altitude(ctx, location?.altitude?.takeIf { location.hasAltitude() }), R.drawable.ic_row_location_altitude))
            add(row(R.string.row_location_speed, MeasurementUnits.speed(ctx, location?.speed?.takeIf { location.hasSpeed() }), R.drawable.ic_row_speed))
            add(row(R.string.row_location_bearing, LocationInfoFormats.bearingDisplay(ctx, location?.bearing?.takeIf { location.hasBearing() }), R.drawable.ic_row_location_bearing))
            if (includeGpsDetails) {
                add(row(R.string.row_location_provider, LocationInfoFormats.providerLabel(ctx, location?.provider), R.drawable.ic_row_location))
                add(row(R.string.row_location_gps_vertical_accuracy, LocationInfoFormats.accuracyDisplay(ctx, location?.verticalAccuracyMeters?.takeIf { location.hasVerticalAccuracy() }), R.drawable.ic_row_location))
                val speedAccuracy = location?.speedAccuracyMetersPerSecond?.takeIf { location.hasSpeedAccuracy() }?.let { ctx.getString(R.string.location_speed_accuracy_format, MeasurementUnits.speed(ctx, it)) }
                add(row(R.string.row_location_gps_speed_accuracy, speedAccuracy, R.drawable.ic_row_speed))
                val bearingAccuracy = location?.bearingAccuracyDegrees?.takeIf { location.hasBearingAccuracy() }?.let { ctx.getString(R.string.location_bearing_accuracy_format, it) }
                add(row(R.string.row_location_gps_bearing_accuracy, bearingAccuracy, R.drawable.ic_row_location_bearing))
                val gnss = GnssInfoStore.snapshot()
                val satellites = if (gnss.satellitesUsedInFix != null && gnss.satellitesVisible != null) ctx.getString(R.string.location_gnss_satellites_format, gnss.satellitesUsedInFix, gnss.satellitesVisible) else null
                add(row(R.string.row_location_gnss_satellites, satellites, R.drawable.ic_row_location))
                add(row(R.string.row_location_gnss_frequency_bands, gnss.frequencyBands.takeIf { it.isNotEmpty() }?.let(DeviceInfoUiShared::joinList), R.drawable.ic_row_location))
            }
        }
    }

    private fun icon(resId: Int): CarIcon = iconCache.getOrPut(resId) {
        val white = Color.WHITE
        CarIcon.Builder(IconCompat.createWithResource(carContext, resId))
            .setTint(CarColor.createCustom(white, white))
            .build()
    }

    companion object {
        private const val CAR_MIN_INVALIDATE_INTERVAL_MS = 2_000L
        private const val MAX_CAR_ROW_TEXT_LINES = 2
    }
}
