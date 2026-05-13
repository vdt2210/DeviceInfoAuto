package com.deviceinfo.auto

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val items: MutableList<InfoItem> = mutableListOf()
    private var adapter: InfoAdapter? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var networkCallbackRegistered = false

    /** Snapshot from [onPause]; used to detect language/theme changes after returning from Settings. */
    private var configSnapshotAtPause: Pair<String, AppPreferences.ThemeMode>? = null

    private lateinit var sensitivePermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        sensitivePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            val info = DeviceInfoProvider.get(this)
            adapter = null
            showDeviceInfoList(info)
        }
        super.onCreate(savedInstanceState)
        applyTransparentSystemBars()
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.app_name)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh)
        swipeRefreshLayout?.apply {
            setColorSchemeColors(obtainStyledColor(androidx.appcompat.R.attr.colorAccent))
            setProgressBackgroundColorSchemeColor(obtainStyledColor(android.R.attr.colorBackground))

            setOnRefreshListener {
                val info = DeviceInfoProvider.get(this@MainActivity)
                adapter = null
                showDeviceInfoList(info)
                isRefreshing = false
            }
        }
        applyStatusBarInset()
        showDeviceInfoList()
        requestMissingSensitivePermissionsIfNeeded()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        R.id.menu_app_info -> {
            startActivity(Intent(this, AppInfoActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun requestMissingSensitivePermissionsIfNeeded() {
        val need = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
            ) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
        }
        val denied = need.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (denied.isEmpty()) return
        if (launchedSensitivePermissionRequestThisProcess) return
        launchedSensitivePermissionRequestThisProcess = true
        sensitivePermissionLauncher.launch(denied.toTypedArray())
    }

    /** Posted after resume so back transition is not blocked by list refresh / receivers. */
    private val resumeAfterVisibleRunnable = Runnable {
        if (isDestroyed) return@Runnable
        resumeVisibleUpdates()
    }

    private fun applyStatusBarInset() {
        findViewById<View>(R.id.root).applyLegacyStatusBarInset()
    }

    override fun onResume() {
        super.onResume()
        val snap = configSnapshotAtPause
        configSnapshotAtPause = null
        if (snap != null) {
            val prefs = AppPreferences(this)
            if (snap.first != prefs.getLanguageTag() || snap.second != prefs.getThemeMode()) {
                recreate()
                return
            }
        }
        val decor = window.decorView
        decor.removeCallbacks(resumeAfterVisibleRunnable)
        decor.post(resumeAfterVisibleRunnable)
    }

    private fun resumeVisibleUpdates() {
        if (adapter != null) {
            val info = DeviceInfoProvider.get(this)
            updateDynamic(
                info,
                updateBattery = true,
                updateMemory = true,
                updateProcessor = true,
                updateNetwork = true
            )
        }
        registerBatteryReceiver()
        registerNetworkCallback()
        handler.post(memoryRefreshRunnable)
    }

    override fun onPause() {
        window.decorView.removeCallbacks(resumeAfterVisibleRunnable)
        handler.removeCallbacks(memoryRefreshRunnable)
        runCatching { unregisterReceiver(batteryReceiver) }
        unregisterNetworkCallback()
        AppPreferences(this).let { configSnapshotAtPause = it.getLanguageTag() to it.getThemeMode() }
        super.onPause()
    }

    private fun showDeviceInfoList() {
        val info = DeviceInfoProvider.get(this)
        showDeviceInfoList(info)
    }

    private fun showDeviceInfoList(info: DeviceInfo) {

        if (adapter == null) {
            // Build static + initial dynamic content once
            items.clear()

            fun mkRow(key: String, value: String, icon: Int) = InfoItem.Row(
                key,
                DeviceInfoUiShared.rowTitle(this@MainActivity, key),
                value,
                icon
            )

            fun rowForKey(key: String): InfoItem.Row = when (key) {
                DeviceInfoUiShared.Row.LEVEL ->
                    mkRow(key, DeviceInfoUiShared.levelText(this@MainActivity, info), DeviceInfoUiShared.levelIconRes(info.batteryLevel, info.isPowerSaveMode))
                DeviceInfoUiShared.Row.PLUGGED ->
                    mkRow(key, DeviceInfoUiShared.pluggedLabel(this@MainActivity, info.plugged), DeviceInfoUiShared.pluggedIconRes(info.plugged))
                DeviceInfoUiShared.Row.HEALTH ->
                    mkRow(key, DeviceInfoUiShared.healthLabel(this@MainActivity, info.health), DeviceInfoUiShared.healthIconRes(info.health))
                DeviceInfoUiShared.Row.TECHNOLOGY ->
                    mkRow(key, DeviceInfoUiShared.technologyLabel(this@MainActivity, info.technology), R.drawable.ic_row_battery_full)
                DeviceInfoUiShared.Row.TEMPERATURE ->
                    mkRow(key, String.format("%.1f °C", info.batteryTemperatureCelsius), DeviceInfoUiShared.temperatureIconRes(info.batteryTemperatureCelsius))
                DeviceInfoUiShared.Row.VOLTAGE ->
                    mkRow(key, String.format("%.2f V", info.batteryVoltageV), R.drawable.ic_row_voltage)
                DeviceInfoUiShared.Row.CURRENT ->
                    mkRow(key, DeviceInfoUiShared.currentText(info.currentNowMicroA), R.drawable.ic_row_current)
                DeviceInfoUiShared.Row.POWER ->
                    mkRow(
                        key,
                        DeviceInfoUiShared.instantPowerText(info.batteryVoltageV, info.currentNowMicroA),
                        R.drawable.ic_row_current
                    )
                DeviceInfoUiShared.Row.CHARGE_COUNTER ->
                    mkRow(key, DeviceInfoUiShared.chargeCounterText(info.chargeCounterMicroAh), R.drawable.ic_row_charge_counter)
                DeviceInfoUiShared.Row.CYCLE_COUNT ->
                    mkRow(key, DeviceInfoUiShared.cycleCountText(info.cycleCount), R.drawable.ic_row_cycle_count)
                DeviceInfoUiShared.Row.RESOLUTION ->
                    mkRow(key, DeviceInfoUiShared.resolutionDisplay(this@MainActivity, info.displayResolution), R.drawable.ic_row_model)
                DeviceInfoUiShared.Row.DENSITY ->
                    mkRow(key, DeviceInfoUiShared.densityLabel(info.screenDensityDpi), R.drawable.ic_row_density)
                DeviceInfoUiShared.Row.REFRESH_RATE ->
                    mkRow(key, String.format("%.0f Hz", info.displayRefreshRateHz), R.drawable.ic_row_refresh_rate)
                DeviceInfoUiShared.Row.STORAGE ->
                    mkRow(key, info.storageSummary, R.drawable.ic_row_storage)
                DeviceInfoUiShared.Row.RAM ->
                    mkRow(key, info.ramSummary, R.drawable.ic_row_memory)
                DeviceInfoUiShared.Row.CONNECTION ->
                    mkRow(key, DeviceInfoUiShared.connectionLabel(this@MainActivity, info.connectionType), DeviceInfoUiShared.connectionIconRes(info.connectionType))
                DeviceInfoUiShared.Row.CELLULAR ->
                    mkRow(key, info.cellularType, R.drawable.ic_row_cell_tower)
                DeviceInfoUiShared.Row.CARRIER ->
                    mkRow(key, info.simCarrier, R.drawable.ic_row_sim_card)
                DeviceInfoUiShared.Row.SIM_STATE ->
                    mkRow(key, DeviceInfoUiShared.simStateSummaryDisplay(this@MainActivity, info.simState), R.drawable.ic_row_sim_card)
                DeviceInfoUiShared.Row.SIM_COUNTRY ->
                    mkRow(key, info.simCountryIso, R.drawable.ic_row_language)
                DeviceInfoUiShared.Row.SIM_TYPE ->
                    mkRow(key, DeviceInfoUiShared.simTypeSummaryDisplay(this@MainActivity, info.simTypeSummary), R.drawable.ic_row_sim_card)
                DeviceInfoUiShared.Row.BLUETOOTH ->
                    mkRow(key, DeviceInfoUiShared.bluetoothState(this@MainActivity, info.bluetoothOn), R.drawable.ic_row_bluetooth)
                DeviceInfoUiShared.Row.VPN ->
                    mkRow(key, DeviceInfoUiShared.yesNo(this@MainActivity, info.isVpn), R.drawable.ic_row_vpn)
                DeviceInfoUiShared.Row.METERED ->
                    mkRow(key, DeviceInfoUiShared.yesNo(this@MainActivity, info.isMeteredConnection), R.drawable.ic_row_data_usage)
                DeviceInfoUiShared.Row.ROAMING ->
                    mkRow(key, DeviceInfoUiShared.yesNo(this@MainActivity, info.isRoaming), R.drawable.ic_row_roaming)
                DeviceInfoUiShared.Row.BANDWIDTH ->
                    mkRow(
                        key,
                        "${DeviceInfoUiShared.formatBandwidthKbps(info.downstreamKbps)} / ${DeviceInfoUiShared.formatBandwidthKbps(info.upstreamKbps)}",
                        R.drawable.ic_row_speed
                    )
                DeviceInfoUiShared.Row.DEVICE_NAME ->
                    mkRow(key, info.deviceName, R.drawable.ic_row_model)
                DeviceInfoUiShared.Row.MODEL ->
                    mkRow(key, DeviceInfoUiShared.unknownWord(this@MainActivity, info.model), R.drawable.ic_row_model)
                DeviceInfoUiShared.Row.MANUFACTURER ->
                    mkRow(key, DeviceInfoUiShared.unknownWord(this@MainActivity, info.manufacturer), R.drawable.ic_row_manufacturer)
                DeviceInfoUiShared.Row.CODENAME ->
                    mkRow(key, info.deviceCodename, R.drawable.ic_row_codename)
                DeviceInfoUiShared.Row.SYSTEM ->
                    mkRow(key, DeviceInfoUiShared.systemVersionDisplay(this@MainActivity, info), R.drawable.ic_row_android)
                DeviceInfoUiShared.Row.SECURITY_PATCH ->
                    mkRow(key, DeviceInfoUiShared.unknownWord(this@MainActivity, info.securityPatch), R.drawable.ic_row_shield)
                DeviceInfoUiShared.Row.BOOTLOADER ->
                    mkRow(key, DeviceInfoUiShared.unknownWord(this@MainActivity, info.bootloader), R.drawable.ic_row_bootloader)
                DeviceInfoUiShared.Row.KERNEL ->
                    mkRow(key, info.kernelVersion, R.drawable.ic_row_terminal)
                DeviceInfoUiShared.Row.BUILD ->
                    mkRow(key, DeviceInfoUiShared.unknownWord(this@MainActivity, info.buildId), R.drawable.ic_row_build)
                DeviceInfoUiShared.Row.CPU ->
                    mkRow(key, info.cpuInfo, R.drawable.ic_row_memory)
                DeviceInfoUiShared.Row.CPU_FREQ ->
                    mkRow(key, info.cpuFreq, R.drawable.ic_row_speed)
                DeviceInfoUiShared.Row.CPU_FREQ_CURRENT ->
                    mkRow(key, info.cpuCurrentFreq, R.drawable.ic_row_speed)
                DeviceInfoUiShared.Row.CPU_ABI ->
                    mkRow(key, info.cpuAbi, R.drawable.ic_row_architecture)
                DeviceInfoUiShared.Row.GPU ->
                    mkRow(key, info.gpuRenderer, R.drawable.ic_row_memory)
                DeviceInfoUiShared.Row.OPENGL_ES ->
                    mkRow(key, info.gpuGlVersion, R.drawable.ic_row_extension)
                else -> mkRow(key, "—", R.drawable.ic_row_unknown)
            }

            val phoneSchema = listOf(
                DeviceInfoUiShared.Section.BATTERY to listOf(
                    DeviceInfoUiShared.Row.LEVEL,
                    DeviceInfoUiShared.Row.PLUGGED,
                    DeviceInfoUiShared.Row.HEALTH,
                    DeviceInfoUiShared.Row.TEMPERATURE,
                    DeviceInfoUiShared.Row.VOLTAGE,
                    DeviceInfoUiShared.Row.CURRENT,
                    DeviceInfoUiShared.Row.POWER,
                    DeviceInfoUiShared.Row.CHARGE_COUNTER,
                    DeviceInfoUiShared.Row.CYCLE_COUNT,
                    DeviceInfoUiShared.Row.TECHNOLOGY
                ),
                DeviceInfoUiShared.Section.DISPLAY to listOf(
                    DeviceInfoUiShared.Row.RESOLUTION,
                    DeviceInfoUiShared.Row.DENSITY,
                    DeviceInfoUiShared.Row.REFRESH_RATE
                ),
                DeviceInfoUiShared.Section.STORAGE_MEMORY to listOf(
                    DeviceInfoUiShared.Row.STORAGE,
                    DeviceInfoUiShared.Row.RAM
                ),
                DeviceInfoUiShared.Section.NETWORK to listOf(
                    DeviceInfoUiShared.Row.CONNECTION,
                    DeviceInfoUiShared.Row.CELLULAR,
                    DeviceInfoUiShared.Row.CARRIER,
                    DeviceInfoUiShared.Row.SIM_STATE,
                    DeviceInfoUiShared.Row.SIM_COUNTRY,
                    DeviceInfoUiShared.Row.SIM_TYPE,
                    DeviceInfoUiShared.Row.BLUETOOTH,
                    DeviceInfoUiShared.Row.VPN,
                    DeviceInfoUiShared.Row.METERED,
                    DeviceInfoUiShared.Row.ROAMING,
                    DeviceInfoUiShared.Row.BANDWIDTH
                ),
                DeviceInfoUiShared.Section.DEVICE to listOf(
                    DeviceInfoUiShared.Row.DEVICE_NAME,
                    DeviceInfoUiShared.Row.MODEL,
                    DeviceInfoUiShared.Row.MANUFACTURER,
                    DeviceInfoUiShared.Row.CODENAME,
                    DeviceInfoUiShared.Row.SYSTEM,
                    DeviceInfoUiShared.Row.SECURITY_PATCH,
                    DeviceInfoUiShared.Row.BOOTLOADER,
                    DeviceInfoUiShared.Row.KERNEL,
                    DeviceInfoUiShared.Row.BUILD
                ),
                DeviceInfoUiShared.Section.PROCESSOR to listOf(
                    DeviceInfoUiShared.Row.CPU,
                    DeviceInfoUiShared.Row.CPU_FREQ,
                    DeviceInfoUiShared.Row.CPU_FREQ_CURRENT,
                    DeviceInfoUiShared.Row.CPU_ABI,
                    DeviceInfoUiShared.Row.GPU,
                    DeviceInfoUiShared.Row.OPENGL_ES
                )
            )
            for ((sectionKey, rows) in phoneSchema) {
                items += InfoItem.Section(sectionKey)
                rows.forEach { rowKey -> items += rowForKey(rowKey) }
            }
            val list = findViewById<RecyclerView>(R.id.list)
            list.layoutManager = LinearLayoutManager(this)
            list.itemAnimator = null
            adapter = InfoAdapter(items)
            list.adapter = adapter
        } else {
            updateDynamic(info, updateBattery = true, updateMemory = true, updateProcessor = true)
        }
    }

    private fun obtainStyledColor(attr: Int): Int {
        val typedArray = obtainStyledAttributes(intArrayOf(attr))
        return try {
            typedArray.getColor(0, Color.GRAY)
        } finally {
            typedArray.recycle()
        }
    }

    private fun updateDynamic(
        info: DeviceInfo,
        updateBattery: Boolean,
        updateMemory: Boolean,
        updateProcessor: Boolean = false,
        updateNetwork: Boolean = false
    ) {
        if (adapter == null) return

        for ((index, item) in items.withIndex()) {
            if (item is InfoItem.Row) {
                when (item.key) {
                    DeviceInfoUiShared.Row.LEVEL,
                    DeviceInfoUiShared.Row.PLUGGED,
                    DeviceInfoUiShared.Row.HEALTH,
                    DeviceInfoUiShared.Row.TECHNOLOGY,
                    DeviceInfoUiShared.Row.TEMPERATURE,
                    DeviceInfoUiShared.Row.VOLTAGE,
                    DeviceInfoUiShared.Row.CURRENT,
                    DeviceInfoUiShared.Row.POWER,
                    DeviceInfoUiShared.Row.CHARGE_COUNTER,
                    DeviceInfoUiShared.Row.CYCLE_COUNT ->
                        if (updateBattery) {
                            items[index] = when (item.key) {
                                DeviceInfoUiShared.Row.LEVEL -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.levelText(this, info),
                                    iconResId = DeviceInfoUiShared.levelIconRes(info.batteryLevel, info.isPowerSaveMode)
                                )
                                DeviceInfoUiShared.Row.PLUGGED -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.pluggedLabel(this, info.plugged),
                                    iconResId = DeviceInfoUiShared.pluggedIconRes(info.plugged)
                                )
                                DeviceInfoUiShared.Row.HEALTH -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.healthLabel(this, info.health),
                                    iconResId = DeviceInfoUiShared.healthIconRes(info.health)
                                )
                                DeviceInfoUiShared.Row.TECHNOLOGY -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.technologyLabel(this, info.technology),
                                    iconResId = R.drawable.ic_row_battery_full
                                )
                                DeviceInfoUiShared.Row.TEMPERATURE -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = String.format("%.1f °C", info.batteryTemperatureCelsius),
                                    iconResId = DeviceInfoUiShared.temperatureIconRes(info.batteryTemperatureCelsius)
                                )
                                DeviceInfoUiShared.Row.VOLTAGE -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = String.format("%.2f V", info.batteryVoltageV),
                                    iconResId = R.drawable.ic_row_voltage
                                )
                                DeviceInfoUiShared.Row.CURRENT -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.currentText(info.currentNowMicroA),
                                    iconResId = R.drawable.ic_row_current
                                )
                                DeviceInfoUiShared.Row.POWER -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.instantPowerText(info.batteryVoltageV, info.currentNowMicroA),
                                    iconResId = R.drawable.ic_row_current
                                )
                                DeviceInfoUiShared.Row.CHARGE_COUNTER -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.chargeCounterText(info.chargeCounterMicroAh),
                                    iconResId = R.drawable.ic_row_charge_counter
                                )
                                DeviceInfoUiShared.Row.CYCLE_COUNT -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.cycleCountText(info.cycleCount),
                                    iconResId = R.drawable.ic_row_cycle_count
                                )
                                else -> item
                            }
                            adapter?.notifyItemChanged(index)
                        }

                    DeviceInfoUiShared.Row.STORAGE, DeviceInfoUiShared.Row.RAM ->
                        if (updateMemory) {
                            when (item.key) {
                                DeviceInfoUiShared.Row.STORAGE -> {
                                    items[index] = item.copy(
                                        title = DeviceInfoUiShared.rowTitle(this, item.key),
                                        value = info.storageSummary,
                                        iconResId = R.drawable.ic_row_storage
                                    )
                                }

                                DeviceInfoUiShared.Row.RAM -> {
                                    items[index] = item.copy(
                                        title = DeviceInfoUiShared.rowTitle(this, item.key),
                                        value = info.ramSummary,
                                        iconResId = R.drawable.ic_row_memory
                                    )
                                }
                            }
                            adapter?.notifyItemChanged(index)
                        }

                    DeviceInfoUiShared.Row.CPU_FREQ_CURRENT ->
                        if (updateProcessor) {
                            items[index] = item.copy(
                                title = DeviceInfoUiShared.rowTitle(this, item.key),
                                value = info.cpuCurrentFreq,
                                iconResId = R.drawable.ic_row_speed
                            )
                            adapter?.notifyItemChanged(index)
                        }

                    DeviceInfoUiShared.Row.CONNECTION,
                    DeviceInfoUiShared.Row.BLUETOOTH,
                    DeviceInfoUiShared.Row.CELLULAR,
                    DeviceInfoUiShared.Row.CARRIER,
                    DeviceInfoUiShared.Row.SIM_STATE,
                    DeviceInfoUiShared.Row.SIM_COUNTRY,
                    DeviceInfoUiShared.Row.SIM_TYPE,
                    DeviceInfoUiShared.Row.VPN,
                    DeviceInfoUiShared.Row.METERED,
                    DeviceInfoUiShared.Row.ROAMING,
                    DeviceInfoUiShared.Row.BANDWIDTH ->
                        if (updateNetwork) {
                            items[index] = when (item.key) {
                                DeviceInfoUiShared.Row.CONNECTION -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.connectionLabel(this, info.connectionType),
                                    iconResId = DeviceInfoUiShared.connectionIconRes(info.connectionType)
                                )
                                DeviceInfoUiShared.Row.BLUETOOTH -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.bluetoothState(this, info.bluetoothOn),
                                    iconResId = R.drawable.ic_row_bluetooth
                                )
                                DeviceInfoUiShared.Row.CELLULAR -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = info.cellularType,
                                    iconResId = R.drawable.ic_row_cell_tower
                                )
                                DeviceInfoUiShared.Row.CARRIER -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = info.simCarrier,
                                    iconResId = R.drawable.ic_row_sim_card
                                )
                                DeviceInfoUiShared.Row.SIM_STATE -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.simStateSummaryDisplay(this, info.simState),
                                    iconResId = R.drawable.ic_row_sim_card
                                )
                                DeviceInfoUiShared.Row.SIM_COUNTRY -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = info.simCountryIso,
                                    iconResId = R.drawable.ic_row_language
                                )
                                DeviceInfoUiShared.Row.SIM_TYPE -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.simTypeSummaryDisplay(this, info.simTypeSummary),
                                    iconResId = R.drawable.ic_row_sim_card
                                )
                                DeviceInfoUiShared.Row.VPN -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.yesNo(this, info.isVpn),
                                    iconResId = R.drawable.ic_row_vpn
                                )
                                DeviceInfoUiShared.Row.METERED -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.yesNo(this, info.isMeteredConnection),
                                    iconResId = R.drawable.ic_row_data_usage
                                )
                                DeviceInfoUiShared.Row.ROAMING -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = DeviceInfoUiShared.yesNo(this, info.isRoaming),
                                    iconResId = R.drawable.ic_row_roaming
                                )
                                DeviceInfoUiShared.Row.BANDWIDTH -> item.copy(
                                    title = DeviceInfoUiShared.rowTitle(this, item.key),
                                    value = "${DeviceInfoUiShared.formatBandwidthKbps(info.downstreamKbps)} / ${DeviceInfoUiShared.formatBandwidthKbps(info.upstreamKbps)}",
                                    iconResId = R.drawable.ic_row_speed
                                )
                                else -> item
                            }
                            adapter?.notifyItemChanged(index)
                        }
                }
            }
        }
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(batteryReceiver, filter)
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        if (networkCallbackRegistered) return
        runCatching {
            cm.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
            networkCallbackRegistered = true
        }
    }

    private fun unregisterNetworkCallback() {
        if (!networkCallbackRegistered) return
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        runCatching {
            cm.unregisterNetworkCallback(networkCallback)
            networkCallbackRegistered = false
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED,
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val info = DeviceInfoProvider.get(this@MainActivity)
                    updateDynamic(info, updateBattery = true, updateMemory = false, updateProcessor = false, updateNetwork = false)
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val info = DeviceInfoProvider.get(this@MainActivity)
                    updateDynamic(info, updateBattery = false, updateMemory = false, updateProcessor = false, updateNetwork = true)
                }
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshNetwork()
        override fun onLost(network: Network) = refreshNetwork()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: android.net.NetworkCapabilities) = refreshNetwork()
        private fun refreshNetwork() {
            val info = DeviceInfoProvider.get(this@MainActivity)
            runOnUiThread {
                updateDynamic(info, updateBattery = false, updateMemory = false, updateProcessor = false, updateNetwork = true)
            }
        }
    }

    private val memoryRefreshRunnable = object : Runnable {
        override fun run() {
            val info = DeviceInfoProvider.get(this@MainActivity)
            updateDynamic(info, updateBattery = true, updateMemory = true, updateProcessor = true, updateNetwork = false)
            handler.postDelayed(this, MEMORY_REFRESH_MS)
        }
    }

    companion object {
        private const val MEMORY_REFRESH_MS = 5_000L
        private var launchedSensitivePermissionRequestThisProcess = false
    }
}

