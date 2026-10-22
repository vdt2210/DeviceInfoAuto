package com.deviceinfo.auto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarScreenRefreshTest {
    private val row = CarScreenRowModel("Speed", listOf("10 km/h"), 1)
    private val model = CarScreenRenderModel(
        title = "Device Info",
        sections = listOf(CarScreenSectionModel("Vehicle", listOf(row))),
    )

    @Test
    fun delayUntilAllowed_enforcesSingleGlobalInterval() {
        assertEquals(0L, CarScreenRefresh.delayUntilAllowed(1_000L, 0L, 2_000L))
        assertEquals(1_500L, CarScreenRefresh.delayUntilAllowed(10_500L, 10_000L, 2_000L))
        assertEquals(0L, CarScreenRefresh.delayUntilAllowed(12_000L, 10_000L, 2_000L))
    }

    @Test
    fun shouldRefresh_comparesRenderedTextAndIcons() {
        assertFalse(CarScreenRefresh.shouldRefresh(model, model.copy(), force = false))
        assertTrue(
            CarScreenRefresh.shouldRefresh(
                model,
                model.copy(sections = listOf(CarScreenSectionModel("Vehicle", listOf(row.copy(texts = listOf("12 km/h"))))))),
                force = false,
            ),
        )
        assertTrue(
            CarScreenRefresh.shouldRefresh(
                model,
                model.copy(sections = listOf(CarScreenSectionModel("Vehicle", listOf(row.copy(iconResId = 2))))),
                force = false,
            ),
        )
    }

    @Test
    fun shouldRefresh_manualRefreshCanForceUnchangedModel() {
        assertTrue(CarScreenRefresh.shouldRefresh(model, model, force = true))
    }

    @Test
    fun renderModel_keepsMissingRowsInStableOrder() {
        val missing = row.copy(title = "Fuel", texts = listOf("-"), iconResId = 2)
        val before = model.copy(
            sections = listOf(CarScreenSectionModel("Vehicle", listOf(row, missing))),
        )
        val after = before.copy(
            sections = listOf(
                CarScreenSectionModel(
                    "Vehicle",
                    listOf(row, missing.copy(texts = listOf("50%"))),
                ),
            ),
        )

        assertEquals(before.sections.map { it.rows.map(CarScreenRowModel::title) }, after.sections.map { it.rows.map(CarScreenRowModel::title) })
        assertEquals(before.sections.sumOf { it.rows.size }, after.sections.sumOf { it.rows.size })
    }
}
