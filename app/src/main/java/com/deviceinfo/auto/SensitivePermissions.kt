package com.deviceinfo.auto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Optional runtime permissions for SIM/cellular and Bluetooth rows. */
object SensitivePermissions {

    fun required(context: Context): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        ) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            add(Manifest.permission.READ_PHONE_STATE)
        }
    }

    fun denied(context: Context): List<String> =
        required(context).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    fun label(context: Context, permission: String): String = when (permission) {
        Manifest.permission.READ_PHONE_STATE ->
            context.getString(R.string.permission_label_phone_state)
        Manifest.permission.BLUETOOTH_CONNECT ->
            context.getString(R.string.permission_label_bluetooth_connect)
        else -> permission
    }

    fun missingLabels(context: Context): List<String> =
        denied(context).map { label(context, it) }
}
