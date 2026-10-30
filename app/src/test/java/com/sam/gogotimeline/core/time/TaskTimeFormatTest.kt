package com.sam.gogotimeline.core.time

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskTimeFormatTest {
    private fun bar(
        start: String,
        end: String,
        isAllDay: Boolean = false,
        calendar: CalendarContext = TAIPEI,
    ): String = TaskTimeFormat.barText(
        TimeRange(calendar.at(start), calendar.at(end)),
        isAllDay,
        calendar.at(NOW),
        calendar,
    )

    @Test
    fun timedSameDayStillShowsBothDates() {
        assertEquals("Oct 29 09:00 – Oct 29 10:30", bar("2026-10-29T09:00", "2026-10-29T10:30"))
    }

    @Test
    fun timedAcrossDays() {
        assertEquals("Oct 29 09:00 – Oct 30 10:30", bar("2026-10-29T09:00", "2026-10-30T10:30"))
    }

    @Test
    fun allDaySingleDay() {
        assertEquals("Oct 30", bar("2026-10-30T00:00", "2026-10-31T00:00", isAllDay = true))
    }

    @Test
    fun allDayShowsTheLastDay() {
        assertEquals("Oct 30 – Nov 1", bar("2026-10-30T00:00", "2026-11-02T00:00", isAllDay = true))
    }

    @Test
    fun timedEndingAtNextMidnight() {
        assertEquals("Oct 30 22:00 – Oct 31 00:00", bar("2026-10-30T22:00", "2026-10-31T00:00"))
    }

    @Test
    fun notThisYearShowsTheYear() {
        assertEquals("Jan 5, 2027 09:00 – Jan 5, 2027 10:00", bar("2027-01-05T09:00", "2027-01-05T10:00"))
    }

    @Test
    fun timedAcrossYearsShowsBothYears() {
        assertEquals("Dec 31, 2026 22:00 – Jan 1, 2027 01:00", bar("2026-12-31T22:00", "2027-01-01T01:00"))
    }

    @Test
    fun allDayAcrossYearsShowsBothYears() {
        assertEquals("Dec 31, 2026 – Jan 1, 2027", bar("2026-12-31T00:00", "2027-01-02T00:00", isAllDay = true))
    }

    @Test
    fun newYearsEveAllDayStaysInThisYear() {
        assertEquals("Dec 31", bar("2026-12-31T00:00", "2027-01-01T00:00", isAllDay = true))
    }

    @Test
    fun brokenAllDayNeverGoesBackwards() {
        assertEquals("Oct 30", bar("2026-10-30T00:00", "2026-10-30T00:00", isAllDay = true))
    }

    @Test
    fun allDayOnA23HourDay() {
        assertEquals("Mar 8", bar("2026-03-08T00:00", "2026-03-09T00:00", isAllDay = true, calendar = NEW_YORK))
    }

    @Test
    fun allDayWhenMidnightIsSkipped() {
        assertEquals("Sep 6", bar("2026-09-06T01:00", "2026-09-07T00:00", isAllDay = true, calendar = SANTIAGO))
    }

    @Test
    fun timedInTheRepeatedHour() {
        val range = TimeRange(NEW_YORK.at("2026-11-01T00:30"), atOffset("2026-11-01T01:30-05:00"))
        assertEquals("Nov 1 00:30 – Nov 1 01:30", TaskTimeFormat.barText(range, false, NEW_YORK.at(NOW), NEW_YORK))
    }

    private companion object {
        const val NOW = "2026-10-30T12:00"
    }
}
