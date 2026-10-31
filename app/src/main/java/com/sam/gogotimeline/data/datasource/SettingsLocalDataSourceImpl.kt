package com.sam.gogotimeline.data.datasource

import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.data.local.SettingsDao
import com.sam.gogotimeline.data.local.SettingsEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SettingsLocalDataSourceImpl @Inject constructor(private val settingsDao: SettingsDao) : SettingsLocalDataSource {
    override fun observe(): Flow<SettingsEntity?> = settingsDao.observe()

    override suspend fun insert(settings: SettingsEntity) = settingsDao.insert(settings)

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
}
