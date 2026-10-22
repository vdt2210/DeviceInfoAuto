package com.deviceinfo.auto

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.abs

/** SwipeRefreshLayout that yields predominantly horizontal drags to [ViewPager2]. */
class HorizontalScrollAwareSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SwipeRefreshLayout(context, attrs) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var initialX = 0f
    private var initialY = 0f
    private var deferToChild = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                deferToChild = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - initialX)
                val dy = abs(ev.y - initialY)
                if (!deferToChild && dx > touchSlop && dx > dy * 1.2f) {
                    deferToChild = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> deferToChild = false
        }
        if (deferToChild) return false
        return super.onInterceptTouchEvent(ev)
    }
}
