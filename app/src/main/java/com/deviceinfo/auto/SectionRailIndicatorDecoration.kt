package com.deviceinfo.auto

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors

/** Draws the shared horizontal/vertical tab indicator and tracks ViewPager2's scroll offset. */
class SectionRailIndicatorDecoration(
    private val rail: RecyclerView,
) : RecyclerView.ItemDecoration() {

    private val indicatorSize = rail.resources.getDimension(R.dimen.tab_active_indicator_size)
    private val itemSize = rail.resources.getDimension(R.dimen.tab_navigation_item_size)
    private val cornerRadius = rail.resources.getDimension(R.dimen.tab_active_indicator_corner_radius)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(
            rail,
            com.google.android.material.R.attr.colorSecondaryContainer,
        )
    }

    private var settledPosition = RecyclerView.NO_POSITION
    private var lowerPosition = RecyclerView.NO_POSITION
    private var pageOffset = 0f

    fun settleOn(position: Int) {
        settledPosition = position
        lowerPosition = RecyclerView.NO_POSITION
        pageOffset = 0f
        rail.invalidate()
    }

    fun moveBetween(position: Int, offset: Float) {
        lowerPosition = position
        pageOffset = offset.coerceIn(0f, 1f)
        rail.invalidate()
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(canvas, parent, state)
        if (state.itemCount == 0) return
        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return
        val centers = indicatorCenters(layoutManager, state.itemCount) ?: return
        val bounds = ActiveIndicatorMotion.axisBounds(
            startCenter = centers.first,
            endCenter = centers.second,
            indicatorSize = indicatorSize,
            progress = pageOffset,
            maxCenterDistance = itemSize,
        )
        val half = indicatorSize / 2f
        if (layoutManager.orientation == RecyclerView.HORIZONTAL) {
            val centerY = parent.paddingTop +
                (parent.height - parent.paddingTop - parent.paddingBottom) / 2f
            canvas.drawRoundRect(
                bounds.start,
                centerY - half,
                bounds.end,
                centerY + half,
                cornerRadius,
                cornerRadius,
                paint,
            )
        } else {
            val centerX = parent.paddingLeft +
                (parent.width - parent.paddingLeft - parent.paddingRight) / 2f
            canvas.drawRoundRect(
                centerX - half,
                bounds.start,
                centerX + half,
                bounds.end,
                cornerRadius,
                cornerRadius,
                paint,
            )
        }
    }

    private fun indicatorCenters(
        layoutManager: LinearLayoutManager,
        itemCount: Int,
    ): Pair<Float, Float>? {
        val lower = lowerPosition.takeIf { it in 0 until itemCount }
        if (lower == null) {
            val settledCenter = itemCenter(layoutManager, settledPosition) ?: return null
            return settledCenter to settledCenter
        }
        val upper = (lower + 1).takeIf { it < itemCount }
        val lowerCenter = itemCenter(layoutManager, lower)
        val upperCenter = upper?.let { itemCenter(layoutManager, it) }
        return when {
            lowerCenter != null && upperCenter != null -> lowerCenter to upperCenter
            lowerCenter != null && upper != null ->
                lowerCenter to (lowerCenter + itemSize * axisDirection(layoutManager))
            upperCenter != null ->
                (upperCenter - itemSize * axisDirection(layoutManager)) to upperCenter
            lowerCenter != null -> lowerCenter to lowerCenter
            else -> itemCenter(layoutManager, settledPosition)?.let { it to it }
        }
    }

    private fun itemCenter(layoutManager: LinearLayoutManager, position: Int): Float? {
        if (position == RecyclerView.NO_POSITION) return null
        val item = layoutManager.findViewByPosition(position) ?: return null
        return if (layoutManager.orientation == RecyclerView.HORIZONTAL) {
            (layoutManager.getDecoratedLeft(item) + layoutManager.getDecoratedRight(item)) / 2f
        } else {
            (layoutManager.getDecoratedTop(item) + layoutManager.getDecoratedBottom(item)) / 2f
        }
    }

    private fun axisDirection(layoutManager: LinearLayoutManager): Float {
        val rtlReversesHorizontal = layoutManager.orientation == RecyclerView.HORIZONTAL &&
            rail.layoutDirection == View.LAYOUT_DIRECTION_RTL
        return if (rtlReversesHorizontal.xor(layoutManager.reverseLayout)) -1f else 1f
    }
}
