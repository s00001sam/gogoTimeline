package com.sam.gogotimeline.core.time

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class TicksTest {
    private fun ticks(
        start: String,
        end: String,
        unit: TimeUnit,
        dpPerUnit: Float,
        calendar: CalendarContext = TAIPEI,
    ): TimelineTicks = Ticks.buildTicks(
        TimeRange(calendar.at(start), calendar.at(end)),
        unit,
        dpPerUnit,
        calendar,
        MIN_SPACING_DP,
    )

    private fun List<Tick>.labels(): List<String?> = map { it.label }

    @Test
    fun hourTicks() {
        val ticks = ticks("2026-10-30T22:00", "2026-10-31T02:00", TimeUnit.HOUR, 64f)
        assertEquals(listOf("22", "23", "00", "01", "02"), ticks.bottom.labels())
        assertEquals(listOf("Fri, Oct 30", "Sat, Oct 31"), ticks.top.labels())
    }

    @Test
    fun dayTicks() {
        val ticks = ticks("2026-10-30T00:00", "2026-11-02T00:00", TimeUnit.DAY, 64f)
        assertEquals(listOf("30 Fri", "31 Sat", "1 Sun", "2 Mon"), ticks.bottom.labels())
        assertEquals(listOf("Oct 2026", "Nov 2026"), ticks.top.labels())
    }

    @Test
    fun weekTicks() {
        val ticks = ticks("2026-10-28T00:00", "2026-11-10T00:00", TimeUnit.WEEK, 112f)
        assertEquals(listOf("Oct 25", "Nov 1", "Nov 8"), ticks.bottom.labels())
        assertEquals(listOf("Oct 2026", "Nov 2026"), ticks.top.labels())
    }

    @Test
    fun monthTicks() {
        val ticks = ticks("2026-11-15T00:00", "2027-02-10T00:00", TimeUnit.MONTH, 90f)
        assertEquals(listOf("Nov", "Dec", "Jan", "Feb"), ticks.bottom.labels())
        assertEquals(listOf("2026", "2027"), ticks.top.labels())
    }

    @Test
    fun denseHourLabelsEveryThreeHours() {
        val ticks = ticks("2026-10-30T00:00", "2026-10-30T06:00", TimeUnit.HOUR, 16f)
        assertEquals(listOf("00", null, null, "03", null, null, "06"), ticks.bottom.labels())
    }

    @Test
    fun denseMonthLabelsStayOnJanAprJulOct() {
        val ticks = ticks("2026-01-01T00:00", "2026-12-01T00:00", TimeUnit.MONTH, 15f)
        assertEquals(
            listOf("Jan", null, null, "Apr", null, null, "Jul", null, null, "Oct", null, null),
            ticks.bottom.labels(),
        )
    }

    @Test
    fun topIncludesPeriodThatStartedOffScreen() {
        val ticks = ticks("2026-10-15T00:00", "2026-10-20T00:00", TimeUnit.DAY, 64f)
        assertEquals(TAIPEI.at("2026-10-01T00:00"), ticks.top.first().time)
        assertEquals("Oct 2026", ticks.top.first().label)
    }

    @Test
    fun rangeStartingOnTickIncludesIt() {
        val ticks = ticks("2026-10-30T00:00", "2026-10-30T01:00", TimeUnit.HOUR, 64f)
        assertEquals(TAIPEI.at("2026-10-30T00:00"), ticks.bottom.first().time)
    }

    @Test
    fun extremelyDenseUsesLargestStep() {
        val ticks = ticks("2026-10-30T00:00", "2026-10-31T00:00", TimeUnit.HOUR, 1f)
        assertEquals(listOf("00", "12", "00"), ticks.bottom.labels().filterNotNull())
    }

    @Test
    fun fallBackRepeatsOneOClock() {
        val ticks = ticks("2026-11-01T00:00", "2026-11-01T04:00", TimeUnit.HOUR, 64f, NEW_YORK)
        assertEquals(listOf("00", "01", "01", "02", "03", "04"), ticks.bottom.labels())
    }

    @Test
    fun springForwardSkipsTwoOClock() {
        val ticks = ticks("2026-03-08T00:00", "2026-03-08T04:00", TimeUnit.HOUR, 64f, NEW_YORK)
        assertEquals(listOf("00", "01", "03", "04"), ticks.bottom.labels())
    }

    @Test
    fun midnightSkippedByDst() {
        val ticks = ticks("2026-09-05T22:00", "2026-09-06T02:00", TimeUnit.HOUR, 64f, SANTIAGO)
        assertEquals(listOf("22", "23", "01", "02"), ticks.bottom.labels())
        assertEquals("Sun, Sep 6", ticks.top.last().label)
        assertEquals("2026-09-06T01:00", SANTIAGO.local(ticks.top.last().time))
    }

    @Test
    fun mondayWeekStart() {
        val monday = TAIPEI.copy(firstDayOfWeek = DayOfWeek.MONDAY)
        val ticks = ticks("2026-10-28T00:00", "2026-11-10T00:00", TimeUnit.WEEK, 112f, monday)
        assertEquals(listOf("Oct 26", "Nov 2", "Nov 9"), ticks.bottom.labels())
    }

    @Test
    fun skippingAcross1970() {
        val ticks = ticks("1969-12-30T00:00", "1970-01-02T00:00", TimeUnit.DAY, 25f)
        assertEquals(listOf("30 Tue", null, "1 Thu", null), ticks.bottom.labels())
    }

    private companion object {
        const val MIN_SPACING_DP = 40f
    }
}