private sealed class InfoItem {
    data class Section(val key: String) : InfoItem()
    data class Row(val key: String, val title: String, val value: String, val iconResId: Int) : InfoItem()
}

private class InfoAdapter(
    private val items: List<InfoItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_SECTION = 0
        const val VIEW_TYPE_ROW = 1
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is InfoItem.Section -> VIEW_TYPE_SECTION
            is InfoItem.Row -> VIEW_TYPE_ROW
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_SECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section_header, parent, false)
                SectionViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_info_row, parent, false)
                RowViewHolder(view)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is InfoItem.Section -> (holder as SectionViewHolder).bind(
                DeviceInfoUiShared.sectionTitle(holder.itemView.context, item.key)
            )
            is InfoItem.Row -> {
                val isFirstInGroup = position > 0 && items[position - 1] is InfoItem.Section
                val isLastInGroup = position == items.size - 1 || items[position + 1] is InfoItem.Section
                val bgRes = when {
                    isFirstInGroup && isLastInGroup -> R.drawable.bg_group_item_single
                    isFirstInGroup -> R.drawable.bg_group_item_top
                    isLastInGroup -> R.drawable.bg_group_item_bottom
                    else -> R.drawable.bg_group_item_middle
                }
                (holder as RowViewHolder).bind(item.title, item.value, item.iconResId, bgRes)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.section_title)
        fun bind(text: String) { title.text = text }
    }

    class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val title: TextView = view.findViewById(R.id.title)
        private val value: TextView = view.findViewById(R.id.value)
        fun bind(t: String, v: String, iconResId: Int, bgResId: Int) {
            itemView.setBackgroundResource(bgResId)
            icon.setImageResource(iconResId)
            title.text = t
            value.text = v
        }
    }
}
