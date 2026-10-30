package com.sam.gogotimeline.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTimesTest {
    private fun newTask(
        now: String,
        unit: TimeUnit,
        calendar: CalendarContext = TAIPEI,
        snap: SnapInterval = SnapInterval.default(unit),
    ): TimeRange = TaskTimes.newTaskRange(calendar.at(now), unit, calendar.grid(snap), calendar)

    private fun range(
        start: String,
        end: String,
        calendar: CalendarContext = TAIPEI,
    ): TimeRange = TimeRange(calendar.at(start), calendar.at(end))

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
    fun newTaskFollowsCurrentUnit() {
        val now = "2026-10-30T14:20"
        assertRange("2026-10-30T14:15", "2026-10-30T16:15", newTask(now, TimeUnit.HOUR))
        assertRange("2026-10-30T14:00", "2026-11-01T14:00", newTask(now, TimeUnit.DAY))
        assertRange("2026-10-30T00:00", "2026-11-13T00:00", newTask(now, TimeUnit.WEEK))
        assertRange("2026-10-30T00:00", "2026-12-30T00:00", newTask(now, TimeUnit.MONTH))
    }

    @Test
    fun oneFullDayIsAllDay() {
        assertTrue(TaskTimes.isAllDay(range("2026-10-30T00:00", "2026-10-31T00:00"), TAIPEI))
    }

    @Test
    fun checkingAllDayCoversTheDay() {
        val result = TaskTimes.expandToAllDay(range("2026-10-30T09:00", "2026-10-30T11:00"), TAIPEI)
        assertRange("2026-10-30T00:00", "2026-10-31T00:00", result)
    }

    @Test
    fun checkingAllDayCoversEveryTouchedDay() {
        val result = TaskTimes.expandToAllDay(range("2026-10-30T09:00", "2026-10-31T15:00"), TAIPEI)
        assertRange("2026-10-30T00:00", "2026-11-01T00:00", result)
    }

    @Test
    fun newTaskWhenNowIsOnGrid() {
        assertRange("2026-10-30T14:15", "2026-10-30T16:15", newTask("2026-10-30T14:15", TimeUnit.HOUR))
    }

    @Test
    fun newTaskFollowsCustomSnap() {
        val result = newTask("2026-10-30T14:20", TimeUnit.DAY, snap = SnapInterval(360))
        assertRange("2026-10-30T12:00", "2026-11-01T12:00", result)
    }

    @Test
    fun allDayNeedsMidnightOnBothEnds() {
        assertTrue(TaskTimes.isAllDay(range("2026-10-30T00:00", "2026-11-02T00:00"), TAIPEI))
        assertFalse(TaskTimes.isAllDay(range("2026-10-30T09:00", "2026-10-31T09:00"), TAIPEI))
        assertFalse(TaskTimes.isAllDay(range("2026-10-30T00:00", "2026-10-30T00:00"), TAIPEI))
        assertFalse(TaskTimes.isAllDay(range("2026-10-31T00:00", "2026-10-30T00:00"), TAIPEI))
        assertFalse(TaskTimes.isAllDay(range("2026-10-30T00:00", "2026-10-30T15:00"), TAIPEI))
    }

    @Test
    fun endAlreadyAtMidnightAddsNoExtraDay() {
        val result = TaskTimes.expandToAllDay(range("2026-10-30T09:00", "2026-10-31T00:00"), TAIPEI)
        assertRange("2026-10-30T00:00", "2026-10-31T00:00", result)
    }

    @Test
    fun allDayTaskStaysTheSame() {
        val task = range("2026-10-30T00:00", "2026-11-01T00:00")
        assertEquals(task, TaskTimes.expandToAllDay(task, TAIPEI))
    }

    @Test
    fun monthTaskAcrossYearEndsOnLastDayOfFebruary() {
        assertRange("2026-12-31T00:00", "2027-02-28T00:00", newTask("2026-12-31T10:00", TimeUnit.MONTH))
    }

    @Test
    fun dayTaskAcrossSpringForwardKeepsWallTime() {
        val result = newTask("2026-03-07T14:20", TimeUnit.DAY, NEW_YORK)
        assertRange("2026-03-07T14:00", "2026-03-09T14:00", result, NEW_YORK)
        assertEquals(47 * HOUR_MS, result.duration)
    }

    @Test
    fun hourTaskAcrossFallBackLastsTwoRealHours() {
        val result = newTask("2026-11-01T00:20", TimeUnit.HOUR, NEW_YORK)
        assertEquals(atOffset("2026-11-01T00:15-04:00"), result.start)
        assertEquals(atOffset("2026-11-01T01:15-05:00"), result.end)
    }

    @Test
    fun shortDstDaysAreStillAllDay() {
        assertTrue(TaskTimes.isAllDay(range("2026-03-08T00:00", "2026-03-09T00:00", NEW_YORK), NEW_YORK))
        val santiagoDay = TimeRange(SANTIAGO.at("2026-09-06T01:00"), SANTIAGO.at("2026-09-07T00:00"))
        assertTrue(TaskTimes.isAllDay(santiagoDay, SANTIAGO))
    }

    @Test
    fun checkingAllDayWhenMidnightIsSkipped() {
        val task = TimeRange(SANTIAGO.at("2026-09-06T09:00"), SANTIAGO.at("2026-09-06T10:00"))
        assertRange("2026-09-06T01:00", "2026-09-07T00:00", TaskTimes.expandToAllDay(task, SANTIAGO), SANTIAGO)
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
    }
}
