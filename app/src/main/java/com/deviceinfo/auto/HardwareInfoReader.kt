package com.deviceinfo.auto

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.AudioManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.PowerManager
import android.util.Size
import java.io.File
import java.util.Locale
import kotlin.math.sqrt

/** Phone-only hardware summaries (camera, NFC, USB, audio, biometrics, thermal). */
object HardwareInfoReader {

    /** [PackageManager.FEATURE_USB_TYPE_C] — not in all public SDK stubs. */
    private const val FEATURE_USB_TYPE_C = "android.hardware.usb.type_c"

    /** [PackageManager.FEATURE_IRIS] — not in all public SDK stubs. */
    private const val FEATURE_IRIS = "android.hardware.biometrics.iris"

    data class Summaries(
        val camera: String,
        val nfc: String,
        val usb: String,
        val audio: String,
        val biometric: String,
        val thermal: String,
    )

    private fun localeContext(context: Context): Context =
        AppPreferences.localizedContext(context)

    fun readSummaries(context: Context): Summaries {
        val ctx = localeContext(context)
        return Summaries(
            camera = readCameraSummary(ctx),
            nfc = readNfcSummary(ctx),
            usb = readUsbSummary(ctx),
            audio = readAudioSummary(ctx),
            biometric = readBiometricSummary(ctx),
            thermal = readThermalSummary(ctx),
        )
    }

    private data class CameraDetail(
        val id: String,
        val facing: Int?,
        val megapixels: Int?,
        val maxSize: Size?,
        val aperture: Float?,
        val focalLengthMm: Float?,
        val hasOis: Boolean,
    )

    fun readCameraSummary(context: Context): String {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return "—"
        val rawIds = runCatching { cm.cameraIdList }.getOrNull()?.toList() ?: return "—"
        if (rawIds.isEmpty()) return "—"

        val ids = enumerateCameraIds(cm, rawIds)
        if (ids.isEmpty()) return "—"

        val details = dedupeCameraDetails(
            ids.mapNotNull { id -> buildCameraDetail(cm, id, rawIds) },
        )
        if (details.isEmpty()) return "—"

        val sorted = details.sortedWith(
            compareBy<CameraDetail>(
                { cameraSortKey(it.facing) },
                { it.focalLengthMm ?: Float.MAX_VALUE },
                { it.id },
            ),
        )

        val lines = mutableListOf<String>()
        for (facing in cameraFacingGroupOrder) {
            val group = sorted.filter { cameraFacingGroupKey(it.facing) == facing }
            if (group.isEmpty()) continue
            lines.add(cameraFacingGroupHeader(context, facing))
            group.forEach { detail ->
                formatCameraBulletLine(context, detail)?.let { lines.add(it) }
            }
        }
        return if (lines.isEmpty()) "—" else DeviceInfoUiShared.joinList(lines)
    }

    /**
     * When a logical camera exposes physical sensors, list **only** those physical IDs
     * (not the logical parent). Standalone IDs in [CameraManager.getCameraIdList] stay as-is.
     */
    private fun enumerateCameraIds(cm: CameraManager, rawIds: List<String>): List<String> {
        val logicalToPhysical = mutableMapOf<String, Set<String>>()
        val allPhysicalChildren = mutableSetOf<String>()

        for (id in rawIds) {
            val chars = runCatching { cm.getCameraCharacteristics(id) }.getOrNull() ?: continue
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val physical = chars.physicalCameraIds
                if (!physical.isNullOrEmpty()) {
                    logicalToPhysical[id] = physical
                    allPhysicalChildren.addAll(physical)
                }
            }
        }

        val result = linkedSetOf<String>()

        for ((logicalId, physicalIds) in logicalToPhysical) {
            val readable = physicalIds.filter { physicalId ->
                runCatching { cm.getCameraCharacteristics(physicalId) }.isSuccess
            }
            if (readable.isNotEmpty()) {
                result.addAll(readable)
            } else {
                result.add(logicalId)
            }
        }

        for (id in rawIds) {
            when {
                id in logicalToPhysical -> Unit
                id in allPhysicalChildren -> if (id !in result) result.add(id)
                else -> result.add(id)
            }
        }

        for (id in probeUndeclaredCameraIds(cm, result)) {
            if (id in logicalToPhysical) continue
            result.add(id)
        }

