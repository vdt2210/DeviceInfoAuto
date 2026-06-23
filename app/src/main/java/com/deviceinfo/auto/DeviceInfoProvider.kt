package com.deviceinfo.auto

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Display.HdrCapabilities
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import android.os.storage.StorageManager
import android.os.storage.StorageVolume

/** Mounted non-primary storage volume (SD, USB, …) for a dynamic list row. */
data class ExternalStorageVolumeInfo(
    val rowKey: String,
    val title: String,
    val summary: String,
)

data class DeviceInfo(
    val batteryLevel: Int,
    val batteryTemperatureCelsius: Float,
    val batteryVoltageV: Float,
    val chargingStatus: String,
    val plugged: String,
    val health: String,
    val technology: String,
    val currentNowMicroA: Long,
    val chargeCounterMicroAh: Long,
    val cycleCount: Int?,
    /** Design capacity (µAh) from [BatteryManager.BATTERY_PROPERTY_CHARGE_FULL_DESIGN], or null. */
    val batteryDesignCapacityMicroAh: Long?,
    /** Ms until full when charging; null if unknown. */
    val chargeTimeRemainingMs: Long?,
    val displayResolution: String,
    /** Diagonal size in inches; 0 if unknown. */
    val displayDiagonalInches: Float,
    val displayRefreshRateHz: Float,
    /** Unique supported refresh rates from [Display.getSupportedModes], one per line on phone. */
    val displayRefreshModesSummary: String,
    /** Localized HDR capability line (e.g. No / HDR10, HLG). */
    val displayHdrSummary: String,
    val screenDensityDpi: Int,
    val connectionType: String,
    val bluetoothOn: Boolean?,
    val cellularType: String,
    val simCarrier: String,
    val simState: String,
    val simCountryIso: String,
    val simTypeSummary: String,
    /**
     * True when the device has telephony and [READ_PHONE_STATE] is granted so cellular + SIM rows
     * are shown (values may be placeholders such as "—" when no subscription).
     */
    val simDetailRowsVisible: Boolean,
    val isVpn: Boolean,
    val isMeteredConnection: Boolean,
    val isRoaming: Boolean,
    val downstreamKbps: Int,
    val upstreamKbps: Int,
    /** Supported Wi‑Fi bands on this device, one per line on phone. */
    val wifiSupportedBandsSummary: String,
    val wifiLinkSpeedSummary: String,
    val wifiSignalSummary: String,
    val cellularSignalSummary: String,
    /** `null` when [ACCESS_NETWORK_STATE] is not granted. */
    val internetValidated: Boolean?,
    val captivePortal: Boolean?,
    val multiNetworkSummary: String,
    val wifiNetworkRowsVisible: Boolean,
    val storageSummary: String,
    val externalStorageVolumes: List<ExternalStorageVolumeInfo>,
    val ramSummary: String,
    val deviceName: String,
    val deviceCodename: String,
    val cpuAbi: String,
    val cpuInfo: String,
    val cpuFreq: String,
    val cpuCurrentFreq: String,
    val gpuRenderer: String,
    val gpuGlVersion: String,
    val bootloader: String,
    val kernelVersion: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val securityPatch: String,
    val buildId: String,
    val isPowerSaveMode: Boolean,
    val cameraSummary: String,
    val nfcSummary: String,
    val usbSummary: String,
    val audioSummary: String,
    val biometricSummary: String,
    val thermalSummary: String,
)

object DeviceInfoProvider {

    /** Which slices of [DeviceInfo] to re-read; omitted slices are taken from [cachedSnapshot]. */
    data class GetOptions(
        val includeBattery: Boolean = true,
        val includeNetwork: Boolean = true,
        val includeCellularSignal: Boolean = false,
        val includeDisplay: Boolean = true,
        val includeMemory: Boolean = true,
        val includeDeviceStatic: Boolean = true,
        val refreshThermal: Boolean = true,
        val refreshCpuCurrentFreq: Boolean = true,
        val refreshBatteryExtras: Boolean = true,
    ) {
        companion object {
            val Full = GetOptions()

            /** Phone: sticky battery + power saver (broadcast-driven). */
            val Battery = GetOptions(
                includeBattery = true,
                includeNetwork = false,
                includeDisplay = false,
                includeMemory = false,
                includeDeviceStatic = false,
                refreshThermal = false,
                refreshCpuCurrentFreq = false,
                refreshBatteryExtras = false,
            )

            /** Phone: connectivity + Wi‑Fi + Bluetooth (callback-driven). */
            val Network = GetOptions(
                includeBattery = false,
                includeNetwork = true,
                includeDisplay = false,
                includeMemory = false,
                includeDeviceStatic = false,
                refreshThermal = false,
                refreshCpuCurrentFreq = false,
                refreshBatteryExtras = false,
            )

            /** Phone: CPU freq + thermal (thermal status listener). */
            val Processor = GetOptions(
                includeBattery = false,
                includeNetwork = false,
                includeCellularSignal = false,
                includeDisplay = false,
                includeMemory = false,
                includeDeviceStatic = false,
                refreshThermal = true,
                refreshCpuCurrentFreq = true,
                refreshBatteryExtras = false,
            )

            /** Phone: periodic poll — RAM, storage, CPU/thermal, cellular signal. */
            val PhonePoll = GetOptions(
                includeBattery = false,
                includeNetwork = false,
                includeCellularSignal = true,
                includeDisplay = false,
                includeMemory = true,
                includeDeviceStatic = false,
                refreshThermal = true,
                refreshCpuCurrentFreq = true,
                refreshBatteryExtras = false,
            )

            /** Phone: display metrics + rotation (display listener / config). */
            val Display = GetOptions(
                includeBattery = false,
                includeNetwork = false,
                includeDisplay = true,
                includeMemory = false,
                includeDeviceStatic = false,
                refreshThermal = false,
                refreshCpuCurrentFreq = false,
                refreshBatteryExtras = false,
            )

            /** Android Auto: battery + RAM poll. */
            val CarTelemetry = GetOptions(
                includeBattery = true,
                includeNetwork = false,
                includeDisplay = false,
                includeMemory = true,
                includeDeviceStatic = false,
                refreshThermal = false,
                refreshCpuCurrentFreq = false,
                refreshBatteryExtras = false,
            )
        }
    }

    private data class CachedBatteryExtras(
        val cycleCount: Int?,
        val designCapacityMicroAh: Long?,
    )

    /** EGL_OPENGL_ES3_BIT — not exposed on all Android EGL14 stubs; value per Khronos EGL 1.5. */
    private const val EGL_OPENGL_ES3_BIT = 0x00000040

    private const val THERMAL_CACHE_TTL_MS = 5_000L

    /**
     * Legacy HAL property ids (pre–API reshuffle). On newer AOSP, id 5 is
     * [ENERGY_COUNTER], not charge full — always prefer reflection by field name first.
     */
    private const val LEGACY_BATTERY_PROPERTY_CHARGE_FULL = 5
    private const val LEGACY_BATTERY_PROPERTY_CHARGE_FULL_DESIGN = 6

    @Volatile
    private var cachedStaticHardware: HardwareInfoReader.Summaries? = null

    @Volatile
    private var cachedStaticHardwareLocale: String? = null

    @Volatile private var cachedGpuRenderer: String? = null
    @Volatile private var cachedGpuGlVersion: String? = null
    @Volatile private var cachedCpuInfo: String? = null
    @Volatile private var cachedCpuFreq: String? = null
    @Volatile private var cachedKernelVersion: String? = null
    @Volatile private var cachedAndroidVersion: String? = null
    @Volatile private var cachedSecurityPatch: String? = null
    @Volatile private var cachedBootloader: String? = null
    @Volatile private var cachedBuildId: String? = null
    @Volatile private var cachedDeviceCodename: String? = null
    @Volatile private var cachedCpuAbi: String? = null

    @Volatile private var cachedLiveThermal: String? = null
    @Volatile private var cachedLiveThermalAtMs: Long = 0L

    @Volatile private var cachedCpuCurrentFreqLive: String? = null

    @Volatile private var cachedBatteryExtras: CachedBatteryExtras? = null

    @Volatile private var cachedSnapshot: DeviceInfo? = null

    /** Call after install/update so camera/hardware rows refresh on next [get]. */
    fun invalidateHardwareCache() {
        cachedStaticHardware = null
        cachedStaticHardwareLocale = null
        cachedGpuRenderer = null
        cachedGpuGlVersion = null
        cachedCpuInfo = null
        cachedCpuFreq = null
        cachedKernelVersion = null
        cachedAndroidVersion = null
        cachedSecurityPatch = null
        cachedBootloader = null
        cachedBuildId = null
        cachedDeviceCodename = null
        cachedCpuAbi = null
        cachedLiveThermal = null
        cachedLiveThermalAtMs = 0L
        cachedCpuCurrentFreqLive = null
        cachedBatteryExtras = null
        cachedSnapshot = null
    }

    private fun cache(info: DeviceInfo): DeviceInfo {
        cachedSnapshot = info
        return info
    }

    private fun cachedGpuRenderer(): String =
        cachedGpuRenderer ?: readGpuRenderer().also { cachedGpuRenderer = it }

    private fun cachedGpuGlVersion(context: Context): String =
        cachedGpuGlVersion ?: getGlEsVersion(context).also { cachedGpuGlVersion = it }

    private fun cachedCpuInfo(): String =
        cachedCpuInfo ?: readCpuInfo().also { cachedCpuInfo = it }

    private fun cachedCpuFreq(): String =
        cachedCpuFreq ?: readCpuFreq().also { cachedCpuFreq = it }

    private fun cachedKernelVersion(): String =
        cachedKernelVersion ?: readKernelVersion().also { cachedKernelVersion = it }

    private fun cachedAndroidVersion(): String =
        cachedAndroidVersion ?: "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            .also { cachedAndroidVersion = it }

    private fun cachedSecurityPatch(): String =
        cachedSecurityPatch ?: Build.VERSION.SECURITY_PATCH.ifEmpty { "Unknown" }
            .also { cachedSecurityPatch = it }

    private fun cachedBootloader(): String =
        cachedBootloader ?: Build.BOOTLOADER.ifEmpty { "—" }.also { cachedBootloader = it }

    private fun cachedBuildId(): String =
        cachedBuildId ?: Build.DISPLAY.ifEmpty { Build.ID }.ifEmpty { "Unknown" }
            .also { cachedBuildId = it }

    private fun cachedDeviceCodename(): String =
        cachedDeviceCodename ?: Build.DEVICE.ifEmpty { "—" }.also { cachedDeviceCodename = it }

