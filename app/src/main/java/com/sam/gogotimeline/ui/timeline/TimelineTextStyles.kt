package com.sam.gogotimeline.ui.timeline

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.core.LayoutConst

/**
 * Text styles for everything inside the scheduler. Sizes are fixed in dp and ignore the system font
 * scale, because the header height and row height are fixed and larger text would clip (spec 10.3.1).
 */
@Immutable
data class TimelineTextStyles(
    val tickTop: TextStyle,
    val tickBottom: TextStyle,
    val taskName: TextStyle,
    val barText: TextStyle,
    val snapHint: TextStyle,
)

@Composable
fun rememberTimelineTextStyles(): TimelineTextStyles {
    val density = LocalDensity.current
    val typography = MaterialTheme.typography
    return remember(density, typography) {
        TimelineTextStyles(
            tickTop = typography.labelMedium.fixed(
                density,
                LayoutConst.TEXT_TICK_TOP_DP,
                LayoutConst.TEXT_TICK_TOP_LINE_DP,
            ),
            tickBottom = typography.labelSmall.fixed(
                density,
                LayoutConst.TEXT_TICK_BOTTOM_DP,
                LayoutConst.TEXT_TICK_BOTTOM_LINE_DP,
            ),
            taskName = typography.labelMedium.fixed(
                density,
                LayoutConst.TEXT_TASK_NAME_DP,
                LayoutConst.TEXT_TASK_NAME_LINE_DP,
            ),
            barText = typography.labelSmall.fixed(
                density,
                LayoutConst.TEXT_BAR_DP,
                LayoutConst.TEXT_BAR_LINE_DP,
            ),
            snapHint = typography.labelSmall.fixed(
                density,
                LayoutConst.TEXT_SNAP_HINT_DP,
                LayoutConst.TEXT_SNAP_HINT_LINE_DP,
            ),
        )
    }
}

// dp.toSp() divides by the font scale, so the drawn size stays the same whatever the system setting.
private fun TextStyle.fixed(
    density: Density,
    sizeDp: Float,
    lineHeightDp: Float,
): TextStyle = with(density) {
    copy(fontSize = sizeDp.dp.toSp(), lineHeight = lineHeightDp.dp.toSp())
}
