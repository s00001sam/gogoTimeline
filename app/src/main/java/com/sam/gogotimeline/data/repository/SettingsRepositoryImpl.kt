package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.core.time.ZoomRules
import com.sam.gogotimeline.data.datasource.SettingsLocalDataSource
import com.sam.gogotimeline.data.datasource.TransactionRunner
import com.sam.gogotimeline.data.local.toDomain
import com.sam.gogotimeline.data.local.toEntity
import com.sam.gogotimeline.data.model.TaskSortMode
import com.sam.gogotimeline.data.model.TimelineSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataSource: SettingsLocalDataSource,
    private val transaction: TransactionRunner,
) : SettingsRepository {
    override fun observeSettings(): Flow<TimelineSettings> =
        settingsDataSource.observe().map { it?.toDomain() ?: TimelineSettings.DEFAULT }

    override suspend fun setZoom(zoom: ZoomLevel) = settingsDataSource.updateZoom(zoom.unit, zoom.dpPerUnit)

    override suspend fun setSnap(
        unit: TimeUnit,
        interval: SnapInterval,
    ) = settingsDataSource.updateSnap(unit, interval.minutes)

    // Read and write in one transaction, so a zoom saved in between cannot be scaled twice or lost.
    override suspend fun setWidthMultiplier(multiplier: Float) = transaction.run {
        val current = current()
        val zoom = ZoomRules.rescale(current.zoom, current.widthMultiplier, multiplier)
        settingsDataSource.updateWidth(multiplier, zoom.dpPerUnit)
    }

    override suspend fun setRowHeight(dp: Float) = settingsDataSource.updateRowHeight(dp)

    override suspend fun setLeftPaneExpanded(expanded: Boolean) = settingsDataSource.updateLeftPaneExpanded(expanded)

    override suspend fun setDrawerExpanded(expanded: Boolean) = settingsDataSource.updateDrawerExpanded(expanded)

    override suspend fun markLockConfirmed() = settingsDataSource.markLockConfirmed()

    override suspend fun setTaskSortMode(mode: TaskSortMode) = settingsDataSource.updateTaskSortMode(mode)

    override suspend fun resetToDefaults() = transaction.run {
        val current = current()
        val defaults = TimelineSettings.DEFAULT
        val zoom = ZoomRules.rescale(current.zoom, current.widthMultiplier, defaults.widthMultiplier)
        settingsDataSource.insert(
            defaults.copy(
                zoom = zoom,
                leftPaneExpanded = current.leftPaneExpanded,
                drawerExpanded = current.drawerExpanded,
                lockConfirmed = current.lockConfirmed,
                taskSortMode = current.taskSortMode,
            ).toEntity(),
        )
    }

    private suspend fun current(): TimelineSettings = settingsDataSource.get()?.toDomain() ?: TimelineSettings.DEFAULT
}
