package com.deviceinfo.auto

import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun AppCompatActivity.applyTransparentSystemBars() {
    window.navigationBarColor = Color.TRANSPARENT
    window.statusBarColor = Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

    val controller = WindowInsetsControllerCompat(window, window.decorView)
    val isNightMode =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    controller.isAppearanceLightStatusBars = !isNightMode
    controller.isAppearanceLightNavigationBars = !isNightMode
}

fun View.applyLegacyStatusBarInset() {
    val initialPaddingTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val sysBars = insets.getInsets(
            WindowInsetsCompat.Type.statusBars() or
                WindowInsetsCompat.Type.displayCutout(),
        )
        v.setPadding(
            v.paddingLeft,
            initialPaddingTop + sysBars.top,
            v.paddingRight,
            v.paddingBottom,
        )
        insets
    }
}
