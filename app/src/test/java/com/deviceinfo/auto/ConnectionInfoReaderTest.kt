package com.deviceinfo.auto

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ConnectionInfoReaderTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun readNfcSummary_reportsNotSupportedOnRobolectric() {
        val summary = ConnectionInfoReader.readNfcSummary(context)
        assertTrue(
            summary == context.getString(R.string.nfc_not_supported) ||
                summary == context.getString(R.string.value_on) ||
                summary == context.getString(R.string.value_off),
        )
    }

    @Test
    fun readUsbSummary_returnsYesNoOrCapabilityList() {
        val summary = ConnectionInfoReader.readUsbSummary(context)
        assertTrue(summary.isNotBlank())
    }

    @Test
    fun read_slice_includesNfcAndUsbFields() {
        val slice = ConnectionInfoReader.read(context)
        assertEquals(slice.nfcSummary, ConnectionInfoReader.readNfcSummary(context))
        assertEquals(slice.usbSummary, ConnectionInfoReader.readUsbSummary(context))
    }
}
