package com.sam.gogotimeline.ui.navigation

import com.sam.gogotimeline.data.model.Board
import com.sam.gogotimeline.data.model.TimelineSettings
import com.sam.gogotimeline.data.repository.BoardRepository
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val boards = MutableStateFlow<List<Board>>(emptyList())
    private val repository = mockk<BoardRepository>(relaxed = true) { every { observeBoards() } returns boards }
    private val settings = MutableStateFlow(TimelineSettings.DEFAULT)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true) {
        every { observeSettings() } returns settings
    }

    private fun viewModel() = AppViewModel(repository, settingsRepository)

    @Test
    fun exposesTheRepositorysOrderedBoardList() {
        val viewModel = viewModel()
        val recent = Board(2, "Recent", null, 20)
        val older = Board(1, "Older", null, 10)
        boards.value = listOf(recent, older)
        assertEquals(listOf(recent, older), viewModel.boards.value)
    }

    @Test
    fun deletingReturnsTheNextMostRecentBoard() = runTest {
        val current = Board(2, "Current", null, 20)
        val next = Board(1, "Next", null, 10)
        boards.value = listOf(current, next)
        coEvery { repository.getStartBoardId() } returns next.id
        val viewModel = viewModel()
        viewModel.requestDelete(current.id)
        viewModel.confirmDelete()
        assertEquals(BoardDeleted(current.id, next.id), viewModel.boardDeleted.first())
        coVerify { repository.delete(current.id) }
    }

    @Test
    fun tabletDrawerStartsOpenAndToggleSavesTheOpposite() {
        val viewModel = viewModel()
        assertTrue(viewModel.drawerExpanded.value)
        viewModel.toggleDrawer()
        coVerify { settingsRepository.setDrawerExpanded(false) }
    }

    @Test
    fun savedDrawerStateComesBackFromSettings() {
        val viewModel = viewModel()
        settings.value = settings.value.copy(drawerExpanded = false)
        assertFalse(viewModel.drawerExpanded.value)
        viewModel.toggleDrawer()
        coVerify { settingsRepository.setDrawerExpanded(true) }
    }

    @Test
    fun failedDrawerSaveKeepsTheSavedState() {
        coEvery { settingsRepository.setDrawerExpanded(any()) } throws IllegalStateException("disk")
        val viewModel = viewModel()
        viewModel.toggleDrawer()
        assertTrue(viewModel.drawerExpanded.value)
    }
}
