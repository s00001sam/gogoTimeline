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
    /** Tablets only: whether the docked board drawer is shown (spec 9). */
    val drawerExpanded: Boolean,
    /** Lock was accepted once in the first-lock confirmation, so locking does not ask again (spec 9). */
    val lockConfirmed: Boolean,
    /** Row order, shared by all boards; each board keeps its own manual order (spec 9). */
    val taskSortMode: TaskSortMode,
) {
    fun snapFor(unit: TimeUnit): SnapInterval = snapIntervals[unit] ?: SnapInterval.default(unit)

    companion object {
        val DEFAULT = TimelineSettings(
            zoom = ZoomLevel(TimeUnit.DAY, TimeUnit.DAY.standardWidthDp),
            widthMultiplier = TimeConst.WIDTH_MULTIPLIER_DEFAULT,
            snapIntervals = TimeUnit.entries.associateWith { SnapInterval.default(it) },
            rowHeightDp = LayoutConst.ROW_HEIGHT_DEFAULT_DP,
            leftPaneExpanded = false,
            drawerExpanded = true,
            lockConfirmed = false,
            taskSortMode = TaskSortMode.MANUAL,
        )
    }
}
