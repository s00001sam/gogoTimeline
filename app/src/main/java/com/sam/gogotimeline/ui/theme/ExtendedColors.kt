package com.sam.gogotimeline.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors that Material's ColorScheme has no slot for.
 * The database stores a task color index (0–6); [taskColor] maps it to the variant for the current mode.
 */
@Immutable
data class ExtendedColors(
    val taskColors: List<Color>,
    val barLabelBackground: Color,
    val selectionOutline: Color,
    val nowLine: Color,
    val gridMinor: Color,
    val gridMajor: Color,
) {
    /** Returns the task color for [index], wrapping out-of-range values instead of crashing. */
    fun taskColor(index: Int): Color = taskColors[Math.floorMod(index, taskColors.size)]
}

internal val LightExtendedColors = ExtendedColors(
    taskColors = TaskColorsLight,
    barLabelBackground = BarLabelBackgroundLight,
    selectionOutline = SelectionOutlineLight,
    nowLine = NowLineLight,
    gridMinor = GridMinorLight,
    gridMajor = GridMajorLight,
)

internal val DarkExtendedColors = ExtendedColors(
    taskColors = TaskColorsDark,
    barLabelBackground = BarLabelBackgroundDark,
    selectionOutline = SelectionOutlineDark,
    nowLine = NowLineDark,
    gridMinor = GridMinorDark,
    gridMajor = GridMajorDark,
)

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
