package com.sam.gogotimeline.ui.sheet

import com.sam.gogotimeline.core.time.CalendarContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetDateLimitsTest {
    @Test
    fun timedDatesUseTheCurrentZoneAndTime() {
        val calendar = CalendarContext(ZoneId.of("Etc/GMT+12"), DayOfWeek.MONDAY)
        val time = LocalTime.of(9, 0)

        assertTrue(isDateWithinTimeline(LocalDate.of(2100, 12, 30), Boundary.END, time, calendar))
        assertFalse(isDateWithinTimeline(LocalDate.of(2100, 12, 31), Boundary.END, time, calendar))
    }

    @Test
    fun allDayEndIncludesTheFollowingMidnight() {
        val calendar = CalendarContext(ZoneId.of("Etc/GMT+12"), DayOfWeek.MONDAY)

        assertTrue(isDateWithinTimeline(LocalDate.of(2100, 12, 29), Boundary.END, null, calendar))
        assertFalse(isDateWithinTimeline(LocalDate.of(2100, 12, 30), Boundary.END, null, calendar))
    }

    @Test
    fun allDayStartExcludesAPartialFirstDate() {
        val calendar = CalendarContext(ZoneId.of("+14:00"), DayOfWeek.MONDAY)

        assertFalse(isDateWithinTimeline(LocalDate.of(1900, 1, 2), Boundary.START, null, calendar))
        assertTrue(isDateWithinTimeline(LocalDate.of(1900, 1, 3), Boundary.START, null, calendar))
    }
}
