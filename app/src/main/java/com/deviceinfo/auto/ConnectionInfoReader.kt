package com.deviceinfo.auto

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRouter
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Short-range links and device connection state for the Connection tab. */
object ConnectionInfoReader {

    private const val FEATURE_UWB = "android.hardware.uwb"
    private const val FEATURE_USB_TYPE_C = "android.hardware.usb.type_c"

    data class Slice(
        val bluetoothCapable: Boolean,
        val bluetoothOn: Boolean?,
        val bluetoothTypeSummary: String,
        val nfcSummary: String,
        val usbSummary: String,
        val castCapable: Boolean,
        val castSummary: String,
        val wifiDirectSummary: String,
        val uwbSummary: String,
        val airplaneModeOn: Boolean?,
    )

    fun read(context: Context): Slice {
        val pm = context.packageManager
        val bluetoothCapable = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        val adapter = if (bluetoothCapable) bluetoothAdapterOrNull(context) else null
        val canReadBt = bluetoothCapable && canReadBluetoothState(context)
        val bluetoothOn = if (canReadBt) {
            runCatching { adapter?.isEnabled }.getOrNull()
        } else {
            null
        }
        val (castCapable, castSummary) = readCast(context)
        return Slice(
            bluetoothCapable = bluetoothCapable,
            bluetoothOn = bluetoothOn,
            bluetoothTypeSummary = readBluetoothTypeSummary(context, adapter),
            nfcSummary = readNfcSummary(context),
            usbSummary = readUsbSummary(context),
            castCapable = castCapable,
            castSummary = castSummary,
            wifiDirectSummary = readWifiDirectSummary(context),
            uwbSummary = readUwbSummary(context),
            airplaneModeOn = readAirplaneModeOn(context),
        )
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

    private fun readBluetoothTypeSummary(context: Context, adapter: BluetoothAdapter?): String {
        val pm = context.packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            return context.getString(R.string.bluetooth_not_supported)
        }
        val hasLe = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val parts = buildList {
            add(context.getString(R.string.bluetooth_type_classic))
            if (hasLe) {
                add(context.getString(R.string.bluetooth_type_le))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && adapter != null) {
                val leAudioSupported = runCatching { adapter.isLeAudioSupported }.getOrDefault(0)
                if (leAudioSupported == android.bluetooth.BluetoothStatusCodes.FEATURE_SUPPORTED) {
                    add(context.getString(R.string.bluetooth_type_le_audio))
                }
            }
        }
        return DeviceInfoUiShared.joinList(parts)
    }

    fun readNfcSummary(context: Context): String {
        val pm = context.packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            return context.getString(R.string.nfc_not_supported)
        }
        val adapter = NfcAdapter.getDefaultAdapter(context)
            ?: return context.getString(R.string.nfc_not_supported)
        return if (adapter.isEnabled) {
            context.getString(R.string.value_on)
        } else {
            context.getString(R.string.value_off)
        }
    }

    fun readUsbSummary(context: Context): String {
        val pm = context.packageManager
        val parts = buildList {
            if (pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
                add(context.getString(R.string.hw_usb_host))
            }
            if (pm.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY)) {
                add(context.getString(R.string.hw_usb_accessory))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                pm.hasSystemFeature(FEATURE_USB_TYPE_C)
            ) {
                add(context.getString(R.string.hw_usb_type_c))
            }
        }
        if (parts.isEmpty()) return context.getString(R.string.value_no)
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? android.hardware.usb.UsbManager
        val attached = usbManager?.deviceList?.size ?: 0
        return if (attached > 0) {
            DeviceInfoUiShared.joinList(
                parts + context.getString(R.string.hw_usb_devices_attached, attached),
            )
        } else {
            DeviceInfoUiShared.joinList(parts)
        }
    }

    @Suppress("DEPRECATION")
    private fun readCast(context: Context): Pair<Boolean, String> {
        return try {
            val router = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? MediaRouter
                ?: return false to context.getString(R.string.cast_not_supported)
            val phoneLabel = context.getString(R.string.cast_route_phone)
            val names = buildList {
                for (i in 0 until router.routeCount) {
                    val route = router.getRouteAt(i)
                    if (!route.isEnabled) continue
                    if (route.playbackType != MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE) continue
                    val name = route.name?.toString()?.trim().orEmpty()
                    if (name.isNotEmpty() && !name.equals(phoneLabel, ignoreCase = true)) {
                        add(name)
                    }
                }
            }
            true to if (names.isEmpty()) {
                context.getString(R.string.cast_none)
            } else {
                DeviceInfoUiShared.joinList(names.distinct())
            }
        } catch (_: Exception) {
            false to context.getString(R.string.word_unknown)
        }
    }

    private fun readWifiDirectSummary(context: Context): String {
        val pm = context.packageManager
        return if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            context.getString(R.string.value_yes)
        } else {
            context.getString(R.string.value_no)
        }
    }

    private fun readUwbSummary(context: Context): String {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            pm.hasSystemFeature(FEATURE_UWB)
        ) {
            context.getString(R.string.value_yes)
        } else {
            context.getString(R.string.value_no)
        }
    }

    private fun readAirplaneModeOn(context: Context): Boolean? =
        runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0,
            ) == 1
        }.getOrNull()
}
