package com.sam.gogotimeline.core.time

import java.time.DayOfWeek
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeLimitsTest {
    private val hour = 3_600_000L

    @Test
    fun constantsMatchTheirUtcTimes() {
        assertEquals(atOffset("1900-01-01T12:00Z"), TimeLimits.MIN)
        assertEquals(atOffset("2100-12-31T10:00Z"), TimeLimits.MAX)
    }

    @Test
    fun timeInsideStaysTheSame() {
        val time = TAIPEI.at("2026-10-30T12:00")
        assertEquals(time, TimeLimits.clamp(time))
    }

    @Test
    fun rangeInsideStaysTheSame() {
        val task = TimeRange(TAIPEI.at("2026-10-30T09:00"), TAIPEI.at("2026-10-30T10:00"))
        assertEquals(task, TimeLimits.clampRange(task))
    }

    @Test
    fun exactlyOnLimitStaysAndOneMsOutIsPulledBack() {
        assertEquals(TimeLimits.MIN, TimeLimits.clamp(TimeLimits.MIN))
        assertEquals(TimeLimits.MIN, TimeLimits.clamp(TimeLimits.MIN - 1))
        assertEquals(TimeLimits.MAX, TimeLimits.clamp(TimeLimits.MAX))
        assertEquals(TimeLimits.MAX, TimeLimits.clamp(TimeLimits.MAX + 1))
    }

    @Test
    fun rangeOutsideIsPushedBackKeepingDuration() {
        val beforeMin = TimeRange(TimeLimits.MIN - hour, TimeLimits.MIN + hour)
        assertEquals(TimeRange(TimeLimits.MIN, TimeLimits.MIN + 2 * hour), TimeLimits.clampRange(beforeMin))
        val afterMax = TimeRange(TimeLimits.MAX - hour, TimeLimits.MAX + hour)
        assertEquals(TimeRange(TimeLimits.MAX - 2 * hour, TimeLimits.MAX), TimeLimits.clampRange(afterMax))
    }

    @Test
    fun doubleVersionClampsToo() {
        assertEquals(TimeLimits.MIN.toDouble(), TimeLimits.clamp(TimeLimits.MIN - 0.5), 0.0)
        assertEquals(TimeLimits.MAX.toDouble(), TimeLimits.clamp(TimeLimits.MAX + 0.5), 0.0)
    }

    @Test
    fun limitsStayWithin1900To2100InFarthestTimeZones() {
        val minusTwelve = CalendarContext(ZoneId.of("Etc/GMT+12"), DayOfWeek.MONDAY)
        val kiritimati = CalendarContext(ZoneId.of("Pacific/Kiritimati"), DayOfWeek.MONDAY)
        assertEquals("1900-01-01T00:00", minusTwelve.local(TimeLimits.MIN))
        assertEquals("2100-12-31T23:59:59.999", kiritimati.local(TimeLimits.MAX - 1))
        listOf(minusTwelve, kiritimati).forEach { calendar ->
            assertEquals(1900, calendar.localDate(TimeLimits.MIN).year)
            assertEquals(2100, calendar.localDate(TimeLimits.MAX - 1).year)
        }
    }

    @Test
    fun extremeNumbers() {
        assertEquals(TimeLimits.MIN, TimeLimits.clamp(Long.MIN_VALUE))
        assertEquals(TimeLimits.MAX, TimeLimits.clamp(Long.MAX_VALUE))
        assertEquals(TimeLimits.MIN.toDouble(), TimeLimits.clamp(Double.NEGATIVE_INFINITY), 0.0)
        assertEquals(TimeLimits.MAX.toDouble(), TimeLimits.clamp(Double.POSITIVE_INFINITY), 0.0)
    }
}
