package com.deviceinfo.auto

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private const val NAV_BAR_MODE_GESTURAL = 2

fun AppCompatActivity.applyTransparentSystemBars() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.navigationBarColor = Color.TRANSPARENT
    window.statusBarColor = Color.TRANSPARENT

    val controller = WindowInsetsControllerCompat(window, window.decorView)
    val isNightMode =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    controller.isAppearanceLightStatusBars = !isNightMode
    controller.isAppearanceLightNavigationBars = !isNightMode
}

/** True when the device uses swipe/gesture navigation instead of 3-button bar. */
fun Context.usesGestureNavigation(): Boolean {
    val resId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
    if (resId != 0) {
        return resources.getInteger(resId) == NAV_BAR_MODE_GESTURAL
    }
    return false
}

/**
 * Top/side: status bar + cutout always.
 * Bottom: navigation-bar inset only for 3-button / 2-button nav — gesture nav stays edge-to-edge.
 */
fun View.applySystemBarInsets() {
    val initialPaddingLeft = paddingLeft
    val initialPaddingTop = paddingTop
    val initialPaddingRight = paddingRight
    val initialPaddingBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val ctx = v.context
        val statusAndCutout = insets.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val bottomInset = if (ctx.usesGestureNavigation()) 0 else navBars.bottom
        v.setPadding(
            initialPaddingLeft + maxOf(statusAndCutout.left, navBars.left),
            initialPaddingTop + statusAndCutout.top,
            initialPaddingRight + maxOf(statusAndCutout.right, navBars.right),
            initialPaddingBottom + bottomInset,
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

@Deprecated("Use applySystemBarInsets", ReplaceWith("applySystemBarInsets()"))
fun View.applyLegacyStatusBarInset() = applySystemBarInsets()
