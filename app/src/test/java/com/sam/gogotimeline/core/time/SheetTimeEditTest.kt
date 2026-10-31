package com.sam.gogotimeline.core.time

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SheetTimeEditTest {
    private fun timed(
        start: String,
        end: String,
        calendar: CalendarContext = TAIPEI,
    ) = TaskSchedule.Timed(TimeRange(calendar.at(start), calendar.at(end)))

    private fun allDay(
        start: LocalDate,
        endExclusive: LocalDate,
    ) = TaskSchedule.AllDay(DateRange(start, endExclusive))

    private val kickoff = timed("2026-10-31T09:00", "2026-10-31T10:30")

    @Test
    fun ordinaryTimesAreValid() {
        assertNull(SheetTimeEdit.error(kickoff))
    }

    @Test
    fun changingTheStartTimeLeavesTheEnd() {
        val changed = SheetTimeEdit.withStartTime(kickoff, LocalTime.of(13, 0), TAIPEI)
        assertEquals(timed("2026-10-31T13:00", "2026-10-31T10:30"), changed)
        assertEquals(TimeError.END_NOT_AFTER_START, SheetTimeEdit.error(changed))
    }

    @Test
    fun changingTheEndDateKeepsItsTime() {
        val changed = SheetTimeEdit.withEndDate(kickoff, LocalDate.of(2026, 11, 2), TAIPEI)
        assertEquals(timed("2026-10-31T09:00", "2026-11-02T10:30"), changed)
    }

    @Test
    fun checkingAllDayCoversTheTouchedDays() {
        val range = TimeRange(TAIPEI.at("2026-10-30T09:00"), TAIPEI.at("2026-10-31T15:00"))
        assertEquals(
            allDay(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 1)),
            SheetTimeEdit.toAllDay(range, TAIPEI),
        )
    }

    @Test
    fun fifteenMinutesIsTheShortestValid() {
        assertNull(SheetTimeEdit.error(timed("2026-10-31T09:00", "2026-10-31T09:15")))
        assertEquals(TimeError.END_NOT_AFTER_START, SheetTimeEdit.error(timed("2026-10-31T09:00", "2026-10-31T09:00")))
    }

    @Test
    fun endAtMidnightIsNotPushedAnotherDay() {
        val range = TimeRange(TAIPEI.at("2026-10-30T09:00"), TAIPEI.at("2026-11-01T00:00"))
        assertEquals(
            allDay(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 1)),
            SheetTimeEdit.toAllDay(range, TAIPEI),
        )
        assertEquals(
            timed("2026-10-30T00:00", "2026-11-01T00:00"),
            SheetTimeEdit.fromAllDay(DateRange(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 1)), TAIPEI),
        )
    }

    @Test
    fun singleAllDayShowsTheSameDateAndStoresTheNextDay() {
        val release = allDay(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8))
        assertEquals(LocalDate.of(2026, 11, 7), SheetTimeEdit.startDate(release, TAIPEI))
        assertEquals(LocalDate.of(2026, 11, 7), SheetTimeEdit.endDate(release, TAIPEI))
        assertNull(SheetTimeEdit.startTime(release, TAIPEI))
        assertEquals(
            allDay(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 10)),
            SheetTimeEdit.withEndDate(release, LocalDate.of(2026, 11, 9), TAIPEI),
        )
    }

    @Test
    fun orderErrorWinsOverLength() {
        assertEquals(TimeError.END_NOT_AFTER_START, SheetTimeEdit.error(timed("2026-10-31T09:00", "2026-10-31T08:50")))
    }

    @Test
    fun allDayEndBeforeItsStartIsAnOrderError() {
        val release = allDay(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8))
        val changed = SheetTimeEdit.withEndDate(release, LocalDate.of(2026, 11, 5), TAIPEI)
        assertEquals(TimeError.END_NOT_AFTER_START, SheetTimeEdit.error(changed))
    }

    @Test
    fun daylightSavingDayKeepsWallTime() {
        val meeting = timed("2026-03-07T09:00", "2026-03-07T10:00", NEW_YORK)
        val moved = SheetTimeEdit.withStartDate(meeting, LocalDate.of(2026, 3, 8), NEW_YORK)
        assertEquals(LocalTime.of(9, 0), SheetTimeEdit.startTime(moved, NEW_YORK))
        val skipped = SheetTimeEdit.withStartTime(moved, LocalTime.of(2, 30), NEW_YORK)
        assertEquals(LocalTime.of(3, 30), SheetTimeEdit.startTime(skipped, NEW_YORK))
    }

    @Test
    fun arrowsStepHoursAndMinutes() {
        assertEquals(14, SheetTimeEdit.stepHour(13, 1))
        assertEquals(45, SheetTimeEdit.stepMinute(30, 1))
    }

    @Test
    fun arrowsWrapAtTheEnds() {
        assertEquals(0, SheetTimeEdit.stepHour(23, 1))
        assertEquals(23, SheetTimeEdit.stepHour(0, -1))
        assertEquals(0, SheetTimeEdit.stepMinute(45, 1))
        assertEquals(45, SheetTimeEdit.stepMinute(0, -1))
    }

    @Test
    fun manyStepsStillWrap() {
        assertEquals(14, SheetTimeEdit.stepHour(13, 25))
        assertEquals(15, SheetTimeEdit.stepMinute(0, 9))
        assertEquals(15, SheetTimeEdit.stepMinute(0, -7))
    }

    @Test
    fun invalidTimesBecomeAtLeastOneAllDay() {
        val range = TimeRange(TAIPEI.at("2026-10-31T13:00"), TAIPEI.at("2026-10-30T10:30"))
        assertEquals(
            allDay(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1)),
            SheetTimeEdit.toAllDay(range, TAIPEI),
        )
    }
}
