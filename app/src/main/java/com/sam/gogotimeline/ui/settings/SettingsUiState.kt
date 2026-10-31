package com.sam.gogotimeline.ui.settings

import com.sam.gogotimeline.core.RowHeight
import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TimeUnit

/**
 * The settings page. Slider values being dragged live only here ([pendingWidthMultiplier],
 * [pendingRowHeightMultiplier]) and are written to the database on release. Row height is stored in dp but shown as
 * a multiplier of the default.
 */
data class SettingsUiState(
    val currentUnit: TimeUnit,
    val selectedSnap: SnapInterval,
    val widthMultiplier: Float,
    val rowHeightDp: Float,
    val pendingWidthMultiplier: Float? = null,
    val pendingRowHeightMultiplier: Float? = null,
    val showResetDialog: Boolean = false,
) {
    val snapOptions: List<SnapInterval> get() = SnapInterval.options(currentUnit)

    /** What the sliders and the sample row show: the value being dragged, else the saved one. */
    val shownWidthMultiplier: Float get() = pendingWidthMultiplier ?: widthMultiplier

    val shownRowHeightMultiplier: Float get() = pendingRowHeightMultiplier ?: RowHeight.toMultiplier(rowHeightDp)
}
