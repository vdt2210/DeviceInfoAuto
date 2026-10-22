package com.deviceinfo.auto

import android.location.Location
import android.location.LocationManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationInfoStoreTest {

    @After
    fun tearDown() {
        LocationInfoStore.clearMotion()
    }

    @Test
    fun snapshots_keepGpsAndNetworkDetailsSeparate() {
        LocationInfoStore.updateFromLocation(
            fix(LocationManager.NETWORK_PROVIDER, time = 10_000L, accuracy = 60f),
        )
        LocationInfoStore.updateFromLocation(
            fix(LocationManager.GPS_PROVIDER, time = 11_000L, accuracy = 6f),
        )

        val snapshot = LocationInfoStore.motionSnapshot()
        assertEquals(LocationManager.GPS_PROVIDER, snapshot.gpsFix.provider)
        assertEquals(LocationManager.NETWORK_PROVIDER, snapshot.networkFix.provider)
        assertEquals(6f, snapshot.gpsFix.accuracyMeters)
        assertEquals(60f, snapshot.networkFix.accuracyMeters)
    }

    @Test
    fun providerSnapshots_ignorePassiveCachedLocation() {
        LocationInfoStore.updateFromLocation(
            fix(LocationManager.NETWORK_PROVIDER, time = 10_000L, accuracy = 100f),
        )
        LocationInfoStore.updateFromLocation(
            fix(LocationManager.PASSIVE_PROVIDER, time = 11_000L, accuracy = 12f),
        )

        val snapshot = LocationInfoStore.motionSnapshot()
        assertEquals(100f, snapshot.networkFix.accuracyMeters)
        assertFalse(snapshot.gpsFix.hasFix)
    }

    private fun fix(provider: String, time: Long, accuracy: Float): Location =
        Location(provider).apply {
            this.time = time
            this.accuracy = accuracy
        }
}