    private fun cachedCpuAbi(): String =
        cachedCpuAbi ?: (Build.SUPPORTED_ABIS?.joinToString(", ")?.ifEmpty { "—" } ?: "—")
            .also { cachedCpuAbi = it }

    private fun resolveThermalSummary(
        context: Context,
        staticFallback: String,
        refresh: Boolean,
    ): String {
        if (!refresh) {
            cachedLiveThermal?.let { return it }
            return staticFallback
        }
        val now = SystemClock.elapsedRealtime()
        cachedLiveThermal?.let { cached ->
            if (now - cachedLiveThermalAtMs < THERMAL_CACHE_TTL_MS) return cached
        }
        return HardwareInfoReader.readThermalSummary(context).also {
            cachedLiveThermal = it
            cachedLiveThermalAtMs = now
        }
    }

    private fun resolveCpuCurrentFreq(refresh: Boolean): String {
        if (!refresh) return cachedCpuCurrentFreqLive ?: "—"
        return readCpuCurrentFreq().also { cachedCpuCurrentFreqLive = it }
    }

    private fun resolveBatteryExtras(
        context: Context,
        bm: BatteryManager?,
        batteryIntent: Intent,
        refresh: Boolean,
    ): Pair<Int?, Long?> {
        if (refresh) {
            val cycleCount = readBatteryCycleCount(batteryIntent)
            val design = readBatteryDesignCapacityMicroAh(context, bm, batteryIntent)
            cachedBatteryExtras = CachedBatteryExtras(cycleCount, design)
            return cycleCount to design
        }
        cachedBatteryExtras?.let {
            return it.cycleCount to it.designCapacityMicroAh
        }
        val cycleCount = readBatteryCycleCount(batteryIntent)
        val design = readBatteryDesignCapacityMicroAh(context, bm, batteryIntent)
        cachedBatteryExtras = CachedBatteryExtras(cycleCount, design)
        return cycleCount to design
    }

    /** [android.telephony.ServiceState] NR state values (API 29+). */
    private const val NR_STATE_NONE = 0
    private const val NR_STATE_NOT_RESTRICTED = 2
    private const val NR_STATE_CONNECTED = 3

    /** [android.telephony.TelephonyDisplayInfo] override types (API 31+). */
    private const val OVERRIDE_NETWORK_TYPE_NR_NSA = 3
    private const val OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE = 4
    private const val OVERRIDE_NETWORK_TYPE_NR_ADVANCED = 5

    private fun reflectInvokeInt(target: Any?, methodName: String): Int? =
        runCatching {
            val m = target?.javaClass?.getMethod(methodName) ?: return null
            when (val v = m.invoke(target)) {
                is Int -> v
                is Integer -> v.toInt()
                is Number -> v.toInt()
                else -> null
            }
        }.getOrNull()

    private fun readTelephonyDisplayInfo(tm: TelephonyManager): Any? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return try {
            val m = TelephonyManager::class.java.getMethod("getTelephonyDisplayInfo")
            m.invoke(tm)
        } catch (_: SecurityException) {
            null
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    private fun readDisplayOverrideNetworkType(tm: TelephonyManager): Int? {
        val tdi = readTelephonyDisplayInfo(tm) ?: return null
        return reflectInvokeInt(tdi, "getOverrideNetworkType")
    }

    /**
     * [TelephonyDisplayInfo.getNetworkType] (API 31+): RAT shown in status bar / quick settings,
     * often **NR** while [TelephonyManager.getDataNetworkType] still reports LTE anchor (NSA).
     */
    private fun readTelephonyDisplayNetworkType(tm: TelephonyManager): Int? {
        val tdi = readTelephonyDisplayInfo(tm) ?: return null
        return reflectInvokeInt(tdi, "getNetworkType")
    }

    /**
     * 5G NR indicators on the default network when it uses [NetworkCapabilities.TRANSPORT_CELLULAR].
     * Capability bit positions changed across releases; resolve by name via reflection when present.
     */
    private fun cellularNetworkDeclaresNr(caps: NetworkCapabilities?): Boolean {
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return false
        for (fieldName in listOf("NET_CAPABILITY_NR_NSA", "NET_CAPABILITY_NR_MMWAVE")) {
            val bit = runCatching {
                NetworkCapabilities::class.java.getField(fieldName).getInt(null)
            }.getOrNull() ?: continue
            if (caps.hasCapability(bit)) return true
        }
        return false
    }

    /**
     * Same idea as [readDisplayOverrideNetworkType] for [ServiceState.getNrState] on API 30+.
     */
    private fun readNrStateReflect(tm: TelephonyManager): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return NR_STATE_NONE
        return runCatching {
            val mSs = TelephonyManager::class.java.getMethod("getServiceState")
            val ss = mSs.invoke(tm) ?: return NR_STATE_NONE
            reflectInvokeInt(ss, "getNrState") ?: NR_STATE_NONE
        }.getOrDefault(NR_STATE_NONE)
    }