        return result.toList()
    }

    /** Some OEMs omit secondary sensors from [CameraManager.getCameraIdList] but still expose them by id. */
    private fun probeUndeclaredCameraIds(cm: CameraManager, known: Set<String>): Set<String> {
        val found = linkedSetOf<String>()
        for (i in 0 until 32) {
            val id = i.toString()
            if (id in known) continue
            if (runCatching { cm.getCameraCharacteristics(id) }.isSuccess) {
                found.add(id)
            }
        }
        return found
    }

    private fun dedupeCameraDetails(details: List<CameraDetail>): List<CameraDetail> {
        val seen = linkedSetOf<String>()
        return details.filter { detail ->
            val key = buildString {
                append(detail.facing ?: -1)
                append('|')
                append(detail.megapixels ?: -1)
                append('|')
                append(detail.maxSize?.width ?: -1)
                append('x')
                append(detail.maxSize?.height ?: -1)
                append('|')
                append(detail.focalLengthMm ?: -1f)
                append('|')
                append(detail.aperture ?: -1f)
            }
            seen.add(key)
        }
    }

    private fun buildCameraDetail(
        cm: CameraManager,
        id: String,
        rawIds: List<String>,
    ): CameraDetail? {
        val chars = openCameraCharacteristics(cm, id) ?: return null
        var facing = chars.get(CameraCharacteristics.LENS_FACING)
        if (facing == null) {
            facing = findLogicalParent(cm, id, rawIds)?.let { parentId ->
                openCameraCharacteristics(cm, parentId)?.get(CameraCharacteristics.LENS_FACING)
            }
        }
        val (maxSize, megapixels) = resolveMaxResolution(chars)
        val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        val oisModes = chars.get(LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        val hasOis = oisModes?.contains(
            CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON,
        ) == true
        return CameraDetail(
            id = id,
            facing = facing,
            megapixels = megapixels,
            maxSize = maxSize,
            aperture = apertures?.firstOrNull()?.takeIf { it > 0f },
            focalLengthMm = focalLengths?.firstOrNull()?.takeIf { it > 0f },
            hasOis = hasOis,
        )
    }

    private fun openCameraCharacteristics(cm: CameraManager, id: String): CameraCharacteristics? =
        runCatching { cm.getCameraCharacteristics(id) }.getOrNull()

    private fun findLogicalParent(cm: CameraManager, cameraId: String, rawIds: List<String>): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        for (candidate in rawIds) {
            if (candidate == cameraId) continue
            val physical = openCameraCharacteristics(cm, candidate)
                ?.physicalCameraIds
                ?: continue
            if (cameraId in physical) return candidate
        }
        return null
    }

    private val cameraFacingGroupOrder = listOf(
        CameraCharacteristics.LENS_FACING_BACK,
        CameraCharacteristics.LENS_FACING_FRONT,
        CameraCharacteristics.LENS_FACING_EXTERNAL,
        null,
    )

    private fun cameraFacingGroupKey(facing: Int?): Int? = when (facing) {
        CameraCharacteristics.LENS_FACING_BACK,
        CameraCharacteristics.LENS_FACING_FRONT,
        CameraCharacteristics.LENS_FACING_EXTERNAL,
        -> facing
        else -> null
    }

    private fun cameraSortKey(facing: Int?): Int = when (facing) {
        CameraCharacteristics.LENS_FACING_BACK -> 0
        CameraCharacteristics.LENS_FACING_FRONT -> 1
        CameraCharacteristics.LENS_FACING_EXTERNAL -> 2
        else -> 3
    }

    private fun cameraFacingGroupHeader(context: Context, facing: Int?): String {
        val label = when (facing) {
            CameraCharacteristics.LENS_FACING_BACK -> context.getString(R.string.hw_camera_rear)
            CameraCharacteristics.LENS_FACING_FRONT -> context.getString(R.string.hw_camera_front)
            CameraCharacteristics.LENS_FACING_EXTERNAL -> context.getString(R.string.hw_camera_external)
            else -> context.getString(R.string.hw_camera_other)
        }
        return "$label:"
    }

    private fun formatCameraBulletLine(context: Context, detail: CameraDetail): String? {
        val parts = mutableListOf<String>()
        detail.megapixels?.let { mp ->
            parts.add(context.getString(R.string.hw_camera_mp_value, mp))
        }
        detail.maxSize?.let { size ->
            parts.add(context.getString(R.string.hw_camera_resolution, size.width, size.height))
        }
        detail.aperture?.let { parts.add(context.getString(R.string.hw_camera_aperture, it)) }
        detail.focalLengthMm?.let { parts.add(context.getString(R.string.hw_camera_focal_mm, it)) }
        if (detail.hasOis) parts.add(context.getString(R.string.hw_camera_ois))
        if (parts.isEmpty()) return null
        return DeviceInfoUiShared.BULLET_SEP + parts.joinToString(DeviceInfoUiShared.BULLET_SEP)
    }

    private fun resolveMaxResolution(chars: CameraCharacteristics): Pair<Size?, Int?> {
        var maxArea = 0L
        var maxSize: Size? = null
        fun consider(size: Size?) {
            if (size == null) return
            val w = size.width
            val h = size.height
            if (w <= 0 || h <= 0) return
            val area = w.toLong() * h
            if (area > maxArea) {
                maxArea = area
                maxSize = size
            }
        }

        chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.let { map ->
            collectStreamSizes(map, ::consider)
        }
        consider(chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE))

        val mp = maxSize?.let { megapixelsFromSize(it) }
        return maxSize to mp
    }

    private fun collectStreamSizes(map: StreamConfigurationMap, consider: (Size?) -> Unit) {
        for (format in map.outputFormats) {
            map.getOutputSizes(format)?.forEach { consider(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                map.getHighResolutionOutputSizes(format)?.forEach { consider(it) }
            }
        }
        map.getOutputSizes(SurfaceTexture::class.java)?.forEach { consider(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            map.getHighResolutionOutputSizes(ImageFormat.JPEG)?.forEach { consider(it) }
        }
    }

    private fun megapixelsFromSize(size: Size): Int {
        val mp = size.width.toLong() * size.height / 1_000_000.0
        return kotlin.math.round(mp).toInt().coerceAtLeast(1)
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

    fun readAudioSummary(context: Context): String {
        val pm = context.packageManager
        val parts = buildList {
            if (pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
                add(context.getString(R.string.hw_audio_output))
            }
            if (pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                add(context.getString(R.string.hw_audio_microphone))
            }
            if (pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)) {
                add(context.getString(R.string.hw_audio_pro))
            }
            if (pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)) {
                add(context.getString(R.string.hw_audio_low_latency))
            }
        }
        if (parts.isEmpty()) return context.getString(R.string.value_no)
        return DeviceInfoUiShared.joinList(parts)
    }

    fun readBiometricSummary(context: Context): String {
        val pm = context.packageManager
        val types = buildList {
            if (pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
                add(context.getString(R.string.hw_bio_fingerprint))
            }
            if (pm.hasSystemFeature(PackageManager.FEATURE_FACE)) {
                add(context.getString(R.string.hw_bio_face))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                pm.hasSystemFeature(FEATURE_IRIS)
            ) {
                add(context.getString(R.string.hw_bio_iris))
            }
        }
        if (types.isEmpty()) return context.getString(R.string.value_no)
        return DeviceInfoUiShared.joinList(types)
    }

    fun readThermalSummary(context: Context): String {
        val ctx = localeContext(context)
        val parts = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null) {
                parts.add(thermalStatusLabel(ctx, pm.currentThermalStatus))
            }
        }
        readSocThermalMaxCelsius()?.let { c ->
            parts.add(String.format(Locale.US, "%.1f °C", c))
        }
        return parts.joinToString(DeviceInfoUiShared.COMPOUND_SEP).ifEmpty { "—" }
    }

    private fun thermalStatusLabel(context: Context, status: Int): String =
        when (status) {
            PowerManager.THERMAL_STATUS_NONE ->
                context.getString(R.string.thermal_status_none)
            PowerManager.THERMAL_STATUS_LIGHT ->
                context.getString(R.string.thermal_status_light)
            PowerManager.THERMAL_STATUS_MODERATE ->
                context.getString(R.string.thermal_status_moderate)
            PowerManager.THERMAL_STATUS_SEVERE ->
                context.getString(R.string.thermal_status_severe)
            PowerManager.THERMAL_STATUS_CRITICAL ->
                context.getString(R.string.thermal_status_critical)
            PowerManager.THERMAL_STATUS_EMERGENCY ->
                context.getString(R.string.thermal_status_emergency)
            PowerManager.THERMAL_STATUS_SHUTDOWN ->
                context.getString(R.string.thermal_status_shutdown)
            else -> context.getString(R.string.word_unknown)
        }

    /** Best-effort max SoC/skin temp from sysfs thermal zones (millidegrees). */
    fun readSocThermalMaxCelsius(): Float? {
        val thermalRoot = File("/sys/class/thermal")
        if (!thermalRoot.isDirectory) return null
        var maxC: Float? = null
        thermalRoot.listFiles()?.forEach { zone ->
            if (!zone.name.startsWith("thermal_zone")) return@forEach
            val type = runCatching {
                File(zone, "type").readText().trim().lowercase(Locale.US)
            }.getOrNull() ?: return@forEach
            if (!type.contains("cpu") && !type.contains("soc") && !type.contains("tsens") &&
                !type.contains("shell") && !type.contains("cluster")
            ) {
                return@forEach
            }
            val raw = runCatching { File(zone, "temp").readText().trim().toIntOrNull() }.getOrNull()
                ?: return@forEach
            val c = when {
                raw > 1000 -> raw / 1000f
                else -> raw.toFloat()
            }
            if (c in 1f..120f) {
                maxC = if (maxC == null) c else maxOf(maxC!!, c)
            }
        }
        return maxC
    }

    fun diagonalInchesFromMetrics(
        widthPixels: Int,
        heightPixels: Int,
        xdpi: Float,
        ydpi: Float,
        densityDpi: Int,
    ): Float {
        val xIn = if (xdpi > 0f) widthPixels / xdpi else widthPixels / densityDpi.toFloat()
        val yIn = if (ydpi > 0f) heightPixels / ydpi else heightPixels / densityDpi.toFloat()
        if (xIn <= 0f || yIn <= 0f) return 0f
        return sqrt(xIn * xIn + yIn * yIn)
    }
}
