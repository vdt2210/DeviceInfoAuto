package com.deviceinfo.auto

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import android.location.LocationListener
import android.location.LocationManager
import android.location.GnssStatus
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import androidx.car.app.connection.CarConnection
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.concurrent.ConcurrentHashMap

class MainActivity : AppCompatActivity(), DeviceInfoTabFragment.Host {

    private val viewModel: DeviceInfoViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val tabItems = mutableMapOf<String, MutableList<InfoItem>>()
    private val tabRowIndexByKey = mutableMapOf<String, Map<String, Int>>()
    private var tabPagerAdapter: DeviceInfoTabPagerAdapter? = null
    private var sectionKeys: List<String> = emptyList()
    private var currentSectionKey: String = DeviceInfoUiShared.Section.BATTERY
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var scrollToTopContainer: View? = null
    private var scrollToTopButton: ImageButton? = null
    private var viewPager: ViewPager2? = null
    private var sectionNavigationAdapter: SectionRailAdapter? = null
    private var sectionNavigationIndicator: SectionRailIndicatorDecoration? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private val tabRecycledViewPool = RecyclerView.RecycledViewPool().apply {
        setMaxRecycledViews(0, 24)
    }
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
            if (isCurrentTab(DeviceInfoUiShared.Section.SENSORS)) {
                applySensorRowUpdatesFromLatestEvents()
            }
            handler.postDelayed(this, SENSOR_UI_REFRESH_INTERVAL_MS)
        }
    }
    private var locationUpdatesRegistered = false
    private var googleLocationUpdatesRegistered = false
    private var gnssStatusCallbackRegistered = false
    private var locationProvidersReceiverRegistered = false
    private var deviceHeadingUpdatesRegistered = false
    private var deviceHeadingRefreshScheduled = false
    private var deviceHeadingReliable = true
    private var deviceHeadingSource = DeviceHeadingSource.ROTATION_VECTOR
    private var headingAccelerometerValues: FloatArray? = null
    private var headingMagneticFieldValues: FloatArray? = null
    private val deviceHeadingRefreshRunnable = Runnable {
        deviceHeadingRefreshScheduled = false
        if (activityResumed && isCurrentTab(DeviceInfoUiShared.Section.LOCATION)) {
            viewModel.refreshLocation()
        }
    }
    private val deviceHeadingListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!deviceHeadingReliable) return
            val heading = when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> DeviceHeadingCalculator.fromRotationVector(
                    event.values,
                    currentDisplayRotationSurface(),
                )
                Sensor.TYPE_ACCELEROMETER -> {
                    headingAccelerometerValues = event.values.clone()
                    fallbackDeviceHeading()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    headingMagneticFieldValues = event.values.clone()
                    fallbackDeviceHeading()
                }
                else -> null
            } ?: return
            DeviceHeadingStore.update(heading, deviceHeadingSource)
            scheduleDeviceHeadingRefresh()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            if (sensor == null) return
            if (sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
                sensor.type != Sensor.TYPE_MAGNETIC_FIELD
            ) {
                return
            }
            val reliable = accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE
            DeviceHeadingStore.updateAccuracy(accuracy)
            if (reliable == deviceHeadingReliable) return
            deviceHeadingReliable = reliable
            if (!reliable) DeviceHeadingStore.clear()
            scheduleDeviceHeadingRefresh()
        }
    }
    private val registeredLocationProviders = mutableSetOf<String>()
    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onFirstFix(ttffMillis: Int) {
            GnssInfoStore.updateTimeToFirstFix(ttffMillis)
            viewModel.refreshLocation()
        }

        override fun onSatelliteStatusChanged(status: GnssStatus) {
            GnssInfoStore.updateStatus(status)
            scheduleDeviceHeadingRefresh()
        }
    }
    private val locationStreamListener = LocationListener { location ->
        LocationInfoStore.updateFromLocation(location)
        if (activityResumed && isCurrentTab(DeviceInfoUiShared.Section.LOCATION)) {
            viewModel.refreshLocation()
        }
    }
    private val googleLocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(GooglePlayServicesLocationStore::update)
            if (activityResumed && isCurrentTab(DeviceInfoUiShared.Section.LOCATION)) {
                viewModel.refreshLocation()
            }
        }
    }
    private val locationProvidersReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reconcileLocationListening()
        }
    }

    private var configSnapshotAtPause: AppConfigSnapshot? = null
    private var listSimDetailRowsVisible: Boolean = false
    private var listExternalVolumeRowKeys: List<String> = emptyList()
    private var listNetworkSchemaSignature: Pair<Boolean, Boolean> = false to false
    private var listConnectionSchemaSignature: String = ""
    private var listWirelessSchemaSignature: String = ""
    private var listBatterySchemaSignature: String = ""
    private var listVehicleSchemaKey: Pair<Boolean, VehiclePowertrain> = false to VehiclePowertrain.UNKNOWN
    private var appliedUnitMode: AppPreferences.UnitMode? = null
    private val unitRefreshedSections = mutableSetOf<String>()
    private var restoredPagerIndex: Int? = null
    private var activityResumed = false
    private var batteryReceiverRegistered = false
    private var resumeLiveRefreshPending = false

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        applyUnitModeIfChanged()
    }

    private lateinit var sensitivePermissionLauncher: ActivityResultLauncher<Array<String>>
    private var sensitivePermissionPromptedThisInstance = false
    private var lastKnownDeniedPermissions: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        sensitivePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            if (grants.values.any { it }) {
                syncLocationTabUpdates()
                viewModel.loadFull()
            }
        }
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(STATE_SECTION_KEY)?.let { currentSectionKey = it }
        restoredPagerIndex = savedInstanceState?.getInt(STATE_PAGER_INDEX, -1)?.takeIf { it >= 0 }
        applyTransparentSystemBars()
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.app_name)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh)
        viewPager = findViewById(R.id.pager)
        scrollToTopContainer = findViewById(R.id.scroll_to_top)
        scrollToTopButton = findViewById(R.id.scroll_to_top_button)
        scrollToTopButton?.setOnClickListener {
            currentTabList()?.smoothScrollToPosition(0)
        }
        swipeRefreshLayout?.apply {
            setColorSchemeColors(obtainStyledColor(com.google.android.material.R.attr.colorPrimary))
            setProgressBackgroundColorSchemeColor(obtainStyledColor(com.google.android.material.R.attr.colorSurface))
            setOnChildScrollUpCallback { _, _ ->
                currentTabList()?.canScrollVertically(-1) == true
            }
            setOnRefreshListener {
                applyCurrentTabPullRefresh()
            }
        }
        applySystemBarInsets()
        lastKnownDeniedPermissions = SensitivePermissions.denied(this).toSet()
        viewModel.uiEvent.observe(this) { event ->
            if (event == null) return@observe
            when (event) {
                is DeviceInfoUiEvent.FullList -> bindFullList(event.info)
                is DeviceInfoUiEvent.Refresh -> {
                    when {
                        event.flags.updateDisplayUnits && event.targetSectionKey != null ->
                            applyDisplayUnitsRefresh(event.info, event.targetSectionKey)
                        event.targetSectionKey != null ->
                            applySectionReload(event.info, event.targetSectionKey)
                        else -> applyRefresh(event.info, event.flags)
                    }
                    if (swipeRefreshLayout?.isRefreshing == true) {
                        swipeRefreshLayout?.isRefreshing = false
                    }
                }
            }
            viewModel.consumeEvent()
        }
        viewModel.invalidateHardwareAndLoadFull()
        registerCarConnectionObserver()
        registerVehicleStoreListener()
    }

    private fun registerCarConnectionObserver() {
        val connection = CarConnection(this)
        connection.type.observe(this) { type ->
            VehicleConnectionState.update(type)
            if (tabPagerAdapter == null || !isCurrentTab(DeviceInfoUiShared.Section.VEHICLE)) return@observe
            scheduleVehicleRefresh()
        }
        connection.type.value?.let { VehicleConnectionState.update(it) }
    }

    private val vehicleRefreshRunnable = Runnable {
        if (!isDestroyed && tabPagerAdapter != null &&
            isCurrentTab(DeviceInfoUiShared.Section.VEHICLE)
        ) {
            viewModel.refreshVehicle()
        }
    }

    private fun scheduleVehicleRefresh() {
        handler.removeCallbacks(vehicleRefreshRunnable)
        handler.post(vehicleRefreshRunnable)
    }

    private val vehicleStoreListener = Runnable {
        if (isDestroyed || tabPagerAdapter == null ||
            !isCurrentTab(DeviceInfoUiShared.Section.VEHICLE)
        ) return@Runnable
        scheduleVehicleRefresh()
    }

    private fun registerVehicleStoreListener() {
        VehicleInfoStore.addListener(::scheduleVehicleStoreRefresh)
    }

    private fun unregisterVehicleStoreListener() {
        VehicleInfoStore.removeListener(::scheduleVehicleStoreRefresh)
        handler.removeCallbacks(vehicleStoreListener)
    }

    private fun scheduleVehicleStoreRefresh() {
        handler.removeCallbacks(vehicleStoreListener)
        handler.post(vehicleStoreListener)
    }

    override fun onTabListReady(sectionKey: String, list: RecyclerView) {
        tabItems[sectionKey]?.let { items ->
            getTabFragment(sectionKey)?.bindItems(items)
        }
        list.removeOnScrollListener(scrollToTopScrollListener)
        list.addOnScrollListener(scrollToTopScrollListener)
        if (sectionKey == currentSectionKey) {
            updateScrollToTopVisibility(list)
        }
    }

    override fun tabRecycledViewPool(): RecyclerView.RecycledViewPool = tabRecycledViewPool

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_settings -> {
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
            true
        }
        R.id.menu_app_info -> {
            startActivity(Intent(this, AppInfoActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun requestMissingSensitivePermissionsIfNeeded() {
        val denied = SensitivePermissions.activityRequestDenied(this)
        if (denied.isEmpty()) return
        if (sensitivePermissionPromptedThisInstance) return
        sensitivePermissionPromptedThisInstance = true
        sensitivePermissionLauncher.launch(denied.toTypedArray())
    }

    private val resumeAfterVisibleRunnable = Runnable {
        if (isDestroyed) return@Runnable
        resumeVisibleUpdates()
    }

    private fun applySystemBarInsets() {
        findViewById<View>(R.id.root).applySystemBarInsets()
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        val snap = configSnapshotAtPause
        configSnapshotAtPause = null
        if (snap != null) {
            val prefs = AppPreferences(this)
            val nightNow = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val systemNightChanged = snap.themeMode == AppPreferences.ThemeMode.SYSTEM &&
                snap.nightModeFlags != nightNow
            if (snap.languageTag != prefs.getLanguageTag() ||
                snap.themeMode != prefs.getThemeMode() ||
                systemNightChanged
            ) {
                recreate()
                return
            }
        }
        applyUnitModeIfChanged()
        val decor = window.decorView
        decor.removeCallbacks(resumeAfterVisibleRunnable)
        decor.post(resumeAfterVisibleRunnable)
    }

    private fun applyUnitModeIfChanged() {
        if (tabPagerAdapter == null) return
        val mode = AppPreferences(this).getUnitMode()
        if (appliedUnitMode != null && appliedUnitMode != mode) {
            unitRefreshedSections.clear()
        }
        appliedUnitMode = mode
        ensureSectionUnitsFresh(currentSectionKey)
    }

    private fun ensureSectionUnitsFresh(sectionKey: String) {
        if (tabPagerAdapter == null || !sectionUsesDisplayUnits(sectionKey)) return
        if (sectionKey in unitRefreshedSections) return
        viewModel.refreshDisplayUnits(sectionKey)
    }

    private fun sectionUsesDisplayUnits(sectionKey: String): Boolean =
        sectionKey == DeviceInfoUiShared.Section.BATTERY ||
            sectionKey == DeviceInfoUiShared.Section.PROCESSOR ||
            sectionKey == DeviceInfoUiShared.Section.SENSORS ||
            sectionKey == DeviceInfoUiShared.Section.LOCATION ||
            sectionKey == DeviceInfoUiShared.Section.VEHICLE

    private fun markAllUnitSectionsFresh(keys: List<String>) {
        appliedUnitMode = AppPreferences(this).getUnitMode()
        unitRefreshedSections.clear()
        keys.filterTo(unitRefreshedSections, ::sectionUsesDisplayUnits)
    }

    private fun resumeVisibleUpdates() {
        val deniedNow = SensitivePermissions.denied(this).toSet()
        val permissionsChanged = deniedNow != lastKnownDeniedPermissions
        lastKnownDeniedPermissions = deniedNow

        window.decorView.post { requestMissingSensitivePermissionsIfNeeded() }

        if (tabPagerAdapter != null) {
            resumeLiveRefreshPending = false
            if (permissionsChanged) {
                viewModel.loadFull()
            } else {
                viewModel.refreshOnResume(currentSectionKey)
                if (isCurrentTab(DeviceInfoUiShared.Section.VEHICLE)) {
                    scheduleVehicleRefresh()
                }
            }
        } else {
            resumeLiveRefreshPending = !permissionsChanged
        }
        registerBatteryReceiver()
        registerBluetoothReceiver()
        registerNetworkCallback()
        registerDisplayListener()
        registerStorageReceiver()
        registerThermalListener()
        syncSensorTabUpdates()
        syncLocationTabUpdates()
        startLivePollLoops()
    }

    private fun applyBatteryPollUpdate() {
        if (tabPagerAdapter == null || !isCurrentTab(DeviceInfoUiShared.Section.BATTERY)) return
        viewModel.refreshBatteryPoll()
    }

    private fun startLivePollLoops() {
        handler.removeCallbacks(phoneLivePollRunnable)
        handler.removeCallbacks(batteryLivePollRunnable)
        handler.postDelayed(phoneLivePollRunnable, DeviceInfoUiShared.PHONE_LIVE_POLL_INTERVAL_MS)
        handler.postDelayed(batteryLivePollRunnable, DeviceInfoUiShared.PHONE_BATTERY_POLL_INTERVAL_MS)
    }

    private fun flushPendingResumeLiveRefresh() {
        if (!activityResumed || !resumeLiveRefreshPending || tabPagerAdapter == null) return
        resumeLiveRefreshPending = false
        viewModel.refreshOnResume(currentSectionKey)
        if (isCurrentTab(DeviceInfoUiShared.Section.VEHICLE)) {
            scheduleVehicleRefresh()
        }
    }

    private fun isCurrentTab(sectionKey: String): Boolean = sectionKey == currentSectionKey

    /** Refresh live data for the tab the user is viewing. */
    private fun refreshVisibleTabLive() {
        if (tabPagerAdapter == null) return
        viewModel.refreshTab(currentSectionKey)
    }

    override fun onPause() {
        activityResumed = false
        resumeLiveRefreshPending = false
        window.decorView.removeCallbacks(resumeAfterVisibleRunnable)
        handler.removeCallbacks(phoneLivePollRunnable)
        handler.removeCallbacks(batteryLivePollRunnable)
        stopSensorUiRefresh()
        unregisterStreamSensors()
        unregisterLocationProvidersReceiver()
        unregisterLocationUpdates()
        unregisterDeviceHeadingUpdates()
        unregisterBatteryReceiver()
        runCatching { unregisterReceiver(bluetoothReceiver) }
        runCatching { unregisterReceiver(storageReceiver) }
        storageReceiverRegistered = false
        unregisterDisplayListener()
        unregisterThermalListener()
        unregisterNetworkCallback()
        AppPreferences(this).let {
            configSnapshotAtPause = AppConfigSnapshot(
                languageTag = it.getLanguageTag(),
                themeMode = it.getThemeMode(),
                nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
            )
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SECTION_KEY, currentSectionKey)
        viewPager?.currentItem?.let { outState.putInt(STATE_PAGER_INDEX, it) }
    }

    override fun onDestroy() {
        handler.removeCallbacks(vehicleRefreshRunnable)
        unregisterVehicleStoreListener()
        super.onDestroy()
    }

    private fun bindFullList(info: DeviceInfo) {
        val schema = DeviceInfoSchema.phoneSchema(info)
        val keys = schema.map { it.first }
        val needsTabSetup = tabPagerAdapter == null || keys != sectionKeys
        if (needsTabSetup) {
            unregisterStreamSensors()
            latestSensorEvents.clear()
            setupTabs(keys)
        }
        listSimDetailRowsVisible = info.simDetailRowsVisible
        listExternalVolumeRowKeys = info.externalStorageVolumes.map { it.rowKey }
        listNetworkSchemaSignature = info.wifiNetworkRowsVisible to info.simDetailRowsVisible
        listConnectionSchemaSignature = DeviceInfoSchema.connectionSchemaSignature(info)
        listWirelessSchemaSignature = DeviceInfoSchema.wirelessSchemaSignature(info)
        listBatterySchemaSignature = batterySchemaSignature(info)
        listVehicleSchemaKey = info.vehicleTab.dataRowsVisible to info.vehicleTab.powertrain
        for (sectionKey in keys) {
            rebuildSection(sectionKey, info)
        }
        if (needsTabSetup) {
            markAllUnitSectionsFresh(keys)
            syncSensorTabUpdates()
            syncLocationTabUpdates()
        }
        currentTabList()?.let { updateScrollToTopVisibility(it) }
        flushPendingResumeLiveRefresh()
    }

    private fun setupTabs(keys: List<String>) {
        val pager = viewPager ?: return
        val hadAdapter = tabPagerAdapter != null
        val pagerIndexBeforeSwap = if (hadAdapter) pager.currentItem else null
        sectionKeys = keys
        tabPagerAdapter = DeviceInfoTabPagerAdapter(this, keys)
        val restoreIndex = restoredPagerIndex?.coerceIn(0, keys.lastIndex)
            ?: pagerIndexBeforeSwap?.coerceIn(0, keys.lastIndex)
            ?: keys.indexOf(currentSectionKey).takeIf { it >= 0 }
            ?: 0
        restoredPagerIndex = null
        pager.adapter = tabPagerAdapter
        pager.setCurrentItem(restoreIndex, false)
        pager.offscreenPageLimit = 1
        pager.overScrollMode = View.OVER_SCROLL_NEVER
        (pager.getChildAt(0) as? RecyclerView)?.overScrollMode = View.OVER_SCROLL_NEVER
        val sectionNavigation = findViewById<RecyclerView>(R.id.section_navigation)
        if (sectionNavigation.layoutManager !is LinearLayoutManager) {
            sectionNavigation.layoutManager = LinearLayoutManager(
                this,
                RecyclerView.HORIZONTAL,
                false,
            )
        }
        sectionNavigation.itemAnimator = null
        sectionNavigationIndicator?.let(sectionNavigation::removeItemDecoration)
        sectionNavigationIndicator = SectionRailIndicatorDecoration(sectionNavigation)
            .also(sectionNavigation::addItemDecoration)
        sectionNavigationAdapter = SectionRailAdapter { position ->
            pager.setCurrentItem(position, false)
            sectionNavigationAdapter?.select(position)
            sectionNavigationIndicator?.settleOn(position)
            centerSectionNavigationItem(sectionNavigation, position)
        }.also { adapter ->
            sectionNavigation.adapter = adapter
            adapter.submitSections(keys, restoreIndex)
            sectionNavigationIndicator?.settleOn(restoreIndex)
            centerSectionNavigationItem(sectionNavigation, restoreIndex)
        }
        pageChangeCallback?.let { pager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
                sectionNavigationIndicator?.moveBetween(position, positionOffset)
            }

            override fun onPageSelected(position: Int) {
                currentSectionKey = keys.getOrElse(position) { DeviceInfoUiShared.Section.BATTERY }
                ensureSectionUnitsFresh(currentSectionKey)
                syncSensorTabUpdates()
                syncLocationTabUpdates()
                refreshVisibleTabLive()
            }

            override fun onPageScrollStateChanged(state: Int) {
                val refreshing = swipeRefreshLayout?.isRefreshing == true
                swipeRefreshLayout?.isEnabled =
                    state == ViewPager2.SCROLL_STATE_IDLE && !refreshing
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    val settledPosition = pager.currentItem
                    sectionNavigationAdapter?.select(settledPosition)
                    sectionNavigationIndicator?.settleOn(settledPosition)
                    centerSectionNavigationItem(sectionNavigation, settledPosition)
                    updateSwipeRefreshEnabledForCurrentTab()
                    currentTabList()?.let { updateScrollToTopVisibility(it) }
                }
            }
        }.also { pager.registerOnPageChangeCallback(it) }
        currentSectionKey = keys.getOrElse(restoreIndex) { DeviceInfoUiShared.Section.BATTERY }
        updateSwipeRefreshEnabledForCurrentTab()
    }

    private fun centerSectionNavigationItem(navigation: RecyclerView, position: Int) {
        navigation.post {
            if (position !in 0 until (navigation.adapter?.itemCount ?: 0)) return@post
            val layoutManager = navigation.layoutManager as? LinearLayoutManager ?: return@post
            val itemSize = resources.getDimensionPixelSize(R.dimen.tab_navigation_item_size)
            val availableExtent = if (layoutManager.orientation == RecyclerView.HORIZONTAL) {
                navigation.width - navigation.paddingLeft - navigation.paddingRight
            } else {
                navigation.height - navigation.paddingTop - navigation.paddingBottom
            }
            val offset = when (position) {
                0 -> 0
                (navigation.adapter?.itemCount ?: 0) - 1 ->
                    (availableExtent - itemSize).coerceAtLeast(0)
                else -> ((availableExtent - itemSize) / 2).coerceAtLeast(0)
            }
            layoutManager.scrollToPositionWithOffset(position, offset)
        }
    }

    private fun updateSwipeRefreshEnabledForCurrentTab() {
        val list = currentTabList()
        swipeRefreshLayout?.isEnabled = list == null || !list.canScrollVertically(-1)
    }

    private fun rebuildSection(sectionKey: String, info: DeviceInfo) {
        val items = DeviceInfoPhoneListBuilder.buildSection(
            context = this,
            sectionKey = sectionKey,
            info = info,
            displayRotation = currentDisplayRotationSurface(),
            sensorDisplayForRow = ::sensorDisplayForRow,
            sensorRowValueForUi = ::sensorRowValueForUi,
            notAvailableText = ::notAvailableText,
        )
        publishSectionItems(sectionKey, items)
    }

    private fun publishSectionItems(sectionKey: String, items: List<InfoItem>) {
        tabItems[sectionKey] = items.toMutableList()
        rebuildRowIndex(sectionKey)
        if (!isCurrentTab(sectionKey)) return
        getTabFragment(sectionKey)?.bindItems(items)
    }

    private fun batterySchemaSignature(info: DeviceInfo): String =
        DeviceInfoSchema.phoneSchema(info)
            .first { it.first == DeviceInfoUiShared.Section.BATTERY }
            .second
            .joinToString(",")

    private fun rebuildRowIndex(sectionKey: String) {
        tabItems[sectionKey]?.let { items ->
            tabRowIndexByKey[sectionKey] = buildMap {
                items.forEachIndexed { index, item ->
                    if (item is InfoItem.Row) put(item.key, index)
                }
            }
        }
    }

    private fun getTabFragment(sectionKey: String): DeviceInfoTabFragment? {
        val index = sectionKeys.indexOf(sectionKey)
        if (index < 0) return null
        return supportFragmentManager.findFragmentByTag("f$index") as? DeviceInfoTabFragment
    }

    private fun currentTabList(): RecyclerView? =
        getTabFragment(currentSectionKey)?.recyclerView()

    private fun obtainStyledColor(attr: Int): Int {
        val typedArray = obtainStyledAttributes(intArrayOf(attr))
        return try {
            typedArray.getColor(0, Color.GRAY)
        } finally {
            typedArray.recycle()
        }
    }

    private fun applyCurrentTabPullRefresh() {
        if (tabPagerAdapter == null) {
            swipeRefreshLayout?.isRefreshing = false
            return
        }
        viewModel.refreshTab(currentSectionKey)
    }

    private fun applySectionReload(info: DeviceInfo, sectionKey: String) {
        if (tabPagerAdapter == null) return
        when (sectionKey) {
            DeviceInfoUiShared.Section.CONNECTION,
            DeviceInfoUiShared.Section.WIRELESS,
            -> {
                listSimDetailRowsVisible = info.simDetailRowsVisible
                listNetworkSchemaSignature = info.wifiNetworkRowsVisible to info.simDetailRowsVisible
                listConnectionSchemaSignature = DeviceInfoSchema.connectionSchemaSignature(info)
                listWirelessSchemaSignature = DeviceInfoSchema.wirelessSchemaSignature(info)
            }
            DeviceInfoUiShared.Section.STORAGE_MEMORY -> {
                listExternalVolumeRowKeys = info.externalStorageVolumes.map { it.rowKey }
            }
            DeviceInfoUiShared.Section.VEHICLE -> {
                listVehicleSchemaKey = info.vehicleTab.dataRowsVisible to info.vehicleTab.powertrain
            }
        }
        rebuildSection(sectionKey, info)
        if (sectionKey == DeviceInfoUiShared.Section.SENSORS) {
            applySensorRowUpdatesFromLatestEvents()
        }
        if (sectionKey == currentSectionKey) {
            currentTabList()?.let { updateScrollToTopVisibility(it) }
        }
    }

    private fun applyDisplayUnitsRefresh(info: DeviceInfo, sectionKey: String) {
        if (tabPagerAdapter == null) return
        rebuildSection(sectionKey, info)
        if (sectionKey == DeviceInfoUiShared.Section.SENSORS) {
            applySensorRowUpdatesFromLatestEvents()
        }
        unitRefreshedSections.add(sectionKey)
    }

    private fun applyRefresh(info: DeviceInfo, flags: DeviceInfoUpdateFlags) {
        if (tabPagerAdapter == null) return
        val visible = currentSectionKey
        if (flags.updateNetwork || flags.updateNetworkPoll) {
            val connSig = DeviceInfoSchema.connectionSchemaSignature(info)
            val wirelessSig = DeviceInfoSchema.wirelessSchemaSignature(info)
            if (info.simDetailRowsVisible != listSimDetailRowsVisible ||
                connSig != listConnectionSchemaSignature ||
                wirelessSig != listWirelessSchemaSignature
            ) {
                listSimDetailRowsVisible = info.simDetailRowsVisible
                listConnectionSchemaSignature = connSig
                listWirelessSchemaSignature = wirelessSig
                rebuildSection(DeviceInfoUiShared.Section.CONNECTION, info)
                rebuildSection(DeviceInfoUiShared.Section.WIRELESS, info)
                return
            }
        }
        if (flags.updateMemory && isCurrentTab(DeviceInfoUiShared.Section.STORAGE_MEMORY)) {
            val volumeKeys = info.externalStorageVolumes.map { it.rowKey }
            if (volumeKeys != listExternalVolumeRowKeys) {
                listExternalVolumeRowKeys = volumeKeys
                rebuildSection(DeviceInfoUiShared.Section.STORAGE_MEMORY, info)
                return
            }
        }
        if (flags.updateVehicle && isCurrentTab(DeviceInfoUiShared.Section.VEHICLE)) {
            val schemaKey = info.vehicleTab.dataRowsVisible to info.vehicleTab.powertrain
            if (schemaKey != listVehicleSchemaKey) {
                listVehicleSchemaKey = schemaKey
                rebuildSection(DeviceInfoUiShared.Section.VEHICLE, info)
                return
            }
        }
        if (flags.updateBatteryBroadcast || flags.updateBatteryPoll) {
            val batterySig = batterySchemaSignature(info)
            if (batterySig != listBatterySchemaSignature) {
                listBatterySchemaSignature = batterySig
                rebuildSection(DeviceInfoUiShared.Section.BATTERY, info)
                return
            }
        }
        if (visible !in sectionsForFlags(flags)) return
        val rotation = currentDisplayRotationSurface()
        patchSection(visible, info, flags, rotation)
    }

    private fun sectionsForFlags(flags: DeviceInfoUpdateFlags): Set<String> = buildSet {
        if (flags.updateBatteryBroadcast || flags.updateBatteryPoll) {
            add(DeviceInfoUiShared.Section.BATTERY)
        }
        if (flags.updateDisplay) add(DeviceInfoUiShared.Section.DISPLAY)
        if (flags.updateMemory) add(DeviceInfoUiShared.Section.STORAGE_MEMORY)
        if (flags.updateNetwork) {
            add(DeviceInfoUiShared.Section.CONNECTION)
            add(DeviceInfoUiShared.Section.WIRELESS)
        }
        if (flags.updateNetworkPoll) {
            add(DeviceInfoUiShared.Section.WIRELESS)
        }
        if (flags.updateProcessor) {
            add(DeviceInfoUiShared.Section.PROCESSOR)
        }
        if (flags.updateVehicle) add(DeviceInfoUiShared.Section.VEHICLE)
        if (flags.updateLocation) add(DeviceInfoUiShared.Section.LOCATION)
    }

    private fun patchSection(
        sectionKey: String,
        info: DeviceInfo,
        flags: DeviceInfoUpdateFlags,
        rotation: Int,
    ) {
        val items = tabItems[sectionKey] ?: return
        var changed = false
        val next = items.toMutableList()
        for (index in next.indices) {
            val item = next[index]
            if (item !is InfoItem.Row) continue
            if (!DeviceInfoPhoneRow.shouldRefresh(
                    item.key,
                    flags.updateBatteryBroadcast,
                    flags.updateBatteryPoll,
                    flags.updateMemory,
                    flags.updateProcessor,
                    flags.updateNetwork,
                    flags.updateNetworkPoll,
                    flags.updateDisplay,
                    flags.updateVehicle,
                    flags.updateLocation,
                    flags.updateDisplayUnits,
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
        if (changed) publishSectionItems(sectionKey, next)
    }

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

    /** Raw sensor streams and their 200 ms UI tick only run while the Sensors tab is visible. */
    private fun syncSensorTabUpdates() {
        if (!activityResumed || tabPagerAdapter == null) return
        if (isCurrentTab(DeviceInfoUiShared.Section.SENSORS)) {
            registerStreamSensors()
            startSensorUiRefresh()
        } else {
            stopSensorUiRefresh()
            unregisterStreamSensors()
            latestSensorEvents.clear()
        }
    }

    /** GPS / network listeners only while the location tab is visible. */
    private fun syncLocationTabUpdates() {
        if (!activityResumed) return
        if (isCurrentTab(DeviceInfoUiShared.Section.LOCATION)) {
            registerLocationProvidersReceiver()
            registerDeviceHeadingUpdates()
            reconcileLocationListening()
        } else {
            unregisterLocationProvidersReceiver()
            unregisterLocationUpdates()
            unregisterDeviceHeadingUpdates()
        }
    }

    private fun registerDeviceHeadingUpdates() {
        if (deviceHeadingUpdatesRegistered) return
        val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        DeviceHeadingStore.clear()
        deviceHeadingReliable = true
        headingAccelerometerValues = null
        headingMagneticFieldValues = null

        val rotationVector = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        deviceHeadingUpdatesRegistered = if (rotationVector != null) {
            deviceHeadingSource = DeviceHeadingSource.ROTATION_VECTOR
            sm.registerListener(
                deviceHeadingListener,
                rotationVector,
                SensorManager.SENSOR_DELAY_UI,
                handler,
            )
        } else {
            val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magneticField = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            if (accelerometer == null || magneticField == null) {
                false
            } else {
                deviceHeadingSource =
                    DeviceHeadingSource.ACCELEROMETER_MAGNETIC_FIELD
                val accelerometerRegistered = sm.registerListener(
                    deviceHeadingListener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI,
                    handler,
                )
                val magneticFieldRegistered = sm.registerListener(
                    deviceHeadingListener,
                    magneticField,
                    SensorManager.SENSOR_DELAY_UI,
                    handler,
                )
                val bothRegistered = accelerometerRegistered && magneticFieldRegistered
                if (!bothRegistered) {
                    runCatching { sm.unregisterListener(deviceHeadingListener) }
                }
                bothRegistered
            }
        }
    }

    private fun unregisterDeviceHeadingUpdates() {
        if (deviceHeadingUpdatesRegistered) {
            val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            runCatching { sm?.unregisterListener(deviceHeadingListener) }
        }
        handler.removeCallbacks(deviceHeadingRefreshRunnable)
        deviceHeadingRefreshScheduled = false
        deviceHeadingUpdatesRegistered = false
        deviceHeadingReliable = true
        deviceHeadingSource = DeviceHeadingSource.ROTATION_VECTOR
        headingAccelerometerValues = null
        headingMagneticFieldValues = null
        DeviceHeadingStore.clear()
    }

    private fun fallbackDeviceHeading(): Float? {
        val accelerometer = headingAccelerometerValues ?: return null
        val magneticField = headingMagneticFieldValues ?: return null
        return DeviceHeadingCalculator.fromAccelerometerAndMagneticField(
            accelerometer,
            magneticField,
            currentDisplayRotationSurface(),
        )
    }

    private fun scheduleDeviceHeadingRefresh() {
        if (deviceHeadingRefreshScheduled) return
        deviceHeadingRefreshScheduled = true
        handler.postDelayed(deviceHeadingRefreshRunnable, SENSOR_UI_REFRESH_INTERVAL_MS)
    }

    private fun registerLocationProvidersReceiver() {
        if (locationProvidersReceiverRegistered) return
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                addAction(LocationManager.MODE_CHANGED_ACTION)
            }
        }
        ContextCompat.registerReceiver(
            this,
            locationProvidersReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        locationProvidersReceiverRegistered = true
    }

    private fun unregisterLocationProvidersReceiver() {
        if (!locationProvidersReceiverRegistered) return
        runCatching { unregisterReceiver(locationProvidersReceiver) }
        locationProvidersReceiverRegistered = false
    }

    /** Re-read system location state, start/stop listeners, refresh the tab. */
    private fun reconcileLocationListening() {
        if (!activityResumed || !isCurrentTab(DeviceInfoUiShared.Section.LOCATION)) return
        unregisterLocationUpdates()
        registerLocationUpdates()
        viewModel.refreshLocation()
    }

    private fun registerLocationUpdates() {
        if (!SensitivePermissions.hasLocationPermission(this)) {
            unregisterLocationUpdates()
            return
        }
        val lm = getSystemService(LocationManager::class.java) ?: return
        val locationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
        if (!locationEnabled) {
            LocationInfoStore.clearMotion()
            GnssInfoStore.clear()
            unregisterLocationUpdates()
            return
        }
        registerGnssStatusCallback(lm)
        registerGooglePlayServicesLocationUpdates()
        try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
            )
            providers.forEach { provider ->
                if (!lm.isProviderEnabled(provider)) return@forEach
                lm.getLastKnownLocation(provider)?.let { LocationInfoStore.updateFromLocation(it) }
                if (provider !in registeredLocationProviders) {
                    lm.requestLocationUpdates(
                        provider,
                        1000L,
                        0f,
                        locationStreamListener,
                        handler.looper,
                    )
                    registeredLocationProviders.add(provider)
                }
            }
            locationUpdatesRegistered = registeredLocationProviders.isNotEmpty()
        } catch (_: SecurityException) {
            unregisterLocationUpdates()
        }
    }

    private fun unregisterLocationUpdates() {
        val lm = getSystemService(LocationManager::class.java)
        if (locationUpdatesRegistered) {
            runCatching { lm?.removeUpdates(locationStreamListener) }
        }
        if (gnssStatusCallbackRegistered) {
            runCatching { lm?.unregisterGnssStatusCallback(gnssStatusCallback) }
        }
        registeredLocationProviders.clear()
        locationUpdatesRegistered = false
        unregisterGooglePlayServicesLocationUpdates()
        gnssStatusCallbackRegistered = false
        GnssInfoStore.clear()
    }

    private fun registerGooglePlayServicesLocationUpdates() {
        if (googleLocationUpdatesRegistered) return
        if (GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS
        ) {
            GooglePlayServicesLocationStore.updateStatus(
                GooglePlayServicesLocationStatus.UNAVAILABLE,
            )
            return
        }
        if (!SensitivePermissions.hasLocationPermission(this)) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
        try {
            LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(request, googleLocationCallback, handler.looper)
                .addOnSuccessListener {
                    googleLocationUpdatesRegistered = true
                    GooglePlayServicesLocationStore.updateStatus(
                        GooglePlayServicesLocationStatus.WAITING,
                    )
                    viewModel.refreshLocation()
                }
                .addOnFailureListener {
                    GooglePlayServicesLocationStore.updateStatus(
                        GooglePlayServicesLocationStatus.ERROR,
                    )
                    viewModel.refreshLocation()
                }
        } catch (_: SecurityException) {
            GooglePlayServicesLocationStore.updateStatus(
                GooglePlayServicesLocationStatus.ERROR,
            )
        }
    }

    private fun unregisterGooglePlayServicesLocationUpdates() {
        if (googleLocationUpdatesRegistered) {
            LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(googleLocationCallback)
        }
        googleLocationUpdatesRegistered = false
        GooglePlayServicesLocationStore.clear()
    }

    @Suppress("DEPRECATION")
    private fun registerGnssStatusCallback(lm: LocationManager) {
        if (gnssStatusCallbackRegistered ||
            !SensitivePermissions.hasFineLocationPermission(this)
        ) {
            return
        }
        gnssStatusCallbackRegistered = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lm.registerGnssStatusCallback(
                    ContextCompat.getMainExecutor(this),
                    gnssStatusCallback,
                )
            } else {
                lm.registerGnssStatusCallback(gnssStatusCallback, handler)
            }
        } catch (_: SecurityException) {
            false
        }
    }

    private fun applySensorRowUpdatesFromLatestEvents() {
        if (tabPagerAdapter == null || !isCurrentTab(DeviceInfoUiShared.Section.SENSORS)) return
        val sectionKey = DeviceInfoUiShared.Section.SENSORS
        val items = tabItems[sectionKey] ?: return
        var changed = false
        val next = items.toMutableList()
        val rowIndex = tabRowIndexByKey[sectionKey].orEmpty()
        for (rowKey in rowIndex.keys) {
            if (!rowKey.startsWith("row_sensor_")) continue
            val index = rowIndex[rowKey] ?: continue
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
        if (changed) publishSectionItems(sectionKey, next)
    }

    private fun registerStreamSensors() {
        if (tabPagerAdapter == null) return
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
        if (!isCurrentTab(DeviceInfoUiShared.Section.DISPLAY)) return
        val sectionKey = DeviceInfoUiShared.Section.DISPLAY
        val index = tabRowIndexByKey[sectionKey]
            ?.get(DeviceInfoUiShared.Row.DISPLAY_ROTATION) ?: return
        val item = tabItems[sectionKey]?.getOrNull(index) as? InfoItem.Row ?: return
        val label = DeviceInfoUiShared.displayRotationLabel(this, currentDisplayRotationSurface())
        if (item.value == label) return
        val next = tabItems[sectionKey]?.toMutableList() ?: return
        next[index] = item.copy(
            title = DeviceInfoUiShared.rowTitle(this, item.key),
            value = label,
            iconResId = R.drawable.ic_row_model,
        )
        publishSectionItems(sectionKey, next)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshDisplayRotationRowItem()
    }

    private fun applyPhonePollUpdate() {
        if (tabPagerAdapter == null || !phonePollAppliesToCurrentTab()) return
        if (isCurrentTab(DeviceInfoUiShared.Section.CONNECTION) ||
            isCurrentTab(DeviceInfoUiShared.Section.WIRELESS)
        ) {
            viewModel.refresh(
                DeviceInfoProvider.GetOptions.Network,
                DeviceInfoUpdateFlags(
                    updateNetwork = true,
                    updateNetworkPoll = true,
                ),
            )
            return
        }
        viewModel.refresh(
            DeviceInfoProvider.GetOptions.PhonePoll,
            DeviceInfoUpdateFlags(
                updateMemory = true,
                updateProcessor = true,
                updateNetworkPoll = true,
            ),
        )
    }

    private fun phonePollAppliesToCurrentTab(): Boolean =
        isCurrentTab(DeviceInfoUiShared.Section.STORAGE_MEMORY) ||
            isCurrentTab(DeviceInfoUiShared.Section.PROCESSOR) ||
            isCurrentTab(DeviceInfoUiShared.Section.CONNECTION) ||
            isCurrentTab(DeviceInfoUiShared.Section.WIRELESS)

    private fun applyProcessorUpdate() {
        if (tabPagerAdapter == null || !isCurrentTab(DeviceInfoUiShared.Section.PROCESSOR)) return
        viewModel.refresh(
            DeviceInfoProvider.GetOptions.Processor,
            DeviceInfoUpdateFlags(updateProcessor = true),
        )
    }

    private fun applyDisplayUpdate() {
        if (tabPagerAdapter == null || !isCurrentTab(DeviceInfoUiShared.Section.DISPLAY)) return
        viewModel.refresh(
            DeviceInfoProvider.GetOptions.Display,
            DeviceInfoUpdateFlags(updateDisplay = true),
        )
    }

    private fun registerBatteryReceiver() {
        if (batteryReceiverRegistered) return
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
        batteryReceiverRegistered = true
    }

    private fun unregisterBatteryReceiver() {
        if (!batteryReceiverRegistered) return
        runCatching { unregisterReceiver(batteryReceiver) }
        batteryReceiverRegistered = false
    }

    private fun registerBluetoothReceiver() {
        if (!packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH)) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
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
        if (!networkCallbackRegistered) {
            runCatching {
                val requestBuilder = NetworkRequest.Builder()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    requestBuilder.clearCapabilities()
                } else {
                    requestBuilder
                        .removeCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                        .removeCapability(android.net.NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                        .removeCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                }
                val request = requestBuilder.build()
                cm.registerNetworkCallback(request, networkCallback)
                networkCallbackRegistered = true
            }
        }
    }

    private fun unregisterNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        if (networkCallbackRegistered) {
            runCatching {
                cm.unregisterNetworkCallback(networkCallback)
                networkCallbackRegistered = false
            }
        }
        DeviceInfoProvider.clearNetworkCapabilities()
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || tabPagerAdapter == null ||
                !isCurrentTab(DeviceInfoUiShared.Section.BATTERY)
            ) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED ->
                    viewModel.refreshBatteryBroadcast(intent)
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED ->
                    viewModel.refreshBatteryBroadcast(null)
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (tabPagerAdapter == null || !isCurrentTab(DeviceInfoUiShared.Section.CONNECTION)) return
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED,
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                -> viewModel.refresh(
                    DeviceInfoProvider.GetOptions.Network,
                    DeviceInfoUpdateFlags(updateNetwork = true),
                )
            }
        }
    }

    private val storageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (tabPagerAdapter == null ||
                !isCurrentTab(DeviceInfoUiShared.Section.STORAGE_MEMORY)
            ) return
            handler.post {
                if (!isDestroyed) applyPhonePollUpdate()
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshNetwork()
        override fun onLost(network: Network) {
            DeviceInfoProvider.onNetworkLost(network)
            refreshNetwork()
        }
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: android.net.NetworkCapabilities,
        ) {
            DeviceInfoProvider.onNetworkCapabilitiesChanged(network, networkCapabilities)
            refreshNetwork()
        }

        private fun refreshNetwork() {
            handler.post {
                if (isDestroyed || tabPagerAdapter == null ||
                    (!isCurrentTab(DeviceInfoUiShared.Section.WIRELESS) &&
                        !isCurrentTab(DeviceInfoUiShared.Section.CONNECTION))
                ) return@post
                viewModel.refresh(
                    DeviceInfoProvider.GetOptions.Network,
                    DeviceInfoUpdateFlags(updateNetwork = true),
                )
            }
        }
    }

    private val phoneLivePollRunnable = object : Runnable {
        override fun run() {
            if (!isDestroyed && tabPagerAdapter != null) {
                applyPhonePollUpdate()
            }
            handler.postDelayed(this, DeviceInfoUiShared.PHONE_LIVE_POLL_INTERVAL_MS)
        }
    }

    private val batteryLivePollRunnable = object : Runnable {
        override fun run() {
            if (!isDestroyed && tabPagerAdapter != null) {
                applyBatteryPollUpdate()
            }
            handler.postDelayed(this, DeviceInfoUiShared.PHONE_BATTERY_POLL_INTERVAL_MS)
        }
    }

    companion object {
        /** Live UI updates and polls only touch [currentSectionKey]; other tabs refresh on select. */
        private const val SENSOR_ROW_WAITING = "—"
        private const val SENSOR_UI_REFRESH_INTERVAL_MS = 200L
        private const val SCROLL_TO_TOP_VISIBLE_AFTER_POSITION = 2
        private const val STATE_SECTION_KEY = "main_section_key"
        private const val STATE_PAGER_INDEX = "main_pager_index"
    }

    private val scrollToTopScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (recyclerView != currentTabList()) return
            updateScrollToTopVisibility(recyclerView)
            updateSwipeRefreshEnabledForCurrentTab()
        }
    }

    private fun updateScrollToTopVisibility(list: RecyclerView) {
        val layoutManager = list.layoutManager as? LinearLayoutManager ?: return
        val show = layoutManager.findFirstVisibleItemPosition() > SCROLL_TO_TOP_VISIBLE_AFTER_POSITION
        val container = scrollToTopContainer ?: return
        val wasVisible = container.visibility == View.VISIBLE
        if (show && !wasVisible) {
            container.visibility = View.VISIBLE
            container.alpha = 0f
            container.scaleX = 0.85f
            container.scaleY = 0.85f
            container.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200L)
                .start()
        } else if (!show && wasVisible) {
            container.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(150L)
                .withEndAction {
                    container.visibility = View.GONE
                    container.alpha = 1f
                    container.scaleX = 1f
                    container.scaleY = 1f
                }
                .start()
        } else if (show) {
            container.visibility = View.VISIBLE
        }
    }
}

private data class AppConfigSnapshot(
    val languageTag: String,
    val themeMode: AppPreferences.ThemeMode,
    val nightModeFlags: Int,
)
