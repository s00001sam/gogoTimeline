package com.sam.gogotimeline.ui.timeline

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.unit.Velocity
import com.sam.gogotimeline.core.time.NEW_YORK
import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TimeLimits
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.at
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineStateTest {
    private val noon = TAIPEI.at("2026-10-30T12:00").toDouble()

    private fun state(
        centerTime: Double = noon,
        scrollY: Float = 0f,
        rowCount: Int = 10,
        heightPx: Float = HEIGHT_PX,
    ) = TimelineState(
        initialCenterTime = centerTime,
        initialUnit = TimeUnit.HOUR,
        initialDpPerUnit = 64f,
        initialScrollY = scrollY,
        calendar = TAIPEI,
    ).apply { updateLayout(DENSITY, WIDTH_PX, heightPx, rowCount, ROW_PX) }

    /** Advances one 16ms frame per call so decay animations run without real time. */
    private class TestFrameClock : MonotonicFrameClock {
        private var frameTimeNanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            frameTimeNanos += 16_000_000L
            return onFrame(frameTimeNanos)
        }
    }

    @Test
    fun panningRightShowsEarlierTime() {
        val state = state()
        assertEquals(128f, state.scrollBy(128f, 0f).x, EPSILON)
        assertEquals(noon - HOUR_MS, state.centerTime, 1.0)
    }

    @Test
    fun fingerUpScrollsDown() {
        val state = state()
        state.scrollBy(0f, -128f)
        assertEquals(128f, state.scrollY, EPSILON)
    }

    @Test
    fun startsAtTheMiddleAndTopRows() {
        val state = state()
        assertEquals(500f, state.timeToX(noon), EPSILON)
        assertEquals(0..4, state.visibleRowRange())
    }

    @Test
    fun saverRestoresTheView() {
        val state = state(scrollY = 256f)
        val saver = TimelineState.saver(TAIPEI)
        val saved = with(saver) { SaverScope { true }.save(state) }!!
        val restored = saver.restore(saved)!!
        assertEquals(state.centerTime, restored.centerTime, 0.0)
        assertEquals(state.unit, restored.unit)
        assertEquals(state.dpPerUnit, restored.dpPerUnit, EPSILON)
        assertEquals(state.scrollY, restored.scrollY, EPSILON)
    }

    @Test
    fun verticalScrollStopsAtTopAndBottom() {
        val state = state()
        assertEquals(0f, state.scrollBy(0f, 100f).y, EPSILON)
        assertEquals(0f, state.scrollY, EPSILON)
        state.scrollBy(0f, -10_000f)
        assertEquals(MAX_SCROLL_PX, state.scrollY, EPSILON)
    }

    @Test
    fun fewRowsCannotScrollVertically() {
        val state = state(rowCount = 2)
        state.scrollBy(0f, -500f)
        assertEquals(0f, state.scrollY, EPSILON)
    }

    @Test
    fun panningPastTheLimitReportsNoMovement() {
        val state = state(centerTime = TimeLimits.MIN.toDouble())
        assertEquals(0f, state.scrollBy(128f, 0f).x, EPSILON)
        assertEquals(TimeLimits.MIN.toDouble(), state.centerTime, 0.0)
    }

    @Test
    fun restoredScrollBeyondContentIsClamped() {
        assertEquals(MAX_SCROLL_PX, state(scrollY = 5_000f).scrollY, EPSILON)
    }

    @Test
    fun tallerViewportAfterRotationClampsScroll() {
        val state = state()
        state.scrollBy(0f, -10_000f)
        state.updateLayout(DENSITY, WIDTH_PX, 600f, 10, ROW_PX)
        assertEquals(680f, state.scrollY, EPSILON)
    }

    @Test
    fun noRowsMeansNoVisibleRows() {
        assertTrue(state(rowCount = 0).visibleRowRange().isEmpty())
    }

    @Test
    fun timeZoneChangeKeepsTheSameInstant() {
        val state = state()
        state.calendar = NEW_YORK
        assertEquals(noon, state.centerTime, 0.0)
        assertEquals(500f, state.timeToX(noon), EPSILON)
    }

    @Test
    fun flingKeepsScrolling() = runTest {
        val state = state()
        withContext(TestFrameClock()) { state.fling(Velocity(-2_000f, 0f)) }
        assertTrue(state.centerTime > noon)
    }

    @Test
    fun flingStopsAtTheLimit() = runTest {
        val state = state(centerTime = TimeLimits.MAX - HOUR_MS)
        withContext(TestFrameClock()) { state.fling(Velocity(-1_000_000f, 0f)) }
        assertEquals(TimeLimits.MAX.toDouble(), state.centerTime, 0.0)
    }

    private companion object {
        const val DENSITY = 2f
        const val WIDTH_PX = 1_000f
        const val HEIGHT_PX = 300f
        const val ROW_PX = 128f
        const val MAX_SCROLL_PX = 980f
        const val HOUR_MS = 3_600_000.0
        const val EPSILON = 1e-3f
    }
}
