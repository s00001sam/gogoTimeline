package com.sam.gogotimeline.ui.board

import androidx.lifecycle.SavedStateHandle
import com.sam.gogotimeline.data.repository.BoardRepository
import com.sam.gogotimeline.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BoardFormViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<BoardRepository>(relaxed = true)

    @Test
    fun createBlocksBlankAndLimitsTypingToFortyCharacters() {
        val viewModel = BoardFormViewModel(repository, SavedStateHandle())
        viewModel.initializeCreate()
        assertFalse(viewModel.state.value.canSubmit)
        viewModel.onNameChange("a".repeat(41))
        assertEquals(40, viewModel.state.value.name.length)
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun createReturnsTheNewBoardId() = runTest {
        coEvery { repository.create("Work") } returns 7
        val viewModel = BoardFormViewModel(repository, SavedStateHandle())
        viewModel.initializeCreate()
        viewModel.onNameChange("Work")
        viewModel.submit()
        assertEquals(BoardFormResult.Created(7), viewModel.result.first())
    }

    @Test
    fun unchangedRenameIsDisabledAndChangedNameIsSaved() = runTest {
        val viewModel = BoardFormViewModel(repository, SavedStateHandle())
        viewModel.initializeRename(7, "Work")
        assertFalse(viewModel.state.value.canSubmit)
        viewModel.onNameChange("Renamed")
        viewModel.submit()
        assertEquals(BoardFormResult.Renamed, viewModel.result.first())
        coVerify { repository.rename(7, "Renamed") }
    }

    @Test
    fun failedWriteKeepsTheFormForRetry() {
        coEvery { repository.create(any()) } throws IllegalStateException("disk")
        val viewModel = BoardFormViewModel(repository, SavedStateHandle())
        viewModel.initializeCreate()
        viewModel.onNameChange("Work")
        viewModel.submit()
        assertTrue(viewModel.state.value.saveFailed)
        assertFalse(viewModel.state.value.isSaving)
        assertEquals("Work", viewModel.state.value.name)
    }
}
