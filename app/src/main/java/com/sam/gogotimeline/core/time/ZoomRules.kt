package com.sam.gogotimeline.core.time

import com.sam.gogotimeline.core.TimeConst
import kotlin.math.abs
import kotlin.math.sqrt

/** Unit and width always travel together, e.g. Week + 112dp means one week is 112dp wide. */
data class ZoomLevel(
    val unit: TimeUnit,
    val dpPerUnit: Float,
)

/**
 * Zoom limits, automatic unit switching, and the unit button. Scales of different units are
 * compared in dp per nominal day, since dpPerUnit values of different units are not comparable.
 */
object ZoomRules {
    /** Max zoom applies only in Hour, min zoom only in Month; the other units switch before reaching them. */
    fun clampDpPerUnit(
        unit: TimeUnit,
        dpPerUnit: Float,
        widthMultiplier: Float,
    ): Float = when (unit) {
        TimeUnit.HOUR -> dpPerUnit.coerceAtMost(
            TimeUnit.HOUR.standardWidthDp(widthMultiplier) * TimeConst.ZOOM_MAX_HOUR_FACTOR,
        )

        TimeUnit.MONTH -> dpPerUnit.coerceAtLeast(
            TimeUnit.MONTH.standardWidthDp(widthMultiplier) * TimeConst.ZOOM_MIN_MONTH_FACTOR,
        )

        else -> dpPerUnit
    }

    /**
     * Unit for [dpPerUnit] given in [current]'s width. Zoom-in and zoom-out thresholds are offset
     * around each switch point, so the result depends on the current unit and does not flicker.
     */
    fun resolveUnit(
        current: TimeUnit,
        dpPerUnit: Float,
        widthMultiplier: Float,
    ): TimeUnit {
        val dpPerDay = dpPerUnit / current.nominalDays
        val hysteresis = TimeConst.UNIT_SWITCH_HYSTERESIS
        var unit = current
        while (true) {
            val finer = unit.finer() ?: break
            if (dpPerDay < switchPoint(finer, widthMultiplier) * hysteresis) break
            unit = finer
        }
        while (true) {
            val coarser = unit.coarser() ?: break
            if (dpPerDay > switchPoint(unit, widthMultiplier) / hysteresis) break
            unit = coarser
        }
        return unit
    }

    fun isAtStandardWidth(
        unit: TimeUnit,
        dpPerUnit: Float,
        widthMultiplier: Float,
    ): Boolean {
        val standard = unit.standardWidthDp(widthMultiplier)
        return abs(dpPerUnit - standard) / standard <= TimeConst.STANDARD_WIDTH_TOLERANCE
    }

    /** Not at the standard width → back to it; already there → the next unit's standard width. */
    fun unitButtonTarget(
        unit: TimeUnit,
        dpPerUnit: Float,
        widthMultiplier: Float,
    ): ZoomLevel {
        val target = if (isAtStandardWidth(unit, dpPerUnit, widthMultiplier)) unit.next() else unit
        return ZoomLevel(target, target.standardWidthDp(widthMultiplier))
    }

    // Geometric mean of the standard scales of [finer] and the unit above it, in dp per day.
    private fun switchPoint(
        finer: TimeUnit,
        widthMultiplier: Float,
    ): Double {
        val coarser = requireNotNull(finer.coarser())
        return sqrt(standardDpPerDay(finer, widthMultiplier) * standardDpPerDay(coarser, widthMultiplier))
    }

    private fun standardDpPerDay(
        unit: TimeUnit,
        widthMultiplier: Float,
    ): Double = unit.standardWidthDp(widthMultiplier) / unit.nominalDays
}
