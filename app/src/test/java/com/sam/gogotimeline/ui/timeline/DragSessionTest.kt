package com.sam.gogotimeline.ui.timeline

import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.at
import com.sam.gogotimeline.core.time.local
import org.junit.Assert.assertEquals
import org.junit.Test

class DragSessionTest {
    private val quarterGrid = TAIPEI.grid(SnapInterval(15))
    private val hourGrid = TAIPEI.grid(SnapInterval(60))
    private val halfDayGrid = TAIPEI.grid(SnapInterval(720))
    private val dayGrid = TAIPEI.grid(SnapInterval(SnapInterval.DAY_MINUTES))

    private fun time(local: String): Double = TAIPEI.at("2026-10-30T$local").toDouble()

    private fun session(
        kind: DragKind,
        start: String = "09:00",
        end: String = "10:00",
        down: String = "09:30",
        index: Int = 2,
    ) = DragSession(
        taskId = 1,
        kind = kind,
        original = TimeRange(TAIPEI.at("2026-10-30T$start"), TAIPEI.at("2026-10-30T$end")),
        originalIndex = index,
        downTime = time(down),
        downContentY = (index + 0.5f) * ROW,
    )

    private fun assertRange(
        expectedStart: String,
        expectedEnd: String,
        actual: TimeRange,
    ) {
        assertEquals(expectedStart, TAIPEI.local(actual.start))
        assertEquals(expectedEnd, TAIPEI.local(actual.end))
    }

    @Test
    fun moveSnapsAndKeepsDuration() {
        val preview = session(DragKind.MOVE).previewRange(time("11:10"), hourGrid)
        assertRange("2026-10-30T11:00", "2026-10-30T12:00", preview)
    }

    @Test
    fun rightHandleSnapsTheEnd() {
        val preview = session(DragKind.RESIZE_END, down = "10:00").previewRange(time("10:40"), quarterGrid)
        assertRange("2026-10-30T09:00", "2026-10-30T10:45", preview)
    }

    @Test
    fun reorderMovesOneRowDown() {
        assertEquals(3, session(DragKind.REORDER).targetIndex(2.5f * ROW + ROW, ROW, ROW_COUNT))
    }

    @Test
    fun rowsInBetweenMoveUpWhenDraggingDown() {
        assertEquals(listOf(0, 0, -1, -1, 0), (0..4).map { reorderShift(it, from = 1, to = 3) })
    }

    @Test
    fun nonReorderDragKeepsTheRow() {
        assertEquals(2, session(DragKind.MOVE).targetIndex(10 * ROW, ROW, ROW_COUNT))
    }

    @Test
    fun rowsInBetweenMoveDownWhenDraggingUp() {
        assertEquals(listOf(0, 1, 1, 0, 0), (0..4).map { reorderShift(it, from = 3, to = 1) })
    }

    @Test
    fun droppingBackInPlaceMovesNothing() {
        assertEquals(listOf(0, 0, 0, 0, 0), (0..4).map { reorderShift(it, from = 2, to = 2) })
    }

    @Test
    fun timeLabelFormat() {
        val move = session(DragKind.MOVE)
        assertEquals("11:00", move.dragTimeLabel(move.previewRange(time("11:10"), hourGrid), hourGrid, TAIPEI))
        assertEquals("12:00", move.dragTimeLabel(move.previewRange(time("12:10"), halfDayGrid), halfDayGrid, TAIPEI))
        val resizeEnd = session(DragKind.RESIZE_END, start = "10:00", end = "10:15", down = "10:15")
        val preview = resizeEnd.previewRange(time("10:15"), dayGrid)
        assertEquals("Nov 1 00:00", resizeEnd.dragTimeLabel(preview, dayGrid, TAIPEI))
    }

    @Test
    fun autoScrollMovesTheTaskWithTheFinger() {
        val preview = session(DragKind.MOVE).previewRange(time("09:30") + 2 * HOUR_MS, hourGrid)
        assertRange("2026-10-30T11:00", "2026-10-30T12:00", preview)
    }

    @Test
    fun reorderBeyondTheEndsIsClamped() {
        val drag = session(DragKind.REORDER)
        assertEquals(ROW_COUNT - 1, drag.targetIndex(100 * ROW, ROW, ROW_COUNT))
        assertEquals(0, drag.targetIndex(-100 * ROW, ROW, ROW_COUNT))
    }

    @Test
    fun leftHandlePastTheEndKeepsTheMinimum() {
        val preview = session(DragKind.RESIZE_START, down = "09:00").previewRange(time("12:00"), quarterGrid)
        assertRange("2026-10-30T09:45", "2026-10-30T10:00", preview)
    }

    @Test
    fun reorderKeepsTheTimes() {
        val drag = session(DragKind.REORDER)
        assertEquals(drag.original, drag.previewRange(time("15:00"), hourGrid))
    }

    private fun range(
        start: String,
        end: String,
    ) = TimeRange(TAIPEI.at("2026-10-30T$start"), TAIPEI.at("2026-10-30T$end"))

    @Test
    fun movingRightLeadsWithTheEndAndLeftWithTheStart() {
        val move = session(DragKind.MOVE)
        assertEquals(TAIPEI.at("2026-10-30T12:00"), move.leadingEdgeTime(range("11:00", "12:00")))
        assertEquals(TAIPEI.at("2026-10-30T07:00"), move.leadingEdgeTime(range("07:00", "08:00")))
    }

    @Test
    fun resizingLeadsWithTheDraggedEdgeAndAnUnmovedBarWithItsEnd() {
        assertEquals(
            TAIPEI.at("2026-10-30T08:00"),
            session(DragKind.RESIZE_START).leadingEdgeTime(range("08:00", "10:00")),
        )
        assertEquals(
            TAIPEI.at("2026-10-30T13:00"),
            session(DragKind.RESIZE_END).leadingEdgeTime(range("09:00", "13:00")),
        )
        // Not moved yet: treated as heading right, so the end leads.
        assertEquals(TAIPEI.at("2026-10-30T10:00"), session(DragKind.MOVE).leadingEdgeTime(range("09:00", "10:00")))
    }

    @Test
    fun reorderingHasNoLeadingEdge() {
        assertEquals(null, session(DragKind.REORDER).leadingEdgeTime(range("09:00", "10:00")))
    }

    private companion object {
        const val ROW = 100f
        const val ROW_COUNT = 5
        const val HOUR_MS = 3_600_000.0
    }
}
