package com.sam.gogotimeline.data.datasource

import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.data.local.SettingsDao
import com.sam.gogotimeline.data.local.SettingsEntity
import com.sam.gogotimeline.data.model.TaskSortMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SettingsLocalDataSourceImpl @Inject constructor(private val settingsDao: SettingsDao) : SettingsLocalDataSource {
    override fun observe(): Flow<SettingsEntity?> = settingsDao.observe()

    override suspend fun get(): SettingsEntity? = settingsDao.get()

    override suspend fun insert(settings: SettingsEntity) = settingsDao.insert(settings)

    override suspend fun updateWidth(
        multiplier: Float,
        dpPerUnit: Float,
    ) = settingsDao.updateWidth(multiplier, dpPerUnit)

    override suspend fun updateZoom(
        unit: TimeUnit,
        dpPerUnit: Float,
    ) = settingsDao.updateZoom(unit, dpPerUnit)

    override suspend fun updateSnap(
        unit: TimeUnit,
        minutes: Int,
    ) = settingsDao.updateSnap(unit, minutes)

    override suspend fun updateRowHeight(dp: Float) = settingsDao.updateRowHeight(dp)

    override suspend fun updateLeftPaneExpanded(expanded: Boolean) = settingsDao.updateLeftPaneExpanded(expanded)

    override suspend fun updateDrawerExpanded(expanded: Boolean) = settingsDao.updateDrawerExpanded(expanded)

    override suspend fun markLockConfirmed() = settingsDao.markLockConfirmed()

    override suspend fun updateTaskSortMode(mode: TaskSortMode) = settingsDao.updateTaskSortMode(mode)
}
