package com.sam.gogotimeline.ui.timeline

import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.NEW_YORK
import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TimeLimits
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.at
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TickCacheTest {
    private val cache = TickCache()

    private fun ticks(
        start: String,
        end: String,
        unit: TimeUnit = TimeUnit.HOUR,
        dpPerUnit: Float = 64f,
        calendar: CalendarContext = TAIPEI,
    ) = cache.ticks(
        TimeRange(calendar.at("2026-10-30T$start"), calendar.at("2026-10-30T$end")),
        unit,
        dpPerUnit,
        calendar,
    )

    @Test
    fun firstCallPreparesOneWidthOnEachSide() {
        val ticks = ticks("09:00", "14:00")
        assertTrue(ticks.bottom.first().time <= TAIPEI.at("2026-10-30T04:00"))
        assertTrue(ticks.bottom.last().time >= TAIPEI.at("2026-10-30T19:00"))
    }

    @Test
    fun smallPanReusesTicks() {
        val first = ticks("09:00", "14:00")
        assertSame(first, ticks("11:00", "16:00"))
    }

    @Test
    fun panningPastTheBufferRebuilds() {
        val first = ticks("09:00", "14:00")
        val second = ticks("13:00", "18:00")
        assertNotSame(first, second)
        assertTrue(second.bottom.last().time >= TAIPEI.at("2026-10-30T23:00"))
    }

    @Test
    fun exactlyHalfAWidthLeftIsStillEnough() {
        val first = ticks("09:00", "14:00")
        assertSame(first, ticks("11:30", "16:30"))
    }

    @Test
    fun onlyALabelStepChangeRebuilds() {
        val first = ticks("09:00", "14:00", dpPerUnit = 64f)
        assertSame(first, ticks("09:00", "14:00", dpPerUnit = 50f))
        assertNotSame(first, ticks("09:00", "14:00", dpPerUnit = 16f))
    }

    @Test
    fun unitChangeRebuilds() {
        val first = ticks("09:00", "14:00")
        assertNotSame(first, ticks("09:00", "14:00", unit = TimeUnit.DAY))
    }

    @Test
    fun nearTheLimitDoesNotRebuildEveryFrame() {
        val width = 5 * 3_600_000L
        val first = cache.ticks(TimeRange(TimeLimits.MIN, TimeLimits.MIN + width), TimeUnit.HOUR, 64f, TAIPEI)
        val second = cache.ticks(
            TimeRange(TimeLimits.MIN + 1_000, TimeLimits.MIN + width + 1_000),
            TimeUnit.HOUR,
            64f,
            TAIPEI,
        )
        assertSame(first, second)
    }

    @Test
    fun timeZoneChangeRebuilds() {
        val first = ticks("09:00", "14:00")
        val range = TimeRange(TAIPEI.at("2026-10-30T09:00"), TAIPEI.at("2026-10-30T14:00"))
        assertNotSame(first, cache.ticks(range, TimeUnit.HOUR, 64f, NEW_YORK))
    }

    @Test
    fun zoomingInFarDropsTheExtraBuffer() {
        val first = ticks("09:00", "14:00")
        assertNotSame(first, ticks("11:00", "12:00"))
    }

    @Test
    fun zoomingOutPastThePreparedRangeRebuilds() {
        val first = ticks("09:00", "14:00")
        assertNotSame(first, ticks("00:00", "23:00"))
    }
}
