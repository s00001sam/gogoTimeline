package com.sam.gogotimeline.core.time

import java.time.LocalDate
import java.time.LocalTime
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

    @Test
    fun sheetShowsDateAndTimeSeparately() {
        assertEquals("Thu, Oct 29", TaskTimeFormat.sheetDate(LocalDate.of(2026, 10, 29)))
        assertEquals("09:00", TaskTimeFormat.sheetTime(LocalTime.of(9, 0)))
    }

    @Test
    fun sheetShowsMidnightIn24HoursAndUnpaddedDays() {
        assertEquals("00:00", TaskTimeFormat.sheetTime(LocalTime.MIDNIGHT))
        assertEquals("Sun, Mar 8", TaskTimeFormat.sheetDate(LocalDate.of(2026, 3, 8)))
    }

    private fun view(
        schedule: TaskSchedule,
        calendar: CalendarContext = TAIPEI,
    ): String = TaskTimeFormat.viewText(schedule, calendar.at(NOW), calendar)

    private fun timed(
        start: String,
        end: String,
        calendar: CalendarContext = TAIPEI,
    ) = TaskSchedule.Timed(TimeRange(calendar.at(start), calendar.at(end)))

    @Test
    fun viewTextShowsTheWeekdayOnceForASameDayTask() {
        assertEquals("Thu, Oct 29  14:00 – 15:30", view(timed("2026-10-29T14:00", "2026-10-29T15:30")))
    }

    @Test
    fun viewTextAcrossDaysAndAllDayReadLikeTheBar() {
        assertEquals("Oct 29 22:00 – Oct 30 02:00", view(timed("2026-10-29T22:00", "2026-10-30T02:00")))
        val days = TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 3)))
        assertEquals("Oct 30 – Nov 2", view(days))
        val oneDay = TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31)))
        assertEquals("Oct 30", view(oneDay))
    }

    @Test
    fun viewTextEndingAtMidnightIsAcrossDays() {
        // Ending exactly at the next local midnight is another date, so both dates show.
        assertEquals("Oct 29 23:00 – Oct 30 00:00", view(timed("2026-10-29T23:00", "2026-10-30T00:00")))
    }

    @Test
    fun viewTextAddsTheYearOutsideThisYearAndHandlesDst() {
        assertEquals("Wed, Dec 31, 2025  09:00 – 10:00", view(timed("2025-12-31T09:00", "2025-12-31T10:00")))
        // New York's 25-hour day: 00:30 to 03:00 is still one date.
        assertEquals(
            "Sun, Nov 1  00:30 – 03:00",
            view(timed("2026-11-01T00:30", "2026-11-01T03:00", NEW_YORK), NEW_YORK),
        )
    }

    @Test
    fun sheetDatesAtThePickerLimitsKeepTheirWeekday() {
        assertEquals("Mon, Jan 1", TaskTimeFormat.sheetDate(LocalDate.of(1900, 1, 1)))
        assertEquals("Fri, Dec 31", TaskTimeFormat.sheetDate(LocalDate.of(2100, 12, 31)))
    }
}
