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
    val updateBattery: Boolean = false,
    val updateMemory: Boolean = false,
    val updateProcessor: Boolean = false,
    val updateNetwork: Boolean = false,
    val updateDisplay: Boolean = false,
)

sealed class DeviceInfoUiEvent {
    /** Rebuild the full phone list (schema may change). */
    data class FullList(val info: DeviceInfo) : DeviceInfoUiEvent()

    /** Patch visible rows from a light [DeviceInfoProvider.get] slice. */
    data class Refresh(
        val info: DeviceInfo,
        val flags: DeviceInfoUpdateFlags,
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
            _uiEvent.postValue(DeviceInfoUiEvent.FullList(info))
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
            _uiEvent.postValue(DeviceInfoUiEvent.Refresh(info, flags))
        }
    }
}
