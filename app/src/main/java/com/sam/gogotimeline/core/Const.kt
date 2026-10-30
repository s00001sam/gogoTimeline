package com.sam.gogotimeline.core

/**
 * All fixed values used by the app live here as `const val`.
 *
 * `const val` only supports primitives and strings, so:
 * - dp values are declared as Float and converted with `.dp` at the call site.
 * - colors are declared as Long (ARGB) and wrapped with `Color(...)` at the call site.
 */

/** App-level colors. */
object ColorConst {
    // Primary (PicCollage teal)
    const val PRIMARY_LIGHT = 0xFF50C2C4
    const val ON_PRIMARY_LIGHT = 0xFFFFFFFF
    const val PRIMARY_DARK = 0xFF3AA9AB
    const val ON_PRIMARY_DARK = 0xFF000000

    // Backgrounds and surfaces
    const val BACKGROUND_LIGHT = 0xFFFAFAFA
    const val BACKGROUND_DARK = 0xFF212121
    const val SURFACE_LIGHT = 0xFFFFFFFF
    const val SURFACE_DARK = 0xFF2A2A2A

    // Text
    const val TEXT_PRIMARY_LIGHT = 0xFF1F1F1F
    const val TEXT_PRIMARY_DARK = 0xFFECECEC
    const val TEXT_SECONDARY_LIGHT = 0xFF5F6368
    const val TEXT_SECONDARY_DARK = 0xFFA0A0A0

    // Selected tab / list item
    const val SELECTED_CONTAINER_LIGHT = 0xFFD9EDF0
    const val ON_SELECTED_CONTAINER_LIGHT = 0xFF00696B
    const val SELECTED_CONTAINER_DARK = 0xFF1F3E40
    const val ON_SELECTED_CONTAINER_DARK = 0xFF7FD3D5

    // Selected task outline
    const val SELECTION_OUTLINE_LIGHT = 0xFF000000
    const val SELECTION_OUTLINE_DARK = 0xFFFFFFFF

    // "Now" line
    const val NOW_LINE_LIGHT = 0xFF00696B
    const val NOW_LINE_DARK = 0xFF7FD3D5

    // Grid lines (tentative)
    const val GRID_MINOR_LIGHT = 0xFFE6E6E6
    const val GRID_MINOR_DARK = 0xFF333333
    const val GRID_MAJOR_LIGHT = 0xFFCFCFCF
    const val GRID_MAJOR_DARK = 0xFF4A4A4A
}

/** Task colors (tentative). Index order: red, orange, yellow, green, blue, indigo, violet. */
object TaskColorConst {
    const val COUNT = 7

    const val RED_LIGHT = 0xFFD32F2F
    const val ORANGE_LIGHT = 0xFFE65100
    const val YELLOW_LIGHT = 0xFFB88A00
    const val GREEN_LIGHT = 0xFF388E3C
    const val BLUE_LIGHT = 0xFF1E88E5
    const val INDIGO_LIGHT = 0xFF3F51B5
    const val VIOLET_LIGHT = 0xFF9C27B0

    const val RED_DARK = 0xFFEF5350
    const val ORANGE_DARK = 0xFFFF9800
    const val YELLOW_DARK = 0xFFFDD835
    const val GREEN_DARK = 0xFF66BB6A
    const val BLUE_DARK = 0xFF42A5F5
    const val INDIGO_DARK = 0xFF7986CB
    const val VIOLET_DARK = 0xFFBA68C8
}

/** Time axis rules. */
object TimeConst {
    // Scroll limits, shared with the date picker range.
    const val MIN_YEAR = 1900
    const val MAX_YEAR = 2100

    // Standard width of one unit at 1x, in dp.
    const val HOUR_STANDARD_WIDTH_DP = 64f   // 1 hour
    const val DAY_STANDARD_WIDTH_DP = 64f    // 1 day
    const val WEEK_STANDARD_WIDTH_DP = 112f  // 1 week
    const val MONTH_STANDARD_WIDTH_DP = 90f  // 1 month (30 days)
    const val MONTH_STANDARD_DAYS = 30

    // Standard-width multiplier (settings slider).
    const val WIDTH_MULTIPLIER_MIN = 0.5f
    const val WIDTH_MULTIPLIER_MAX = 2f
    const val WIDTH_MULTIPLIER_DEFAULT = 1f
    const val WIDTH_MULTIPLIER_STEP = 0.1f

    // Default snap interval per unit, in minutes.
    const val SNAP_HOUR_DEFAULT_MINUTES = 15
    const val SNAP_DAY_DEFAULT_MINUTES = 60
    const val SNAP_WEEK_DEFAULT_MINUTES = 1_440
    const val SNAP_MONTH_DEFAULT_MINUTES = 1_440

    // Smallest time step in the sheet's time picker and minimum task duration in the sheet.
    const val PICKER_MINUTE_STEP = 15
    const val SHEET_MIN_DURATION_MINUTES = 15
}

/** Layout and interaction values. */
object LayoutConst {
    // Rows
    const val ROW_HEIGHT_MIN_DP = 48f
    const val ROW_HEIGHT_MAX_DP = 96f
    const val ROW_HEIGHT_DEFAULT_DP = 64f
    const val ROW_HEIGHT_STEP_DP = 4f
    const val ROW_BUFFER = 2

    // Header
    const val HEADER_TOP_TIER_HEIGHT_DP = 20f
    const val HEADER_BOTTOM_TIER_HEIGHT_DP = 28f
    const val MIN_TICK_LABEL_SPACING_DP = 40f

    // Left pane
    const val NAME_COLUMN_PHONE_COLLAPSED_DP = 96f
    const val NAME_COLUMN_PHONE_EXPANDED_DP = 160f
    const val NAME_COLUMN_TABLET_COLLAPSED_DP = 160f
    const val NAME_COLUMN_TABLET_EXPANDED_DP = 240f
    const val TIME_COLUMN_WIDTH_DP = 110f

    // Task bar
    const val BAR_VERTICAL_PADDING_DP = 8f
    const val BAR_CORNER_RADIUS_DP = 6f
    const val SELECTION_OUTLINE_WIDTH_DP = 1.5f
    const val HANDLE_OUTSIDE_DP = 32f
    const val HANDLE_INSIDE_DP = 16f

    // Gestures
    const val EDGE_AUTO_SCROLL_ZONE_DP = 48f
    const val MAX_AUTO_SCROLL_SPEED_DP_PER_SECOND = 600f
    const val LONG_PRESS_TIMEOUT_MILLIS = 400L

    // Names
    const val NAME_MAX_LENGTH = 40

    // Phone vs. tablet
    const val TABLET_MIN_WIDTH_DP = 600f
    const val TABLET_MIN_HEIGHT_DP = 480f

    // Tablet dialogs
    const val DIALOG_WIDTH_DP = 560f
    const val DIALOG_MAX_HEIGHT_DP = 560f
    const val DIALOG_HEIGHT_FRACTION = 0.8f
    const val DIALOG_CORNER_RADIUS_DP = 28f

    // Drawer
    const val DRAWER_WIDTH_DP = 300f
}
