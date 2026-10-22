package com.deviceinfo.auto

internal data class CarScreenRowModel(
    val title: String,
    val texts: List<String>,
    val iconResId: Int,
)

internal data class CarScreenSectionModel(
    val title: String,
    val rows: List<CarScreenRowModel>,
)

internal data class CarScreenRenderModel(
    val title: String,
    val sections: List<CarScreenSectionModel>,
)

/** Pure refresh decisions kept separate from Android scheduling for unit testing. */
internal object CarScreenRefresh {
    fun delayUntilAllowed(nowMs: Long, lastRefreshAtMs: Long, minimumIntervalMs: Long): Long {
        if (lastRefreshAtMs <= 0L) return 0L
        return (minimumIntervalMs - (nowMs - lastRefreshAtMs)).coerceAtLeast(0L)
    }

    fun shouldRefresh(
        displayed: CarScreenRenderModel?,
        candidate: CarScreenRenderModel,
        force: Boolean,
    ): Boolean = force || displayed != candidate
}
