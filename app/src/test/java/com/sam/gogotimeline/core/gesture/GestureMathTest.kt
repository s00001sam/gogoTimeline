package com.sam.gogotimeline.core.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMathTest {
    private fun speed(
        position: Float,
        end: Float = 400f,
    ): Float = GestureMath.autoScrollSpeed(position, 0f, end, ZONE, MAX_SPEED)

    private fun target(
        fromIndex: Int,
        deltaY: Float,
        rowCount: Int = 10,
    ): Int = GestureMath.reorderTargetIndex(fromIndex, deltaY, ROW, rowCount)

    @Test
    fun lockDirectionPicksTheLargerMove() {
        assertEquals(DragDirection.HORIZONTAL, GestureMath.lockDirection(20f, 5f, SLOP))
        assertEquals(DragDirection.VERTICAL, GestureMath.lockDirection(3f, 15f, SLOP))
    }

    @Test
    fun reorderByDragDistance() {
        assertEquals(3, target(2, 40f))
        assertEquals(1, target(2, -40f))
    }

    @Test
    fun moveItemInsertsInsteadOfSwapping() {
        assertEquals(listOf("B", "C", "A", "D"), GestureMath.moveItem(listOf("A", "B", "C", "D"), 0, 2))
    }

    @Test
    fun autoScrollOnlyNearEdges() {
        assertEquals(0f, speed(200f), EPSILON)
        assertEquals(MAX_SPEED, speed(400f), EPSILON)
    }

    @Test
    fun noDirectionWithinTouchSlop() {
        assertNull(GestureMath.lockDirection(5f, 5f, SLOP))
        assertNull(GestureMath.lockDirection(8f, 0f, SLOP))
    }

    @Test
    fun equalMovesCountAsVertical() {
        assertEquals(DragDirection.VERTICAL, GestureMath.lockDirection(10f, 10f, SLOP))
    }

    @Test
    fun reorderSwitchesAtHalfARow() {
        assertEquals(2, target(2, 31f))
        assertEquals(3, target(2, 32f))
    }

    @Test
    fun reorderStopsAtFirstAndLastRow() {
        assertEquals(0, target(0, -500f, rowCount = 5))
        assertEquals(4, target(4, 500f, rowCount = 5))
    }

    @Test
    fun moveItemOtherPositions() {
        val items = listOf("A", "B", "C", "D")
        assertEquals(items, GestureMath.moveItem(items, 2, 2))
        assertEquals(listOf("D", "A", "B", "C"), GestureMath.moveItem(items, 3, 0))
        assertEquals(listOf("A", "D", "B", "C"), GestureMath.moveItem(items, 3, 1))
    }

    @Test
    fun autoScrollSpeedGrowsTowardTheEdge() {
        assertEquals(-300f, speed(24f), EPSILON)
        assertEquals(300f, speed(376f), EPSILON)
        assertEquals(0f, speed(48f), EPSILON)
    }

    @Test
    fun clampScrollYToContent() {
        assertEquals(340f, GestureMath.clampScrollY(500f, 10, ROW, 300f), EPSILON)
        assertEquals(0f, GestureMath.clampScrollY(-10f, 10, ROW, 300f), EPSILON)
    }

    @Test
    fun visibleRowsIncludeBuffer() {
        assertEquals(0..8, GestureMath.visibleRowRange(130f, 300f, ROW, 20, BUFFER))
        assertEquals(13..19, GestureMath.visibleRowRange(980f, 300f, ROW, 20, BUFFER))
    }

    @Test
    fun noTasksMeansNoVisibleRows() {
        assertTrue(GestureMath.visibleRowRange(0f, 300f, ROW, 0, BUFFER).isEmpty())
    }

    @Test
    fun singleTaskAlwaysStaysFirst() {
        assertEquals(0, target(0, 500f, rowCount = 1))
        assertEquals(0, target(0, -500f, rowCount = 1))
    }

    @Test
    fun fingerPastTheEdgeKeepsMaxSpeed() {
        assertEquals(-MAX_SPEED, speed(-10f), EPSILON)
        assertEquals(MAX_SPEED, speed(410f), EPSILON)
    }

    @Test
    fun narrowAreaUsesTheNearerEdge() {
        assertTrue(speed(20f, end = 60f) < 0f)
        assertTrue(speed(40f, end = 60f) > 0f)
    }

    @Test
    fun fewRowsCannotScroll() {
        assertEquals(0f, GestureMath.clampScrollY(100f, 3, ROW, 300f), EPSILON)
    }

    @Test
    fun longPressIsAHundredMillisShorter() {
        assertEquals(400L, GestureMath.longPressTimeout(500L))
    }

    @Test
    fun shortestSystemSettingLandsOnTheFloor() {
        assertEquals(300L, GestureMath.longPressTimeout(400L))
    }

    @Test
    fun longPressNeverGoesBelowTheFloorAndKeepsLongDelays() {
        assertEquals(300L, GestureMath.longPressTimeout(350L))
        assertEquals(1_400L, GestureMath.longPressTimeout(1_500L))
    }

    private fun fill(
        visible: IntRange,
        target: IntRange,
        from: IntRange = IntRange.EMPTY,
    ): List<IntRange> = generateSequence(from) { current ->
        GestureMath.nextPreparedRows(current, visible, target).takeIf { it != current }
    }.drop(1).toList()

    @Test
    fun preparesOnePageEachWay() {
        assertEquals(2..25, GestureMath.preparedRowRange(10..17, rowCount = 100))
    }

    @Test
    fun fillsOneRowPerStepAndStops() {
        val steps = fill(visible = 10..17, target = 2..25)
        assertEquals(9..17, steps.first())
        assertEquals(2..25, steps.last())
        assertEquals(16, steps.size)
    }

    @Test
    fun pageNearTheTopStaysWithinTheRows() {
        assertEquals(0..15, GestureMath.preparedRowRange(0..7, rowCount = 100))
    }

    @Test
    fun growsTheSideWithFewerRowsReady() {
        assertEquals(9..19, GestureMath.nextPreparedRows(10..19, visible = 10..17, target = 2..25))
    }

    @Test
    fun jumpComposesOnlyTheVisibleRows() {
        assertEquals(50..57, GestureMath.composedRows(visible = 50..57, prepared = 2..25))
        assertEquals(49..57, GestureMath.nextPreparedRows(2..25, visible = 50..57, target = 42..65))
    }

    @Test
    fun scrollingDownDropsRowsBeyondTheTarget() {
        assertEquals(12..28, GestureMath.nextPreparedRows(2..25, visible = 20..27, target = 12..35))
    }

    @Test
    fun noRowsPrepareNothing() {
        assertEquals(IntRange.EMPTY, GestureMath.preparedRowRange(IntRange.EMPTY, rowCount = 0))
        assertEquals(IntRange.EMPTY, GestureMath.nextPreparedRows(2..25, IntRange.EMPTY, IntRange.EMPTY))
    }

    private companion object {
        const val SLOP = 8f
        const val ROW = 64f
        const val ZONE = 48f
        const val MAX_SPEED = 600f
        const val BUFFER = 2
        const val EPSILON = 1e-3f
    }
}
