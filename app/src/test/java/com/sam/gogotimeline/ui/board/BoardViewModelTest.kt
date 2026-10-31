package com.sam.gogotimeline.ui.board

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private val later = TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T13:00"), TAIPEI.at("2026-10-31T14:30")))

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

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        BoardViewModel(boardRepository, taskRepository, settingsRepository, savedState)

    private val BoardViewModel.ready get() = uiState.value as BoardUiState.Ready

    private fun BoardViewModel.editBWithLaterTime() {
        onTaskTap(B)
        onTaskScheduleChange(B, later)
    }

    @Test
    fun boardTasksAndSettingsMakeReady() {
        val viewModel = viewModel()
        assertEquals(
            BoardUiState.Ready(board.value!!, listOf(a, b, c), TimelineSettings.DEFAULT, edit = null),
            viewModel.uiState.value,
        )
        coVerify { boardRepository.markOpened(BOARD) }
    }

    @Test
    fun tapStartsEditingTheTask() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        assertEquals(B, viewModel.ready.selectedTaskId)
        assertFalse(viewModel.ready.edit!!.isDirty)
    }

    @Test
    fun dragChangesOnlyTheDraft() {
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        assertEquals(later, viewModel.ready.edit!!.draft.schedule)
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
    }

    @Test
    fun closingWithChangesAsksSaveOrDiscard() {
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onEmptyTap()
        assertTrue(viewModel.ready.editStatus.confirmingClose)
        assertEquals(B, viewModel.ready.selectedTaskId)
    }

    @Test
    fun saveWritesTheDraftAndEndsEditing() {
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onEmptyTap()
        viewModel.onSave()
        coVerify { taskRepository.saveTask(TaskEdit(B, "Task $B", later, colorIndex = 1)) }
        assertNull(viewModel.ready.edit)
        assertEquals(EditStatus(), viewModel.ready.editStatus)
    }

    @Test
    fun settledScrollSavesTheTopTaskAsAnchor() {
        val viewModel = viewModel()
        viewModel.onVerticalScrollSettled(2)
        coVerify { boardRepository.saveAnchor(BOARD, C) }
    }

    @Test
    fun reorderZoomAndLeftPaneAreSavedRightAway() {
        val viewModel = viewModel()
        viewModel.onTaskMove(A, 2)
        viewModel.onZoomChange(ZoomLevel(TimeUnit.WEEK, 90f))
        viewModel.onToggleLeftPane()
        coVerify { taskRepository.moveTask(BOARD, A, 2) }
        coVerify { settingsRepository.setZoom(ZoomLevel(TimeUnit.WEEK, 90f)) }
        coVerify { settingsRepository.setLeftPaneExpanded(true) }
    }

    @Test
    fun closingWithoutChangesEndsRightAway() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onEmptyTap()
        assertNull(viewModel.ready.edit)
        assertFalse(viewModel.ready.editStatus.confirmingClose)
    }

    @Test
    fun draggingBackCountsAsNoChange() {
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onTaskScheduleChange(B, b.schedule)
        viewModel.onBack()
        assertNull(viewModel.ready.edit)
    }

    @Test
    fun cancelKeepsEditingWithTheDraft() {
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onEmptyTap()
        viewModel.onCancelClose()
        assertFalse(viewModel.ready.editStatus.confirmingClose)
        assertEquals(later, viewModel.ready.edit!!.draft.schedule)
    }

    @Test
    fun discardDropsTheDraft() {
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onEmptyTap()
        viewModel.onDiscard()
        assertNull(viewModel.ready.edit)
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
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
    fun failedSaveKeepsTheDraftAndShowsTheError() {
        coEvery { taskRepository.saveTask(any()) } throws IllegalStateException("disk")
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onEmptyTap()
        viewModel.onSave()
        assertEquals(
            EditStatus(confirmingClose = true, isSaving = false, saveFailed = true),
            viewModel.ready.editStatus,
        )
        assertEquals(later, viewModel.ready.edit!!.draft.schedule)
    }

    @Test
    fun nothingChangesTheDraftWhileSaving() {
        val saving = CompletableDeferred<Unit>()
        coEvery { taskRepository.saveTask(any()) } coAnswers { saving.await() }
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onEmptyTap()
        viewModel.onSave()
        viewModel.onTaskScheduleChange(B, b.schedule)
        viewModel.onCancelClose()
        viewModel.onDiscard()
        assertTrue(viewModel.ready.editStatus.isSaving)
        assertEquals(later, viewModel.ready.edit!!.draft.schedule)
        saving.complete(Unit)
        assertNull(viewModel.ready.edit)
    }

    @Test
    fun draftSurvivesRecreation() {
        val handle = SavedStateHandle()
        viewModel(handle).editBWithLaterTime()
        val recreated = viewModel(handle)
        assertEquals(B, recreated.ready.selectedTaskId)
        assertEquals(later, recreated.ready.edit!!.draft.schedule)
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
