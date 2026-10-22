package com.deviceinfo.auto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareInfoReaderThermalTest {

    @Test
    fun preferredSocThermalZoneType_matchesCommonOemNames() {
        assertTrue(HardwareInfoReader.isPreferredSocThermalZoneType("cpu-0-0"))
        assertTrue(HardwareInfoReader.isPreferredSocThermalZoneType("soc_max"))
        assertTrue(HardwareInfoReader.isPreferredSocThermalZoneType("ap_skin"))
        assertTrue(HardwareInfoReader.isPreferredSocThermalZoneType("mtkts_cpu"))
        assertTrue(HardwareInfoReader.isPreferredSocThermalZoneType("gpu-throttle-alert"))
        assertTrue(HardwareInfoReader.isPreferredSocThermalZoneType("cluster1"))
    }

    @Test
    fun peripheralThermalZoneType_excludesBatteryAndRadios() {
        assertTrue(HardwareInfoReader.isPeripheralThermalZoneType("battery"))
        assertTrue(HardwareInfoReader.isPeripheralThermalZoneType("usb_port"))
        assertTrue(HardwareInfoReader.isPeripheralThermalZoneType("wlan"))
        assertFalse(HardwareInfoReader.isPeripheralThermalZoneType("cpu0"))
    }

    @Test
    fun parseThermalZoneTempCelsius_handlesMilliAndWholeDegrees() {
        assertEquals(42.5f, HardwareInfoReader.parseThermalZoneTempCelsius(42500)!!, 0.01f)
        assertEquals(45f, HardwareInfoReader.parseThermalZoneTempCelsius(45)!!, 0.01f)
        assertNull(HardwareInfoReader.parseThermalZoneTempCelsius(0))
        assertNull(HardwareInfoReader.parseThermalZoneTempCelsius(150000))
    }
}
