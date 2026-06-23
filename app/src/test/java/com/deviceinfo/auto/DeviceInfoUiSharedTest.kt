package com.deviceinfo.auto

import android.view.Surface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DeviceInfoUiSharedTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun temperatureText_formatsCelsius() {
        assertEquals("25.0 °C", DeviceInfoUiShared.temperatureText(25f))
    }

    @Test
    fun joinCompound_phoneUsesNewlines() {
        val joined = DeviceInfoUiShared.joinCompound(
            listOf("A", "B"),
            DeviceInfoUiShared.DisplaySurface.PHONE,
        )
        assertEquals("A\nB", joined)
    }

    @Test
    fun joinCompound_carUsesBullet() {
        val joined = DeviceInfoUiShared.joinCompound(
            listOf("A", "B"),
            DeviceInfoUiShared.DisplaySurface.CAR,
        )
        assertEquals("A • B", joined)
    }

    @Test
    fun listSummaryForDisplay_carReplacesNewlines() {
        val raw = "Line1\nLine2"
        val out = DeviceInfoUiShared.listSummaryForDisplay(
            context,
            raw,
            DeviceInfoUiShared.DisplaySurface.CAR,
        )
        assertEquals("Line1 • Line2", out)
    }

    @Test
    fun levelIconRes_powerSaveCapsEffectiveLevel() {
        val normal = DeviceInfoUiShared.levelIconRes(80, isPowerSaveMode = false)
        val saver = DeviceInfoUiShared.levelIconRes(80, isPowerSaveMode = true)
        assertNotEquals(normal, saver)
        assertEquals(
            DeviceInfoUiShared.levelIconRes(62, isPowerSaveMode = false),
            saver,
        )
    }

    @Test
    fun displayRotationLabel_knownRotations() {
        assertEquals(
            context.getString(R.string.display_rotation_0),
            DeviceInfoUiShared.displayRotationLabel(context, Surface.ROTATION_0),
        )
        assertEquals(
            context.getString(R.string.display_rotation_90),
            DeviceInfoUiShared.displayRotationLabel(context, Surface.ROTATION_90),
        )
    }

    @Test
    fun multiNetworkSummaryDisplay_translatesNoneToken() {
        val out = DeviceInfoUiShared.multiNetworkSummaryDisplay(
            context,
            DeviceInfoUiShared.MultiNetworkToken.NONE,
        )
        assertEquals(context.getString(R.string.multi_network_none), out)
    }

    @Test
    fun multiNetworkSummaryDisplay_translatesCombinedLabels() {
        val raw = listOf(
            DeviceInfoUiShared.MultiNetworkToken.WIFI,
            DeviceInfoUiShared.MultiNetworkToken.CELLULAR,
        ).joinToString(DeviceInfoUiShared.LIST_LINE_SEP)
        val out = DeviceInfoUiShared.multiNetworkSummaryDisplay(context, raw)
        assertEquals(
            listOf(
                context.getString(R.string.network_wifi_short),
                context.getString(R.string.network_cellular),
            ).joinToString(DeviceInfoUiShared.LIST_LINE_SEP),
            out,
        )
    }

    @Test
    fun cellularNetworkLabelDisplay_mapsLteAnd5g() {
        assertEquals(
            context.getString(R.string.cellular_network_lte),
            DeviceInfoUiShared.cellularNetworkLabelDisplay(context, "LTE"),
        )
        assertEquals(
            context.getString(R.string.cellular_network_5g),
            DeviceInfoUiShared.cellularNetworkLabelDisplay(context, "5G NR"),
        )
    }

    @Test
    fun rowTitle_knownKeys_notRawKey() {
        val title = DeviceInfoUiShared.rowTitle(context, DeviceInfoUiShared.Row.LEVEL)
        assertNotEquals(DeviceInfoUiShared.Row.LEVEL, title)
    }
}
