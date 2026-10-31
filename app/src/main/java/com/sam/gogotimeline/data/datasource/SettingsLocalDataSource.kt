package com.sam.gogotimeline.data.datasource

import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.data.local.SettingsEntity
import kotlinx.coroutines.flow.Flow

interface SettingsLocalDataSource {
    fun observe(): Flow<SettingsEntity?>

    suspend fun get(): SettingsEntity?

    suspend fun insert(settings: SettingsEntity)

    suspend fun updateWidth(
        multiplier: Float,
        dpPerUnit: Float,
    )

    suspend fun updateZoom(
        unit: TimeUnit,
        dpPerUnit: Float,
    )

    suspend fun updateSnap(
        unit: TimeUnit,
        minutes: Int,
    )

    suspend fun updateRowHeight(dp: Float)

    suspend fun updateLeftPaneExpanded(expanded: Boolean)
}
