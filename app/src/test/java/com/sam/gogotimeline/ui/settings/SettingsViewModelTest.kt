package com.sam.gogotimeline.ui.settings

import com.sam.gogotimeline.core.RowHeight
import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.data.model.TimelineSettings
import com.sam.gogotimeline.data.repository.SettingsRepository
import com.sam.gogotimeline.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val settings = MutableStateFlow(TimelineSettings.DEFAULT.copy(zoom = ZoomLevel(TimeUnit.HOUR, 64f)))

    // The fake database echoes each write back through the flow, like Room does.
    private val repository = mockk<SettingsRepository>(relaxed = true) {
        every { observeSettings() } returns settings
        coEvery { setWidthMultiplier(any()) } answers { settings.value = settings.value.copy(widthMultiplier = arg(0)) }
        coEvery { setRowHeight(any()) } answers { settings.value = settings.value.copy(rowHeightDp = arg(0)) }
    }

    private fun viewModel() = SettingsViewModel(repository)

    private val SettingsViewModel.state get() = uiState.value!!

    @Test
    fun showsTheCurrentUnitsSnaps() {
        val state = viewModel().state
        assertEquals(TimeUnit.HOUR, state.currentUnit)
        assertEquals(SnapInterval.options(TimeUnit.HOUR), state.snapOptions)
        assertEquals(SnapInterval(15), state.selectedSnap)
    }

    @Test
    fun snapIsSavedForTheCurrentUnitOnly() {
        viewModel().onSnapSelected(SnapInterval(60))
        coVerify(exactly = 1) { repository.setSnap(TimeUnit.HOUR, SnapInterval(60)) }
    }

    @Test
    fun draggingWidthShowsItButDoesNotSave() {
        val viewModel = viewModel()
        viewModel.onWidthDrag(1.37f)
        assertEquals(1.37f, viewModel.state.shownWidthMultiplier)
        coVerify(exactly = 0) { repository.setWidthMultiplier(any()) }
    }

    @Test
    fun releasingWidthSavesItRoundedToATenth() {
        val viewModel = viewModel()
        viewModel.onWidthDrag(1.37f)
        viewModel.onWidthDragFinished()
        coVerify { repository.setWidthMultiplier(1.4f) }
        assertNull(viewModel.state.pendingWidthMultiplier)
        assertEquals(1.4f, viewModel.state.shownWidthMultiplier)
    }

    @Test
    fun rowHeightIsShownAsAMultiplierAndSavedInDp() {
        val viewModel = viewModel()
        assertEquals(1f, viewModel.state.shownRowHeightMultiplier)
        viewModel.onRowHeightDrag(1.27f)
        assertEquals(1.27f, viewModel.state.shownRowHeightMultiplier)
        coVerify(exactly = 0) { repository.setRowHeight(any()) }
        viewModel.onRowHeightDragFinished()
        coVerify { repository.setRowHeight(RowHeight.toDp(1.3f)) }
        assertNull(viewModel.state.pendingRowHeightMultiplier)
        assertEquals(RowHeight.toDp(1.3f), viewModel.state.rowHeightDp)
    }

    @Test
    fun releaseWithoutADragSavesNothing() {
        val viewModel = viewModel()
        viewModel.onWidthDragFinished()
        viewModel.onRowHeightDragFinished()
        coVerify(exactly = 0) { repository.setWidthMultiplier(any()) }
        coVerify(exactly = 0) { repository.setRowHeight(any()) }
    }

    @Test
    fun resetAsksFirst() {
        val viewModel = viewModel()
        viewModel.onResetClick()
        assertTrue(viewModel.state.showResetDialog)
        viewModel.onResetDismiss()
        assertFalse(viewModel.state.showResetDialog)
        coVerify(exactly = 0) { repository.resetToDefaults() }
        viewModel.onResetClick()
        viewModel.onResetConfirm()
        assertFalse(viewModel.state.showResetDialog)
        coVerify(exactly = 1) { repository.resetToDefaults() }
    }

    @Test
    fun failedSaveFallsBackToTheDatabaseAndReportsIt() = runTest {
        coEvery { repository.setWidthMultiplier(any()) } throws IllegalStateException("disk")
        val viewModel = viewModel()
        viewModel.onWidthDrag(1.8f)
        viewModel.onWidthDragFinished()
        assertNull(viewModel.state.pendingWidthMultiplier)
        assertEquals(1f, viewModel.state.shownWidthMultiplier)
        viewModel.saveFailed.first()
    }

    @Test
    fun unitChangedOnTheBoardSwitchesTheSnapOptions() {
        val viewModel = viewModel()
        settings.value = settings.value.copy(zoom = ZoomLevel(TimeUnit.MONTH, 90f))
        assertEquals(TimeUnit.MONTH, viewModel.state.currentUnit)
        assertEquals(SnapInterval(SnapInterval.DAY_MINUTES), viewModel.state.selectedSnap)
        viewModel.onSnapSelected(SnapInterval(SnapInterval.WEEK_MINUTES))
        coVerify { repository.setSnap(TimeUnit.MONTH, SnapInterval(SnapInterval.WEEK_MINUTES)) }
    }
}
