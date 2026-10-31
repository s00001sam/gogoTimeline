package com.sam.gogotimeline.core.gesture

import com.sam.gogotimeline.core.LayoutConst
import kotlin.math.abs
import kotlin.math.floor

/** What a touch-down landed on. */
sealed interface HitTarget {
    data class StartHandle(val taskId: Long) : HitTarget

    data class EndHandle(val taskId: Long) : HitTarget

    data class BarBody(val taskId: Long) : HitTarget

    /** A bar's time label, including the one pinned to the left when the bar is off screen. Tap only. */
    data class BarLabel(val taskId: Long) : HitTarget

    data class LeftPaneRow(val taskId: Long) : HitTarget

    /** Blank space on the time axis inside a task's row. A tap counts as empty; a long press picks the row. */
    data class RowBlank(val taskId: Long) : HitTarget

    data object Header : HitTarget

    data object Empty : HitTarget
}

/** Horizontal extent of one row's bar and label, in px from the time axis' left edge. */
data class RowHitArea(
    val taskId: Long,
    val barLeft: Float,
    val barRight: Float,
    val labelLeft: Float?,
    val labelRight: Float?,
)

/** Sizes in px needed to turn a touch position into a row and a column. */
data class HitLayout(
    val axisLeft: Float,
    val headerHeight: Float,
    val rowHeight: Float,
    val scrollY: Float,
    val handleOutside: Float,
    val handleInside: Float,
)

object HitTest {
    /**
     * Order: header → row (empty below the last row) → left pane → handles of the selected task (they reach
     * outside the bar, so they come first) → bar → label → blank space in that row. A whole row's height counts
     * for its bar.
     */
    fun hitTest(
        x: Float,
        y: Float,
        layout: HitLayout,
        rows: List<RowHitArea>,
        selectedTaskId: Long?,
    ): HitTarget {
        if (y < layout.headerHeight) return HitTarget.Header
        val row = floor((y - layout.headerHeight + layout.scrollY) / layout.rowHeight).toInt()
        val area = rows.getOrNull(row) ?: return HitTarget.Empty
        if (x < layout.axisLeft) return HitTarget.LeftPaneRow(area.taskId)

        val axisX = x - layout.axisLeft
        if (area.taskId == selectedTaskId) handleAt(axisX, area, layout)?.let { return it }
        if (axisX >= area.barLeft && axisX <= area.barRight) return HitTarget.BarBody(area.taskId)
        val labelLeft = area.labelLeft
        val labelRight = area.labelRight
        if (labelLeft != null && labelRight != null && axisX >= labelLeft && axisX <= labelRight) {
            return HitTarget.BarLabel(area.taskId)
        }
        return HitTarget.RowBlank(area.taskId)
    }

    // When both handle zones contain the touch, the nearer real edge wins instead of always the left one.
    private fun handleAt(
        axisX: Float,
        area: RowHitArea,
        layout: HitLayout,
    ): HitTarget? {
        val inside = minOf(layout.handleInside, (area.barRight - area.barLeft) * LayoutConst.HANDLE_INSIDE_MAX_FRACTION)
        val inStart = axisX >= area.barLeft - layout.handleOutside && axisX <= area.barLeft + inside
        val inEnd = axisX >= area.barRight - inside && axisX <= area.barRight + layout.handleOutside
        return when {
            inStart && inEnd -> if (abs(axisX - area.barLeft) <= abs(axisX - area.barRight)) {
                HitTarget.StartHandle(area.taskId)
            } else {
                HitTarget.EndHandle(area.taskId)
            }

            inStart -> HitTarget.StartHandle(area.taskId)

            inEnd -> HitTarget.EndHandle(area.taskId)

            else -> null
        }
    }
}
