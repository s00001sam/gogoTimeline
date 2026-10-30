package com.sam.gogotimeline.core.time

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeGridTest {
    private val quarter = SnapInterval(15)
    private val hour = SnapInterval(60)
    private val halfDay = SnapInterval(720)
    private val day = SnapInterval(SnapInterval.DAY_MINUTES)
    private val week = SnapInterval(SnapInterval.WEEK_MINUTES)

    @Test
    fun dayGridIsLocalMidnightNotUtc() {
        val grid = TAIPEI.grid(day)
        assertEquals("2026-10-30T00:00", TAIPEI.local(grid.floor(TAIPEI.at("2026-10-30T14:20"))))
    }

    @Test
    fun fifteenMinuteFloorCeilNearest() {
        val grid = TAIPEI.grid(quarter)
        val time = TAIPEI.at("2026-10-30T10:52")
        assertEquals("2026-10-30T10:45", TAIPEI.local(grid.floor(time)))
        assertEquals("2026-10-30T11:00", TAIPEI.local(grid.ceil(time)))
        assertEquals("2026-10-30T10:45", TAIPEI.local(grid.nearest(time)))
        assertEquals("2026-10-30T11:00", TAIPEI.local(grid.nearest(TAIPEI.at("2026-10-30T10:53"))))
    }

    @Test
    fun weekGridFollowsLocaleWeekStart() {
        val wednesday = TAIPEI.at("2026-10-28T14:00")
        val sundayGrid = TAIPEI.grid(week)
        assertEquals("2026-10-25T00:00", TAIPEI.local(sundayGrid.floor(wednesday)))
        assertEquals("2026-11-01T00:00", TAIPEI.local(sundayGrid.ceil(wednesday)))
        val mondayGrid = TAIPEI.copy(firstDayOfWeek = DayOfWeek.MONDAY).grid(week)
        assertEquals("2026-10-26T00:00", TAIPEI.local(mondayGrid.floor(wednesday)))
    }

    @Test
    fun onGridStaysPut() {
        val grid = TAIPEI.grid(quarter)
        val time = TAIPEI.at("2026-10-30T11:00")
        assertEquals(time, grid.floor(time))
        assertEquals(time, grid.ceil(time))
        assertEquals(time, grid.nearest(time))
        assertTrue(grid.isOnGrid(time))
        assertFalse(grid.isOnGrid(time + 1))
    }

    @Test
    fun tieGoesToLater() {
        val grid = TAIPEI.grid(quarter)
        assertEquals("2026-10-30T11:00", TAIPEI.local(grid.nearest(TAIPEI.at("2026-10-30T10:52:30"))))
    }

    @Test
    fun secondsAreDropped() {
        val grid = TAIPEI.grid(quarter)
        assertEquals("2026-10-30T10:45", TAIPEI.local(grid.floor(TAIPEI.at("2026-10-30T10:59:59"))))
    }

    @Test
    fun crossesYearBoundary() {
        val grid = TAIPEI.grid(quarter)
        assertEquals("2027-01-01T00:00", TAIPEI.local(grid.ceil(TAIPEI.at("2026-12-31T23:50"))))
    }

    @Test
    fun springForwardGap() {
        val grid = NEW_YORK.grid(hour)
        assertEquals("2026-03-08T03:00", NEW_YORK.local(grid.ceil(NEW_YORK.at("2026-03-08T01:30"))))
        // 01:40 is 40 minutes after 01:00 but only 20 real minutes before 03:00.
        assertEquals("2026-03-08T03:00", NEW_YORK.local(grid.nearest(NEW_YORK.at("2026-03-08T01:40"))))
    }

    @Test
    fun twelveHourOnDstDays() {
        val grid = NEW_YORK.grid(halfDay)
        assertEquals("2026-03-08T12:00", NEW_YORK.local(grid.ceil(NEW_YORK.at("2026-03-08T01:00"))))
        assertEquals("2026-11-01T12:00", NEW_YORK.local(grid.ceil(NEW_YORK.at("2026-11-01T01:00"))))
        assertEquals("2026-11-02T00:00", NEW_YORK.local(grid.ceil(NEW_YORK.at("2026-11-01T13:00"))))
    }

    @Test
    fun dayGridOn23And25HourDays() {
        val grid = NEW_YORK.grid(day)
        assertEquals("2026-03-09T00:00", NEW_YORK.local(grid.ceil(NEW_YORK.at("2026-03-08T12:00"))))
        assertEquals("2026-11-02T00:00", NEW_YORK.local(grid.ceil(NEW_YORK.at("2026-11-01T12:00"))))
    }

    @Test
    fun repeatedHourFallBack() {
        val grid = NEW_YORK.grid(quarter)
        assertEquals(
            atOffset("2026-11-01T01:30-05:00"),
            grid.floor(atOffset("2026-11-01T01:40-05:00")),
        )
        assertEquals(
            atOffset("2026-11-01T01:00-05:00"),
            grid.ceil(atOffset("2026-11-01T01:50-04:00")),
        )
    }

    @Test
    fun midnightSkippedByDst() {
        val grid = SANTIAGO.grid(day)
        assertEquals("2026-09-06T01:00", SANTIAGO.local(grid.floor(SANTIAGO.at("2026-09-06T12:00"))))
        assertEquals("2026-09-06T01:00", SANTIAGO.local(grid.ceil(SANTIAGO.at("2026-09-05T12:00"))))
    }

    @Test
    fun halfHourZone() {
        val grid = KOLKATA.grid(quarter)
        assertEquals("2026-10-30T10:45", KOLKATA.local(grid.floor(KOLKATA.at("2026-10-30T10:52"))))
    }

    @Test
    fun before1970() {
        val grid = TAIPEI.grid(day)
        assertEquals("1970-01-01T00:00", TAIPEI.local(grid.ceil(TAIPEI.at("1969-12-31T12:00"))))
    }
}
