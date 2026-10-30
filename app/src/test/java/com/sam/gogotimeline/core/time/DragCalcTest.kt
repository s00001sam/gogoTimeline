package com.sam.gogotimeline.core.time

import org.junit.Assert.assertEquals
import org.junit.Test

class DragCalcTest {
    private val quarterGrid = TAIPEI.grid(SnapInterval(15))
    private val hourGrid = TAIPEI.grid(SnapInterval(60))
    private val dayGrid = TAIPEI.grid(SnapInterval(SnapInterval.DAY_MINUTES))

    private fun range(
        start: String,
        end: String,
    ): TimeRange = TimeRange(TAIPEI.at("2026-10-30T$start"), TAIPEI.at("2026-10-30T$end"))

    private fun minutes(count: Int): Double = count * 60_000.0

    private fun assertRange(
        expectedStart: String,
        expectedEnd: String,
        actual: TimeRange,
        calendar: CalendarContext = TAIPEI,
    ) {
        assertEquals(expectedStart, calendar.local(actual.start))
        assertEquals(expectedEnd, calendar.local(actual.end))
    }

    @Test
    fun moveSnapsStartAndKeepsDuration() {
        val task = range("00:45", "01:30")
        assertRange("2026-10-30T01:00", "2026-10-30T01:45", DragCalc.move(task, minutes(40), hourGrid))
        assertRange("2026-10-30T02:00", "2026-10-30T02:45", DragCalc.move(task, minutes(80), hourGrid))
    }

    @Test
    fun resizeStartSnaps() {
        val result = DragCalc.resizeStart(range("09:00", "11:00"), minutes(-50), quarterGrid)
        assertRange("2026-10-30T08:15", "2026-10-30T11:00", result)
    }

    @Test
    fun resizeEndSnaps() {
        val result = DragCalc.resizeEnd(range("09:00", "11:00"), minutes(22), quarterGrid)
        assertRange("2026-10-30T09:00", "2026-10-30T11:15", result)
    }

    @Test
    fun latestStartIsFlooredToGrid() {
        val result = DragCalc.resizeStart(range("09:00", "11:10"), minutes(180), quarterGrid)
        assertRange("2026-10-30T10:45", "2026-10-30T11:10", result)
    }

    @Test
    fun earliestEndIsCeiledToGrid() {
        val result = DragCalc.resizeEnd(range("10:05", "12:00"), minutes(-180), quarterGrid)
        assertRange("2026-10-30T10:05", "2026-10-30T10:30", result)
    }

    @Test
    fun slowDragUsesTotalDelta() {
        val task = range("00:00", "01:00")
        assertRange("2026-10-30T00:00", "2026-10-30T01:00", DragCalc.move(task, minutes(29), hourGrid))
        assertRange("2026-10-30T01:00", "2026-10-30T02:00", DragCalc.move(task, minutes(31), hourGrid))
    }

    @Test
    fun zeroDeltaKeepsOnGridTask() {
        val task = range("01:00", "02:00")
        assertEquals(task, DragCalc.move(task, 0.0, hourGrid))
        assertEquals(task, DragCalc.resizeStart(task, 0.0, hourGrid))
        assertEquals(task, DragCalc.resizeEnd(task, 0.0, hourGrid))
    }

    @Test
    fun moveStopsAtLimits() {
        val twoHours = minutes(120).toLong()
        val atMin = DragCalc.move(TimeRange(TimeLimits.MIN, TimeLimits.MIN + twoHours), minutes(-1_440), hourGrid)
        assertEquals(TimeRange(TimeLimits.MIN, TimeLimits.MIN + twoHours), atMin)
        val atMax = DragCalc.move(TimeRange(TimeLimits.MAX - twoHours, TimeLimits.MAX), minutes(1_440), hourGrid)
        assertEquals(TimeRange(TimeLimits.MAX - twoHours, TimeLimits.MAX), atMax)
    }

    @Test
    fun resizeEndStopsAtMax() {
        val hour = minutes(60).toLong()
        val task = TimeRange(TimeLimits.MAX - 2 * hour, TimeLimits.MAX - hour)
        assertEquals(TimeLimits.MAX, DragCalc.resizeEnd(task, minutes(1_440), hourGrid).end)
    }

    @Test
    fun monthModeMoveKeepsShortDuration() {
        val result = DragCalc.move(range("10:00", "10:15"), minutes(180), dayGrid)
        assertRange("2026-10-31T00:00", "2026-10-31T00:15", result)
    }

    @Test
    fun monthModeResizeGrowsToAtLeastADay() {
        val result = DragCalc.resizeEnd(range("10:00", "10:15"), 0.0, dayGrid)
        assertRange("2026-10-30T10:00", "2026-11-01T00:00", result)
    }

    @Test
    fun resizeEndAcrossSpringGap() {
        val task = TimeRange(NEW_YORK.at("2026-03-08T00:00"), NEW_YORK.at("2026-03-08T01:00"))
        val result = DragCalc.resizeEnd(task, minutes(60), NEW_YORK.grid(SnapInterval(60)))
        assertRange("2026-03-08T00:00", "2026-03-08T03:00", result, NEW_YORK)
    }

    @Test
    fun moveAcrossDstKeepsRealDuration() {
        val task = TimeRange(NEW_YORK.at("2026-03-07T22:00"), NEW_YORK.at("2026-03-08T06:00"))
        val result = DragCalc.move(task, minutes(1_440), NEW_YORK.grid(SnapInterval(60)))
        assertEquals(task.duration, result.duration)
    }
}
