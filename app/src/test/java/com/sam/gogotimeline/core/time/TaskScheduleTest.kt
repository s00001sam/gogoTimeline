package com.sam.gogotimeline.core.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskScheduleTest {
    private val oct30 = DateRange(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31))

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
    fun timedTaskKeepsItsInstants() {
        val meeting = range("2026-10-30T09:00", "2026-10-30T10:00")
        assertEquals(meeting, TaskSchedule.Timed(meeting).toTimeRange(NEW_YORK))
    }

    @Test
    fun allDayTaskInTaipei() {
        assertRange("2026-10-30T00:00", "2026-10-31T00:00", TaskSchedule.AllDay(oct30).toTimeRange(TAIPEI))
    }

    @Test
    fun fullDayResultBecomesAllDay() {
        assertEquals(
            TaskSchedule.AllDay(oct30),
            TaskSchedule.from(range("2026-10-30T00:00", "2026-10-31T00:00"), TAIPEI),
        )
    }

    @Test
    fun partialDayResultStaysTimed() {
        val task = range("2026-10-30T09:00", "2026-10-30T15:00")
        assertEquals(TaskSchedule.Timed(task), TaskSchedule.from(task, TAIPEI))
    }

    @Test
    fun multiDayAllDayRoundTrip() {
        val task = range("2026-10-30T00:00", "2026-11-02T00:00")
        val schedule = TaskSchedule.from(task, TAIPEI)
        assertEquals(TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 2))), schedule)
        assertEquals(task, schedule.toTimeRange(TAIPEI))
    }

    @Test
    fun twentyFourHoursNotFromMidnightStaysTimed() {
        val task = range("2026-10-30T09:00", "2026-10-31T09:00")
        assertEquals(TaskSchedule.Timed(task), TaskSchedule.from(task, TAIPEI))
    }

    @Test
    fun zeroLengthStaysTimed() {
        val task = range("2026-10-30T00:00", "2026-10-30T00:00")
        assertEquals(TaskSchedule.Timed(task), TaskSchedule.from(task, TAIPEI))
    }

    @Test
    fun allDayAcrossYearEnd() {
        val newYearsEve = TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)))
        assertRange("2026-12-31T00:00", "2027-01-01T00:00", newYearsEve.toTimeRange(TAIPEI))
    }

    @Test
    fun allDayKeepsItsDatesInAnotherTimeZone() {
        val schedule = TaskSchedule.from(range("2026-10-30T00:00", "2026-10-31T00:00"), TAIPEI)
        val inNewYork = schedule.toTimeRange(NEW_YORK)
        assertRange("2026-10-30T00:00", "2026-10-31T00:00", inNewYork, NEW_YORK)
        assertEquals(schedule, TaskSchedule.from(inNewYork, NEW_YORK))
    }

    @Test
    fun timedTaskFollowsItsInstantsInAnotherTimeZone() {
        val schedule = TaskSchedule.from(range("2026-10-30T09:00", "2026-10-30T10:00"), TAIPEI)
        assertRange("2026-10-29T21:00", "2026-10-29T22:00", schedule.toTimeRange(NEW_YORK), NEW_YORK)
    }

    @Test
    fun allDayOnDstDay() {
        val schedule = TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 9)))
        val range = schedule.toTimeRange(NEW_YORK)
        assertEquals(23 * HOUR_MS, range.duration)
        assertEquals(schedule, TaskSchedule.from(range, NEW_YORK))
    }

    @Test
    fun allDayWhenMidnightIsSkipped() {
        val schedule = TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 7)))
        val range = schedule.toTimeRange(SANTIAGO)
        assertRange("2026-09-06T01:00", "2026-09-07T00:00", range, SANTIAGO)
        assertEquals(schedule, TaskSchedule.from(range, SANTIAGO))
    }

    @Test
    fun allDayInFarthestApartTimeZones() {
        val kiritimati = CalendarContext(ZoneId.of("Pacific/Kiritimati"), DayOfWeek.MONDAY)
        val minusTwelve = CalendarContext(ZoneId.of("Etc/GMT+12"), DayOfWeek.MONDAY)
        val schedule = TaskSchedule.AllDay(oct30)
        assertRange("2026-10-30T00:00", "2026-10-31T00:00", schedule.toTimeRange(kiritimati), kiritimati)
        assertRange("2026-10-30T00:00", "2026-10-31T00:00", schedule.toTimeRange(minusTwelve), minusTwelve)
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
    }
}
