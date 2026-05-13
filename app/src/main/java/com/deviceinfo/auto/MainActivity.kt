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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val items: MutableList<InfoItem> = mutableListOf()
    private var adapter: InfoAdapter? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var networkCallbackRegistered = false

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
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        controller.isAppearanceLightStatusBars = !isNightMode
        controller.isAppearanceLightNavigationBars = !isNightMode

        setContentView(R.layout.activity_main)
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

    private fun applyStatusBarInset() {
        val root = findViewById<View>(R.id.root)
        val initialPaddingTop = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sysBars = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(
                v.paddingLeft,
                initialPaddingTop + sysBars.top,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }
    }

    override fun onResume() {
        super.onResume()
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
        handler.removeCallbacks(memoryRefreshRunnable)
        unregisterReceiver(batteryReceiver)
        unregisterNetworkCallback()
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

            fun rowForKey(key: String): InfoItem.Row = when (key) {
                DeviceInfoUiShared.Row.LEVEL ->
                    InfoItem.Row(key, DeviceInfoUiShared.levelText(info), DeviceInfoUiShared.levelIconRes(info.batteryLevel, info.isPowerSaveMode))
                DeviceInfoUiShared.Row.PLUGGED ->
                    InfoItem.Row(key, info.plugged, DeviceInfoUiShared.pluggedIconRes(info.plugged))
                DeviceInfoUiShared.Row.HEALTH ->
                    InfoItem.Row(key, info.health, DeviceInfoUiShared.healthIconRes(info.health))
                DeviceInfoUiShared.Row.TECHNOLOGY ->
                    InfoItem.Row(key, info.technology, R.drawable.ic_row_battery_full)
                DeviceInfoUiShared.Row.TEMPERATURE ->
                    InfoItem.Row(key, String.format("%.1f °C", info.batteryTemperatureCelsius), DeviceInfoUiShared.temperatureIconRes(info.batteryTemperatureCelsius))
                DeviceInfoUiShared.Row.VOLTAGE ->
                    InfoItem.Row(key, String.format("%.2f V", info.batteryVoltageV), R.drawable.ic_row_voltage)
                DeviceInfoUiShared.Row.CURRENT ->
                    InfoItem.Row(key, DeviceInfoUiShared.currentText(info.currentNowMicroA), R.drawable.ic_row_current)
                DeviceInfoUiShared.Row.CHARGE_COUNTER ->
                    InfoItem.Row(key, DeviceInfoUiShared.chargeCounterText(info.chargeCounterMicroAh), R.drawable.ic_row_charge_counter)
                DeviceInfoUiShared.Row.CYCLE_COUNT ->
                    InfoItem.Row(key, DeviceInfoUiShared.cycleCountText(info.cycleCount), R.drawable.ic_row_cycle_count)
                DeviceInfoUiShared.Row.RESOLUTION ->
                    InfoItem.Row(key, info.displayResolution, R.drawable.ic_row_model)
                DeviceInfoUiShared.Row.DENSITY ->
                    InfoItem.Row(key, if (info.screenDensityDpi > 0) "${info.screenDensityDpi} dpi" else "—", R.drawable.ic_row_density)
                DeviceInfoUiShared.Row.REFRESH_RATE ->
                    InfoItem.Row(key, String.format("%.0f Hz", info.displayRefreshRateHz), R.drawable.ic_row_refresh_rate)
                DeviceInfoUiShared.Row.STORAGE ->
                    InfoItem.Row(key, info.storageSummary, R.drawable.ic_row_storage)
                DeviceInfoUiShared.Row.RAM ->
                    InfoItem.Row(key, info.ramSummary, R.drawable.ic_row_memory)
                DeviceInfoUiShared.Row.CONNECTION ->
                    InfoItem.Row(key, info.connectionType, DeviceInfoUiShared.connectionIconRes(info.connectionType))
                DeviceInfoUiShared.Row.CELLULAR ->
                    InfoItem.Row(key, info.cellularType, R.drawable.ic_row_cell_tower)
                DeviceInfoUiShared.Row.CARRIER ->
                    InfoItem.Row(key, info.simCarrier, R.drawable.ic_row_sim_card)
                DeviceInfoUiShared.Row.SIM_STATE ->
                    InfoItem.Row(key, info.simState, R.drawable.ic_row_sim_card)
                DeviceInfoUiShared.Row.SIM_COUNTRY ->
                    InfoItem.Row(key, info.simCountryIso, R.drawable.ic_row_language)
                DeviceInfoUiShared.Row.SIM_TYPE ->
                    InfoItem.Row(key, info.simTypeSummary, R.drawable.ic_row_sim_card)
                DeviceInfoUiShared.Row.BLUETOOTH ->
                    InfoItem.Row(key, info.bluetoothOn?.let { if (it) "On" else "Off" } ?: "—", R.drawable.ic_row_bluetooth)
                DeviceInfoUiShared.Row.VPN ->
                    InfoItem.Row(key, if (info.isVpn) "Yes" else "No", R.drawable.ic_row_vpn)
                DeviceInfoUiShared.Row.METERED ->
                    InfoItem.Row(key, if (info.isMeteredConnection) "Yes" else "No", R.drawable.ic_row_data_usage)
                DeviceInfoUiShared.Row.ROAMING ->
                    InfoItem.Row(key, if (info.isRoaming) "Yes" else "No", R.drawable.ic_row_roaming)
                DeviceInfoUiShared.Row.BANDWIDTH ->
                    InfoItem.Row(
                        key,
                        "${DeviceInfoUiShared.formatBandwidthKbps(info.downstreamKbps)} / ${DeviceInfoUiShared.formatBandwidthKbps(info.upstreamKbps)}",
                        R.drawable.ic_row_speed
                    )
                DeviceInfoUiShared.Row.DEVICE_NAME ->
                    InfoItem.Row(key, info.deviceName, R.drawable.ic_row_model)
                DeviceInfoUiShared.Row.MODEL ->
                    InfoItem.Row(key, info.model, R.drawable.ic_row_model)
                DeviceInfoUiShared.Row.MANUFACTURER ->
                    InfoItem.Row(key, info.manufacturer, R.drawable.ic_row_manufacturer)
                DeviceInfoUiShared.Row.CODENAME ->
                    InfoItem.Row(key, info.deviceCodename, R.drawable.ic_row_codename)
                DeviceInfoUiShared.Row.SYSTEM ->
                    InfoItem.Row(key, info.androidVersion, R.drawable.ic_row_android)
                DeviceInfoUiShared.Row.SECURITY_PATCH ->
                    InfoItem.Row(key, info.securityPatch, R.drawable.ic_row_shield)
                DeviceInfoUiShared.Row.BOOTLOADER ->
                    InfoItem.Row(key, info.bootloader, R.drawable.ic_row_bootloader)
                DeviceInfoUiShared.Row.KERNEL ->
                    InfoItem.Row(key, info.kernelVersion, R.drawable.ic_row_terminal)
                DeviceInfoUiShared.Row.BUILD ->
                    InfoItem.Row(key, info.buildId, R.drawable.ic_row_build)
                DeviceInfoUiShared.Row.CPU ->
                    InfoItem.Row(key, info.cpuInfo, R.drawable.ic_row_memory)
                DeviceInfoUiShared.Row.CPU_FREQ ->
                    InfoItem.Row(key, info.cpuFreq, R.drawable.ic_row_speed)
                DeviceInfoUiShared.Row.CPU_CURRENT ->
                    InfoItem.Row(key, info.cpuCurrentFreq, R.drawable.ic_row_speed)
                DeviceInfoUiShared.Row.CPU_ABI ->
                    InfoItem.Row(key, info.cpuAbi, R.drawable.ic_row_architecture)
                DeviceInfoUiShared.Row.GPU ->
                    InfoItem.Row(key, info.gpuRenderer, R.drawable.ic_row_memory)
                DeviceInfoUiShared.Row.OPENGL_ES ->
                    InfoItem.Row(key, info.gpuGlVersion, R.drawable.ic_row_extension)
                else -> InfoItem.Row(key, "—", R.drawable.ic_row_unknown)
            }

            val phoneSchema = listOf(
                DeviceInfoUiShared.Section.BATTERY to listOf(
                    DeviceInfoUiShared.Row.LEVEL,
                    DeviceInfoUiShared.Row.PLUGGED,
                    DeviceInfoUiShared.Row.HEALTH,
                    DeviceInfoUiShared.Row.TEMPERATURE,
                    DeviceInfoUiShared.Row.VOLTAGE,
                    DeviceInfoUiShared.Row.CURRENT,
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
                    DeviceInfoUiShared.Row.CPU_CURRENT,
                    DeviceInfoUiShared.Row.CPU_ABI,
                    DeviceInfoUiShared.Row.GPU,
                    DeviceInfoUiShared.Row.OPENGL_ES
                )
            )
            for ((sectionTitle, rows) in phoneSchema) {
                items += InfoItem.Section(sectionTitle)
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
                when (item.title) {
                    DeviceInfoUiShared.Row.LEVEL,
                    DeviceInfoUiShared.Row.PLUGGED,
                    DeviceInfoUiShared.Row.HEALTH,
                    DeviceInfoUiShared.Row.TECHNOLOGY,
                    DeviceInfoUiShared.Row.TEMPERATURE,
                    DeviceInfoUiShared.Row.VOLTAGE,
                    DeviceInfoUiShared.Row.CURRENT,
                    DeviceInfoUiShared.Row.CHARGE_COUNTER,
                    DeviceInfoUiShared.Row.CYCLE_COUNT ->
                        if (updateBattery) {
                            items[index] = when (item.title) {
                                DeviceInfoUiShared.Row.LEVEL -> item.copy(
                                    value = DeviceInfoUiShared.levelText(info),
                                    iconResId = DeviceInfoUiShared.levelIconRes(info.batteryLevel, info.isPowerSaveMode)
                                )
                                DeviceInfoUiShared.Row.PLUGGED -> item.copy(value = info.plugged, iconResId = DeviceInfoUiShared.pluggedIconRes(info.plugged))
                                DeviceInfoUiShared.Row.HEALTH -> item.copy(value = info.health, iconResId = DeviceInfoUiShared.healthIconRes(info.health))
                                DeviceInfoUiShared.Row.TECHNOLOGY -> item.copy(value = info.technology, iconResId = R.drawable.ic_row_battery_full)
                                DeviceInfoUiShared.Row.TEMPERATURE -> item.copy(
                                    value = String.format("%.1f °C", info.batteryTemperatureCelsius),
                                    iconResId = DeviceInfoUiShared.temperatureIconRes(info.batteryTemperatureCelsius)
                                )
                                DeviceInfoUiShared.Row.VOLTAGE -> item.copy(
                                    value = String.format("%.2f V", info.batteryVoltageV),
                                    iconResId = R.drawable.ic_row_voltage
                                )
                                DeviceInfoUiShared.Row.CURRENT -> item.copy(value = DeviceInfoUiShared.currentText(info.currentNowMicroA), iconResId = R.drawable.ic_row_current)
                                DeviceInfoUiShared.Row.CHARGE_COUNTER -> item.copy(value = DeviceInfoUiShared.chargeCounterText(info.chargeCounterMicroAh), iconResId = R.drawable.ic_row_charge_counter)
                                DeviceInfoUiShared.Row.CYCLE_COUNT -> item.copy(value = DeviceInfoUiShared.cycleCountText(info.cycleCount), iconResId = R.drawable.ic_row_cycle_count)
                                else -> item
                            }
                            adapter?.notifyItemChanged(index)
                        }

                    DeviceInfoUiShared.Row.STORAGE, DeviceInfoUiShared.Row.RAM ->
                        if (updateMemory) {
                            when (item.title) {
                                DeviceInfoUiShared.Row.STORAGE -> {
                                    items[index] = item.copy(
                                        value = info.storageSummary,
                                        iconResId = R.drawable.ic_row_storage
                                    )
                                }

                                DeviceInfoUiShared.Row.RAM -> {
                                    items[index] = item.copy(
                                        value = info.ramSummary,
                                        iconResId = R.drawable.ic_row_memory
                                    )
                                }
                            }
                            adapter?.notifyItemChanged(index)
                        }

                    DeviceInfoUiShared.Row.CPU_CURRENT ->
                        if (updateProcessor) {
                            items[index] = item.copy(value = info.cpuCurrentFreq, iconResId = R.drawable.ic_row_speed)
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
                            items[index] = when (item.title) {
                                DeviceInfoUiShared.Row.CONNECTION -> item.copy(value = info.connectionType, iconResId = DeviceInfoUiShared.connectionIconRes(info.connectionType))
                                DeviceInfoUiShared.Row.BLUETOOTH -> item.copy(value = info.bluetoothOn?.let { if (it) "On" else "Off" } ?: "—", iconResId = R.drawable.ic_row_bluetooth)
                                DeviceInfoUiShared.Row.CELLULAR -> item.copy(value = info.cellularType, iconResId = R.drawable.ic_row_cell_tower)
                                DeviceInfoUiShared.Row.CARRIER -> item.copy(value = info.simCarrier, iconResId = R.drawable.ic_row_sim_card)
                                DeviceInfoUiShared.Row.SIM_STATE -> item.copy(value = info.simState, iconResId = R.drawable.ic_row_sim_card)
                                DeviceInfoUiShared.Row.SIM_COUNTRY -> item.copy(value = info.simCountryIso, iconResId = R.drawable.ic_row_language)
                                DeviceInfoUiShared.Row.SIM_TYPE -> item.copy(value = info.simTypeSummary, iconResId = R.drawable.ic_row_sim_card)
                                DeviceInfoUiShared.Row.VPN -> item.copy(value = if (info.isVpn) "Yes" else "No", iconResId = R.drawable.ic_row_vpn)
                                DeviceInfoUiShared.Row.METERED -> item.copy(value = if (info.isMeteredConnection) "Yes" else "No", iconResId = R.drawable.ic_row_data_usage)
                                DeviceInfoUiShared.Row.ROAMING -> item.copy(value = if (info.isRoaming) "Yes" else "No", iconResId = R.drawable.ic_row_roaming)
                                DeviceInfoUiShared.Row.BANDWIDTH -> item.copy(
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
    data class Section(val title: String) : InfoItem()
    data class Row(val title: String, val value: String, val iconResId: Int) : InfoItem()
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
            is InfoItem.Section -> (holder as SectionViewHolder).bind(item.title)
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
