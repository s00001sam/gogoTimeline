package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.data.model.TimelineSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Emits the defaults until the settings row exists. */
    fun observeSettings(): Flow<TimelineSettings>

    suspend fun setZoom(zoom: ZoomLevel)

    suspend fun setSnap(
        unit: TimeUnit,
        interval: SnapInterval,
    )

    /** Saves the multiplier and scales the current width by the same ratio, together (spec 8). */
    suspend fun setWidthMultiplier(multiplier: Float)

    suspend fun setRowHeight(dp: Float)

    suspend fun setLeftPaneExpanded(expanded: Boolean)

    suspend fun setDrawerExpanded(expanded: Boolean)

    /**
     * Resets the settings page's values: multiplier, row height and all four snap intervals. The current unit, the
     * left pane and the tablet drawer stay; the width scales back with the multiplier.
     */
    suspend fun resetToDefaults()
}
