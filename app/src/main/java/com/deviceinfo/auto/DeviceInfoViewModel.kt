package com.deviceinfo.auto

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Partial row update flags for [DeviceInfoUiEvent.Refresh]. */
data class DeviceInfoUpdateFlags(
    /** Level, plugged, health, temp, charge counter, power saver (broadcast only). */
    val updateBatteryBroadcast: Boolean = false,
    /** Current, voltage, power, charge-time estimate (poll only). */
    val updateBatteryPoll: Boolean = false,
    /** Periodic poll — Wi‑Fi / mobile signal strength and per-radio link bandwidth. */
    val updateNetworkPoll: Boolean = false,
    val updateMemory: Boolean = false,
    val updateProcessor: Boolean = false,
    val updateNetwork: Boolean = false,
    val updateDisplay: Boolean = false,
    val updateVehicle: Boolean = false,
    val updateLocation: Boolean = false,
    /** Reformat temperature / distance rows after unit preference change. */
    val updateDisplayUnits: Boolean = false,
)

sealed class DeviceInfoUiEvent {
    /** Rebuild the full phone list (schema may change). */
    data class FullList(val info: DeviceInfo) : DeviceInfoUiEvent()

    /** Patch visible rows from a light [DeviceInfoProvider.get] slice. */
    data class Refresh(
        val info: DeviceInfo,
        val flags: DeviceInfoUpdateFlags,
        /** When set, rebuild only this section (pull-to-refresh on one tab). */
        val targetSectionKey: String? = null,
    ) : DeviceInfoUiEvent()
}

class DeviceInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private fun localizedContext(): Context = AppPreferences.localizedContext(appContext)

    private val _uiEvent = MutableLiveData<DeviceInfoUiEvent?>()
    val uiEvent: LiveData<DeviceInfoUiEvent?> = _uiEvent

    fun consumeEvent() {
        _uiEvent.value = null
    }

    fun invalidateHardwareAndLoadFull() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                DeviceInfoProvider.invalidateHardwareCache()
            }
            loadFull()
        }
    }

    fun loadFull() {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                DeviceInfoProvider.get(localizedContext(), DeviceInfoProvider.GetOptions.Full)
            }
            _uiEvent.value = DeviceInfoUiEvent.FullList(info)
        }
    }

    /** CarInfo listener push — vehicle tab rows only (no poll). */
    fun refreshVehicle() {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                DeviceInfoProvider.refreshVehicle(localizedContext())
            }
            _uiEvent.value =
                DeviceInfoUiEvent.Refresh(
                    info,
                    DeviceInfoUpdateFlags(updateVehicle = true),
                    targetSectionKey = DeviceInfoUiShared.Section.VEHICLE,
                )
        }
    }

    /** Location listener / fix-age tick — location tab rows only. */
    fun refreshLocation() {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                DeviceInfoProvider.refreshLocation(localizedContext())
            }
            _uiEvent.value =
                DeviceInfoUiEvent.Refresh(
                    info,
                    DeviceInfoUpdateFlags(updateLocation = true),
                    targetSectionKey = DeviceInfoUiShared.Section.LOCATION,
                )
        }
    }

    fun refreshDisplayUnits(sectionKey: String) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                DeviceInfoProvider.refreshDisplayUnits(localizedContext(), sectionKey)
            }
            _uiEvent.value =
                DeviceInfoUiEvent.Refresh(
                    info,
                    DeviceInfoUpdateFlags(updateDisplayUnits = true),
                    targetSectionKey = sectionKey,
                )
        }
    }

    fun refreshTab(sectionKey: String) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                DeviceInfoProvider.refreshSection(localizedContext(), sectionKey)
            }
            _uiEvent.value =
                DeviceInfoUiEvent.Refresh(
                    info = info,
                    flags = DeviceInfoUpdateFlags(),
                    targetSectionKey = sectionKey,
                )
        }
    }

    fun refresh(
        options: DeviceInfoProvider.GetOptions,
        flags: DeviceInfoUpdateFlags,
    ) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                DeviceInfoProvider.get(localizedContext(), options)
            }
            _uiEvent.value = DeviceInfoUiEvent.Refresh(info, flags)
        }
    }

    /** [Intent.ACTION_BATTERY_CHANGED] / power saver — broadcast-owned battery fields only. */
    fun refreshBatteryBroadcast(batteryIntent: android.content.Intent? = null) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                DeviceInfoProvider.refreshBatteryBroadcast(localizedContext(), batteryIntent)
            }
            _uiEvent.value =
                DeviceInfoUiEvent.Refresh(
                    info,
                    DeviceInfoUpdateFlags(updateBatteryBroadcast = true),
                )
        }
    }

    /** Periodic telemetry — poll-owned battery fields only (current, voltage, power, charge ETA). */
    fun refreshBatteryPoll() {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                DeviceInfoProvider.refreshBatteryPoll(localizedContext())
            }
            _uiEvent.value =
                DeviceInfoUiEvent.Refresh(
                    info,
                    DeviceInfoUpdateFlags(updateBatteryPoll = true),
                )
        }
    }

    /** One-shot refresh when the activity returns to the foreground (visible tab only). */
    fun refreshOnResume(sectionKey: String) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                DeviceInfoProvider.refreshSection(localizedContext(), sectionKey)
            }
            _uiEvent.value =
                DeviceInfoUiEvent.Refresh(
                    info,
                    DeviceInfoUpdateFlags(),
                    targetSectionKey = sectionKey,
                )
        }
    }
}
