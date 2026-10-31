package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.data.datasource.SettingsLocalDataSource
import com.sam.gogotimeline.data.local.toEntity
import com.sam.gogotimeline.data.model.TimelineSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryImplTest {
    private val settings = mockk<SettingsLocalDataSource>(relaxed = true)
    private val repository = SettingsRepositoryImpl(settings)

    @Test
    fun zoomSavesUnitAndWidthTogether() = runTest {
        repository.setZoom(ZoomLevel(TimeUnit.WEEK, 90f))
        coVerify { settings.updateZoom(TimeUnit.WEEK, 90f) }
    }

    @Test
    fun storedRowBecomesSettings() = runTest {
        val stored = TimelineSettings.DEFAULT.copy(rowHeightDp = 80f, leftPaneExpanded = true)
        coEvery { settings.observe() } returns flowOf(stored.toEntity())
        assertEquals(stored, repository.observeSettings().first())
    }

    @Test
    fun snapIsSavedInMinutes() = runTest {
        repository.setSnap(TimeUnit.MONTH, SnapInterval(SnapInterval.WEEK_MINUTES))
        coVerify { settings.updateSnap(TimeUnit.MONTH, 10_080) }
    }

    @Test
    fun missingRowMeansDefaults() = runTest {
        coEvery { settings.observe() } returns flowOf(null)
        assertEquals(TimelineSettings.DEFAULT, repository.observeSettings().first())
    }

    @Test
    fun resetWritesTheDefaultRow() = runTest {
        repository.resetToDefaults()
        coVerify { settings.insert(TimelineSettings.DEFAULT.toEntity()) }
    }
}
