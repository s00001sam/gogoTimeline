package com.sam.gogotimeline.core

/**
 * All fixed values used by the app live here as `const val`.
 *
 * `const val` only supports primitives and strings, so dp values are declared as Float and
 * converted with `.dp` at the call site. Colors live in ui/theme/Color.kt as Compose `Color`.
 */

/** Time axis rules. */
object TimeConst {
    // Scroll limits, shared with the date picker range.
    const val MIN_YEAR = 1900
    const val MAX_YEAR = 2100

    // Fixed instants inside MIN_YEAR..MAX_YEAR in every zone from UTC-12 to UTC+14, so a time zone change
    // never moves the limits: 1900-01-01 12:00 UTC and 2100-12-31 10:00 UTC.
    const val MIN_TIME_MS = -2_208_945_600_000L
    const val MAX_TIME_MS = 4_133_930_400_000L

    // Standard width of one unit at 1x, in dp.
    const val HOUR_STANDARD_WIDTH_DP = 64f // 1 hour
    const val DAY_STANDARD_WIDTH_DP = 64f // 1 day
    const val WEEK_STANDARD_WIDTH_DP = 112f // 1 week
    const val MONTH_STANDARD_WIDTH_DP = 90f // 1 month (30 days)
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

    // Zoom limits: Hour standard width x 4, Month standard width x 0.5.
    const val ZOOM_MAX_HOUR_FACTOR = 4f
    const val ZOOM_MIN_MONTH_FACTOR = 0.5f

    // Unit switch point = geometric mean of two units' standard scales; zoom in at x factor, out at / factor.
    const val UNIT_SWITCH_HYSTERESIS = 1.25f

    const val STANDARD_WIDTH_TOLERANCE = 0.001f

    // A new task lasts this many of the current unit.
    const val NEW_TASK_UNIT_COUNT = 2L

    // Label every Nth bottom tick; the first step that leaves enough spacing wins.
    @JvmField val HOUR_LABEL_STEPS = intArrayOf(1, 2, 3, 6, 12)

    @JvmField val DAY_LABEL_STEPS = intArrayOf(1, 2, 7, 14)

    @JvmField val WEEK_LABEL_STEPS = intArrayOf(1, 2, 4, 8)

    @JvmField val MONTH_LABEL_STEPS = intArrayOf(1, 2, 3, 6, 12)
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
    const val TICK_LABEL_PADDING_DP = 4f

    // Tick data is prepared one viewport width beyond each side (3 widths in total), refilled when less
    // than half a width remains, and rebuilt after zooming in leaves more than 5 widths (tentative).
    const val TICK_BUFFER_WIDTHS = 1f
    const val TICK_REFILL_WIDTHS = 0.5f
    const val TICK_MAX_PREPARED_WIDTHS = 5f

    // Grid and "now" line
    const val GRID_LINE_WIDTH_DP = 1f
    const val NOW_LINE_WIDTH_DP = 2f
    const val NOW_DOT_RADIUS_DP = 3f

    // Left pane
    const val NAME_COLUMN_PHONE_COLLAPSED_DP = 64f
    const val NAME_COLUMN_PHONE_EXPANDED_DP = 80f
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
