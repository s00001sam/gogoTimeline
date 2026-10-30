package com.sam.gogotimeline.core.time

import kotlin.math.ceil
import kotlin.math.floor

/** What the timeline shows: the time at the horizontal center and the zoom. */
data class Viewport(
    // Double: a few pixels can be fractional milliseconds, and Long would accumulate rounding.
    val centerTime: Double,
    val unit: TimeUnit,
    val dpPerUnit: Float,
)

/**
 * Time ↔ x conversion for one [viewport] on a screen. x is in px from the left edge of the time
 * axis, measured from the center so values stay small and precise.
 */
class TimeScale(
    val viewport: Viewport,
    private val calendar: CalendarContext,
    private val density: Float,
    val widthPx: Float,
) {
    private val axis = calendar.axis(viewport.unit)
    private val pxPerUnit = viewport.dpPerUnit.toDouble() * density
    private val centerPosition = axis.position(viewport.centerTime)

    fun timeToX(time: Double): Float = ((axis.position(time) - centerPosition) * pxPerUnit + widthPx / 2).toFloat()

    fun timeToX(time: Long): Float = timeToX(time.toDouble())

    fun xToTime(x: Float): Double = axis.timeAt(centerPosition + (x - widthPx / 2) / pxPerUnit)

    fun visibleTimeRange(): TimeRange = TimeRange(
        floor(xToTime(0f)).toLong(),
        ceil(xToTime(widthPx)).toLong(),
    )

    /** Content follows the finger: dragging right by [dxPx] shows earlier times. */
    fun panBy(dxPx: Float): Viewport = viewport.copy(
        centerTime = TimeLimits.clamp(xToTime(widthPx / 2 - dxPx)),
    )

    /** Zooms around [focusX] so the time under the fingers stays put; may switch units. */
    fun zoomBy(
        factor: Float,
        focusX: Float,
        widthMultiplier: Float,
    ): Viewport {
        val focusTime = xToTime(focusX)
        val oldUnit = viewport.unit
        var dpPerUnit = ZoomRules.clampDpPerUnit(oldUnit, viewport.dpPerUnit * factor, widthMultiplier)
        val unit = ZoomRules.resolveUnit(oldUnit, dpPerUnit, widthMultiplier)
        if (unit != oldUnit) {
            dpPerUnit = ZoomRules.clampDpPerUnit(
                unit,
                convertDpPerUnit(dpPerUnit, oldUnit, unit, focusTime.toLong()),
                widthMultiplier,
            )
        }
        val newAxis = calendar.axis(unit)
        val centerPosition = newAxis.position(focusTime) - (focusX - widthPx / 2) / (dpPerUnit.toDouble() * density)
        return Viewport(TimeLimits.clamp(newAxis.timeAt(centerPosition)), unit, dpPerUnit)
    }

    /** Same center and on-screen scale expressed in [unit]; used before animating to another unit. */
    fun convertTo(unit: TimeUnit): Viewport = viewport.copy(
        unit = unit,
        dpPerUnit = convertDpPerUnit(viewport.dpPerUnit, viewport.unit, unit, viewport.centerTime.toLong()),
    )

    // Keeps dp per millisecond at [time], using the real lengths of the units containing it.
    private fun convertDpPerUnit(
        dpPerUnit: Float,
        from: TimeUnit,
        to: TimeUnit,
        time: Long,
    ): Float {
        val dpPerMs = dpPerUnit.toDouble() / calendar.axis(from).unitDurationAt(time)
        return (dpPerMs * calendar.axis(to).unitDurationAt(time)).toFloat()
    }
}
