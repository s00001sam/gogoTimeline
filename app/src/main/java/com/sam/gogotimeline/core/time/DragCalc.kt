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

    /**
     * The start can go no later than one grid line before the end's line. Counted in grid lines, not elapsed time,
     * so on a 23- or 25-hour DST day the shortest result is still exactly one day.
     */
    fun resizeStart(
        original: TimeRange,
        deltaMs: Double,
        grid: TimeGrid,
    ): TimeRange {
        val latest = grid.floor(grid.floor(original.end) - 1)
        val start = grid.nearest((original.start + deltaMs).roundToLong()).coerceAtMost(latest)
        return original.copy(start = TimeLimits.clamp(start))
    }

    /**
     * Whether resizing keeps the times as they are: both ends on the grid and at least one grid step long. The step is
     * "the next grid line after the start", so a 23-hour DST day still counts as one day. Otherwise the snap hint shows.
     */
    fun fitsGrid(
        range: TimeRange,
        grid: TimeGrid,
    ): Boolean = grid.isOnGrid(range.start) &&
        grid.isOnGrid(range.end) &&
        grid.ceil(range.start + 1) <= range.end

    /** The end can go no earlier than one grid line after the start's line; counted in lines like [resizeStart]. */
    fun resizeEnd(
        original: TimeRange,
        deltaMs: Double,
        grid: TimeGrid,
    ): TimeRange {
        val earliest = grid.ceil(grid.ceil(original.start) + 1)
        val end = grid.nearest((original.end + deltaMs).roundToLong()).coerceAtLeast(earliest)
        return original.copy(end = TimeLimits.clamp(end))
    }
}
