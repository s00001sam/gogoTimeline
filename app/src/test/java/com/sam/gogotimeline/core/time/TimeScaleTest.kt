package com.sam.gogotimeline.core.time

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeScaleTest {
    private val noon = TAIPEI.at("2026-10-30T12:00").toDouble()

    private fun scale(
        unit: TimeUnit,
        dpPerUnit: Float,
        centerTime: Double = noon,
        calendar: CalendarContext = TAIPEI,
    ): TimeScale = TimeScale(Viewport(centerTime, unit, dpPerUnit), calendar, DENSITY, WIDTH_PX)

    private fun TimeScale.after(viewport: Viewport): TimeScale = TimeScale(viewport, TAIPEI, DENSITY, WIDTH_PX)

    @Test
    fun centerTimeIsInTheMiddle() {
        assertEquals(500f, scale(TimeUnit.HOUR, 64f).timeToX(noon), PX)
    }

    @Test
    fun oneHourIs128Px() {
        val scale = scale(TimeUnit.HOUR, 64f)
        assertEquals(628f, scale.timeToX(noon + HOUR_MS), PX)
        assertEquals(372f, scale.timeToX(noon - HOUR_MS), PX)
    }

    @Test
    fun xToTimeInverts() {
        assertEquals(noon + HOUR_MS, scale(TimeUnit.HOUR, 64f).xToTime(628f), 1.0)
    }

    @Test
    fun panningRightShowsEarlierTime() {
        val viewport = scale(TimeUnit.HOUR, 64f).panBy(128f)
        assertEquals(noon - HOUR_MS, viewport.centerTime, 1.0)
    }

    @Test
    fun zoomKeepsTimeUnderFingers() {
        val scale = scale(TimeUnit.HOUR, 64f)
        val focusTime = scale.xToTime(200f)
        val zoomed = scale.zoomBy(1.5f, 200f, ONE_X)
        assertEquals(96f, zoomed.dpPerUnit, PX)
        assertEquals(200f, scale.after(zoomed).timeToX(focusTime), PX)
    }

    @Test
    fun monthsHaveEqualWidthOnScreen() {
        val scale = scale(TimeUnit.MONTH, 90f, centerTime = TAIPEI.at("2026-03-01T00:00").toDouble())
        val feb = scale.timeToX(TAIPEI.at("2026-02-01T00:00"))
        val mar = scale.timeToX(TAIPEI.at("2026-03-01T00:00"))
        val apr = scale.timeToX(TAIPEI.at("2026-04-01T00:00"))
        assertEquals(180f, mar - feb, PX)
        assertEquals(180f, apr - mar, PX)
    }

    @Test
    fun visibleTimeRangeCoversTheWidth() {
        val range = scale(TimeUnit.HOUR, 64f).visibleTimeRange()
        val halfWidthMs = 500.0 / 128 * HOUR_MS
        assertEquals(noon - halfWidthMs, range.start.toDouble(), 1.0)
        assertEquals(noon + halfWidthMs, range.end.toDouble(), 1.0)
    }

    @Test
    fun panningStopsAtLimits() {
        val nearMin = scale(TimeUnit.HOUR, 64f, centerTime = TimeLimits.MIN + HOUR_MS)
        assertEquals(TimeLimits.MIN.toDouble(), nearMin.panBy(1e9f).centerTime, 0.0)
        val nearMax = scale(TimeUnit.HOUR, 64f, centerTime = TimeLimits.MAX - HOUR_MS)
        assertEquals(TimeLimits.MAX.toDouble(), nearMax.panBy(-1e9f).centerTime, 0.0)
    }

    @Test
    fun zoomStopsAtMaxAndKeepsFocus() {
        val scale = scale(TimeUnit.HOUR, 250f)
        val focusTime = scale.xToTime(300f)
        val zoomed = scale.zoomBy(2f, 300f, ONE_X)
        assertEquals(256f, zoomed.dpPerUnit, PX)
        assertEquals(300f, scale.after(zoomed).timeToX(focusTime), PX)
    }

    @Test
    fun switchingDayToHourKeepsTheLook() {
        val scale = scale(TimeUnit.DAY, 380f)
        val focusTime = scale.xToTime(300f)
        val zoomed = scale.zoomBy(1.05f, 300f, ONE_X)
        assertEquals(TimeUnit.HOUR, zoomed.unit)
        assertEquals(399f / 24, zoomed.dpPerUnit, PX)
        assertEquals(300f, scale.after(zoomed).timeToX(focusTime), PX)
    }

    @Test
    fun switchingMonthToWeekUsesRealMonthLength() {
        val scale = scale(TimeUnit.MONTH, 255f, centerTime = TAIPEI.at("2026-10-15T00:00").toDouble())
        val zoomed = scale.zoomBy(1.02f, 500f, ONE_X)
        assertEquals(TimeUnit.WEEK, zoomed.unit)
        assertEquals(255f * 1.02f * 7 / 31, zoomed.dpPerUnit, PX)
    }

    @Test
    fun switchingOnDstDayUses23Hours() {
        val center = NEW_YORK.at("2026-03-08T12:00").toDouble()
        val scale = scale(TimeUnit.DAY, 380f, centerTime = center, calendar = NEW_YORK)
        val zoomed = scale.zoomBy(1.05f, 500f, ONE_X)
        assertEquals(TimeUnit.HOUR, zoomed.unit)
        assertEquals(399f / 23, zoomed.dpPerUnit, PX)
    }

    @Test
    fun convertToKeepsEverythingInPlace() {
        val scale = scale(TimeUnit.DAY, 64f)
        val converted = scale.convertTo(TimeUnit.WEEK)
        assertEquals(TimeUnit.WEEK, converted.unit)
        assertEquals(448f, converted.dpPerUnit, PX)
        val sixHoursLater = noon + 6 * HOUR_MS
        assertEquals(scale.timeToX(sixHoursLater), scale.after(converted).timeToX(sixHoursLater), PX)
    }

    private companion object {
        const val DENSITY = 2f
        const val WIDTH_PX = 1_000f
        const val ONE_X = 1f
        const val PX = 0.01f
        const val HOUR_MS = 3_600_000.0
    }
}
