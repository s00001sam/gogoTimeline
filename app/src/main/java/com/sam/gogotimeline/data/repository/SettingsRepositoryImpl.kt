package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.data.datasource.SettingsLocalDataSource
import com.sam.gogotimeline.data.local.toDomain
import com.sam.gogotimeline.data.local.toEntity
import com.sam.gogotimeline.data.model.TimelineSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl @Inject constructor(private val settingsDataSource: SettingsLocalDataSource) :
    SettingsRepository {
    override fun observeSettings(): Flow<TimelineSettings> =
        settingsDataSource.observe().map { it?.toDomain() ?: TimelineSettings.DEFAULT }

    override suspend fun setZoom(zoom: ZoomLevel) = settingsDataSource.updateZoom(zoom.unit, zoom.dpPerUnit)

    override suspend fun setSnap(
        unit: TimeUnit,
        interval: SnapInterval,
    ) = settingsDataSource.updateSnap(unit, interval.minutes)

    override suspend fun setRowHeight(dp: Float) = settingsDataSource.updateRowHeight(dp)

    override suspend fun setLeftPaneExpanded(expanded: Boolean) = settingsDataSource.updateLeftPaneExpanded(expanded)

    override suspend fun resetToDefaults() = settingsDataSource.insert(TimelineSettings.DEFAULT.toEntity())
}
