package com.sam.gogotimeline.core.gesture

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot

enum class DragDirection {
    HORIZONTAL,
    VERTICAL,
}

/** Gesture math in px: direction lock, reorder, edge auto-scroll, and vertical scrolling. */
object GestureMath {
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

    private fun closeness(
        distance: Float,
        zone: Float,
    ): Float = (1f - distance / zone).coerceIn(0f, 1f)
}
