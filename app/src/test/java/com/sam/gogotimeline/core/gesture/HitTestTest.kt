package com.sam.gogotimeline.core.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class HitTestTest {
    private val layout = HitLayout(
        axisLeft = AXIS_LEFT,
        headerHeight = HEADER,
        rowHeight = ROW,
        scrollY = 0f,
        handleOutside = 64f,
        handleInside = 32f,
    )

    private val rows = listOf(
        RowHitArea(taskId = 1, barLeft = 100f, barRight = 400f, labelLeft = 108f, labelRight = 300f),
        RowHitArea(taskId = 2, barLeft = 500f, barRight = 530f, labelLeft = 538f, labelRight = 700f),
        RowHitArea(taskId = 3, barLeft = -2_000f, barRight = -1_000f, labelLeft = 8f, labelRight = 200f),
    )

    private fun hit(
        axisX: Float,
        row: Int,
        selectedTaskId: Long? = null,
        layout: HitLayout = this.layout,
        rows: List<RowHitArea> = this.rows,
        offsetInRow: Float = ROW / 2,
    ) = HitTest.hitTest(AXIS_LEFT + axisX, HEADER + row * ROW + offsetInRow, layout, rows, selectedTaskId)

    @Test
    fun tapOnBar() {
        assertEquals(HitTarget.BarBody(1), hit(250f, row = 0))
    }

    @Test
    fun tapOnLeftPane() {
        assertEquals(HitTarget.LeftPaneRow(2), HitTest.hitTest(50f, HEADER + ROW * 1.5f, layout, rows, null))
    }

    @Test
    fun tapOnHeaderOrCorner() {
        assertEquals(HitTarget.Header, HitTest.hitTest(500f, 50f, layout, rows, null))
        assertEquals(HitTarget.Header, HitTest.hitTest(50f, 50f, layout, rows, null))
    }

    @Test
    fun selectedTaskHasHandles() {
        assertEquals(HitTarget.StartHandle(1), hit(60f, row = 0, selectedTaskId = 1))
        assertEquals(HitTarget.EndHandle(1), hit(420f, row = 0, selectedTaskId = 1))
    }

    @Test
    fun belowTheLastRowIsEmpty() {
        assertEquals(HitTarget.Empty, hit(250f, row = 3))
        assertEquals(HitTarget.Empty, HitTest.hitTest(50f, HEADER + ROW * 3.5f, layout, rows, null))
    }

    @Test
    fun unselectedTaskHasNoHandles() {
        assertEquals(HitTarget.RowBlank(1), hit(60f, row = 0))
    }

    @Test
    fun blankSpaceInARowKnowsItsTask() {
        assertEquals(HitTarget.RowBlank(2), hit(900f, row = 1))
        assertEquals(HitTarget.Empty, hit(900f, row = 3))
    }

    @Test
    fun tapOnTimeLabel() {
        assertEquals(HitTarget.BarLabel(2), hit(600f, row = 1))
        assertEquals(HitTarget.BarLabel(3), hit(50f, row = 2))
    }

    @Test
    fun scrolledDownOneRow() {
        assertEquals(HitTarget.BarBody(2), hit(515f, row = 0, layout = layout.copy(scrollY = ROW)))
    }

    @Test
    fun wholeRowHeightCountsForTheBar() {
        assertEquals(HitTarget.BarBody(1), hit(250f, row = 0, offsetInRow = 2f))
    }

    @Test
    fun shortBarKeepsAMoveAreaInTheMiddle() {
        assertEquals(HitTarget.BarBody(2), hit(515f, row = 1, selectedTaskId = 2))
        assertEquals(HitTarget.StartHandle(2), hit(505f, row = 1, selectedTaskId = 2))
        assertEquals(HitTarget.EndHandle(2), hit(525f, row = 1, selectedTaskId = 2))
        assertEquals(HitTarget.StartHandle(2), hit(470f, row = 1, selectedTaskId = 2))
    }

    @Test
    fun zeroWidthBarPicksTheNearerHandle() {
        val zeroWidth =
            listOf(RowHitArea(taskId = 9, barLeft = 500f, barRight = 500f, labelLeft = null, labelRight = null))
        assertEquals(HitTarget.StartHandle(9), hit(499f, row = 0, selectedTaskId = 9, rows = zeroWidth))
        assertEquals(HitTarget.EndHandle(9), hit(501f, row = 0, selectedTaskId = 9, rows = zeroWidth))
    }

    @Test
    fun scrolledPastAllRowsOrNoTasks() {
        assertEquals(HitTarget.Empty, hit(250f, row = 0, layout = layout.copy(scrollY = 10_000f)))
        assertEquals(HitTarget.Empty, hit(250f, row = 0, rows = emptyList()))
    }

    private companion object {
        const val AXIS_LEFT = 200f
        const val HEADER = 100f
        const val ROW = 100f
    }
}
