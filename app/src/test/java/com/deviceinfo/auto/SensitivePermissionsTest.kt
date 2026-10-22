package com.deviceinfo.auto

import android.Manifest
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class SensitivePermissionsTest {

    @Test
    fun denied_listsDeclaredRuntimePermissionsNotGranted() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val shadowApp = Shadows.shadowOf(context.applicationContext as android.app.Application)
        shadowApp.denyPermissions(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH_CONNECT,
            SensitivePermissions.CAR_SPEED,
            SensitivePermissions.CAR_FUEL,
            SensitivePermissions.CAR_MILEAGE,
        )
        val denied = SensitivePermissions.denied(context)
        assertTrue(Manifest.permission.READ_PHONE_STATE in denied)
        assertTrue(Manifest.permission.BLUETOOTH_CONNECT in denied)
        assertTrue(SensitivePermissions.CAR_SPEED in denied)
        assertFalse(SensitivePermissions.allGranted(context))
        assertTrue(SensitivePermissions.missingLabels(context).contains("Android Auto vehicle data"))
    }

    @Test
    fun allGranted_whenEveryRuntimePermissionGranted() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val shadowApp = Shadows.shadowOf(context.applicationContext as android.app.Application)
        shadowApp.grantPermissions(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            SensitivePermissions.CAR_SPEED,
            SensitivePermissions.CAR_FUEL,
            SensitivePermissions.CAR_MILEAGE,
        )
        assertTrue(SensitivePermissions.allGranted(context))
    }

    @Test
    fun vehicleDenied_includedInMissingLabelsOnce() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val shadowApp = Shadows.shadowOf(context.applicationContext as android.app.Application)
        shadowApp.grantPermissions(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
        shadowApp.denyPermissions(
            SensitivePermissions.CAR_SPEED,
            SensitivePermissions.CAR_FUEL,
            SensitivePermissions.CAR_MILEAGE,
        )
        val labels = SensitivePermissions.missingLabels(context)
        assertTrue(labels.contains("Android Auto vehicle data"))
        assertEquals(1, labels.count { it == "Android Auto vehicle data" })
    }

    @Test
    fun activityRequestDenied_includesVehiclePermissions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val shadowApp = Shadows.shadowOf(context.applicationContext as android.app.Application)
        shadowApp.grantPermissions(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
        shadowApp.denyPermissions(
            SensitivePermissions.CAR_SPEED,
            SensitivePermissions.CAR_FUEL,
            SensitivePermissions.CAR_MILEAGE,
        )
        val denied = SensitivePermissions.activityRequestDenied(context)
        assertTrue(SensitivePermissions.CAR_SPEED in denied)
        assertTrue(SensitivePermissions.CAR_FUEL in denied)
        assertTrue(SensitivePermissions.CAR_MILEAGE in denied)
    }

    @Test
    fun activityRequestDenied_includesLocation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val shadowApp = Shadows.shadowOf(context.applicationContext as android.app.Application)
        shadowApp.grantPermissions(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH_CONNECT,
            SensitivePermissions.CAR_SPEED,
            SensitivePermissions.CAR_FUEL,
            SensitivePermissions.CAR_MILEAGE,
        )
        shadowApp.denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        assertTrue(
            SensitivePermissions.activityRequestDenied(context)
                .contains(Manifest.permission.ACCESS_FINE_LOCATION),
        )
        assertTrue(SensitivePermissions.locationDenied(context).isNotEmpty())
    }
}
