package com.deviceinfo.auto

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.ConcurrentHashMap

class MainActivity : AppCompatActivity() {

    private val viewModel: DeviceInfoViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val items: MutableList<InfoItem> = mutableListOf()
    private var listAdapter: InfoListAdapter? = null
    private var rowIndexByKey: Map<String, Int> = emptyMap()
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var scrollToTopContainer: View? = null
    private var scrollToTopFrost: ImageView? = null
    private var scrollToTopButton: ImageButton? = null
    private var networkCallbackRegistered = false
    private var displayListenerRegistered = false
    private var storageReceiverRegistered = false
    private var thermalListenerRegistered = false

    private var displayListener: DisplayManager.DisplayListener? = null
    private var thermalStatusListener: PowerManager.OnThermalStatusChangedListener? = null

    private data class LatestSensorEvent(val values: FloatArray, val sensor: Sensor)

    private val latestSensorEvents = ConcurrentHashMap<String, LatestSensorEvent>()
    private val sensorStreamListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val key = SensorRowFormat.rowKeyForSensorType(event.sensor.type) ?: return
            latestSensorEvents[key] = LatestSensorEvent(event.values.clone(), event.sensor)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    private val sensorUiRefreshRunnable = object : Runnable {
        override fun run() {
            applySensorRowUpdatesFromLatestEvents()
            handler.postDelayed(this, SENSOR_UI_REFRESH_INTERVAL_MS)
        }
    }

    /** Snapshot from [onPause]; used to detect language/theme changes after returning from Settings. */
    private var configSnapshotAtPause: Pair<String, AppPreferences.ThemeMode>? = null

    /** Match [DeviceInfo.simDetailRowsVisible]; rebuild when phone-state permission toggles the block. */
    private var listSimDetailRowsVisible: Boolean = false

    /** Row keys for [DeviceInfo.externalStorageVolumes]; rebuild list when mount set changes. */
    private var listExternalVolumeRowKeys: List<String> = emptyList()

    /** Tracks Wi‑Fi row block + SIM block for network section rebuild. */
    private var listNetworkSchemaSignature: Pair<Boolean, Boolean> = false to false

    private lateinit var sensitivePermissionLauncher: ActivityResultLauncher<Array<String>>

    /** One system dialog per activity instance; use Settings to ask again after deny. */
    private var sensitivePermissionPromptedThisInstance = false

