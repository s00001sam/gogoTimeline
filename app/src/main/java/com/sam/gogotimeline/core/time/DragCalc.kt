package com.sam.gogotimeline.core.time

import kotlin.math.roundToLong

/**
 * Preview of a task being dragged. [deltaMs] is the raw time moved since touch-down; it is snapped
 * as a whole instead of per frame, which avoids sticking and jitter.
 */
object DragCalc {
    /** Snaps the start and keeps the duration, even if it is shorter than one snap interval. */
    fun move(
        original: TimeRange,
        deltaMs: Double,
        grid: TimeGrid,
    ): TimeRange {
        val start = grid.nearest((original.start + deltaMs).roundToLong())
        return TimeLimits.clampRange(TimeRange(start, start + original.duration))
    }

    /** The start can go no later than (end − one interval), floored to the grid. */
    fun resizeStart(
        original: TimeRange,
        deltaMs: Double,
        grid: TimeGrid,
    ): TimeRange {
        val latest = grid.floor(original.end - grid.interval.millis)
        val start = grid.nearest((original.start + deltaMs).roundToLong()).coerceAtMost(latest)
        return original.copy(start = TimeLimits.clamp(start))
    }

    /** The end can go no earlier than (start + one interval), ceiled to the grid. */
    fun resizeEnd(
        original: TimeRange,
        deltaMs: Double,
        grid: TimeGrid,
    ): TimeRange {
        val earliest = grid.ceil(original.start + grid.interval.millis)
        val end = grid.nearest((original.end + deltaMs).roundToLong()).coerceAtLeast(earliest)
        return original.copy(end = TimeLimits.clamp(end))
    }
}
