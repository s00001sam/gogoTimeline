package com.sam.gogotimeline.core.time

import java.time.DayOfWeek
import kotlin.math.floor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TimeAxisTest {
    private fun CalendarContext.position(
        unit: TimeUnit,
        local: String,
    ): Double = axis(unit).position(at(local).toDouble())

    @Test
    fun hourUsesElapsedTime() {
        val diff = TAIPEI.position(TimeUnit.HOUR, "2026-10-30T14:30") -
            TAIPEI.position(TimeUnit.HOUR, "2026-10-30T14:00")
        assertEquals(0.5, diff, EPSILON)
    }

    @Test
    fun everyMonthHasTheSameWidth() {
        val feb = TAIPEI.position(TimeUnit.MONTH, "2026-02-01T00:00")
        val mar = TAIPEI.position(TimeUnit.MONTH, "2026-03-01T00:00")
        val apr = TAIPEI.position(TimeUnit.MONTH, "2026-04-01T00:00")
        assertEquals(1.0, mar - feb, EPSILON)
        assertEquals(1.0, apr - mar, EPSILON)
        assertEquals(0.5, TAIPEI.position(TimeUnit.MONTH, "2026-02-15T00:00") - feb, EPSILON)
    }

    @Test
    fun weekStartFollowsLocale() {
        val wednesday = TAIPEI.at("2026-10-28T14:00")
        val sundayAxis = TAIPEI.axis(TimeUnit.WEEK)
        val mondayAxis = TAIPEI.copy(firstDayOfWeek = DayOfWeek.MONDAY).axis(TimeUnit.WEEK)
        assertEquals("2026-10-25T00:00", TAIPEI.local(sundayAxis.startOf(sundayAxis.indexOf(wednesday))))
        assertEquals("2026-10-26T00:00", TAIPEI.local(mondayAxis.startOf(mondayAxis.indexOf(wednesday))))
    }

    @Test
    fun timeAtInvertsPosition() {
        val time = NEW_YORK.at("2026-10-30T14:20").toDouble()
        TimeUnit.entries.forEach { unit ->
            val axis = NEW_YORK.axis(unit)
            assertEquals("$unit", time, axis.timeAt(axis.position(time)), 1.0)
        }
    }

    @Test
    fun unitBoundaryIsWholeNumber() {
        val axis = TAIPEI.axis(TimeUnit.MONTH)
        val november = TAIPEI.at("2026-11-01T00:00")
        val position = axis.position(november.toDouble())
        assertEquals(0.0, position - floor(position), EPSILON)
        assertEquals(axis.indexOf(november) - 1, axis.indexOf(november - 1))
    }

    @Test
    fun yearRolloverIsContinuous() {
        val diff = TAIPEI.position(TimeUnit.MONTH, "2027-01-01T00:00") -
            TAIPEI.position(TimeUnit.MONTH, "2026-12-01T00:00")
        assertEquals(1.0, diff, EPSILON)
    }

    @Test
    fun limitsInvert() {
        listOf(TimeLimits.MIN, TimeLimits.MAX).forEach { limit ->
            TimeUnit.entries.forEach { unit ->
                val axis = TAIPEI.axis(unit)
                assertEquals("$unit $limit", limit.toDouble(), axis.timeAt(axis.position(limit.toDouble())), 1.0)
            }
        }
    }

    @Test
    fun hourAxisAlwaysUsesElapsedTime() {
        val first = KOLKATA.position(TimeUnit.HOUR, "2026-10-30T14:00")
        val second = KOLKATA.position(TimeUnit.HOUR, "2026-10-30T15:00")
        assertEquals(1.0, second - first, EPSILON)
    }

    @Test
    fun dstDayIsStillOneDayWide() {
        val start = NEW_YORK.position(TimeUnit.DAY, "2026-03-08T00:00")
        assertEquals(1.0, NEW_YORK.position(TimeUnit.DAY, "2026-03-09T00:00") - start, EPSILON)
        // Only 11 real hours pass between midnight and noon on the 23-hour day.
        assertEquals(11.0 / 23, NEW_YORK.position(TimeUnit.DAY, "2026-03-08T12:00") - start, EPSILON)
    }

    @Test
    fun repeatedHourHasTwoPositions() {
        val axis = NEW_YORK.axis(TimeUnit.DAY)
        val start = axis.position(NEW_YORK.at("2026-11-01T00:00").toDouble())
        val first = axis.position(atOffset("2026-11-01T01:30-04:00").toDouble()) - start
        val second = axis.position(atOffset("2026-11-01T01:30-05:00").toDouble()) - start
        assertEquals(1.5 / 25, first, EPSILON)
        assertEquals(2.5 / 25, second, EPSILON)
        assertNotEquals(first, second, EPSILON)
    }

    @Test
    fun midnightSkippedByDst() {
        val axis = SANTIAGO.axis(TimeUnit.DAY)
        val noon = SANTIAGO.at("2026-09-06T12:00")
        assertEquals("2026-09-06T01:00", SANTIAGO.local(axis.startOf(axis.indexOf(noon))))
        assertEquals(23 * HOUR_MS, axis.unitDurationAt(noon))
    }

    @Test
    fun before1970() {
        val axis = TAIPEI.axis(TimeUnit.WEEK)
        val wednesday = TAIPEI.at("1969-12-31T12:00")
        assertEquals("1969-12-28T00:00", TAIPEI.local(axis.startOf(axis.indexOf(wednesday))))
    }

    @Test
    fun precisionNear2100() {
        val diff = TAIPEI.position(TimeUnit.HOUR, "2100-12-30T10:01") -
            TAIPEI.position(TimeUnit.HOUR, "2100-12-30T10:00")
        assertEquals(1.0 / 60, diff, 1e-6)
    }

    @Test
    fun unitDurationAtFollowsDst() {
        val day = NEW_YORK.axis(TimeUnit.DAY)
        assertEquals(23 * HOUR_MS, day.unitDurationAt(NEW_YORK.at("2026-03-08T12:00")))
        assertEquals(25 * HOUR_MS, day.unitDurationAt(NEW_YORK.at("2026-11-01T12:00")))
        assertEquals(31 * 24 * HOUR_MS, TAIPEI.axis(TimeUnit.MONTH).unitDurationAt(TAIPEI.at("2026-10-15T00:00")))
    }

    private companion object {
        const val EPSILON = 1e-9
        const val HOUR_MS = 3_600_000L
    }
}
