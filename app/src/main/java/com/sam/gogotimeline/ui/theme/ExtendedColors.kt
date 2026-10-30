package com.sam.gogotimeline.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.sam.gogotimeline.core.ColorConst
import com.sam.gogotimeline.core.TaskColorConst

/**
 * Colors that Material's ColorScheme has no slot for.
 * The database stores a task color index (0–6); [taskColor] maps it to the variant for the current mode.
 */
@Immutable
data class ExtendedColors(
    val taskColors: List<Color>,
    val selectionOutline: Color,
    val nowLine: Color,
    val gridMinor: Color,
    val gridMajor: Color,
) {
    /** Returns the task color for [index], wrapping out-of-range values instead of crashing. */
    fun taskColor(index: Int): Color = taskColors[Math.floorMod(index, taskColors.size)]
}

internal val LightExtendedColors = ExtendedColors(
    taskColors = listOf(
        Color(TaskColorConst.RED_LIGHT),
        Color(TaskColorConst.ORANGE_LIGHT),
        Color(TaskColorConst.YELLOW_LIGHT),
        Color(TaskColorConst.GREEN_LIGHT),
        Color(TaskColorConst.BLUE_LIGHT),
        Color(TaskColorConst.INDIGO_LIGHT),
        Color(TaskColorConst.VIOLET_LIGHT),
    ),
    selectionOutline = Color(ColorConst.SELECTION_OUTLINE_LIGHT),
    nowLine = Color(ColorConst.NOW_LINE_LIGHT),
    gridMinor = Color(ColorConst.GRID_MINOR_LIGHT),
    gridMajor = Color(ColorConst.GRID_MAJOR_LIGHT),
)

internal val DarkExtendedColors = ExtendedColors(
    taskColors = listOf(
        Color(TaskColorConst.RED_DARK),
        Color(TaskColorConst.ORANGE_DARK),
        Color(TaskColorConst.YELLOW_DARK),
        Color(TaskColorConst.GREEN_DARK),
        Color(TaskColorConst.BLUE_DARK),
        Color(TaskColorConst.INDIGO_DARK),
        Color(TaskColorConst.VIOLET_DARK),
    ),
    selectionOutline = Color(ColorConst.SELECTION_OUTLINE_DARK),
    nowLine = Color(ColorConst.NOW_LINE_DARK),
    gridMinor = Color(ColorConst.GRID_MINOR_DARK),
    gridMajor = Color(ColorConst.GRID_MAJOR_DARK),
)

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
