package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.data.datasource.SettingsLocalDataSource
import com.sam.gogotimeline.data.local.toEntity
import com.sam.gogotimeline.data.model.TaskSortMode
import com.sam.gogotimeline.data.model.TimelineSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryImplTest {
    private val settings = mockk<SettingsLocalDataSource>(relaxed = true)
    private val transaction = RecordingTransactionRunner()
    private val repository = SettingsRepositoryImpl(settings, transaction)

    // Hour pinched to 96dp at 1x, left pane open.
    private val stored = TimelineSettings.DEFAULT.copy(
        zoom = ZoomLevel(TimeUnit.HOUR, 96f),
        widthMultiplier = 1f,
        snapIntervals = TimeUnit.entries.associateWith { SnapInterval.options(it).last() },
        rowHeightDp = 80f,
        leftPaneExpanded = true,
    )

    @Test
    fun zoomSavesUnitAndWidthTogether() = runTest {
        repository.setZoom(ZoomLevel(TimeUnit.WEEK, 90f))
        coVerify { settings.updateZoom(TimeUnit.WEEK, 90f) }
    }

    @Test
    fun storedRowBecomesSettings() = runTest {
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
    fun widthMultiplierScalesTheCurrentWidthInOneTransaction() = runTest {
        coEvery { settings.get() } returns stored.toEntity()
        coEvery { settings.updateWidth(any(), any()) } answers { assertTrue(transaction.active) }
        repository.setWidthMultiplier(2f)
        coVerify { settings.updateWidth(2f, 192f) }
        assertEquals(1, transaction.runs)
    }

    @Test
    fun widthMultiplierWithoutARowScalesTheDefaults() = runTest {
        coEvery { settings.get() } returns null
        repository.setWidthMultiplier(0.5f)
        coVerify { settings.updateWidth(0.5f, TimeUnit.DAY.standardWidthDp * 0.5f) }
    }

    @Test
    fun failedWidthWriteLeavesTheTransactionToRollBack() = runTest {
        coEvery { settings.get() } returns stored.toEntity()
        coEvery { settings.updateWidth(any(), any()) } throws IllegalStateException("disk")
        val result = runCatching { repository.setWidthMultiplier(2f) }
        assertTrue(result.isFailure)
        assertEquals(1, transaction.runs)
    }

    @Test
    fun resetKeepsUnitPaneDrawerAndLockConfirmationAndScalesTheWidthBack() = runTest {
        // 2x with Hour pinched to 192dp goes back to 1x at 96dp (spec 8); a closed tablet drawer stays closed, and the
        // first-lock confirmation is not asked again (spec 9).
        coEvery { settings.get() } returns stored.copy(
            zoom = ZoomLevel(TimeUnit.HOUR, 192f),
            widthMultiplier = 2f,
            drawerExpanded = false,
            lockConfirmed = true,
            taskSortMode = TaskSortMode.START_TIME,
        ).toEntity()
        coEvery { settings.insert(any()) } answers { assertTrue(transaction.active) }
        repository.resetToDefaults()
        val expected = TimelineSettings.DEFAULT.copy(
            zoom = ZoomLevel(TimeUnit.HOUR, 96f),
            leftPaneExpanded = true,
            drawerExpanded = false,
            lockConfirmed = true,
            taskSortMode = TaskSortMode.START_TIME,
        )
        coVerify { settings.insert(expected.toEntity()) }
    }

    @Test
    fun drawerStateIsSavedOnItsOwn() = runTest {
        repository.setDrawerExpanded(false)
        coVerify { settings.updateDrawerExpanded(false) }
    }

    @Test
    fun lockConfirmationIsSavedOnItsOwnAndStartsUnseen() = runTest {
        repository.markLockConfirmed()
        coVerify(exactly = 1) { settings.markLockConfirmed() }
        assertFalse(TimelineSettings.DEFAULT.lockConfirmed)
    }

    @Test
    fun sortModeIsSavedOnItsOwnAndStartsManual() = runTest {
        repository.setTaskSortMode(TaskSortMode.DURATION)
        coVerify(exactly = 1) { settings.updateTaskSortMode(TaskSortMode.DURATION) }
        assertEquals(TaskSortMode.MANUAL, TimelineSettings.DEFAULT.taskSortMode)
    }

    @Test
    fun resetWithoutARowWritesTheDefaults() = runTest {
        coEvery { settings.get() } returns null
        repository.resetToDefaults()
        coVerify { settings.insert(TimelineSettings.DEFAULT.toEntity()) }
    }
}
