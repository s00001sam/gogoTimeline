package com.sam.gogotimeline.data.model

import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.TimeConst
import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel

/** Scheduler settings shared by all boards. */
data class TimelineSettings(
    val zoom: ZoomLevel,
    val widthMultiplier: Float,
    val snapIntervals: Map<TimeUnit, SnapInterval>,
    val rowHeightDp: Float,
    val leftPaneExpanded: Boolean,
) {
    fun snapFor(unit: TimeUnit): SnapInterval = snapIntervals[unit] ?: SnapInterval.default(unit)

    companion object {
        val DEFAULT = TimelineSettings(
            zoom = ZoomLevel(TimeUnit.DAY, TimeUnit.DAY.standardWidthDp),
            widthMultiplier = TimeConst.WIDTH_MULTIPLIER_DEFAULT,
            snapIntervals = TimeUnit.entries.associateWith { SnapInterval.default(it) },
            rowHeightDp = LayoutConst.ROW_HEIGHT_DEFAULT_DP,
            leftPaneExpanded = false,
        )
    }
}
