package com.deviceinfo.auto

import android.view.View
import android.widget.ImageButton
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat

fun AppCompatActivity.setContentWithEdgeBarsAndUpToolbar(
    @LayoutRes layoutResId: Int,
    @StringRes titleResId: Int,
) {
    applyTransparentSystemBars()
    setContentView(layoutResId)
    findViewById<View>(R.id.root).applyLegacyStatusBarInset()
    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setTitle(titleResId)
    toolbar.tightenNavigationAndClearNavTooltip()
}

fun AppCompatActivity.defaultFinishOnNavigateUp(): Boolean {
    finish()
    return true
}

fun Toolbar.tightenNavigationAndClearNavTooltip() {
    setContentInsetStartWithNavigation(0)
    post {
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            if (v is ImageButton) {
                ViewCompat.setTooltipText(v, null)
                break
            }
        }
    }
}
