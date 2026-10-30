package com.sam.gogotimeline.core.time

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomRulesTest {
    @Test
    fun clampsZoomLimits() {
        assertEquals(256f, ZoomRules.clampDpPerUnit(TimeUnit.HOUR, 300f, ONE_X), EPSILON)
        assertEquals(45f, ZoomRules.clampDpPerUnit(TimeUnit.MONTH, 30f, ONE_X), EPSILON)
        assertEquals(1_000f, ZoomRules.clampDpPerUnit(TimeUnit.DAY, 1_000f, ONE_X), EPSILON)
    }

    @Test
    fun zoomingInOnDaySwitchesToHour() {
        assertEquals(TimeUnit.HOUR, ZoomRules.resolveUnit(TimeUnit.DAY, 400f, ONE_X))
    }

    @Test
    fun zoomingOutOnDaySwitchesToWeek() {
        assertEquals(TimeUnit.WEEK, ZoomRules.resolveUnit(TimeUnit.DAY, 25f, ONE_X))
    }

    @Test
    fun unitButtonReturnsToStandardThenMovesOn() {
        assertEquals(ZoomLevel(TimeUnit.DAY, 64f), ZoomRules.unitButtonTarget(TimeUnit.DAY, 90f, ONE_X))
        assertEquals(ZoomLevel(TimeUnit.WEEK, 112f), ZoomRules.unitButtonTarget(TimeUnit.DAY, 64f, ONE_X))
    }

    @Test
    fun bufferZoneKeepsCurrentUnit() {
        // Both mean one day is 300dp wide.
        assertEquals(TimeUnit.DAY, ZoomRules.resolveUnit(TimeUnit.DAY, 300f, ONE_X))
        assertEquals(TimeUnit.HOUR, ZoomRules.resolveUnit(TimeUnit.HOUR, 12.5f, ONE_X))
    }

    @Test
    fun justBelowAndAboveThresholds() {
        assertEquals(TimeUnit.DAY, ZoomRules.resolveUnit(TimeUnit.DAY, 391f, ONE_X))
        assertEquals(TimeUnit.HOUR, ZoomRules.resolveUnit(TimeUnit.DAY, 392f, ONE_X))
        assertEquals(TimeUnit.HOUR, ZoomRules.resolveUnit(TimeUnit.HOUR, 10.5f, ONE_X))
        assertEquals(TimeUnit.DAY, ZoomRules.resolveUnit(TimeUnit.HOUR, 10.4f, ONE_X))
    }

    @Test
    fun standardWidthsStayInTheirOwnUnit() {
        TimeUnit.entries.forEach { unit ->
            assertEquals(unit, ZoomRules.resolveUnit(unit, unit.standardWidthDp(ONE_X), ONE_X))
        }
    }

    @Test
    fun unitButtonWrapsFromMonthToHour() {
        assertEquals(ZoomLevel(TimeUnit.HOUR, 64f), ZoomRules.unitButtonTarget(TimeUnit.MONTH, 90f, ONE_X))
    }

    @Test
    fun tinyFloatErrorCountsAsStandardWidth() {
        assertEquals(ZoomLevel(TimeUnit.WEEK, 112f), ZoomRules.unitButtonTarget(TimeUnit.DAY, 64.00001f, ONE_X))
    }

    @Test
    fun fastPinchJumpsTwoUnits() {
        assertEquals(TimeUnit.MONTH, ZoomRules.resolveUnit(TimeUnit.DAY, 1f, ONE_X))
        assertEquals(TimeUnit.DAY, ZoomRules.resolveUnit(TimeUnit.MONTH, 2_000f, ONE_X))
    }

    @Test
    fun widthMultiplierScalesEverything() {
        val twoX = 2f
        assertEquals(512f, ZoomRules.clampDpPerUnit(TimeUnit.HOUR, 1_000f, twoX), EPSILON)
        assertEquals(90f, ZoomRules.clampDpPerUnit(TimeUnit.MONTH, 10f, twoX), EPSILON)
        assertEquals(TimeUnit.DAY, ZoomRules.resolveUnit(TimeUnit.DAY, 782f, twoX))
        assertEquals(TimeUnit.HOUR, ZoomRules.resolveUnit(TimeUnit.DAY, 784f, twoX))
        assertEquals(ZoomLevel(TimeUnit.WEEK, 224f), ZoomRules.unitButtonTarget(TimeUnit.DAY, 128f, twoX))
        TimeUnit.entries.forEach { unit ->
            assertEquals(unit, ZoomRules.resolveUnit(unit, unit.standardWidthDp(twoX), twoX))
        }
    }

    @Test
    fun extremeValues() {
        assertEquals(256f, ZoomRules.clampDpPerUnit(TimeUnit.HOUR, 1_000_000f, ONE_X), EPSILON)
        assertEquals(45f, ZoomRules.clampDpPerUnit(TimeUnit.MONTH, 0.001f, ONE_X), EPSILON)
        assertEquals(TimeUnit.MONTH, ZoomRules.resolveUnit(TimeUnit.HOUR, 0.0001f, ONE_X))
    }

    private companion object {
        const val ONE_X = 1f
        const val EPSILON = 1e-4f
    }
}
