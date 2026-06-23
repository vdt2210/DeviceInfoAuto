package com.deviceinfo.auto

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/** Frosted-glass scroll-to-top button: blurs list content behind the chip on API 31+. */
object ScrollToTopBlurStyle {

    private const val BLUR_RADIUS_PX = 28f

    fun prepareBlur(container: View, frostLayer: ImageView) {
        if (!supportsLiveBlur()) return
        applyCircularClip(container)
        container.elevation = 6f * container.resources.displayMetrics.density
        frostLayer.setRenderEffect(
            RenderEffect.createBlurEffect(
                BLUR_RADIUS_PX,
                BLUR_RADIUS_PX,
                Shader.TileMode.CLAMP,
            ),
        )
    }

    fun preparePlain(container: View) {
        applyCircularClip(container)
        container.elevation = 0f
    }

    private fun applyCircularClip(container: View) {
        container.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        container.clipToOutline = true
    }

    fun supportsLiveBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun refreshFrostLayer(list: RecyclerView, container: View, frostLayer: ImageView) {
        if (!supportsLiveBlur()) return
        if (container.width <= 0 || container.height <= 0) {
            container.post { refreshFrostLayer(list, container, frostLayer) }
            return
        }
        val captured = captureListRegion(list, container) ?: return
        val previous = frostLayer.drawable
        frostLayer.setImageBitmap(captured)
        if (previous is android.graphics.drawable.BitmapDrawable &&
            previous.bitmap !== captured
        ) {
            previous.bitmap.recycle()
        }
    }

    private fun captureListRegion(list: RecyclerView, target: View): Bitmap? {
        val w = target.width
        val h = target.height
        if (w <= 0 || h <= 0) return null
        return try {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val listPos = IntArray(2)
            val targetPos = IntArray(2)
            list.getLocationOnScreen(listPos)
            target.getLocationOnScreen(targetPos)
            canvas.translate(
                (listPos[0] - targetPos[0]).toFloat(),
                (listPos[1] - targetPos[1]).toFloat(),
            )
            list.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
