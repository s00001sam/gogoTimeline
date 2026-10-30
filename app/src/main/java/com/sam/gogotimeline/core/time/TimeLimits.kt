package com.sam.gogotimeline.core.time

import com.sam.gogotimeline.core.TimeConst

object TimeLimits {
    const val MIN: Long = TimeConst.MIN_TIME_MS
    const val MAX: Long = TimeConst.MAX_TIME_MS

    fun clamp(time: Long): Long = time.coerceIn(MIN, MAX)

    fun clamp(time: Double): Double = time.coerceIn(MIN.toDouble(), MAX.toDouble())

    /** Shifts [range] back inside the limits, keeping its duration. */
    fun clampRange(range: TimeRange): TimeRange {
        val start = range.start.coerceAtMost(MAX - range.duration).coerceAtLeast(MIN)
        return TimeRange(start, start + range.duration)
    }
}
