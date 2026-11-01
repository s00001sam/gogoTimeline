package com.sam.gogotimeline.ui.timeline

import com.sam.gogotimeline.core.gesture.GestureMath
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.DragCalc
import com.sam.gogotimeline.core.time.TaskTimeFormat
import com.sam.gogotimeline.core.time.TimeGrid
import com.sam.gogotimeline.core.time.TimeRange

enum class DragKind {
    MOVE,
    RESIZE_START,
    RESIZE_END,
    REORDER,
}

/**
 * One drag of a task. It remembers the time and content-space y under the finger at touch-down instead of
 * screen positions, so the task stays under the finger while the view auto-scrolls.
 */
data class DragSession(
    val taskId: Long,
    val kind: DragKind,
    val original: TimeRange,
    val originalIndex: Int,
    val downTime: Double,
    val downContentY: Float,
) {
    /** Times while moving or resizing, snapped to [grid]; reordering keeps the original times. */
    fun previewRange(
        timeUnderFinger: Double,
        grid: TimeGrid,
    ): TimeRange {
        val delta = timeUnderFinger - downTime
        return when (kind) {
            DragKind.MOVE -> DragCalc.move(original, delta, grid)
            DragKind.RESIZE_START -> DragCalc.resizeStart(original, delta, grid)
            DragKind.RESIZE_END -> DragCalc.resizeEnd(original, delta, grid)
            DragKind.REORDER -> original
        }
    }

    /**
     * The time of the bar's edge that leads the drag, which decides sideways edge auto-scroll: the dragged edge when
     * resizing, and when moving, the end going right or the start going left. Null while reordering.
     */
    fun leadingEdgeTime(preview: TimeRange): Long? = when (kind) {
        DragKind.RESIZE_START -> preview.start
        DragKind.RESIZE_END -> preview.end
        DragKind.MOVE -> if (preview.start >= original.start) preview.end else preview.start
        DragKind.REORDER -> null
    }

    /** Row the dragged row would be inserted at; only reordering moves it. */
    fun targetIndex(
        contentY: Float,
        rowHeightPx: Float,
        rowCount: Int,
    ): Int = if (kind == DragKind.REORDER) {
        GestureMath.reorderTargetIndex(originalIndex, contentY - downContentY, rowHeightPx, rowCount)
    } else {
        originalIndex
    }

    /** Text of the label near the finger: the start when moving or dragging the left edge, else the end. */
    fun dragTimeLabel(
        preview: TimeRange,
        grid: TimeGrid,
        calendar: CalendarContext,
    ): String = TaskTimeFormat.dragLabel(
        if (kind == DragKind.RESIZE_END) preview.end else preview.start,
        grid.interval,
        calendar,
    )
}

/** While reordering, rows between the original and target slots shift one row to make room. */
fun reorderShift(
    index: Int,
    from: Int,
    to: Int,
): Int = when {
    from < to && index in (from + 1)..to -> -1
    from > to && index in to until from -> 1
    else -> 0
}
