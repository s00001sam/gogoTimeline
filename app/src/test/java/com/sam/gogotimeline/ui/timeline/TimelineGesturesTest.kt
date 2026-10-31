package com.sam.gogotimeline.ui.timeline

import com.sam.gogotimeline.core.gesture.DragDirection
import com.sam.gogotimeline.core.gesture.HitTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineGesturesTest {
    private val bar = HitTarget.BarBody(1)

    @Test
    fun selectedBarMovesRightAwayHorizontally() {
        assertEquals(DragKind.MOVE, dragKind(bar, longPressed = false, canMoveTime = true, DragDirection.HORIZONTAL))
    }

    @Test
    fun anyRowReordersAfterALongPress() {
        val row = HitTarget.RowBlank(2)
        assertEquals(DragKind.REORDER, dragKind(row, longPressed = true, canMoveTime = false, DragDirection.VERTICAL))
    }

    @Test
    fun verticalOnTheSelectedBarWithoutALongPressScrolls() {
        assertNull(dragKind(bar, longPressed = false, canMoveTime = true, DragDirection.VERTICAL))
    }

    @Test
    fun handlesResizeWithOrWithoutALongPress() {
        val start = HitTarget.StartHandle(1)
        val end = HitTarget.EndHandle(1)
        assertEquals(
            DragKind.RESIZE_START,
            dragKind(start, longPressed = false, canMoveTime = false, DragDirection.HORIZONTAL),
        )
        assertEquals(
            DragKind.RESIZE_END,
            dragKind(end, longPressed = true, canMoveTime = false, DragDirection.VERTICAL),
        )
    }

    @Test
    fun longPressOnAnUnselectedRowThenHorizontalDoesNothing() {
        assertNull(dragKind(HitTarget.BarBody(2), longPressed = true, canMoveTime = false, DragDirection.HORIZONTAL))
    }

    @Test
    fun longPressOnTheSelectedBarStillMovesHorizontally() {
        assertEquals(DragKind.MOVE, dragKind(bar, longPressed = true, canMoveTime = true, DragDirection.HORIZONTAL))
    }
}
