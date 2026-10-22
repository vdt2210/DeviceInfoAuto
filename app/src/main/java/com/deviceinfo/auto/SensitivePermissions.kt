package com.deviceinfo.auto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.car.app.CarContext
import androidx.core.content.ContextCompat

/** Runtime permissions declared in the manifest that the app may request. */
object SensitivePermissions {

    const val CAR_SPEED = "com.google.android.gms.permission.CAR_SPEED"
    const val CAR_FUEL = "com.google.android.gms.permission.CAR_FUEL"
    const val CAR_MILEAGE = "com.google.android.gms.permission.CAR_MILEAGE"

    private val VEHICLE_DATA = listOf(CAR_SPEED, CAR_FUEL, CAR_MILEAGE)

    private val PHONE_RUNTIME_BY_MIN_API: List<Pair<String, Int>> = listOf(
        Manifest.permission.BLUETOOTH_CONNECT to Build.VERSION_CODES.S,
        Manifest.permission.READ_PHONE_STATE to Build.VERSION_CODES.M,
    )

    fun phoneRequired(context: Context): List<String> =
        filterDeclaredByApi(context, PHONE_RUNTIME_BY_MIN_API)

    fun locationRequired(context: Context): List<String> =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            .filter { it in declaredPermissions(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M }

    fun vehicleRequired(context: Context): List<String> =
        VEHICLE_DATA.filter { it in declaredPermissions(context) }

    fun phoneDenied(context: Context): List<String> =
        phoneRequired(context).filter { !isGranted(context, it) }

    fun locationDenied(context: Context): List<String> {
        if (hasLocationPermission(context)) return emptyList()
        return locationRequired(context)
    }

    fun hasLocationPermission(context: Context): Boolean =
        isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasFineLocationPermission(context: Context): Boolean =
        isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun vehicleDenied(context: Context): List<String> =
        vehicleRequired(context).filter { !isGranted(context, it) }

    fun denied(context: Context): List<String> =
        phoneDenied(context) + locationDenied(context) + vehicleDenied(context)

    fun activityRequestDenied(context: Context): List<String> = denied(context)

    fun allGranted(context: Context): Boolean = denied(context).isEmpty()

    fun label(context: Context, permission: String): String = when (permission) {
        Manifest.permission.READ_PHONE_STATE ->
            context.getString(R.string.permission_label_phone_state)
        Manifest.permission.BLUETOOTH_CONNECT ->
            context.getString(R.string.permission_label_bluetooth_connect)
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        ->
            context.getString(R.string.permission_label_location)
        CAR_SPEED, CAR_FUEL, CAR_MILEAGE ->
            context.getString(R.string.permission_label_vehicle_data)
        else -> permission
    }

    fun missingLabels(context: Context): List<String> {
        val labels = phoneDenied(context).map { label(context, it) }.toMutableList()
        if (locationDenied(context).isNotEmpty()) {
            labels.add(context.getString(R.string.permission_label_location))
        }
        if (vehicleDenied(context).isNotEmpty()) {
            labels.add(context.getString(R.string.permission_label_vehicle_data))
        }
        return labels.distinct()
    }

    fun requestVehiclePermissions(carContext: CarContext, onComplete: () -> Unit) {
        val denied = vehicleDenied(carContext)
        if (denied.isEmpty()) {
            onComplete()
            return
        }
        carContext.requestPermissions(
            denied.toMutableList(),
            carContext.mainExecutor,
        ) { _, _ -> onComplete() }
    }

    fun requestLocationPermissions(carContext: CarContext, onComplete: () -> Unit) {
        val denied = locationDenied(carContext)
        if (denied.isEmpty()) {
            onComplete()
            return
        }
        carContext.requestPermissions(
            denied.toMutableList(),
            carContext.mainExecutor,
        ) { _, _ -> onComplete() }
    }

    private fun filterDeclaredByApi(
        context: Context,
        entries: List<Pair<String, Int>>,
    ): List<String> {
        val declared = declaredPermissions(context)
        val sdk = Build.VERSION.SDK_INT
        return entries
            .filter { (permission, minApi) -> sdk >= minApi && permission in declared }
            .map { it.first }
    }

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun declaredPermissions(context: Context): Set<String> {
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        return info.requestedPermissions?.toSet().orEmpty()
    }
}
