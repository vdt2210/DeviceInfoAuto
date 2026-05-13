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
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionInfo
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
import android.provider.Settings
import java.io.File

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
    val displayResolution: String,
    val displayRefreshRateHz: Float,
    val screenDensityDpi: Int,
    val connectionType: String,
    val bluetoothOn: Boolean?,
    val cellularType: String,
    val simCarrier: String,
    val simState: String,
    val simCountryIso: String,
    val simTypeSummary: String,
    val isVpn: Boolean,
    val isMeteredConnection: Boolean,
    val isRoaming: Boolean,
    val downstreamKbps: Int,
    val upstreamKbps: Int,
    val storageSummary: String,
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
    val isPowerSaveMode: Boolean
)

object DeviceInfoProvider {

    /** EGL_OPENGL_ES3_BIT — not exposed on all Android EGL14 stubs; value per Khronos EGL 1.5. */
    private const val EGL_OPENGL_ES3_BIT = 0x00000040

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

    /**
     * Some toolchains / compile paths lack [TelephonyManager.getTelephonyDisplayInfo] in stubs.
     * Reflection keeps release compile working while still reading override on API 31+ devices.
     */
    private fun readDisplayOverrideNetworkType(tm: TelephonyManager): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return runCatching {
            val m = TelephonyManager::class.java.getMethod("getTelephonyDisplayInfo")
            val tdi = m.invoke(tm) ?: return null
            reflectInvokeInt(tdi, "getOverrideNetworkType")
        }.getOrNull()
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

    fun get(context: Context): DeviceInfo {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return createEmpty()

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

        val cycleCount = readBatteryCycleCount(batteryIntent)

        // In Android Auto/Car host, app context may not be associated with a visual display.
        // Accessing context.display there can throw UnsupportedOperationException.
        val metrics = DisplayMetrics()
        val displayRefreshRateHz = try {
            val display = context.display
            if (display != null) {
                display.getRealMetrics(metrics)
                display.refreshRate
            } else {
                metrics.setTo(context.resources.displayMetrics)
                0f
            }
        } catch (_: Exception) {
            metrics.setTo(context.resources.displayMetrics)
            0f
        }
        val displayResolution = "${metrics.widthPixels} x ${metrics.heightPixels}"
        val screenDensityDpi = metrics.densityDpi

        val hasNetworkPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

        var connectionType = "Unknown"
        var isVpn = false
        var isMeteredConnection = false
        var isRoaming = false
        var downstreamKbps = 0
        var upstreamKbps = 0

        if (hasNetworkPermission) {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivity != null) {
                val active = connectivity.activeNetwork
                val caps = active?.let { connectivity.getNetworkCapabilities(it) }
                if (caps != null) {
                    connectionType = when {
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
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
                } else {
                    connectionType = "Offline"
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
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                    if (tm != null) {
                        val sm = context.getSystemService(SubscriptionManager::class.java)
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
                        cellularType = if (simUi.hasActiveSubscriptions) {
                            resolveCellularNetworkLabel(dataTm)
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

        val storageSummary = formatFreeOf(freeGB, totalGB)
        val ramSummary = formatFreeOf(ramAvailGB, ramTotalGB)

        val deviceName = resolveUserDeviceName(context)
        val deviceCodename = Build.DEVICE.ifEmpty { "—" }
        val cpuAbi = Build.SUPPORTED_ABIS?.joinToString(", ")?.ifEmpty { "—" } ?: "—"
        val bootloader = Build.BOOTLOADER.ifEmpty { "—" }
        val cpuInfo = readCpuInfo()
        val cpuFreq = readCpuFreq()
        val cpuCurrentFreq = readCpuCurrentFreq()
        val gpuRenderer = readGpuRenderer()
        val gpuGlVersion = getGlEsVersion(context)
        val kernelVersion = readKernelVersion()
        val manufacturer = Build.MANUFACTURER.ifEmpty { "Unknown" }
        val model = Build.MODEL.ifEmpty { "Unknown" }
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val securityPatch = Build.VERSION.SECURITY_PATCH.ifEmpty { "Unknown" }
        val buildId = Build.DISPLAY.ifEmpty { Build.ID }.ifEmpty { "Unknown" }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaveMode = powerManager?.isPowerSaveMode == true

        return DeviceInfo(
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
            displayResolution = displayResolution,
            displayRefreshRateHz = displayRefreshRateHz,
            screenDensityDpi = screenDensityDpi,
            connectionType = connectionType,
            bluetoothOn = bluetoothOn,
            cellularType = cellularType,
            simCarrier = simCarrier,
            simState = simState,
            simCountryIso = simCountryIso,
            simTypeSummary = simTypeSummary,
            isVpn = isVpn,
            isMeteredConnection = isMeteredConnection,
            isRoaming = isRoaming,
            downstreamKbps = downstreamKbps,
            upstreamKbps = upstreamKbps,
            storageSummary = storageSummary,
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
            isPowerSaveMode = isPowerSaveMode
        )
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
     * Use TelephonyDisplayInfo (API 31+) and ServiceState NR state (API 29+) when possible.
     */
    private fun resolveCellularNetworkLabel(dataTm: TelephonyManager): String {
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
            gb >= 0.1 -> String.format("%.1f GB", gb)
            gb >= 1.0 / 1024 -> String.format("%.0f MB", gb * 1024)
            gb >= 1.0 / (1024 * 1024) -> String.format("%.0f KB", gb * 1024 * 1024)
            else -> String.format("%.0f B", gb * 1024 * 1024 * 1024)
        }

    private fun formatFreeOf(freeGb: Double, totalGb: Double): String =
        "${formatSize(freeGb)} free of ${formatSize(totalGb)}"

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
            }.joinToString(" · ")
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
        displayResolution = "Unknown",
        displayRefreshRateHz = 0f,
        screenDensityDpi = 0,
        connectionType = "Unknown",
        bluetoothOn = null,
        cellularType = "—",
        simCarrier = "—",
        simState = "—",
        simCountryIso = "—",
        simTypeSummary = "—",
        isVpn = false,
        isMeteredConnection = false,
        isRoaming = false,
        downstreamKbps = 0,
        upstreamKbps = 0,
        storageSummary = "—",
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
        isPowerSaveMode = false
    )
}
