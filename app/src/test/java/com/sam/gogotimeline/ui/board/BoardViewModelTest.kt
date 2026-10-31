package com.sam.gogotimeline.ui.board

import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.core.time.at
import com.sam.gogotimeline.data.model.Board
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
import com.sam.gogotimeline.data.model.TimelineSettings
import com.sam.gogotimeline.data.repository.BoardRepository
import com.sam.gogotimeline.data.repository.SettingsRepository
import com.sam.gogotimeline.data.repository.TaskRepository
import com.sam.gogotimeline.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BoardViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private fun task(
        id: Long,
        order: Int,
    ) = Task(
        id = id,
        boardId = BOARD,
        title = "Task $id",
        schedule = TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T09:00"), TAIPEI.at("2026-10-31T10:30"))),
        colorIndex = order,
        sortOrder = order,
        createdAt = TAIPEI.at("2026-10-16T09:00"),
        updatedAt = TAIPEI.at("2026-10-20T09:00"),
    )

    private val a = task(A, 0)
    private val b = task(B, 1)
    private val c = task(C, 2)

    private val board = MutableStateFlow<Board?>(Board(BOARD, "Sample Project", anchorTaskId = A, lastOpenedAt = 0))
    private val tasks = MutableStateFlow(listOf(a, b, c))
    private val settings = MutableSharedFlow<TimelineSettings>(replay = 1).apply { tryEmit(TimelineSettings.DEFAULT) }

    private val boardRepository = mockk<BoardRepository>(relaxed = true) {
        coEvery { getStartBoardId() } returns BOARD
        every { observeBoard(BOARD) } returns board
    }
    private val taskRepository = mockk<TaskRepository>(relaxed = true) {
        every { observeTasks(BOARD) } returns tasks
    }
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true) {
        every { observeSettings() } returns settings
    }

    private fun viewModel() = BoardViewModel(boardRepository, taskRepository, settingsRepository)

    private val BoardViewModel.ready get() = uiState.value as BoardUiState.Ready

    @Test
    fun boardTasksAndSettingsMakeReady() {
        val viewModel = viewModel()
        assertEquals(
            BoardUiState.Ready(board.value!!, listOf(a, b, c), TimelineSettings.DEFAULT, selectedTaskId = null),
            viewModel.uiState.value,
        )
        coVerify { boardRepository.markOpened(BOARD) }
    }

    @Test
    fun dragKeepsTitleAndColorAndSavesTheNewTime() {
        val viewModel = viewModel()
        val later = TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T13:00"), TAIPEI.at("2026-10-31T14:30")))
        viewModel.onTaskScheduleChange(B, later)
        coVerify { taskRepository.saveTask(TaskEdit(B, "Task $B", later, colorIndex = 1)) }
    }

    @Test
    fun settledScrollSavesTheTopTaskAsAnchor() {
        val viewModel = viewModel()
        viewModel.onVerticalScrollSettled(2)
        coVerify { boardRepository.saveAnchor(BOARD, C) }
    }

    @Test
    fun tapSelectsAndEmptyTapDeselects() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        assertEquals(B, viewModel.ready.selectedTaskId)
        viewModel.onEmptyTap()
        assertEquals(null, viewModel.ready.selectedTaskId)
    }

    @Test
    fun zoomAndLeftPaneAreSaved() {
        val viewModel = viewModel()
        viewModel.onZoomChange(ZoomLevel(TimeUnit.WEEK, 90f))
        viewModel.onToggleLeftPane()
        coVerify { settingsRepository.setZoom(ZoomLevel(TimeUnit.WEEK, 90f)) }
        coVerify { settingsRepository.setLeftPaneExpanded(true) }
    }

    @Test
    fun staysLoadingUntilSettingsArrive() {
        val lateSettings = MutableSharedFlow<TimelineSettings>(replay = 1)
        every { settingsRepository.observeSettings() } returns lateSettings
        val viewModel = viewModel()
        assertEquals(BoardUiState.Loading, viewModel.uiState.value)
        lateSettings.tryEmit(TimelineSettings.DEFAULT)
        assertTrue(viewModel.uiState.value is BoardUiState.Ready)
    }

    @Test
    fun sameTopTaskDoesNotRewriteTheAnchor() {
        val viewModel = viewModel()
        viewModel.onVerticalScrollSettled(0)
        coVerify(exactly = 0) { boardRepository.saveAnchor(any(), any()) }
    }

    @Test
    fun reorderRechecksTheTopRow() {
        val viewModel = viewModel()
        viewModel.onVerticalScrollSettled(0)
        tasks.value = listOf(b.copy(sortOrder = 0), a.copy(sortOrder = 1), c)
        coVerify { boardRepository.saveAnchor(BOARD, B) }
    }

    @Test
    fun failedLoadShowsErrorAndRetryRecovers() {
        coEvery { boardRepository.getStartBoardId() } throws IllegalStateException("disk") andThen BOARD
        val viewModel = viewModel()
        assertEquals(BoardUiState.Error, viewModel.uiState.value)
        viewModel.retry()
        assertTrue(viewModel.uiState.value is BoardUiState.Ready)
    }

    @Test
    fun noBoardIsAnError() {
        coEvery { boardRepository.getStartBoardId() } returns null
        assertEquals(BoardUiState.Error, viewModel().uiState.value)
    }

    @Test
    fun failedSaveKeepsTheScreenReady() {
        coEvery { taskRepository.saveTask(any()) } throws IllegalStateException("disk")
        val viewModel = viewModel()
        viewModel.onTaskScheduleChange(A, a.schedule)
        assertTrue(viewModel.uiState.value is BoardUiState.Ready)
    }

    @Test
    fun selectionSurvivesNewTaskData() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        tasks.value = listOf(b.copy(sortOrder = 0), a.copy(sortOrder = 1), c)
        assertEquals(B, viewModel.ready.selectedTaskId)
    }

    private companion object {
        const val BOARD = 1L
        const val A = 10L
        const val B = 11L
        const val C = 12L
    }
}
