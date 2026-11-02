package com.sam.gogotimeline.core.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun scrollingStopsWhenTheLastTaskIsTheTopRow() {
        assertEquals(9 * ROW, GestureMath.clampScrollY(5_000f, 10, ROW), EPSILON)
        assertEquals(500f, GestureMath.clampScrollY(500f, 10, ROW), EPSILON)
        assertEquals(0f, GestureMath.clampScrollY(-10f, 10, ROW), EPSILON)
    }

    @Test
    fun rowLinesFillTheViewportPastTheTasks() {
        // 300px tall, rows 64px, not scrolled: lines at 64, 128, 192, 256 whether or not tasks are there.
        assertEquals(listOf(64f, 128f, 192f, 256f), GestureMath.rowLineYs(0f, ROW, 300f))
        assertEquals(listOf(44f, 108f, 172f, 236f, 300f), GestureMath.rowLineYs(20f, ROW, 300f))
    }

    @Test
    fun rowLinesNeedARowHeight() {
        assertTrue(GestureMath.rowLineYs(0f, 0f, 300f).isEmpty())
        assertTrue(GestureMath.rowLineYs(0f, ROW, 0f).isEmpty())
    }

    // The editor card in the bottom right of an 800 × 1000 scheduler.
    private val card = FloatRect(left = 424f, top = 440f, right = 784f, bottom = 984f)

    @Test
    fun barUnderTheCardIsCoveredEvenPartly() {
        assertTrue(GestureMath.isCovered(500f, 700f, 600f, 664f, card))
        assertTrue(GestureMath.isCovered(100f, 500f, 600f, 664f, card))
    }

    @Test
    fun barTouchingTheCardEdgeIsNotCovered() {
        assertFalse(GestureMath.isCovered(100f, 424f, 600f, 664f, card))
        assertFalse(GestureMath.isCovered(500f, 700f, 376f, 440f, card))
    }

    @Test
    fun barAboveTheCardOrFarOffScreenIsNotCovered() {
        assertFalse(GestureMath.isCovered(500f, 700f, 100f, 164f, card))
        assertFalse(GestureMath.isCovered(-50_000f, -40_000f, 600f, 664f, card))
        // A bar spanning far past both sides of the card at its height still counts.
        assertTrue(GestureMath.isCovered(-50_000f, 50_000f, 600f, 664f, card))
    }

    @Test
    fun barEdgeThatStartedOnScreenDecidesInsteadOfTheFinger() {
        // A long bar held near its left: its right edge at 390 is already near the 400px edge.
        assertEquals(390f, GestureMath.autoScrollPosition(edgeX = 390f, fingerX = 120f, edgeStartedOnScreen = true))
    }

    @Test
    fun fastDragPastTheSideKeepsFollowingTheEdge() {
        // A quick drag can carry the edge past the card within one frame; scrolling keeps following the edge.
        assertEquals(650f, GestureMath.autoScrollPosition(edgeX = 650f, fingerX = 300f, edgeStartedOnScreen = true))
        assertEquals(MAX_SPEED, speed(650f), EPSILON)
    }

    @Test
    fun edgeThatStartedOffScreenFollowsTheFinger() {
        assertEquals(120f, GestureMath.autoScrollPosition(edgeX = 5_000f, fingerX = 120f, edgeStartedOnScreen = false))
        assertEquals(120f, GestureMath.autoScrollPosition(edgeX = -5_000f, fingerX = 120f, edgeStartedOnScreen = false))
    }

    @Test
    fun fingerLevelWithTheCardScrollsAtItsLeftSide() {
        assertEquals(424f, GestureMath.autoScrollRightEdge(pointerY = 700f, screenRight = 800f, cover = card), EPSILON)
    }

    @Test
    fun fingerAboveTheCardOrWithoutOneUsesTheScreenEdge() {
        assertEquals(800f, GestureMath.autoScrollRightEdge(pointerY = 200f, screenRight = 800f, cover = card), EPSILON)
        assertEquals(800f, GestureMath.autoScrollRightEdge(pointerY = 700f, screenRight = 800f, cover = null), EPSILON)
        // Exactly on the card's top edge already counts as level with it.
        assertEquals(424f, GestureMath.autoScrollRightEdge(pointerY = 440f, screenRight = 800f, cover = card), EPSILON)
    }

    @Test
    fun cardPastTheScreenNeverPushesTheEdgeOutward() {
        val offScreen = FloatRect(left = 900f, top = 440f, right = 1_200f, bottom = 984f)
        assertEquals(
            800f,
            GestureMath.autoScrollRightEdge(pointerY = 700f, screenRight = 800f, cover = offScreen),
            EPSILON,
        )
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
    fun fewRowsStillScrollUntilTheLastIsOnTop() {
        assertEquals(2 * ROW, GestureMath.clampScrollY(1_000f, 3, ROW), EPSILON)
        assertEquals(0f, GestureMath.clampScrollY(100f, 1, ROW), EPSILON)
        assertEquals(0f, GestureMath.clampScrollY(100f, 0, ROW), EPSILON)
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

    @Test
    fun selectedRowEndsTheGapAboveTheSheet() {
        // Rows 64px, sheet top at 500px, 64px gap: row 10's bottom (704px) has to end at 436px, so scroll 268px.
        assertEquals(268f, GestureMath.rowAboveSheetScrollY(10, ROW, sheetTopPx = 500f, gapPx = ROW))
    }

    @Test
    fun rowAlreadyAtTheGapNeedsNoScroll() {
        // Row 5 ends at 384px = 448px sheet top − 64px gap.
        assertEquals(0f, GestureMath.rowAboveSheetScrollY(5, ROW, sheetTopPx = 448f, gapPx = ROW))
    }

    @Test
    fun topRowsAndAVeryTallSheetLeaveTheLimitsToTheCaller() {
        // Row 0 would need a negative scroll; the caller clamps it to 0, so the top row stays put.
        assertEquals(-372f, GestureMath.rowAboveSheetScrollY(0, ROW, sheetTopPx = 500f, gapPx = ROW))
        // A sheet reaching above the header (huge font) still gives a finite target to clamp.
        assertEquals(9_999 * ROW + 2 * ROW + 10f, GestureMath.rowAboveSheetScrollY(9_999, ROW, -10f, ROW))
    }

    @Test
    fun selectedRowMovesToTheSecondVisibleLine() {
        assertEquals(576f, GestureMath.secondRowScrollY(10, ROW))
    }

    @Test
    fun aRowAlreadyInViewKeepsTheScroll() {
        // Rows 2–6 fit in 320px from scroll 128; row 4 is in the middle.
        assertEquals(128f, GestureMath.scrollYToReveal(4, ROW, scrollY = 128f, viewportHeightPx = 320f))
    }

    @Test
    fun aRowAboveOrBelowMovesToTheNearerEdge() {
        assertEquals(64f, GestureMath.scrollYToReveal(1, ROW, scrollY = 128f, viewportHeightPx = 320f))
        // Row 9 ends at 640; the body then shows 320..640.
        assertEquals(320f, GestureMath.scrollYToReveal(9, ROW, scrollY = 128f, viewportHeightPx = 320f))
    }

    @Test
    fun aRowTouchingAnEdgeOrTallerThanTheBodyStillGetsItsTop() {
        // Row 6 ends exactly at the bottom edge (128 + 320 = 448), so nothing moves.
        assertEquals(128f, GestureMath.scrollYToReveal(6, ROW, scrollY = 128f, viewportHeightPx = 320f))
        // A row taller than the body (huge row height) shows from its top when scrolled up to.
        assertEquals(0f, GestureMath.scrollYToReveal(0, ROW, scrollY = 10f, viewportHeightPx = 40f))
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
