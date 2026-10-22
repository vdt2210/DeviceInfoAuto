package com.deviceinfo.auto

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton

/**
 * App [MaterialButton] — disabled styling comes from theme color selectors
 * ([R.color.btn_primary_background_tint], [R.color.btn_danger_background_tint], etc.)
 * using [android:state_enabled] and Material's standard 38% content alpha.
 */
class DeviceInfoButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.deviceInfoButtonStyle,
) : MaterialButton(context, attrs, defStyleAttr)
