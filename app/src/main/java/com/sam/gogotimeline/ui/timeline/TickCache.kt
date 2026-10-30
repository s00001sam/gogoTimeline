package com.sam.gogotimeline.ui.timeline

import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.Ticks
import com.sam.gogotimeline.core.time.TimeLimits
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.TimelineTicks

/**
 * Keeps header ticks for a range wider than the screen, so panning only moves them instead of
 * rebuilding labels every frame.
 */
class TickCache {
    private data class Key(
        val unit: TimeUnit,
        val labelStep: Int,
        val calendar: CalendarContext,
    )

    private var key: Key? = null
    private var prepared: TimeRange? = null
    private var ticks = TimelineTicks(emptyList(), emptyList())

    fun ticks(
        visible: TimeRange,
        unit: TimeUnit,
        dpPerUnit: Float,
        calendar: CalendarContext,
    ): TimelineTicks {
        val width = visible.duration
        val newKey = Key(unit, Ticks.labelStep(unit, dpPerUnit, LayoutConst.MIN_TICK_LABEL_SPACING_DP), calendar)
        val refill = (width * LayoutConst.TICK_REFILL_WIDTHS).toLong()
        // Nothing exists beyond the limits, so do not ask for buffer there or it would rebuild every frame.
        val neededStart = TimeLimits.clamp(visible.start - refill)
        val neededEnd = TimeLimits.clamp(visible.end + refill)
        val current = prepared
        val reusable = current != null &&
            newKey == key &&
            current.start <= neededStart &&
            current.end >= neededEnd &&
            current.duration <= width * LayoutConst.TICK_MAX_PREPARED_WIDTHS
        if (!reusable) {
            val buffer = (width * LayoutConst.TICK_BUFFER_WIDTHS).toLong()
            val range = TimeRange(TimeLimits.clamp(visible.start - buffer), TimeLimits.clamp(visible.end + buffer))
            ticks = Ticks.buildTicks(range, unit, dpPerUnit, calendar, LayoutConst.MIN_TICK_LABEL_SPACING_DP)
            prepared = range
            key = newKey
        }
        return ticks
    }
}
