package com.sam.gogotimeline.ui.navigation

import com.sam.gogotimeline.data.model.Board
import com.sam.gogotimeline.data.repository.BoardRepository
import com.sam.gogotimeline.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val boards = MutableStateFlow<List<Board>>(emptyList())
    private val repository = mockk<BoardRepository>(relaxed = true) { every { observeBoards() } returns boards }

    @Test
    fun exposesTheRepositorysOrderedBoardList() {
        val viewModel = AppViewModel(repository)
        val recent = Board(2, "Recent", null, 20)
        val older = Board(1, "Older", null, 10)
        boards.value = listOf(recent, older)
        assertEquals(listOf(recent, older), viewModel.boards.value)
    }

    @Test
    fun deletingReturnsTheNextMostRecentBoard() = kotlinx.coroutines.test.runTest {
        val current = Board(2, "Current", null, 20)
        val next = Board(1, "Next", null, 10)
        boards.value = listOf(current, next)
        coEvery { repository.getStartBoardId() } returns next.id
        val viewModel = AppViewModel(repository)
        viewModel.requestDelete(current.id)
        viewModel.confirmDelete()
        assertEquals(BoardDeleted(current.id, next.id), viewModel.boardDeleted.first())
        coVerify { repository.delete(current.id) }
    }
}
