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

    suspend fun setRowHeight(dp: Float)

    suspend fun setLeftPaneExpanded(expanded: Boolean)

    suspend fun resetToDefaults()
}
