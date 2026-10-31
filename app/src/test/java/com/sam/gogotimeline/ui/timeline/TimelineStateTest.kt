package com.sam.gogotimeline.ui.timeline

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.unit.Velocity
import com.sam.gogotimeline.core.time.NEW_YORK
import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TimeLimits
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.core.time.at
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineStateTest {
    private val noon = TAIPEI.at("2026-10-30T12:00").toDouble()

    private fun state(
        centerTime: Double = noon,
        unit: TimeUnit = TimeUnit.HOUR,
        dpPerUnit: Float = 64f,
        scrollY: Float = 0f,
        rowCount: Int = 10,
        heightPx: Float = HEIGHT_PX,
    ) = TimelineState(
        initialCenterTime = centerTime,
        initialUnit = unit,
        initialDpPerUnit = dpPerUnit,
        initialScrollY = scrollY,
        calendar = TAIPEI,
    ).apply { updateLayout(DENSITY, AXIS_LEFT_PX, WIDTH_PX, heightPx, rowCount, ROW_PX) }

    /** Advances one 16ms frame per call so animations finish without real time. */
    private class TestFrameClock : MonotonicFrameClock {
        private var frameTimeNanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            frameTimeNanos += 16_000_000L
            return onFrame(frameTimeNanos)
        }
    }

    /** Waits 16ms of virtual time per frame, so a test can interrupt an animation part-way. */
    private class DelayFrameClock(private val scheduler: TestCoroutineScheduler) : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            delay(16)
            return onFrame(scheduler.currentTime * 1_000_000L)
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
    fun pinchKeepsTimeUnderFingers() {
        val state = state()
        val focusTime = state.xToTime(200f)
        state.zoomBy(1.5f, 200f)
        assertEquals(96f, state.dpPerUnit, EPSILON)
        assertEquals(200f, state.timeToX(focusTime), EPSILON)
    }

    @Test
    fun unitButtonReturnsToStandardWidth() = runTest {
        val state = state(unit = TimeUnit.DAY, dpPerUnit = 90f)
        withContext(TestFrameClock()) { state.animateZoomTo(ZoomLevel(TimeUnit.DAY, 64f)) }
        assertEquals(ZoomLevel(TimeUnit.DAY, 64f), state.zoomLevel)
        assertEquals(noon, state.centerTime, 0.0)
    }

    @Test
    fun unitButtonMovesToNextUnit() = runTest {
        val state = state(unit = TimeUnit.DAY, dpPerUnit = 64f)
        withContext(TestFrameClock()) { state.animateZoomTo(ZoomLevel(TimeUnit.WEEK, 112f)) }
        assertEquals(ZoomLevel(TimeUnit.WEEK, 112f), state.zoomLevel)
        assertEquals(noon, state.centerTime, 0.0)
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
    fun fewRowsScrollOnlyUntilTheLastIsOnTop() {
        val state = state(rowCount = 2)
        state.scrollBy(0f, -500f)
        assertEquals(ROW_PX, state.scrollY, EPSILON)
        assertEquals(0f, state(rowCount = 1).apply { scrollBy(0f, -500f) }.scrollY, EPSILON)
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
    fun zoomStopsAtTheMaximum() {
        val state = state(dpPerUnit = 250f)
        state.zoomBy(2f, 500f)
        assertEquals(256f, state.dpPerUnit, EPSILON)
    }

    @Test
    fun zoomPastAThresholdSwitchesUnit() {
        val state = state(unit = TimeUnit.DAY, dpPerUnit = 380f)
        state.zoomBy(1.05f, 500f)
        assertEquals(TimeUnit.HOUR, state.unit)
        assertEquals(399f / 24, state.dpPerUnit, EPSILON)
    }

    @Test
    fun widthMultiplierRaisesTheMaximum() {
        val state = state(dpPerUnit = 500f)
        state.widthMultiplier = 2f
        state.zoomBy(2f, 500f)
        assertEquals(512f, state.dpPerUnit, EPSILON)
    }

    @Test
    fun monthToHourAnimationEndsExactlyOnTarget() = runTest {
        val state = state(unit = TimeUnit.MONTH, dpPerUnit = 90f)
        withContext(TestFrameClock()) { state.animateZoomTo(ZoomLevel(TimeUnit.HOUR, 64f)) }
        assertEquals(ZoomLevel(TimeUnit.HOUR, 64f), state.zoomLevel)
    }

    @Test
    fun fewerRowsAfterADeleteClampScroll() {
        val state = state()
        state.scrollBy(0f, -10_000f)
        state.updateLayout(DENSITY, AXIS_LEFT_PX, WIDTH_PX, 600f, 4, ROW_PX)
        assertEquals(3 * ROW_PX, state.scrollY, EPSILON)
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

    @Test
    fun touchStopsFling() = runTest {
        val state = state()
        val fling = launch(DelayFrameClock(testScheduler)) { state.fling(Velocity(-20_000f, 0f)) }
        advanceTimeBy(100)
        state.stopAnimation()
        val stoppedAt = state.centerTime
        advanceTimeBy(1_000)
        assertTrue(fling.isCancelled)
        assertEquals(stoppedAt, state.centerTime, 0.0)
    }

    @Test
    fun touchStopsUnitButtonAnimation() = runTest {
        val state = state(unit = TimeUnit.DAY, dpPerUnit = 64f)
        val animation = launch(DelayFrameClock(testScheduler)) {
            state.animateZoomTo(ZoomLevel(TimeUnit.WEEK, 112f))
        }
        advanceTimeBy(100)
        state.stopAnimation()
        val stoppedAt = state.zoomLevel
        advanceTimeBy(1_000)
        assertTrue(animation.isCancelled)
        assertEquals(stoppedAt, state.zoomLevel)
        assertNotEquals(ZoomLevel(TimeUnit.WEEK, 112f), state.zoomLevel)
    }

    @Test
    fun goToTimeCentersItAndKeepsTheRest() = runTest {
        val state = state(scrollY = 256f)
        withContext(TestFrameClock()) { state.animateCenterTo(noon + 5 * HOUR_MS) }
        assertEquals(noon + 5 * HOUR_MS, state.centerTime, 0.0)
        assertEquals(ZoomLevel(TimeUnit.HOUR, 64f), state.zoomLevel)
        assertEquals(256f, state.scrollY, EPSILON)
    }

    @Test
    fun goToTimeFromFarAwayIsJustAsFast() = runTest {
        val state = state(centerTime = TAIPEI.at("1950-01-01T12:00").toDouble())
        val clock = CountingFrameClock()
        withContext(clock) { state.animateCenterTo(noon) }
        assertEquals(noon, state.centerTime, 0.0)
        assertTrue(clock.frames <= 21)
    }

    @Test
    fun goToTimeBeyondTheLimitStopsAtTheLimit() = runTest {
        val state = state()
        withContext(TestFrameClock()) { state.animateCenterTo(TimeLimits.MAX + 1e12) }
        assertEquals(TimeLimits.MAX.toDouble(), state.centerTime, 0.0)
    }

    @Test
    fun goToTimeInMonthModeLandsExactly() = runTest {
        val target = TAIPEI.at("2027-04-01T00:00").toDouble()
        val state = state(unit = TimeUnit.MONTH, dpPerUnit = 90f)
        withContext(TestFrameClock()) { state.animateCenterTo(target) }
        assertEquals(target, state.centerTime, 0.0)
    }

    @Test
    fun touchStopsGoToTime() = runTest {
        val state = state()
        val animation = launch(DelayFrameClock(testScheduler)) { state.animateCenterTo(noon + 100 * HOUR_MS) }
        advanceTimeBy(100)
        state.stopAnimation()
        val stoppedAt = state.centerTime
        advanceTimeBy(1_000)
        assertTrue(animation.isCancelled)
        assertEquals(stoppedAt, state.centerTime, 0.0)
        assertNotEquals(noon + 100 * HOUR_MS, state.centerTime, 0.0)
    }

    /** Like [TestFrameClock], and counts how many frames an animation took. */
    private class CountingFrameClock : MonotonicFrameClock {
        var frames = 0
            private set

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            frames++
            return onFrame(frames * 16_000_000L)
        }
    }

    @Test
    fun timeLandsAtTheRequestedX() = runTest {
        val state = state()
        val target = TAIPEI.at("2026-10-30T14:00").toDouble()
        withContext(TestFrameClock()) { state.animateTimeToX(target, 48f) }
        assertEquals(48f, state.timeToX(target), EPSILON)
        assertEquals(ZoomLevel(TimeUnit.HOUR, 64f), state.zoomLevel)
    }

    @Test
    fun timeToXStopsAtTheLimit() = runTest {
        val state = state()
        withContext(TestFrameClock()) { state.animateTimeToX(TAIPEI.at("1900-01-01T00:00").toDouble(), 48f) }
        assertEquals(48f, state.timeToX(TimeLimits.MIN.toDouble()), EPSILON)
        assertTrue(state.centerTime >= TimeLimits.MIN)
    }

    @Test
    fun timeToXFromFarAwayIsJustAsFast() = runTest {
        val state = state(centerTime = TAIPEI.at("1951-10-30T12:00").toDouble())
        val clock = CountingFrameClock()
        withContext(clock) { state.animateTimeToX(noon, 48f) }
        assertEquals(48f, state.timeToX(noon), EPSILON)
        assertTrue(clock.frames <= 21)
    }

    @Test
    fun jumpPutsTheRowAtTheTopAndTheTimeAtX() {
        val state = state()
        val time = TAIPEI.at("2026-10-30T14:00").toDouble()
        state.jumpTo(ZoomLevel(TimeUnit.DAY, 64f), rowIndex = 3, time = time, x = 48f)
        assertEquals(ZoomLevel(TimeUnit.DAY, 64f), state.zoomLevel)
        assertEquals(3 * ROW_PX, state.scrollY, EPSILON)
        assertEquals(48f, state.timeToX(time), EPSILON)
    }

    @Test
    fun jumpToTheLastRowStopsAtTheBottom() {
        val state = state()
        state.jumpTo(ZoomLevel(TimeUnit.HOUR, 64f), rowIndex = 9, time = noon, x = 48f)
        assertEquals(MAX_SCROLL_PX, state.scrollY, EPSILON)
    }

    @Test
    fun jumpBeforeTheRowsAreLaidOutWaitsForThem() {
        val state = state(rowCount = 0)
        state.jumpTo(ZoomLevel(TimeUnit.HOUR, 64f), rowIndex = 3, time = noon, x = 48f)
        assertEquals(0f, state.scrollY, EPSILON)
        state.updateLayout(DENSITY, AXIS_LEFT_PX, WIDTH_PX, HEIGHT_PX, 10, ROW_PX)
        assertEquals(3 * ROW_PX, state.scrollY, EPSILON)
    }

    private companion object {
        const val DENSITY = 2f
        const val AXIS_LEFT_PX = 380f
        const val WIDTH_PX = 1_000f
        const val HEIGHT_PX = 300f
        const val ROW_PX = 128f

        // 10 rows of 128px: the 10th (last) row can scroll up to the top, blank rows below it.
        const val MAX_SCROLL_PX = 9 * ROW_PX
        const val HOUR_MS = 3_600_000.0
        const val EPSILON = 1e-3f
    }
}