    private var lastKnownDeniedPermissions: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        sensitivePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            listAdapter = null
            viewModel.loadFull()
        }
        super.onCreate(savedInstanceState)
        applyTransparentSystemBars()
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.app_name)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh)
        scrollToTopContainer = findViewById(R.id.scroll_to_top)
        scrollToTopFrost = findViewById(R.id.scroll_to_top_frost)
        scrollToTopButton = findViewById(R.id.scroll_to_top_button)
        val frostLayer = scrollToTopFrost
        val scrollContainer = scrollToTopContainer
        if (scrollContainer != null && frostLayer != null) {
            if (ScrollToTopBlurStyle.supportsLiveBlur()) {
                ScrollToTopBlurStyle.prepareBlur(scrollContainer, frostLayer)
                scrollContainer.setBackgroundResource(android.R.color.transparent)
            } else {
                frostLayer.visibility = View.GONE
                findViewById<View>(R.id.scroll_to_top_glass_overlay)?.visibility = View.GONE
                ScrollToTopBlurStyle.preparePlain(scrollContainer)
            }
        }
        scrollToTopButton?.setOnClickListener {
            findViewById<RecyclerView>(R.id.list).smoothScrollToPosition(0)
        }
        swipeRefreshLayout?.apply {
            setColorSchemeColors(obtainStyledColor(androidx.appcompat.R.attr.colorAccent))
            setProgressBackgroundColorSchemeColor(obtainStyledColor(android.R.attr.colorBackground))

            setOnRefreshListener {
                listAdapter = null
                viewModel.invalidateHardwareAndLoadFull()
                isRefreshing = false
            }
        }
        applyStatusBarInset()
        lastKnownDeniedPermissions = SensitivePermissions.denied(this).toSet()
        viewModel.uiEvent.observe(this) { event ->
            if (event == null) return@observe
            when (event) {
                is DeviceInfoUiEvent.FullList -> bindFullList(event.info)
                is DeviceInfoUiEvent.Refresh -> applyRefresh(event.info, event.flags)
            }
            viewModel.consumeEvent()
        }
        viewModel.invalidateHardwareAndLoadFull()
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
        val denied = SensitivePermissions.denied(this)
        if (denied.isEmpty()) return
        if (sensitivePermissionPromptedThisInstance) return
        sensitivePermissionPromptedThisInstance = true
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
        val deniedNow = SensitivePermissions.denied(this).toSet()
        val permissionsChanged = deniedNow != lastKnownDeniedPermissions
        lastKnownDeniedPermissions = deniedNow

        if (listAdapter != null) {
            if (permissionsChanged) {
                listAdapter = null
                viewModel.loadFull()
            } else {
                applyPhonePollUpdate()
            }
        }
        registerBatteryReceiver()
        registerBluetoothReceiver()
        registerNetworkCallback()
        registerDisplayListener()
        registerStorageReceiver()
        registerThermalListener()
        registerStreamSensors()
        startSensorUiRefresh()
        handler.post(phoneLivePollRunnable)
    }

    override fun onPause() {
        window.decorView.removeCallbacks(resumeAfterVisibleRunnable)
        handler.removeCallbacks(phoneLivePollRunnable)
        stopSensorUiRefresh()
        unregisterStreamSensors()
        runCatching { unregisterReceiver(batteryReceiver) }
        runCatching { unregisterReceiver(bluetoothReceiver) }
        runCatching { unregisterReceiver(storageReceiver) }
        storageReceiverRegistered = false
        unregisterDisplayListener()
        unregisterThermalListener()
        unregisterNetworkCallback()
        AppPreferences(this).let { configSnapshotAtPause = it.getLanguageTag() to it.getThemeMode() }
        super.onPause()
    }

    private fun bindFullList(info: DeviceInfo) {
        unregisterStreamSensors()
        latestSensorEvents.clear()
        val built = DeviceInfoPhoneListBuilder.build(
            context = this,
            info = info,
            displayRotation = currentDisplayRotationSurface(),
            sensorDisplayForRow = ::sensorDisplayForRow,
            sensorRowValueForUi = ::sensorRowValueForUi,
            notAvailableText = ::notAvailableText,
        )
        listSimDetailRowsVisible = info.simDetailRowsVisible
        listExternalVolumeRowKeys = info.externalStorageVolumes.map { it.rowKey }
        listNetworkSchemaSignature = info.wifiNetworkRowsVisible to info.simDetailRowsVisible

        val list = findViewById<RecyclerView>(R.id.list)
        if (listAdapter == null) {
            list.layoutManager = LinearLayoutManager(this)
            list.itemAnimator = null
            listAdapter = InfoListAdapter()
            list.adapter = listAdapter
            list.addOnScrollListener(scrollToTopScrollListener)
        }
        updateScrollToTopVisibility(list)
        scheduleScrollToTopFrostRefresh()
        publishItems(built)
        registerStreamSensors()
    }

    private fun publishItems(newItems: List<InfoItem>) {
        items.clear()
        items.addAll(newItems)
        rebuildRowIndex()
        listAdapter?.submitList(newItems.toList())
    }

    private fun rebuildRowIndex() {
        rowIndexByKey = buildMap {
            items.forEachIndexed { index, item ->
                if (item is InfoItem.Row) put(item.key, index)
            }
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

    private fun applyRefresh(info: DeviceInfo, flags: DeviceInfoUpdateFlags) {
        if (listAdapter == null) return
        if (flags.updateNetwork) {
            val schemaSig = info.wifiNetworkRowsVisible to info.simDetailRowsVisible
            if (info.simDetailRowsVisible != listSimDetailRowsVisible ||
                schemaSig != listNetworkSchemaSignature
            ) {
                listAdapter = null
                bindFullList(info)
                return
            }
        }
        if (flags.updateMemory) {
            val volumeKeys = info.externalStorageVolumes.map { it.rowKey }
            if (volumeKeys != listExternalVolumeRowKeys) {
                listAdapter = null
                bindFullList(info)
                return
            }
        }
        val rotation = currentDisplayRotationSurface()
        var changed = false
        val next = items.toMutableList()
        for (index in next.indices) {
            val item = next[index]
            if (item !is InfoItem.Row) continue
            if (!DeviceInfoPhoneRow.shouldRefresh(
                    item.key,
                    flags.updateBattery,
                    flags.updateMemory,
                    flags.updateProcessor,
                    flags.updateNetwork,
                    flags.updateDisplay,
                )
            ) {
                continue
            }
            val sensorValue = if (item.key.startsWith("row_sensor_")) {
                sensorRowValueForUi(item.key, sensorDisplayForRow(item.key))
            } else {
                ""
            }
            val ui = DeviceInfoPhoneRow.uiState(
                context = this,
                key = item.key,
                info = info,
                displayRotation = rotation,
                sensorDisplayValue = sensorValue,
                fallbackNotAvailable = notAvailableText(),
            ) ?: continue
            if (item.title == ui.title && item.value == ui.value && item.iconResId == ui.iconResId) continue
            next[index] = item.copy(title = ui.title, value = ui.value, iconResId = ui.iconResId)
            changed = true
        }
        if (changed) publishItems(next)
    }

    /** Em dash only when a default sensor exists; otherwise localized not_available via UI mapping. */
    private fun sensorRowValueForUi(rowKey: String, raw: String): String {
        if (raw != SENSOR_ROW_WAITING) return raw
        val type = SensorRowFormat.sensorTypeForRowKey(rowKey) ?: return notAvailableText()
        val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return notAvailableText()
        return if (sm.getDefaultSensor(type) != null) raw else notAvailableText()
    }

    private fun sensorDisplayForRow(rowKey: String): String {
        latestSensorEvents[rowKey]?.let { e ->
            return SensorRowFormat.format(e.sensor.type, e.values, e.sensor, this)
        }
        val type = SensorRowFormat.sensorTypeForRowKey(rowKey) ?: return notAvailableText()
        val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return notAvailableText()
        return if (sm.getDefaultSensor(type) != null) SENSOR_ROW_WAITING else notAvailableText()
    }

    private fun currentDisplayRotationSurface(): Int =
        try {
            window.decorView.display?.rotation
        } catch (_: Exception) {
            null
        } ?: @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation

    private fun startSensorUiRefresh() {
        handler.removeCallbacks(sensorUiRefreshRunnable)
        handler.post(sensorUiRefreshRunnable)
    }

    private fun stopSensorUiRefresh() {
        handler.removeCallbacks(sensorUiRefreshRunnable)
    }

    private fun applySensorRowUpdatesFromLatestEvents() {
        if (listAdapter == null) return
        var changed = false
        val next = items.toMutableList()
        for (rowKey in rowIndexByKey.keys) {
            if (!rowKey.startsWith("row_sensor_")) continue
            val index = rowIndexByKey[rowKey] ?: continue
            val item = next.getOrNull(index) as? InfoItem.Row ?: continue
            val display = sensorRowValueForUi(rowKey, sensorDisplayForRow(rowKey))
            if (display == item.value) continue
            next[index] = item.copy(
                title = DeviceInfoUiShared.rowTitle(this, rowKey),
                value = display,
                iconResId = R.drawable.ic_row_sensor,
            )
            changed = true
        }
        if (changed) publishItems(next)
    }

    private fun registerStreamSensors() {
        if (listAdapter == null) return
        unregisterStreamSensors()
        val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        for (t in SensorRowFormat.streamSensorTypes) {
            sm.getDefaultSensor(t)?.let { s ->
                sm.registerListener(sensorStreamListener, s, SensorManager.SENSOR_DELAY_UI, handler)
            }
        }
    }

    private fun unregisterStreamSensors() {
        val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        for (t in SensorRowFormat.streamSensorTypes) {
            sm.getDefaultSensor(t)?.let { s ->
                runCatching { sm.unregisterListener(sensorStreamListener, s) }
            }
        }
    }

    private fun refreshDisplayRotationRowItem() {
        val index = rowIndexByKey[DeviceInfoUiShared.Row.DISPLAY_ROTATION] ?: return
        val item = items.getOrNull(index) as? InfoItem.Row ?: return
        val label = DeviceInfoUiShared.displayRotationLabel(this, currentDisplayRotationSurface())
        if (item.value == label) return
        val next = items.toMutableList()
        next[index] = item.copy(
            title = DeviceInfoUiShared.rowTitle(this, item.key),
            value = label,
            iconResId = R.drawable.ic_row_model,
        )
        publishItems(next)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshDisplayRotationRowItem()
    }

    private fun applyPhonePollUpdate() {
        if (listAdapter == null) return
        viewModel.refresh(
            DeviceInfoProvider.GetOptions.PhonePoll,
            DeviceInfoUpdateFlags(
                updateMemory = true,
                updateProcessor = true,
                updateNetwork = true,
            ),
        )
    }

    private fun applyProcessorUpdate() {
        if (listAdapter == null) return
        viewModel.refresh(
            DeviceInfoProvider.GetOptions.Processor,
            DeviceInfoUpdateFlags(updateProcessor = true),
        )
    }

    private fun applyDisplayUpdate() {
        if (listAdapter == null) return
        viewModel.refresh(
            DeviceInfoProvider.GetOptions.Display,
            DeviceInfoUpdateFlags(updateDisplay = true),
        )
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun registerBluetoothReceiver() {
        if (!packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH)) return
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        ContextCompat.registerReceiver(
            this,
            bluetoothReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun registerStorageReceiver() {
        if (storageReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addDataScheme("file")
        }
        ContextCompat.registerReceiver(
            this,
            storageReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        storageReceiverRegistered = true
    }

    private fun registerDisplayListener() {
        if (displayListenerRegistered) return
        val dm = getSystemService(DisplayManager::class.java) ?: return
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                if (displayId != Display.DEFAULT_DISPLAY) return
                handler.post {
                    if (!isDestroyed) applyDisplayUpdate()
                }
            }
        }
        displayListener = listener
        dm.registerDisplayListener(listener, handler)
        displayListenerRegistered = true
    }

    private fun unregisterDisplayListener() {
        if (!displayListenerRegistered) return
        val dm = getSystemService(DisplayManager::class.java) ?: return
        displayListener?.let { runCatching { dm.unregisterDisplayListener(it) } }
        displayListener = null
        displayListenerRegistered = false
    }

    private fun registerThermalListener() {
        if (thermalListenerRegistered || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val pm = getSystemService(PowerManager::class.java) ?: return
        val listener = PowerManager.OnThermalStatusChangedListener {
            handler.post {
                if (!isDestroyed) applyProcessorUpdate()
            }
        }
        thermalStatusListener = listener
        pm.addThermalStatusListener(listener)
        thermalListenerRegistered = true
    }

    private fun unregisterThermalListener() {
        if (!thermalListenerRegistered || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val pm = getSystemService(PowerManager::class.java) ?: return
        thermalStatusListener?.let { runCatching { pm.removeThermalStatusListener(it) } }
        thermalStatusListener = null
        thermalListenerRegistered = false
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
            if (intent == null || listAdapter == null) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED,
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED ->
                    viewModel.refresh(
                        DeviceInfoProvider.GetOptions.Battery,
                        DeviceInfoUpdateFlags(updateBattery = true),
                    )
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED || listAdapter == null) return
            viewModel.refresh(
                DeviceInfoProvider.GetOptions.Network,
                DeviceInfoUpdateFlags(updateNetwork = true),
            )
        }
    }

    private val storageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (listAdapter == null) return
            handler.post {
                if (!isDestroyed) applyPhonePollUpdate()
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshNetwork()
        override fun onLost(network: Network) = refreshNetwork()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: android.net.NetworkCapabilities) = refreshNetwork()
        private fun refreshNetwork() {
            handler.post {
                if (isDestroyed || listAdapter == null) return@post
                viewModel.refresh(
                    DeviceInfoProvider.GetOptions.Network,
                    DeviceInfoUpdateFlags(updateNetwork = true),
                )
            }
        }
    }

    /** RAM, storage, CPU/thermal, cellular signal — no battery (broadcast) or full network (callback). */
    private val phoneLivePollRunnable = object : Runnable {
        override fun run() {
            if (!isDestroyed && listAdapter != null) {
                applyPhonePollUpdate()
            }
            handler.postDelayed(this, DeviceInfoUiShared.PHONE_LIVE_POLL_INTERVAL_MS)
        }
    }

    companion object {
        private const val SENSOR_ROW_WAITING = "—"
        private const val SENSOR_UI_REFRESH_INTERVAL_MS = 200L
        private const val SCROLL_TO_TOP_VISIBLE_AFTER_POSITION = 2
        private const val SCROLL_TO_TOP_FROST_DEBOUNCE_MS = 80L
    }

    private val refreshScrollToTopFrostRunnable = Runnable {
        if (isDestroyed) return@Runnable
        val container = scrollToTopContainer ?: return@Runnable
        val frost = scrollToTopFrost ?: return@Runnable
        if (container.visibility != View.VISIBLE) return@Runnable
        val list = findViewById<RecyclerView>(R.id.list)
        ScrollToTopBlurStyle.refreshFrostLayer(list, container, frost)
    }

    private fun scheduleScrollToTopFrostRefresh() {
        if (!ScrollToTopBlurStyle.supportsLiveBlur()) return
        handler.removeCallbacks(refreshScrollToTopFrostRunnable)
        handler.postDelayed(refreshScrollToTopFrostRunnable, SCROLL_TO_TOP_FROST_DEBOUNCE_MS)
    }

    private val scrollToTopScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            updateScrollToTopVisibility(recyclerView)
            scheduleScrollToTopFrostRefresh()
        }
    }

    private fun updateScrollToTopVisibility(list: RecyclerView) {
        val layoutManager = list.layoutManager as? LinearLayoutManager ?: return
        val show = layoutManager.findFirstVisibleItemPosition() > SCROLL_TO_TOP_VISIBLE_AFTER_POSITION
        val container = scrollToTopContainer ?: return
        val wasVisible = container.visibility == View.VISIBLE
        container.visibility = if (show) View.VISIBLE else View.GONE
        if (show && (!wasVisible || ScrollToTopBlurStyle.supportsLiveBlur())) {
            scheduleScrollToTopFrostRefresh()
        }
    }
}