    private fun bluetoothAdapterOrNull(context: Context): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter ?: run {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }
    }

    private fun canReadBluetoothState(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun resolveUserDeviceName(context: Context): String {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val fromGlobal = Settings.Global.getString(
                    context.contentResolver,
                    Settings.Global.DEVICE_NAME
                )?.trim()?.takeIf { it.isNotEmpty() }
                if (fromGlobal != null) return fromGlobal
            }
        } catch (_: Exception) { }
        try {
            val fromSystem = Settings.System.getString(context.contentResolver, "device_name")
                ?.trim()?.takeIf { it.isNotEmpty() }
            if (fromSystem != null) return fromSystem
        } catch (_: Exception) { }
        try {
            if (canReadBluetoothState(context)) {
                val bt = bluetoothAdapterOrNull(context)?.name?.trim()?.takeIf { it.isNotEmpty() }
                if (bt != null) return bt
            }
        } catch (_: SecurityException) { }
        val model = Build.MODEL.trim().takeIf { it.isNotEmpty() }
        return model ?: "—"
    }

    fun get(context: Context, options: GetOptions = GetOptions.Full): DeviceInfo =
        when (options) {
            GetOptions.Full -> buildFull(context)
            GetOptions.Battery -> refreshBattery(context)
            GetOptions.Network -> refreshNetwork(context)
            GetOptions.PhonePoll -> refreshPhonePoll(context)
            GetOptions.Processor -> refreshProcessor(context)
            GetOptions.Display -> refreshDisplay(context)
            GetOptions.CarTelemetry -> refreshCarTelemetry(context)
            else -> buildFull(context)
        }

    private fun buildFull(context: Context): DeviceInfo {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ) ?: return cache(createEmpty())

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val batteryPct = if (scale > 0) (level * 100 / scale) else 0

        val tempRaw = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempCelsius = tempRaw / 10f

        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val chargingStatus = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }

        val health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
            else -> "Unknown"
        }

        val voltageMv = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageV = voltageMv / 1000f

        val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val pluggedStr = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "None"
        }

        val technology = batteryIntent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.ifEmpty { null } ?: "Unknown"

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val currentNowMicroA = if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        } else 0L
        val chargeCounterMicroAh = if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        } else 0L

        val (cycleCount, batteryDesignCapacityMicroAh) =
            resolveBatteryExtras(context, bm, batteryIntent, refresh = true)
        val chargeFullMicroAh = bm?.let { readBatteryChargeFullMicroAh(it, batteryIntent) }
        val chargeTimeRemainingMs = readChargeTimeRemainingMs(
            batteryIntent = batteryIntent,
            bm = bm,
            status = status,
            plugged = plugged,
            level = level,
            scale = scale,
            chargeFullMicroAh = chargeFullMicroAh,
            designMicroAh = batteryDesignCapacityMicroAh,
            currentNowMicroA = currentNowMicroA,
        )

        // In Android Auto/Car host, app context may not be associated with a visual display.
        // Accessing context.display there can throw UnsupportedOperationException.
        val displayObj: Display? = try {
            val dm = context.getSystemService(DisplayManager::class.java)
            dm?.getDisplay(Display.DEFAULT_DISPLAY) ?: context.display
        } catch (_: Exception) {
            null
        }
        val metrics = DisplayMetrics()
        val displayRefreshRateHz = try {
            if (displayObj != null) {
                displayObj.getRealMetrics(metrics)
                displayObj.refreshRate
            } else {
                metrics.setTo(context.resources.displayMetrics)
                0f
            }
        } catch (_: Exception) {
            metrics.setTo(context.resources.displayMetrics)
            0f
        }
        val displayResolution = "${metrics.widthPixels} x ${metrics.heightPixels}"
        val displayRefreshModesSummary = formatSupportedRefreshRates(displayObj)
        val displayHdrSummary = formatHdrSummary(context, displayObj)
        val screenDensityDpi = metrics.densityDpi
        val displayDiagonalInches = HardwareInfoReader.diagonalInchesFromMetrics(
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.xdpi,
            metrics.ydpi,
            metrics.densityDpi,
        )

        val hasNetworkPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

        var connectionType = "Unknown"
        var isVpn = false
        var isMeteredConnection = false
        var isRoaming = false
        var downstreamKbps = 0
        var upstreamKbps = 0
        var activeNetworkCaps: NetworkCapabilities? = null
        var cellularNetworkCaps: NetworkCapabilities? = null

        val wifiNetworkRowsVisible =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
        var wifiSupportedBandsSummary = "—"
        var wifiLinkSpeedSummary = "—"
        var wifiSignalSummary = "—"
        var internetValidated: Boolean? = null
        var captivePortal: Boolean? = null
        var multiNetworkSummary = "—"

        val hasWifiStatePermission = hasWifiStatePermission(context)
        if (wifiNetworkRowsVisible && hasWifiStatePermission) {
            wifiSupportedBandsSummary = readWifiSupportedBands(context)
        }

        if (hasNetworkPermission) {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivity != null) {
                cellularNetworkCaps = findCellularNetworkCapabilities(connectivity)
                val active = connectivity.activeNetwork
                val caps = active?.let { connectivity.getNetworkCapabilities(it) }
                if (caps != null) {
                    activeNetworkCaps = caps
                    connectionType = when {
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                        else -> "Online"
                    }
                    isVpn = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    isMeteredConnection = connectivity.isActiveNetworkMetered
                    isRoaming = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                    downstreamKbps = caps.linkDownstreamBandwidthKbps
                    upstreamKbps = caps.linkUpstreamBandwidthKbps
                    internetValidated =
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    captivePortal =
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
                    multiNetworkSummary = readMultiNetworkSummary(connectivity)
                    if (wifiNetworkRowsVisible && hasWifiStatePermission && connectionType == "Wi-Fi") {
                        val wifiLive = readWifiLinkAndSignal(context, caps)
                        wifiLinkSpeedSummary = wifiLive.first
                        wifiSignalSummary = wifiLive.second
                    }
                } else {
                    connectionType = "Offline"
                    internetValidated = false
                    captivePortal = false
                    multiNetworkSummary = DeviceInfoUiShared.MultiNetworkToken.NONE
                }
            }
        }

        val bluetoothOn = try {
            if (canReadBluetoothState(context)) bluetoothAdapterOrNull(context)?.isEnabled else null
        } catch (_: SecurityException) {
            null
        }

        var cellularType = "—"
        var simCarrier = "—"
        var simState = "—"
        var simCountryIso = "—"
        var simTypeSummary = "—"
        var simDetailRowsVisible = false
        var cellularSignalSummary = "—"
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                    if (tm != null) {
                        simDetailRowsVisible = true
                        val sm = context.getSystemService(SubscriptionManager::class.java)
                        val simUi = buildSimSlotSummaries(tm, sm)
                        val dataSubId = try {
                            SubscriptionManager.getDefaultDataSubscriptionId()
                        } catch (_: Exception) {
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID
                        }
                        val dataTm = if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                            tm.createForSubscriptionId(dataSubId)
                        } else {
                            tm
                        }
                        if (hasInsertedSim(tm, sm)) {
                            cellularSignalSummary = readCellularSignalSummaries(tm, sm)
                        }
                        cellularType = if (simUi.hasActiveSubscriptions) {
                            resolveCellularNetworkLabel(
                                dataTm,
                                cellularNetworkCaps ?: activeNetworkCaps,
                            )
                        } else {
                            "—"
                        }
                        simCarrier = simUi.carrier
                        simState = simUi.state
                        simCountryIso = simUi.country
                        simTypeSummary = simUi.type
                    }
                }
            } catch (_: SecurityException) { }
        }

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0)
        val freeGB = freeBytes / (1024.0 * 1024.0 * 1024.0)

        val memInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(memInfo)
        val ramTotalGB = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val ramAvailGB = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)

        val storageSummary = formatFreeOf(context, freeGB, totalGB)
        val externalStorageVolumes = readMountedExternalStorageVolumes(context)
        val ramSummary = formatFreeOf(context, ramAvailGB, ramTotalGB)

        val deviceName = resolveUserDeviceName(context)
        val deviceCodename = cachedDeviceCodename()
        val cpuAbi = cachedCpuAbi()
        val bootloader = cachedBootloader()
        val cpuInfo = cachedCpuInfo()
        val cpuFreq = cachedCpuFreq()
        val cpuCurrentFreq = resolveCpuCurrentFreq(refresh = true)
        val gpuRenderer = cachedGpuRenderer()
        val gpuGlVersion = cachedGpuGlVersion(context)
        val kernelVersion = cachedKernelVersion()
        val manufacturer = Build.MANUFACTURER.ifEmpty { "Unknown" }
        val model = Build.MODEL.ifEmpty { "Unknown" }
        val androidVersion = cachedAndroidVersion()
        val securityPatch = cachedSecurityPatch()
        val buildId = cachedBuildId()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaveMode = powerManager?.isPowerSaveMode == true

        val localeKey = AppPreferences(context.applicationContext).getLanguageTag()
        val staticHw = if (cachedStaticHardware != null && cachedStaticHardwareLocale == localeKey) {
            cachedStaticHardware!!
        } else {
            HardwareInfoReader.readSummaries(context).also {
                cachedStaticHardware = it
                cachedStaticHardwareLocale = localeKey
            }
        }
        val thermalSummary = resolveThermalSummary(context, staticHw.thermal, refresh = true)
        val hw = staticHw.copy(thermal = thermalSummary)

        return cache(DeviceInfo(
            batteryLevel = batteryPct,
            batteryTemperatureCelsius = tempCelsius,
            batteryVoltageV = voltageV,
            chargingStatus = chargingStatus,
            plugged = pluggedStr,
            health = healthStr,
            technology = technology,
            currentNowMicroA = currentNowMicroA,
            chargeCounterMicroAh = chargeCounterMicroAh,
            cycleCount = cycleCount,
            batteryDesignCapacityMicroAh = batteryDesignCapacityMicroAh,
            chargeTimeRemainingMs = chargeTimeRemainingMs,
            displayResolution = displayResolution,
            displayDiagonalInches = displayDiagonalInches,
            displayRefreshRateHz = displayRefreshRateHz,
            displayRefreshModesSummary = displayRefreshModesSummary,
            displayHdrSummary = displayHdrSummary,
            screenDensityDpi = screenDensityDpi,
            connectionType = connectionType,
            bluetoothOn = bluetoothOn,
            cellularType = cellularType,
            simCarrier = simCarrier,
            simState = simState,
            simCountryIso = simCountryIso,
            simTypeSummary = simTypeSummary,
            simDetailRowsVisible = simDetailRowsVisible,
            isVpn = isVpn,
            isMeteredConnection = isMeteredConnection,
            isRoaming = isRoaming,
            downstreamKbps = downstreamKbps,
            upstreamKbps = upstreamKbps,
            wifiSupportedBandsSummary = wifiSupportedBandsSummary,
            wifiLinkSpeedSummary = wifiLinkSpeedSummary,
            wifiSignalSummary = wifiSignalSummary,
            cellularSignalSummary = cellularSignalSummary,
            internetValidated = internetValidated,
            captivePortal = captivePortal,
            multiNetworkSummary = multiNetworkSummary,
            wifiNetworkRowsVisible = wifiNetworkRowsVisible,
            storageSummary = storageSummary,
            externalStorageVolumes = externalStorageVolumes,
            ramSummary = ramSummary,
            deviceName = deviceName,
            deviceCodename = deviceCodename,
            cpuAbi = cpuAbi,
            cpuInfo = cpuInfo,
            cpuFreq = cpuFreq,
            cpuCurrentFreq = cpuCurrentFreq,
            gpuRenderer = gpuRenderer,
            gpuGlVersion = gpuGlVersion,
            bootloader = bootloader,
            kernelVersion = kernelVersion,
            manufacturer = manufacturer,
            model = model,
            androidVersion = androidVersion,
            securityPatch = securityPatch,
            buildId = buildId,
            isPowerSaveMode = isPowerSaveMode,
            cameraSummary = hw.camera,
            nfcSummary = hw.nfc,
            usbSummary = hw.usb,
            audioSummary = hw.audio,
            biometricSummary = hw.biometric,
            thermalSummary = hw.thermal,
        ))
    }

    private fun refreshBattery(context: Context): DeviceInfo {
        val base = cachedSnapshot ?: return buildFull(context)
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ) ?: return base
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val batteryPct = if (scale > 0) (level * 100 / scale) else 0
        val tempRaw = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempCelsius = tempRaw / 10f
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val chargingStatus = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }
        val health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
            else -> "Unknown"
        }
        val voltageMv = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageV = voltageMv / 1000f
        val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val pluggedStr = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "None"
        }
        val technology = batteryIntent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.ifEmpty { null } ?: "Unknown"
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val currentNowMicroA = if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        } else {
            0L
        }
        val chargeCounterMicroAh = if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        } else {
            0L
        }
        val chargeTimeRemainingMs = readChargeTimeRemainingMs(
            batteryIntent = batteryIntent,
            bm = bm,
            status = status,
            plugged = plugged,
            level = level,
            scale = scale,
            chargeFullMicroAh = bm?.let { readBatteryChargeFullMicroAh(it, batteryIntent) },
            designMicroAh = base.batteryDesignCapacityMicroAh,
            currentNowMicroA = currentNowMicroA,
        )
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaveMode = powerManager?.isPowerSaveMode == true
        return cache(
            base.copy(
                batteryLevel = batteryPct,
                batteryTemperatureCelsius = tempCelsius,
                batteryVoltageV = voltageV,
                chargingStatus = chargingStatus,
                plugged = pluggedStr,
                health = healthStr,
                technology = technology,
                currentNowMicroA = currentNowMicroA,
                chargeCounterMicroAh = chargeCounterMicroAh,
                chargeTimeRemainingMs = chargeTimeRemainingMs,
                isPowerSaveMode = isPowerSaveMode,
            ),
        )
    }

    private fun refreshNetwork(context: Context): DeviceInfo {
        val base = cachedSnapshot ?: return buildFull(context)
        val slice = readNetworkSlice(context)
        return cache(
            base.copy(
                connectionType = slice.connectionType,
                bluetoothOn = slice.bluetoothOn,
                cellularType = slice.cellularType,
                simCarrier = slice.simCarrier,
                simState = slice.simState,
                simCountryIso = slice.simCountryIso,
                simTypeSummary = slice.simTypeSummary,
                simDetailRowsVisible = slice.simDetailRowsVisible,
                isVpn = slice.isVpn,
                isMeteredConnection = slice.isMeteredConnection,
                isRoaming = slice.isRoaming,
                downstreamKbps = slice.downstreamKbps,
                upstreamKbps = slice.upstreamKbps,
                wifiSupportedBandsSummary = slice.wifiSupportedBandsSummary,
                wifiLinkSpeedSummary = slice.wifiLinkSpeedSummary,
                wifiSignalSummary = slice.wifiSignalSummary,
                cellularSignalSummary = slice.cellularSignalSummary,
                internetValidated = slice.internetValidated,
                captivePortal = slice.captivePortal,
                multiNetworkSummary = slice.multiNetworkSummary,
                wifiNetworkRowsVisible = slice.wifiNetworkRowsVisible,
            ),
        )
    }

    private fun refreshPhonePoll(context: Context): DeviceInfo {
        val base = cachedSnapshot ?: return buildFull(context)
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalGB = stat.totalBytes / (1024.0 * 1024.0 * 1024.0)
        val freeGB = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
        val memInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(memInfo)
        val ramTotalGB = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val ramAvailGB = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        val storageSummary = formatFreeOf(context, freeGB, totalGB)
        val externalStorageVolumes = readMountedExternalStorageVolumes(context)
        val ramSummary = formatFreeOf(context, ramAvailGB, ramTotalGB)
        val cpuCurrentFreq = resolveCpuCurrentFreq(refresh = true)
        val thermalSummary = resolveThermalSummary(context, base.thermalSummary, refresh = true)
        val cellular = readCellularSignalSlice(context)
        return cache(
            base.copy(
                storageSummary = storageSummary,
                externalStorageVolumes = externalStorageVolumes,
                ramSummary = ramSummary,
                cpuCurrentFreq = cpuCurrentFreq,
                thermalSummary = thermalSummary,
                cellularType = cellular.cellularType,
                cellularSignalSummary = cellular.cellularSignalSummary,
            ),
        )
    }

    private fun refreshProcessor(context: Context): DeviceInfo {
        val base = cachedSnapshot ?: return buildFull(context)
        val cpuCurrentFreq = resolveCpuCurrentFreq(refresh = true)
        val thermalSummary = resolveThermalSummary(context, base.thermalSummary, refresh = true)
        return cache(
            base.copy(
                cpuCurrentFreq = cpuCurrentFreq,
                thermalSummary = thermalSummary,
            ),
        )
    }

    private fun refreshDisplay(context: Context): DeviceInfo {
        val base = cachedSnapshot ?: return buildFull(context)
        val slice = readDisplaySlice(context)
        return cache(
            base.copy(
                displayResolution = slice.displayResolution,
                displayDiagonalInches = slice.displayDiagonalInches,
                displayRefreshRateHz = slice.displayRefreshRateHz,
                displayRefreshModesSummary = slice.displayRefreshModesSummary,
                displayHdrSummary = slice.displayHdrSummary,
                screenDensityDpi = slice.screenDensityDpi,
            ),
        )
    }

    private fun refreshCarTelemetry(context: Context): DeviceInfo {
        val battery = refreshBattery(context)
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalGB = stat.totalBytes / (1024.0 * 1024.0 * 1024.0)
        val freeGB = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
        val memInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(memInfo)
        val ramTotalGB = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val ramAvailGB = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        return cache(
            battery.copy(
                ramSummary = formatFreeOf(context, ramAvailGB, ramTotalGB),
            ),
        )
    }

    private data class NetworkSlice(
        val connectionType: String,
        val bluetoothOn: Boolean?,
        val cellularType: String,
        val simCarrier: String,
        val simState: String,
        val simCountryIso: String,
        val simTypeSummary: String,
        val simDetailRowsVisible: Boolean,
        val isVpn: Boolean,
        val isMeteredConnection: Boolean,
        val isRoaming: Boolean,
        val downstreamKbps: Int,
        val upstreamKbps: Int,
        val wifiSupportedBandsSummary: String,
        val wifiLinkSpeedSummary: String,
        val wifiSignalSummary: String,
        val cellularSignalSummary: String,
        val internetValidated: Boolean?,
        val captivePortal: Boolean?,
        val multiNetworkSummary: String,
        val wifiNetworkRowsVisible: Boolean,
    )

    private data class DisplaySlice(
        val displayResolution: String,
        val displayDiagonalInches: Float,
        val displayRefreshRateHz: Float,
        val displayRefreshModesSummary: String,
        val displayHdrSummary: String,
        val screenDensityDpi: Int,
    )

    private data class CellularSignalSlice(
        val cellularType: String,
        val cellularSignalSummary: String,
    )

    private fun readNetworkSlice(context: Context): NetworkSlice {
        val hasNetworkPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) ==
                PackageManager.PERMISSION_GRANTED

        var connectionType = "Unknown"
        var isVpn = false
        var isMeteredConnection = false
        var isRoaming = false
        var downstreamKbps = 0
        var upstreamKbps = 0
        var activeNetworkCaps: NetworkCapabilities? = null
        var cellularNetworkCaps: NetworkCapabilities? = null

        val wifiNetworkRowsVisible =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
        var wifiSupportedBandsSummary = "—"
        var wifiLinkSpeedSummary = "—"
        var wifiSignalSummary = "—"
        var internetValidated: Boolean? = null
        var captivePortal: Boolean? = null
        var multiNetworkSummary = "—"

        val hasWifiStatePermission = hasWifiStatePermission(context)
        if (wifiNetworkRowsVisible && hasWifiStatePermission) {
            wifiSupportedBandsSummary = readWifiSupportedBands(context)
        }

        if (hasNetworkPermission) {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivity != null) {
                cellularNetworkCaps = findCellularNetworkCapabilities(connectivity)
                val active = connectivity.activeNetwork
                val caps = active?.let { connectivity.getNetworkCapabilities(it) }
                if (caps != null) {
                    activeNetworkCaps = caps
                    connectionType = when {
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                        else -> "Online"
                    }
                    isVpn = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    isMeteredConnection = connectivity.isActiveNetworkMetered
                    isRoaming = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                    downstreamKbps = caps.linkDownstreamBandwidthKbps
                    upstreamKbps = caps.linkUpstreamBandwidthKbps
                    internetValidated =
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    captivePortal =
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
                    multiNetworkSummary = readMultiNetworkSummary(connectivity)
                    if (wifiNetworkRowsVisible && hasWifiStatePermission && connectionType == "Wi-Fi") {
                        val wifiLive = readWifiLinkAndSignal(context, caps)
                        wifiLinkSpeedSummary = wifiLive.first
                        wifiSignalSummary = wifiLive.second
                    }
                } else {
                    connectionType = "Offline"
                    internetValidated = false
                    captivePortal = false
                    multiNetworkSummary = DeviceInfoUiShared.MultiNetworkToken.NONE
                }
            }
        }

        val bluetoothOn = try {
            if (canReadBluetoothState(context)) bluetoothAdapterOrNull(context)?.isEnabled else null
        } catch (_: SecurityException) {
            null
        }

        var cellularType = "—"
        var simCarrier = "—"
        var simState = "—"
        var simCountryIso = "—"
        var simTypeSummary = "—"
        var simDetailRowsVisible = false
        var cellularSignalSummary = "—"
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                    if (tm != null) {
                        simDetailRowsVisible = true
                        val sm = context.getSystemService(SubscriptionManager::class.java)
                        val simUi = buildSimSlotSummaries(tm, sm)
                        val dataSubId = try {
                            SubscriptionManager.getDefaultDataSubscriptionId()
                        } catch (_: Exception) {
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID
                        }
                        val dataTm = if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                            tm.createForSubscriptionId(dataSubId)
                        } else {
                            tm
                        }
                        if (hasInsertedSim(tm, sm)) {
                            cellularSignalSummary = readCellularSignalSummaries(tm, sm)
                        }
                        cellularType = if (simUi.hasActiveSubscriptions) {
                            resolveCellularNetworkLabel(
                                dataTm,
                                cellularNetworkCaps ?: activeNetworkCaps,
                            )
                        } else {
                            "—"
                        }
                        simCarrier = simUi.carrier
                        simState = simUi.state
                        simCountryIso = simUi.country
                        simTypeSummary = simUi.type
                    }
                }
            } catch (_: SecurityException) {
            }
        }

        return NetworkSlice(
            connectionType = connectionType,
            bluetoothOn = bluetoothOn,
            cellularType = cellularType,
            simCarrier = simCarrier,
            simState = simState,
            simCountryIso = simCountryIso,
            simTypeSummary = simTypeSummary,
            simDetailRowsVisible = simDetailRowsVisible,
            isVpn = isVpn,
            isMeteredConnection = isMeteredConnection,
            isRoaming = isRoaming,
            downstreamKbps = downstreamKbps,
            upstreamKbps = upstreamKbps,
            wifiSupportedBandsSummary = wifiSupportedBandsSummary,
            wifiLinkSpeedSummary = wifiLinkSpeedSummary,
            wifiSignalSummary = wifiSignalSummary,
            cellularSignalSummary = cellularSignalSummary,
            internetValidated = internetValidated,
            captivePortal = captivePortal,
            multiNetworkSummary = multiNetworkSummary,
            wifiNetworkRowsVisible = wifiNetworkRowsVisible,
        )
    }

    private fun readCellularSignalSlice(context: Context): CellularSignalSlice {
        var cellularType = "—"
        var cellularSignalSummary = "—"
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return CellularSignalSlice(cellularType, cellularSignalSummary)
        }
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return CellularSignalSlice(cellularType, cellularSignalSummary)
            }
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return CellularSignalSlice(cellularType, cellularSignalSummary)
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val cellularNetworkCaps = connectivity?.let { findCellularNetworkCapabilities(it) }
            val active = connectivity?.activeNetwork
            val activeNetworkCaps = active?.let { connectivity.getNetworkCapabilities(it) }
            val dataSubId = try {
                SubscriptionManager.getDefaultDataSubscriptionId()
            } catch (_: Exception) {
                SubscriptionManager.INVALID_SUBSCRIPTION_ID
            }
            val dataTm = if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                tm.createForSubscriptionId(dataSubId)
            } else {
                tm
            }
            val simUi = buildSimSlotSummaries(tm, sm)
            if (hasInsertedSim(tm, sm)) {
                cellularSignalSummary = readCellularSignalSummaries(tm, sm)
            }
            cellularType = if (simUi.hasActiveSubscriptions) {
                resolveCellularNetworkLabel(dataTm, cellularNetworkCaps ?: activeNetworkCaps)
            } else {
                "—"
            }
        } catch (_: SecurityException) {
        }
        return CellularSignalSlice(cellularType, cellularSignalSummary)
    }

    private fun readDisplaySlice(context: Context): DisplaySlice {
        val displayObj: Display? = try {
            val dm = context.getSystemService(DisplayManager::class.java)
            dm?.getDisplay(Display.DEFAULT_DISPLAY) ?: context.display
        } catch (_: Exception) {
            null
        }
        val metrics = DisplayMetrics()
        val displayRefreshRateHz = try {
            if (displayObj != null) {
                displayObj.getRealMetrics(metrics)
                displayObj.refreshRate
            } else {
                metrics.setTo(context.resources.displayMetrics)
                0f
            }
        } catch (_: Exception) {
            metrics.setTo(context.resources.displayMetrics)
            0f
        }
        return DisplaySlice(
            displayResolution = "${metrics.widthPixels} x ${metrics.heightPixels}",
            displayDiagonalInches = HardwareInfoReader.diagonalInchesFromMetrics(
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.xdpi,
                metrics.ydpi,
                metrics.densityDpi,
            ),
            displayRefreshRateHz = displayRefreshRateHz,
            displayRefreshModesSummary = formatSupportedRefreshRates(displayObj),
            displayHdrSummary = formatHdrSummary(context, displayObj),
            screenDensityDpi = metrics.densityDpi,
        )
    }

    /** Typical phone batteries are ≥ ~1000 mAh; reject HAL noise that formats as 0 mAh. */
    private const val MIN_PLAUSIBLE_CAPACITY_MICRO_AH = 100_000L

    private fun isPlausibleBatteryCapacityMicroAh(microAh: Long): Boolean =
        microAh in MIN_PLAUSIBLE_CAPACITY_MICRO_AH..100_000_000L

    /** Many HALs return mAh (e.g. 4500) instead of µAh (4_500_000). */
    private fun normalizeCapacityToMicroAh(raw: Long): Long? {
        if (raw <= 0L) return null
        val candidates = buildList {
            add(raw)
            if (raw in 100L..100_000L) add(raw * 1_000L)
            if (raw > 100_000_000L) add(raw / 1_000L)
            if (raw > 100_000_000_000L) add(raw / 1_000_000L)
        }
        for (microAh in candidates) {
            if (isPlausibleBatteryCapacityMicroAh(microAh)) return microAh
        }
        return null
    }

    private fun readBatteryDesignCapacityMicroAh(
        context: Context,
        bm: BatteryManager?,
        batteryIntent: Intent,
    ): Long? = readBatteryDesignCapacityInternal(
        context,
        bm,
        batteryIntent,
        allowChargeFullFallback = true,
    )

    private fun readBatteryDesignCapacityInternal(
        context: Context,
        bm: BatteryManager?,
        batteryIntent: Intent,
        allowChargeFullFallback: Boolean,
    ): Long? {
        readDesignCapacityFromBatteryManagerExtras(batteryIntent)?.let { return it }
        readDesignCapacityFromPowerProfile(context)?.let { return it }
        readDesignCapacityFromBatteryIntent(batteryIntent)?.let { return it }
        readDesignCapacityFromSysfs()?.let { return it }
        if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            readBatteryDesignCapacityProperty(bm)?.let { return it }
        }
        if (allowChargeFullFallback && bm != null) {
            readBatteryChargeFullMicroAh(bm, batteryIntent)?.let { return it }
        }
        return null
    }

    private fun batteryPropertyId(fieldName: String): Int? =
        runCatching {
            BatteryManager::class.java.getField(fieldName).getInt(null)
        }.getOrNull()

    private fun readBatteryLongPropertyByFieldName(bm: BatteryManager, fieldName: String): Long? {
        val id = batteryPropertyId(fieldName) ?: return null
        return readBatteryLongProperty(bm, id)
    }

    private fun readBatteryDesignCapacityProperty(bm: BatteryManager): Long? {
        readBatteryLongPropertyByFieldName(bm, "BATTERY_PROPERTY_CHARGE_FULL_DESIGN")
            ?.let { return it }
        return readBatteryLongProperty(bm, LEGACY_BATTERY_PROPERTY_CHARGE_FULL_DESIGN)
    }

    private fun readBatteryChargeFullProperty(bm: BatteryManager): Long? {
        readBatteryLongPropertyByFieldName(bm, "BATTERY_PROPERTY_CHARGE_FULL")?.let { return it }
        return readBatteryLongProperty(bm, LEGACY_BATTERY_PROPERTY_CHARGE_FULL)
    }

    private fun readDesignCapacityFromBatteryManagerExtras(batteryIntent: Intent): Long? {
        val keys = mutableListOf(
            "android.os.extra.DESIGN_CAPACITY",
            "android.os.extra.MAXIMUM_CAPACITY",
        )
        for (field in listOf("EXTRA_DESIGN_CAPACITY", "EXTRA_MAXIMUM_CAPACITY")) {
            runCatching {
                BatteryManager::class.java.getField(field).get(null) as? String
            }.getOrNull()?.let { keys.add(it) }
        }
        for (key in keys.distinct()) {
            readCapacityFromIntentExtra(batteryIntent, key)?.let { return it }
        }
        return null
    }

    private fun readBatteryChargeFullMicroAh(bm: BatteryManager?, batteryIntent: Intent): Long? {
        if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            readBatteryChargeFullProperty(bm)?.let { return it }
        }
        readChargeFullFromBatteryIntent(batteryIntent)?.let { return it }
        readChargeFullFromSysfs()?.let { return it }
        return null
    }

    private fun readChargeFullFromBatteryIntent(batteryIntent: Intent): Long? {
        val keys = listOf(
            "charge_full",
            "charge_full_uah",
            "ChargeFull",
            "android.os.extra.CHARGE_FULL",
        )
        for (key in keys) {
            readCapacityFromIntentExtra(batteryIntent, key)?.let { return it }
        }
        return null
    }

    private fun readChargeFullFromSysfs(): Long? {
        val fullFiles = listOf("charge_full", "charge_full_uah")
        val supplies = listOf(
            "battery",
            "Battery",
            "bms",
            "max170xx_battery",
            "sec-battery",
            "samsung_battery",
            "mtk-battery",
        )
        for (supply in supplies) {
            for (file in fullFiles) {
                readSysfsCapacityAt("/sys/class/power_supply/$supply/$file")?.let { return it }
            }
        }
        try {
            val root = File("/sys/class/power_supply")
            if (root.isDirectory) {
                for (child in root.listFiles() ?: emptyArray()) {
                    for (file in fullFiles) {
                        readSysfsCapacityAt(File(child, file).path)?.let { return it }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    /** AOSP [com.android.internal.os.PowerProfile#getBatteryCapacity] in mAh — works on many retail devices. */
    private fun readDesignCapacityFromPowerProfile(context: Context): Long? {
        return try {
            val clazz = Class.forName("com.android.internal.os.PowerProfile")
            val instance = clazz.getConstructor(Context::class.java)
                .newInstance(context.applicationContext)
            val mAh = clazz.getMethod("getBatteryCapacity").invoke(instance)
            when (mAh) {
                is Double -> normalizeCapacityToMicroAh(kotlin.math.round(mAh).toLong())
                is Float -> normalizeCapacityToMicroAh(kotlin.math.round(mAh.toDouble()).toLong())
                is Int -> normalizeCapacityToMicroAh(mAh.toLong())
                is Long -> normalizeCapacityToMicroAh(mAh)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readDesignCapacityFromBatteryIntent(batteryIntent: Intent): Long? {
        val keys = listOf(
            "charge_full_design",
            "charge_full_design_uah",
            "ChargeFullDesign",
            "android.os.extra.CHARGE_FULL_DESIGN",
            "BATTERY_CHARGE_FULL_DESIGN",
            "battery_charge_full_design",
            "mBatteryChargeFullDesign",
            "design_capacity",
            "rated_capacity",
            "energy_full_design",
        )
        for (key in keys) {
            readCapacityFromIntentExtra(batteryIntent, key)?.let { return it }
        }
        return null
    }

    private fun readCapacityFromIntentExtra(intent: Intent, key: String): Long? {
        if (!intent.hasExtra(key)) return null
        val asLong = intent.getLongExtra(key, Long.MIN_VALUE)
        if (asLong != Long.MIN_VALUE && asLong > 0L) {
            normalizeCapacityToMicroAh(asLong)?.let { return it }
        }
        val asInt = intent.getIntExtra(key, Int.MIN_VALUE)
        if (asInt != Int.MIN_VALUE && asInt > 0) {
            normalizeCapacityToMicroAh(asInt.toLong())?.let { return it }
        }
        return null
    }

    private fun parseCapacityMicroAhFromSysfs(text: String): Long? {
        val trimmed = text.trim()
        trimmed.toLongOrNull()?.let { return normalizeCapacityToMicroAh(it) }
        trimmed.toDoubleOrNull()?.let { d ->
            if (d > 0) return normalizeCapacityToMicroAh(kotlin.math.round(d).toLong())
        }
        return null
    }

    private fun readSysfsCapacityAt(path: String): Long? {
        return try {
            val f = File(path)
            if (!f.exists() || !f.canRead()) return null
            parseCapacityMicroAhFromSysfs(f.readText())
        } catch (_: Exception) {
            null
        }
    }

    private fun readDesignCapacityFromSysfs(): Long? {
        val designFiles = listOf(
            "charge_full_design",
            "charge_full_design_uah",
            "design_capacity",
            "energy_full_design",
            "rated_capacity",
            "learned_capacity",
            "full_charge_design",
            "charge_nominal",
        )
        val fullFiles = listOf(
            "charge_full",
            "charge_full_uah",
        )
        val preferredSupplies = listOf(
            "battery",
            "Battery",
            "google,battery",
            "bms",
            "max170xx_battery",
            "main-battery",
            "sec-battery",
            "samsung_battery",
            "mtk-battery",
            "cw-bat",
            "pm8150b-battery",
            "usb",
        )
        for (supply in preferredSupplies) {
            for (file in designFiles) {
                readSysfsCapacityAt("/sys/class/power_supply/$supply/$file")?.let { return it }
            }
        }
        try {
            val root = File("/sys/class/power_supply")
            if (root.isDirectory) {
                for (child in root.listFiles() ?: emptyArray()) {
                    for (file in designFiles) {
                        readSysfsCapacityAt(File(child, file).path)?.let { return it }
                    }
                    child.listFiles()?.forEach { node ->
                        val name = node.name.lowercase(Locale.US)
                        if (name.contains("design") || name.contains("rated") || name.contains("nominal")) {
                            readSysfsCapacityAt(node.path)?.let { return it }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        for (supply in preferredSupplies) {
            for (file in fullFiles) {
                readSysfsCapacityAt("/sys/class/power_supply/$supply/$file")?.let { return it }
            }
        }
        try {
            val root = File("/sys/class/power_supply")
            if (root.isDirectory) {
                for (child in root.listFiles() ?: emptyArray()) {
                    for (file in fullFiles) {
                        readSysfsCapacityAt(File(child, file).path)?.let { return it }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun readBatteryLongProperty(bm: BatteryManager, property: Int): Long? {
        val longV = runCatching { bm.getLongProperty(property) }.getOrDefault(Long.MIN_VALUE)
        if (longV != Long.MIN_VALUE && longV > 0L) {
            normalizeCapacityToMicroAh(longV)?.let { return it }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val intV = runCatching { bm.getIntProperty(property) }.getOrDefault(Int.MIN_VALUE)
            if (intV != Int.MIN_VALUE && intV > 0) {
                normalizeCapacityToMicroAh(intV.toLong())?.let { return it }
            }
        }
        return null
    }

    private fun readChargeTimeRemainingMs(
        batteryIntent: Intent,
        bm: BatteryManager?,
        status: Int,
        plugged: Int,
        level: Int,
        scale: Int,
        chargeFullMicroAh: Long?,
        designMicroAh: Long?,
        currentNowMicroA: Long,
    ): Long? {
        if (status == BatteryManager.BATTERY_STATUS_FULL) return null
        if (status != BatteryManager.BATTERY_STATUS_CHARGING || plugged == 0) return null

        if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val apiMs = runCatching { bm.computeChargeTimeRemaining() }.getOrDefault(-1L)
            if (apiMs > 0L) return apiMs
        }

        readChargeTimeRemainingFromBatteryIntent(batteryIntent)?.let { return it }
        readTimeToFullFromSysfs()?.let { sec -> return sec * 1_000L }

        val currentMicroA = pickChargeCurrentMicroA(bm, currentNowMicroA)
        return estimateChargeTimeRemainingMs(
            level = level,
            scale = scale,
            chargeFullMicroAh = chargeFullMicroAh,
            designMicroAh = designMicroAh,
            currentMicroA = currentMicroA,
        )
    }

    private fun pickChargeCurrentMicroA(bm: BatteryManager?, currentNowMicroA: Long): Long {
        if (bm == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return kotlin.math.abs(currentNowMicroA)
        }
        val average = runCatching {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        }.getOrDefault(0L)
        val now = kotlin.math.abs(currentNowMicroA)
        val avg = kotlin.math.abs(average)
        return when {
            avg in 80_000L..20_000_000L -> avg
            now in 80_000L..20_000_000L -> now
            else -> kotlin.math.max(now, avg)
        }
    }

    private fun readChargeTimeRemainingFromBatteryIntent(batteryIntent: Intent): Long? {
        val keys = listOf(
            "time_to_full_now",
            "charge_time",
            "ChargeTime",
            "TimeToFull",
        )
        for (key in keys) {
            if (!batteryIntent.hasExtra(key)) continue
            val asInt = batteryIntent.getIntExtra(key, -1)
            parseTimeToFullSeconds(asInt.toLong())?.let { return it * 1_000L }
            val asLong = batteryIntent.getLongExtra(key, -1L)
            parseTimeToFullSeconds(asLong)?.let { return it * 1_000L }
        }
        return null
    }

    private fun parseTimeToFullSeconds(raw: Long): Long? {
        if (raw <= 0L) return null
        if (raw == Long.MAX_VALUE || raw >= 4_294_967_000L) return null
        val seconds = when {
            raw > 86_400L * 14L -> null
            raw > 86_400L -> raw / 1_000L
            else -> raw
        } ?: return null
        return seconds.takeIf { it in 1L..86_400L * 2L }
    }

    private fun readTimeToFullFromSysfs(): Long? {
        val direct = listOf(
            "/sys/class/power_supply/battery/time_to_full_now",
            "/sys/class/power_supply/Battery/time_to_full_now",
            "/sys/class/power_supply/bms/time_to_full_now",
        )
        for (path in direct) {
            try {
                val f = File(path)
                if (!f.exists() || !f.canRead()) continue
                parseTimeToFullSeconds(f.readText().trim().toLongOrNull() ?: continue)?.let { return it }
            } catch (_: Exception) {
            }
        }
        try {
            val root = File("/sys/class/power_supply")
            if (!root.isDirectory) return null
            for (child in root.listFiles() ?: emptyArray()) {
                try {
                    val f = File(child, "time_to_full_now")
                    if (!f.exists() || !f.canRead()) continue
                    parseTimeToFullSeconds(f.readText().trim().toLongOrNull() ?: continue)?.let { return it }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun estimateChargeTimeRemainingMs(
        level: Int,
        scale: Int,
        chargeFullMicroAh: Long?,
        designMicroAh: Long?,
        currentMicroA: Long,
    ): Long? {
        if (scale <= 0 || level < 0) return null
        val capacityMicroAh = chargeFullMicroAh
            ?: designMicroAh?.takeIf { isPlausibleBatteryCapacityMicroAh(it) }
            ?: return null
        if (currentMicroA < 80_000L) return null
        val currentMa = currentMicroA / 1_000.0
        if (currentMa < 80.0 || currentMa > 20_000.0) return null
        val fraction = level.toDouble() / scale.toDouble()
        if (fraction >= 0.995) return null
        val remainingMicroAh = ((1.0 - fraction) * capacityMicroAh).toLong()
        if (remainingMicroAh <= 0L) return null
        val hours = remainingMicroAh / 1_000.0 / currentMa
        val ms = (hours * 3_600_000.0).toLong()
        return ms.takeIf { it in 60_000L..172_800_000L }
    }

    private data class SimSlotSummaries(
        val carrier: String,
        val state: String,
        val country: String,
        val type: String,
        val hasActiveSubscriptions: Boolean
    )

    private fun simSlotLabel(slotIndex: Int): String =
        if (slotIndex >= 0) "SIM ${slotIndex + 1}" else "SIM"

    private fun buildSimSlotSummaries(
        tm: TelephonyManager,
        sm: SubscriptionManager?
    ): SimSlotSummaries {
        val subs = sm?.activeSubscriptionInfoList
            ?.sortedWith(
                compareBy<SubscriptionInfo> { if (it.simSlotIndex >= 0) it.simSlotIndex else 99 }
                    .thenBy { it.subscriptionId }
            )
            ?: emptyList()
        if (subs.isEmpty()) {
            return SimSlotSummaries(
                carrier = tm.simOperatorName?.trim()?.takeIf { it.isNotEmpty() } ?: "—",
                state = simStateToString(tm.simState),
                country = tm.simCountryIso?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: "—",
                type = "—",
                hasActiveSubscriptions = false
            )
        }
        val carriers = subs.joinToString("\n") { sub ->
            val name = sub.carrierName?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                ?: sub.displayName?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                ?: "—"
            "${simSlotLabel(sub.simSlotIndex)}: $name"
        }
        val states = subs.joinToString("\n") { sub ->
            val stm = tm.createForSubscriptionId(sub.subscriptionId)
            "${simSlotLabel(sub.simSlotIndex)}: ${simStateToString(stm.simState)}"
        }
        val countries = subs.joinToString("\n") { sub ->
            val iso = sub.countryIso.trim().uppercase()
            "${simSlotLabel(sub.simSlotIndex)}: ${if (iso.isNotEmpty()) iso else "—"}"
        }
        val types = subs.joinToString("\n") { sub ->
            val kind = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sub.isEmbedded) {
                "eSIM"
            } else {
                "Physical"
            }
            "${simSlotLabel(sub.simSlotIndex)}: $kind"
        }
        return SimSlotSummaries(
            carrier = carriers,
            state = states,
            country = countries,
            type = types,
            hasActiveSubscriptions = true
        )
    }

    private fun isSimInsertedState(state: Int): Boolean =
        when (state) {
            TelephonyManager.SIM_STATE_ABSENT,
            TelephonyManager.SIM_STATE_UNKNOWN,
            TelephonyManager.SIM_STATE_PERM_DISABLED -> false
            else -> true
        }

    /** True if any slot has a SIM inserted (state other than absent/unknown). */
    private fun hasInsertedSim(tm: TelephonyManager, sm: SubscriptionManager?): Boolean {
        val subs = sm?.activeSubscriptionInfoList
        if (!subs.isNullOrEmpty()) {
            return subs.any { sub ->
                isSimInsertedState(tm.createForSubscriptionId(sub.subscriptionId).simState)
            }
        }
        return isSimInsertedState(tm.simState)
    }

    private fun simStateToString(state: Int): String =
        when (state) {
            TelephonyManager.SIM_STATE_READY -> "Ready"
            TelephonyManager.SIM_STATE_ABSENT -> "Absent"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN required"
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK required"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network locked"
            TelephonyManager.SIM_STATE_NOT_READY -> "Not ready"
            TelephonyManager.SIM_STATE_PERM_DISABLED -> "Disabled"
            10 -> "Loaded" // SIM_STATE_LOADED: exists in platform source but not in public SDK API (not in android.jar stubs)
            TelephonyManager.SIM_STATE_UNKNOWN -> "Unknown"
            else -> "—"
        }

    /**
     * [TelephonyManager.getDataNetworkType] often stays **LTE** on **5G NSA** (LTE anchor).
     * Prefer [TelephonyDisplayInfo] (API 31+), [NetworkCapabilities] NR bits, and
     * [ServiceState] NR state when possible.
     */
    private fun resolveCellularNetworkLabel(
        dataTm: TelephonyManager,
        activeCaps: NetworkCapabilities?
    ): String {
        val rawType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dataTm.dataNetworkType
        } else {
            @Suppress("DEPRECATION")
            dataTm.networkType
        }
        when (readDisplayOverrideNetworkType(dataTm)) {
            OVERRIDE_NETWORK_TYPE_NR_NSA,
            OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> return "5G NR (NSA)"
            OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> return "5G NR"
            else -> { }
        }
        val displayRat = readTelephonyDisplayNetworkType(dataTm)
        if (displayRat == TelephonyManager.NETWORK_TYPE_NR) {
            return "5G NR"
        }
        if (cellularNetworkDeclaresNr(activeCaps) ||
            cellInfoDeclaresNr(dataTm) ||
            signalStrengthDeclaresNr(dataTm)
        ) {
            return when (rawType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_LTE -> "5G NR (NSA)"
                else -> "5G NR"
            }
        }
        val nr = readNrStateReflect(dataTm)
        val onNr = nr == NR_STATE_CONNECTED || nr == NR_STATE_NOT_RESTRICTED
        if (onNr) {
            return when (rawType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_LTE -> "5G NR (NSA)"
                else -> "5G NR"
            }
        }
        return dataNetworkTypeToString(rawType)
    }

    private fun cellInfoDeclaresNr(tm: TelephonyManager): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            tm.allCellInfo?.any { cell ->
                cell is CellInfoNr && cell.isRegistered
            } == true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun signalStrengthDeclaresNr(tm: TelephonyManager): Boolean {
        val strengths = tm.signalStrength?.cellSignalStrengths ?: return false
        return strengths.any { it is CellSignalStrengthNr }
    }

    private fun dataNetworkTypeToString(type: Int): String =
        when (type) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
            else -> "—"
        }

    private fun formatSize(gb: Double): String =
        when {
            gb >= 0.1 -> String.format(Locale.US, "%.1f GB", gb)
            gb >= 1.0 / 1024 -> String.format(Locale.US, "%.0f MB", gb * 1024)
            gb >= 1.0 / (1024 * 1024) -> String.format(Locale.US, "%.0f KB", gb * 1024 * 1024)
            else -> String.format(Locale.US, "%.0f B", gb * 1024 * 1024 * 1024)
        }

    private fun formatFreeOf(context: Context, freeGb: Double, totalGb: Double): String =
        context.getString(R.string.storage_free_of, formatSize(freeGb), formatSize(totalGb))

    private fun hasWifiStatePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) ==
            PackageManager.PERMISSION_GRANTED

    private fun readWifiSupportedBands(context: Context): String =
        runCatching {
            val wm = context.applicationContext.getSystemService(WifiManager::class.java) ?: return "—"
            val bands = mutableListOf("2.4 GHz")
            if (wm.is5GHzBandSupported) bands += "5 GHz"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (wm.is6GHzBandSupported) bands += "6 GHz"
                if (wm.is60GHzBandSupported) bands += "60 GHz"
            }
            DeviceInfoUiShared.joinList(bands)
        }.getOrElse { "—" }

    private fun readWifiLinkAndSignal(context: Context, caps: NetworkCapabilities): Pair<String, String> =
        runCatching {
            var link = "—"
            var signal = "—"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val wifiInfo = caps.transportInfo as? WifiInfo
                if (wifiInfo != null) {
                    val speed = wifiInfo.linkSpeed
                    if (speed > 0) link = String.format(Locale.US, "%d Mbps", speed)
                    formatWifiRssi(wifiInfo.rssi)?.let { signal = it }
                }
            } else {
                val wm = context.applicationContext.getSystemService(WifiManager::class.java)
                @Suppress("DEPRECATION")
                val info = wm?.connectionInfo
                if (info != null) {
                    val speed = info.linkSpeed
                    if (speed > 0) link = String.format(Locale.US, "%d Mbps", speed)
                    formatWifiRssi(info.rssi)?.let { signal = it }
                }
            }
            link to signal
        }.getOrElse { "—" to "—" }

    private fun formatWifiRssi(rssi: Int): String? {
        if (rssi in -127..0) return String.format(Locale.US, "%d dBm", rssi)
        return null
    }

    private fun findCellularNetworkCapabilities(cm: ConnectivityManager): NetworkCapabilities? {
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return caps
        }
        return null
    }

    private fun readCellularSignalSummaries(tm: TelephonyManager, sm: SubscriptionManager?): String {
        val subs = sm?.activeSubscriptionInfoList
            ?.sortedWith(
                compareBy<SubscriptionInfo> { if (it.simSlotIndex >= 0) it.simSlotIndex else 99 }
                    .thenBy { it.subscriptionId },
            )
            ?: emptyList()
        if (subs.isEmpty()) {
            val ss = tm.signalStrength ?: return "—"
            val dbm = bestCellularDbm(ss) ?: return "—"
            return String.format(Locale.US, "%d dBm", dbm)
        }
        val lines = subs.mapNotNull { sub ->
            val stm = tm.createForSubscriptionId(sub.subscriptionId)
            val ss = stm.signalStrength ?: return@mapNotNull null
            val dbm = bestCellularDbm(ss) ?: return@mapNotNull null
            "${simSlotLabel(sub.simSlotIndex)}: ${String.format(Locale.US, "%d dBm", dbm)}"
        }
        return if (lines.isEmpty()) "—" else DeviceInfoUiShared.joinList(lines)
    }

    private fun bestCellularDbm(ss: android.telephony.SignalStrength): Int? {
        val values = ss.cellSignalStrengths.mapNotNull { cell ->
            val d = cell.dbm
            if (d in -140..0) d else null
        }
        return values.maxOrNull()
    }

    private fun readMultiNetworkSummary(cm: ConnectivityManager): String {
        val labels = linkedSetOf<String>()
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                labels.add(DeviceInfoUiShared.MultiNetworkToken.WIFI)
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                labels.add(DeviceInfoUiShared.MultiNetworkToken.CELLULAR)
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                labels.add(DeviceInfoUiShared.MultiNetworkToken.ETHERNET)
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                labels.add(DeviceInfoUiShared.MultiNetworkToken.BLUETOOTH)
            }
        }
        return if (labels.size >= 2) DeviceInfoUiShared.joinList(labels.toList())
        else DeviceInfoUiShared.MultiNetworkToken.NONE
    }

    private fun readMountedExternalStorageVolumes(context: Context): List<ExternalStorageVolumeInfo> {
        val sm = context.getSystemService(StorageManager::class.java) ?: return emptyList()
        val dataRoot = runCatching { Environment.getDataDirectory().canonicalFile }.getOrNull()
        val out = mutableListOf<ExternalStorageVolumeInfo>()
        for (volume in sm.storageVolumes) {
            if (volume.isPrimary) continue
            val dir = volume.directory ?: continue
            val root = runCatching { dir.canonicalFile }.getOrNull() ?: continue
            if (dataRoot != null && root == dataRoot) continue
            // StorageManager doesn't expose a stable "getStorageVolumeState(File)" across all SDKs.
            // StorageVolume already knows its own mount state.
            if (volume.state != Environment.MEDIA_MOUNTED) continue
            val totalBytes = runCatching { StatFs(root.path).totalBytes }.getOrDefault(0L)
            val freeBytes = runCatching { StatFs(root.path).availableBytes }.getOrDefault(0L)
            if (totalBytes <= 0L) continue
            val title = volume.getDescription(context)?.toString()?.trim().orEmpty()
                .ifEmpty { context.getString(R.string.storage_volume_fallback_title) }
            val rowKey = DeviceInfoUiShared.Row.storageVolumeRowKey(volumeIdForRowKey(volume, root))
            val summary = formatFreeOf(
                context,
                freeBytes / (1024.0 * 1024.0 * 1024.0),
                totalBytes / (1024.0 * 1024.0 * 1024.0),
            )
            out += ExternalStorageVolumeInfo(rowKey = rowKey, title = title, summary = summary)
        }
        return out.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    private fun volumeIdForRowKey(volume: StorageVolume, root: File): String {
        volume.uuid?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return root.absolutePath
    }

    /**
     * Official [BatteryManager.EXTRA_CYCLE_COUNT] exists from API 34.
     * On older Android, some OEMs expose the value via sysfs or non-standard intent keys.
     */
    private fun readBatteryCycleCount(batteryIntent: Intent): Int? {
        if (Build.VERSION.SDK_INT >= 34) {
            val v = batteryIntent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
            if (v >= 0) return v
        }
        val fromIntent = readCycleCountFromBatteryIntentExtras(batteryIntent)
        if (fromIntent != null) return fromIntent
        return readCycleCountFromSysfs()
    }

    private fun readCycleCountFromBatteryIntentExtras(batteryIntent: Intent): Int? {
        val keys = listOf(
            "cycle_count",
            "android.os.extra.CYCLE_COUNT",
            "com.android.battery.cycle_count",
            "battery_cycle_count",
            "CycleCount"
        )
        for (key in keys) {
            if (!batteryIntent.hasExtra(key)) continue
            val asInt = batteryIntent.getIntExtra(key, -1)
            if (asInt >= 0) return asInt.coerceAtMost(1_000_000)
            val asLong = batteryIntent.getLongExtra(key, -1L)
            if (asLong >= 0L && asLong <= Int.MAX_VALUE) return asLong.toInt()
        }
        return null
    }

    private fun parseCycleCountText(text: String): Int? {
        val v = text.trim().toLongOrNull() ?: return null
        if (v < 0L || v > 1_000_000L) return null
        return v.toInt()
    }

    private fun readCycleCountFromSysfs(): Int? {
        val direct = listOf(
            "/sys/class/power_supply/battery/cycle_count",
            "/sys/class/power_supply/Battery/cycle_count",
            "/sys/class/power_supply/bms/cycle_count",
            "/sys/class/power_supply/bms/cycle_counts",
            "/sys/class/power_supply/max170xx_battery/cycle_count"
        )
        for (path in direct) {
            try {
                val f = File(path)
                if (!f.exists() || !f.canRead()) continue
                parseCycleCountText(f.readText())?.let { return it }
            } catch (_: Exception) { }
        }
        try {
            val root = File("/sys/class/power_supply")
            if (!root.isDirectory) return null
            for (child in root.listFiles() ?: emptyArray()) {
                for (name in listOf("cycle_count", "cycle_counts")) {
                    try {
                        val f = File(child, name)
                        if (!f.exists() || !f.canRead()) continue
                        parseCycleCountText(f.readText())?.let { return it }
                    } catch (_: Exception) { }
                }
            }
        } catch (_: Exception) { }
        return null
    }

    private fun readCpuInfo(): String {
        val logicalCores = runCatching { Runtime.getRuntime().availableProcessors() }.getOrDefault(0)
        val coresStr = if (logicalCores > 0) " ($logicalCores cores)" else ""
        fun readTextFile(path: String): String? {
            return try {
                val f = File(path)
                if (!f.exists() || !f.canRead()) return null
                val bytes = f.readBytes()
                if (bytes.isEmpty()) return null
                val s = String(bytes, Charsets.UTF_8)
                    .replace('\u0000', ' ')
                    .trim()
                s.takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }
        }

        fun extractTensorName(text: String): String? {
            // Expected like: "Tensor G3", "Tensor G2", etc.
            return Regex("Tensor\\s*([A-Za-z0-9]+)", RegexOption.IGNORE_CASE)
                .find(text)
                ?.let { m -> "Tensor ${m.groupValues[1]}" }
        }

        // 1) Try device-tree first (Pixel often exposes marketing Tensor name here).
        for (path in listOf(
            "/proc/device-tree/model",
            "/sys/firmware/devicetree/base/model"
        )) {
            val text = readTextFile(path) ?: continue
            extractTensorName(text)?.let { return it + coresStr }
        }
        for (path in listOf(
            "/proc/device-tree/compatible",
            "/sys/firmware/devicetree/base/compatible"
        )) {
            val text = readTextFile(path) ?: continue
            extractTensorName(text)?.let { return it + coresStr }
        }

        // 2) Fallback to Build.SOC_MODEL (if exposed on this Android version).
        val socModel = runCatching {
            Build::class.java.getField("SOC_MODEL").get(null) as? String
        }.getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (!socModel.isNullOrEmpty()) return socModel + coresStr

        // 3) Finally, parse /proc/cpuinfo; on Pixel this may be generic "ARM ..." so keep it as fallback only.
        return try {
            val lines = File("/proc/cpuinfo").readLines()
            var hardware = ""
            var modelName = ""
            var implementer = ""
            var part = ""
            var architecture = ""
            for (line in lines) {
                val colon = line.indexOf(':')
                if (colon <= 0) continue
                val key = line.substring(0, colon).trim()
                val value = line.substring(colon + 1).trim()
                when (key) {
                    "Hardware" -> hardware = value
                    "Processor", "model name" -> if (modelName.isEmpty()) modelName = value
                    "CPU implementer" -> if (implementer.isEmpty()) implementer = value
                    "CPU part" -> if (part.isEmpty()) part = value
                    "CPU architecture" -> if (architecture.isEmpty()) architecture = value
                }
            }
            val primary = hardware.ifEmpty { modelName }.trim()
            val isGenericArmName = primary.contains("ARM", ignoreCase = true) ||
                primary.contains("processor", ignoreCase = true) ||
                primary.startsWith("ARMv", ignoreCase = true)
            if (primary.isNotEmpty() && !isGenericArmName) return primary + coresStr
            val chipHint = buildList {
                Build.HARDWARE.trim().takeIf { it.isNotEmpty() }?.let { add(it) }
                if (architecture.isNotEmpty()) add("arch $architecture")
                if (part.isNotEmpty()) add("part $part")
                if (implementer.isNotEmpty()) add("impl $implementer")
            }.let { DeviceInfoUiShared.joinList(it) }
            chipHint.ifEmpty { "—" }.let { base ->
                if (base == "—") base else base + coresStr
            }
        } catch (_: Exception) {
            val hw = Build.HARDWARE.trim().takeIf { it.isNotEmpty() }
            if (hw == null) "—" else hw + coresStr
        }
    }

    private fun formatCpuMinMaxGhz(minKHz: Long?, maxKHz: Long?): String =
        when {
            minKHz != null && maxKHz != null ->
                String.format("%.2f - %.2f GHz", minKHz / 1_000_000.0, maxKHz / 1_000_000.0)
            maxKHz != null -> String.format("%.2f GHz (max)", maxKHz / 1_000_000.0)
            minKHz != null -> String.format("%.2f GHz (min)", minKHz / 1_000_000.0)
            else -> "—"
    }

    private fun readCpuFreqFromDir(base: String): String? {
        val minF = File("$base/cpuinfo_min_freq")
        val maxF = File("$base/cpuinfo_max_freq")
        if (!minF.exists() && !maxF.exists()) return null
        val minKHz = if (minF.exists()) minF.readText().trim().toLongOrNull() else null
        val maxKHz = if (maxF.exists()) maxF.readText().trim().toLongOrNull() else null
        if (minKHz == null && maxKHz == null) return null
        val s = formatCpuMinMaxGhz(minKHz, maxKHz)
        return s.takeIf { it != "—" }
    }

    private fun readCpuFreq(): String = try {
        listOf(
            "/sys/devices/system/cpu/cpu0/cpufreq",
            "/sys/devices/system/cpu/cpufreq/policy0"
        ).firstNotNullOfOrNull { readCpuFreqFromDir(it) } ?: "—"
    } catch (_: Exception) { "—" }

    private fun readCpuCurrentFreq(): String {
        return try {
            val bases = listOf(
                "/sys/devices/system/cpu/cpu0/cpufreq",
                "/sys/devices/system/cpu/cpufreq/policy0"
            )
            for (base in bases) {
                val kHz = listOf("$base/scaling_cur_freq", "$base/cpuinfo_cur_freq")
                    .firstOrNull { path -> File(path).exists() }
                    ?.let { path -> File(path).readText().trim().toLongOrNull() }
                if (kHz != null && kHz > 0) {
                    return String.format("%.2f GHz", kHz / 1_000_000.0)
                }
            }
            "—"
        } catch (_: Exception) {
            "—"
        }
    }

    private fun readGpuRenderer(): String {
        return try {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) "—"
            else {
                val version = IntArray(2)
                if (!EGL14.eglInitialize(display, version, 0, version, 1)) "—"
                else {
                    val configAttribs = intArrayOf(
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                        EGL14.EGL_NONE
                    )
                    val configs = arrayOfNulls<EGLConfig>(1)
                    val numConfigs = IntArray(1)
                    if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
                        EGL14.eglTerminate(display)
                        "—"
                    } else {
                        val config = configs[0]
                        if (config == null) {
                            EGL14.eglTerminate(display)
                            "—"
                        } else {
                            val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
                            val surface = EGL14.eglCreatePbufferSurface(display, config, surfaceAttribs, 0)
                            if (surface == EGL14.EGL_NO_SURFACE) {
                                EGL14.eglTerminate(display)
                                "—"
                            } else {
                                val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                                val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
                                if (context == EGL14.EGL_NO_CONTEXT) {
                                    EGL14.eglDestroySurface(display, surface)
                                    EGL14.eglTerminate(display)
                                    "—"
                                } else {
                                    val ok = EGL14.eglMakeCurrent(display, surface, surface, context)
                                    val renderer = if (ok) GLES20.glGetString(GLES20.GL_RENDERER) ?: "—" else "—"
                                    EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                                    EGL14.eglDestroyContext(display, context)
                                    EGL14.eglDestroySurface(display, surface)
                                    EGL14.eglTerminate(display)
                                    renderer
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { "—" }
    }

    private fun getGlEsVersion(context: Context): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (am != null) {
                val version = am.deviceConfigurationInfo.reqGlEsVersion
                if (version != 0) {
                    val major = ((version.toLong() and 0xffff0000L) shr 16).toInt()
                    val minor = (version.toLong() and 0xffffL).toInt()
                    if (major > 0 || minor > 0) return "$major.$minor"
                }
            }
            readGlEsVersionFromEgl()
        } catch (_: Exception) {
            readGlEsVersionFromEgl()
        }
    }

    private fun readGlEsVersionFromEgl(): String {
        fun tryWithClientVersion(clientVer: Int): String? {
            return try {
                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                if (display == EGL14.EGL_NO_DISPLAY) return null
                val version = IntArray(2)
                if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
                    EGL14.eglTerminate(display)
                    return null
                }
                val configAttribs = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE,
                    if (clientVer >= 3) EGL_OPENGL_ES3_BIT else EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
                )
                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) ||
                    numConfigs[0] == 0
                ) {
                    EGL14.eglTerminate(display)
                    return null
                }
                val config = configs[0] ?: run {
                    EGL14.eglTerminate(display)
                    return null
                }
                val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
                val surface = EGL14.eglCreatePbufferSurface(display, config, surfaceAttribs, 0)
                if (surface == EGL14.EGL_NO_SURFACE) {
                    EGL14.eglTerminate(display)
                    return null
                }
                val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, clientVer, EGL14.EGL_NONE)
                val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
                if (context == EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroySurface(display, surface)
                    EGL14.eglTerminate(display)
                    return null
                }
                val ok = EGL14.eglMakeCurrent(display, surface, surface, context)
                val parsed = if (ok) {
                    val glVersion = GLES20.glGetString(GLES20.GL_VERSION)
                    Regex("OpenGL ES\\s+(\\d+)\\.(\\d+)").find(glVersion ?: "")?.let { m ->
                        "${m.groupValues[1]}.${m.groupValues[2]}"
                    }
                } else null
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroyContext(display, context)
                EGL14.eglDestroySurface(display, surface)
                EGL14.eglTerminate(display)
                parsed
            } catch (_: Exception) {
                null
            }
        }
        return tryWithClientVersion(3) ?: tryWithClientVersion(2) ?: "—"
    }

    private fun readKernelVersion(): String {
        try {
            val raw = File("/proc/version").readText().trim()
            val line = raw.lineSequence().firstOrNull()?.take(200)?.trim()
            if (!line.isNullOrEmpty()) return line
        } catch (_: Exception) { }
        val osv = System.getProperty("os.version")?.trim()
        if (!osv.isNullOrEmpty()) return osv
        return "—"
    }

    private fun formatSupportedRefreshRates(display: Display?): String {
        if (display == null) return "—"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val rates = display.supportedModes
                .map { it.refreshRate }
                .filter { it > 0f }
                .map { round(it * 100f) / 100f }
                .distinct()
                .sorted()
            if (rates.isEmpty()) return "—"
            return DeviceInfoUiShared.joinList(rates.map { hz ->
                formatRefreshRateHz(hz)
            })
        }
        val r = display.refreshRate
        return if (r > 0f) formatRefreshRateHz(round(r * 100f) / 100f) else "—"
    }

    private fun formatRefreshRateHz(hz: Float): String =
        if (abs(hz - hz.toInt()) < 0.02f) "${hz.toInt()} Hz" else String.format(Locale.US, "%.2f Hz", hz)

    private fun formatHdrSummary(context: Context, display: Display?): String {
        if (display == null) return context.getString(R.string.not_available)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!display.isHdr) return context.getString(R.string.hdr_no)
            val caps = display.hdrCapabilities
            if (caps == null) return context.getString(R.string.hdr_yes)
            val types = caps.supportedHdrTypes
            if (types.isEmpty()) return context.getString(R.string.hdr_yes)
            return DeviceInfoUiShared.joinList(types.map { hdrTypeLabel(context, it) })
        }
        return context.getString(R.string.not_available)
    }

    private fun hdrTypeLabel(context: Context, type: Int): String =
        when (type) {
            HdrCapabilities.HDR_TYPE_DOLBY_VISION -> context.getString(R.string.hdr_type_dolby_vision)
            HdrCapabilities.HDR_TYPE_HDR10 -> context.getString(R.string.hdr_type_hdr10)
            HdrCapabilities.HDR_TYPE_HLG -> context.getString(R.string.hdr_type_hlg)
            HdrCapabilities.HDR_TYPE_HDR10_PLUS -> context.getString(R.string.hdr_type_hdr10_plus)
            else -> context.getString(R.string.hdr_type_other, type)
        }

    private fun createEmpty(): DeviceInfo = DeviceInfo(
        batteryLevel = 0,
        batteryTemperatureCelsius = 0f,
        batteryVoltageV = 0f,
        chargingStatus = "Unknown",
        plugged = "None",
        health = "Unknown",
        technology = "Unknown",
        currentNowMicroA = 0L,
        chargeCounterMicroAh = 0L,
        cycleCount = null,
        batteryDesignCapacityMicroAh = null,
        chargeTimeRemainingMs = null,
        displayResolution = "Unknown",
        displayDiagonalInches = 0f,
        displayRefreshRateHz = 0f,
        displayRefreshModesSummary = "—",
        displayHdrSummary = "—",
        screenDensityDpi = 0,
        connectionType = "Unknown",
        bluetoothOn = null,
        cellularType = "—",
        simCarrier = "—",
        simState = "—",
        simCountryIso = "—",
        simTypeSummary = "—",
        simDetailRowsVisible = false,
        isVpn = false,
        isMeteredConnection = false,
        isRoaming = false,
        downstreamKbps = 0,
        upstreamKbps = 0,
        wifiSupportedBandsSummary = "—",
        wifiLinkSpeedSummary = "—",
        wifiSignalSummary = "—",
        cellularSignalSummary = "—",
        internetValidated = null,
        captivePortal = null,
        multiNetworkSummary = "—",
        wifiNetworkRowsVisible = false,
        storageSummary = "—",
        externalStorageVolumes = emptyList(),
        ramSummary = "—",
        deviceName = "—",
        deviceCodename = "—",
        cpuAbi = "—",
        cpuInfo = "—",
        cpuFreq = "—",
        cpuCurrentFreq = "—",
        gpuRenderer = "—",
        gpuGlVersion = "—",
        bootloader = "—",
        kernelVersion = "—",
        manufacturer = "Unknown",
        model = "Unknown",
        androidVersion = "Unknown",
        securityPatch = "Unknown",
        buildId = "Unknown",
        isPowerSaveMode = false,
        cameraSummary = "—",
        nfcSummary = "—",
        usbSummary = "—",
        audioSummary = "—",
        biometricSummary = "—",
        thermalSummary = "—",
    )
}
