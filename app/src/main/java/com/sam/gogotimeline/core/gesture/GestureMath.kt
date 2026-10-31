package com.sam.gogotimeline.core.gesture

import com.sam.gogotimeline.core.LayoutConst
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot

enum class DragDirection {
    HORIZONTAL,
    VERTICAL,
}

/** Gesture math in px: direction lock, reorder, edge auto-scroll, and vertical scrolling. */
object GestureMath {
    /**
     * A bit shorter than the system long-press timeout, which felt slow on a device. Subtracting keeps it longer
     * for people who raised the accessibility touch-and-hold delay; the floor keeps scrolls from reordering.
     */
    fun longPressTimeout(systemMillis: Long): Long =
        (systemMillis - LayoutConst.LONG_PRESS_SHORTEN_MILLIS).coerceAtLeast(LayoutConst.LONG_PRESS_MIN_MILLIS)

    /** Null until the pointer has moved past [touchSlop]; then the larger axis wins. */
    fun lockDirection(
        dx: Float,
        dy: Float,
        touchSlop: Float,
    ): DragDirection? {
        if (hypot(dx, dy) <= touchSlop) return null
        return if (abs(dx) > abs(dy)) DragDirection.HORIZONTAL else DragDirection.VERTICAL
    }

    /** Row under the dragged row's center; independent of which rows are composed. */
    fun reorderTargetIndex(
        fromIndex: Int,
        deltaY: Float,
        rowHeightPx: Float,
        rowCount: Int,
    ): Int {
        val centerY = (fromIndex + 0.5f) * rowHeightPx + deltaY
        return floor(centerY / rowHeightPx).toInt().coerceIn(0, rowCount - 1)
    }

    /** Insert, not swap: rows in between shift by one. */
    fun <T> moveItem(
        items: List<T>,
        from: Int,
        to: Int,
    ): List<T> = items.toMutableList().apply { add(to, removeAt(from)) }

    /** Negative near [start], positive near [end], zero outside both zones; linear up to [maxSpeed]. */
    fun autoScrollSpeed(
        position: Float,
        start: Float,
        end: Float,
        zone: Float,
        maxSpeed: Float,
    ): Float {
        val toStart = position - start
        val toEnd = end - position
        return when {
            toStart < zone && toStart <= toEnd -> -maxSpeed * closeness(toStart, zone)
            toEnd < zone -> maxSpeed * closeness(toEnd, zone)
            else -> 0f
        }
    }

    fun clampScrollY(
        scrollY: Float,
        rowCount: Int,
        rowHeightPx: Float,
        viewportHeightPx: Float,
    ): Float = scrollY.coerceIn(0f, (rowCount * rowHeightPx - viewportHeightPx).coerceAtLeast(0f))

    /** Visible rows plus [buffer] rows above and below. */
    fun visibleRowRange(
        scrollY: Float,
        viewportHeightPx: Float,
        rowHeightPx: Float,
        rowCount: Int,
        buffer: Int,
    ): IntRange {
        if (rowCount == 0) return IntRange.EMPTY
        val first = floor(scrollY / rowHeightPx).toInt() - buffer
        val last = floor((scrollY + viewportHeightPx) / rowHeightPx).toInt() + buffer
        return first.coerceAtLeast(0)..last.coerceAtMost(rowCount - 1)
    }

    /** Rows to prepare ahead: [visible] extended by one page (its own size) each way, within the rows. */
    fun preparedRowRange(
        visible: IntRange,
        rowCount: Int,
    ): IntRange {
        if (visible.isEmpty() || rowCount == 0) return IntRange.EMPTY
        val page = visible.last - visible.first + 1
        return (visible.first - page).coerceAtLeast(0)..(visible.last + page).coerceAtMost(rowCount - 1)
    }

    /**
     * One frame's step toward [target]: rows outside it are dropped at once, [visible] rows are always kept, and
     * at most one row is added, on the side with fewer rows ready. Spreads row composition over spare frames.
     */
    fun nextPreparedRows(
        current: IntRange,
        visible: IntRange,
        target: IntRange,
    ): IntRange {
        if (visible.isEmpty()) return IntRange.EMPTY
        var first = visible.first
        var last = visible.last
        if (touches(current, visible)) {
            first = maxOf(current.first, target.first).coerceAtMost(visible.first)
            last = minOf(current.last, target.last).coerceAtLeast(visible.last)
        }
        val canGrowUp = first > target.first
        val canGrowDown = last < target.last
        when {
            canGrowUp && (!canGrowDown || visible.first - first <= last - visible.last) -> first--
            canGrowDown -> last++
        }
        return first..last
    }

    /** Rows to compose: both ranges when they touch; after a jump only [visible], never everything in between. */
    fun composedRows(
        visible: IntRange,
        prepared: IntRange,
    ): IntRange = if (touches(prepared, visible)) {
        minOf(visible.first, prepared.first)..maxOf(visible.last, prepared.last)
    } else {
        visible
    }

    private fun touches(
        a: IntRange,
        b: IntRange,
    ): Boolean = !a.isEmpty() && !b.isEmpty() && a.first <= b.last + 1 && a.last >= b.first - 1

    private fun closeness(
        distance: Float,
        zone: Float,
    ): Float = (1f - distance / zone).coerceIn(0f, 1f)
}
