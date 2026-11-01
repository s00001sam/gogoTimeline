package com.sam.gogotimeline.ui.board

import androidx.lifecycle.SavedStateHandle
import com.sam.gogotimeline.core.time.DateRange
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
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
        every { observeBoard(BOARD) } returns board
    }
    private val taskRepository = mockk<TaskRepository>(relaxed = true) {
        every { observeTasks(BOARD) } returns tasks
    }
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true) {
        every { observeSettings() } returns settings
    }

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        BoardViewModel(boardRepository, taskRepository, settingsRepository, savedState).also { it.openBoard(BOARD) }

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
        viewModel.onSave(TAIPEI)
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
        viewModel.onCancelDialog()
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
        viewModel.onSave(TAIPEI)
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
        viewModel.onSave(TAIPEI)
        viewModel.onTaskScheduleChange(B, b.schedule)
        viewModel.onCancelDialog()
        viewModel.onDiscard()
        assertTrue(viewModel.ready.editStatus.isSaving)
        assertEquals(later, viewModel.ready.edit!!.draft.schedule)
        saving.complete(Unit)
        assertNull(viewModel.ready.edit)
    }

    @Test
    fun deleteAsksFirstThenDeletesWithoutAskingToSave() {
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onDeleteRequest()
        assertEquals(EditStatus(confirmingDelete = true), viewModel.ready.editStatus)
        coVerify(exactly = 0) { taskRepository.deleteTask(any(), any()) }
        viewModel.onDeleteConfirm()
        coVerify { taskRepository.deleteTask(BOARD, B) }
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
        assertNull(viewModel.ready.edit)
        assertEquals(EditStatus(), viewModel.ready.editStatus)
    }

    @Test
    fun cancellingDeleteKeepsEditingWithTheDraft() {
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onDeleteRequest()
        viewModel.onCancelDialog()
        assertEquals(EditStatus(), viewModel.ready.editStatus)
        assertEquals(later, viewModel.ready.edit!!.draft.schedule)
        coVerify(exactly = 0) { taskRepository.deleteTask(any(), any()) }
    }

    @Test
    fun deleteNeedsATaskBeingEdited() {
        val viewModel = viewModel()
        viewModel.onDeleteRequest()
        viewModel.onDeleteConfirm()
        assertEquals(EditStatus(), viewModel.ready.editStatus)
        coVerify(exactly = 0) { taskRepository.deleteTask(any(), any()) }
    }

    @Test
    fun failedDeleteKeepsEditingAndCanRetry() {
        coEvery { taskRepository.deleteTask(any(), any()) } throws IllegalStateException("disk")
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onDeleteRequest()
        viewModel.onDeleteConfirm()
        assertEquals(EditStatus(confirmingDelete = true, deleteFailed = true), viewModel.ready.editStatus)
        assertEquals(later, viewModel.ready.edit!!.draft.schedule)
        coEvery { taskRepository.deleteTask(any(), any()) } returns Unit
        viewModel.onDeleteConfirm()
        assertNull(viewModel.ready.edit)
    }

    @Test
    fun nothingChangesOrClosesWhileDeleting() {
        val deleting = CompletableDeferred<Unit>()
        coEvery { taskRepository.deleteTask(any(), any()) } coAnswers { deleting.await() }
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onDeleteRequest()
        viewModel.onDeleteConfirm()
        viewModel.onTitleChange("Renamed")
        viewModel.onTaskScheduleChange(B, b.schedule)
        viewModel.onCancelDialog()
        viewModel.onEmptyTap()
        viewModel.onSave(TAIPEI)
        viewModel.onDeleteConfirm()
        assertTrue(viewModel.ready.editStatus.isDeleting)
        assertFalse(viewModel.ready.canSave)
        assertEquals("Task $B", viewModel.ready.edit!!.draft.title)
        assertEquals(later, viewModel.ready.edit!!.draft.schedule)
        coVerify(exactly = 1) { taskRepository.deleteTask(BOARD, B) }
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
        deleting.complete(Unit)
        assertNull(viewModel.ready.edit)
    }

    @Test
    fun settingsOpensRightAwayWhenNotEditing() = runTest {
        val viewModel = viewModel()
        viewModel.onAppAction(BoardAction.OpenSettings)
        assertEquals(BoardAction.OpenSettings, viewModel.actions.first())
    }

    @Test
    fun settingsWithUnsavedChangesAsksFirstAndOpensAfterDiscard() = runTest {
        val viewModel = viewModel()
        val opened = mutableListOf<BoardAction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.actions.toList(opened) }
        viewModel.editBWithLaterTime()
        viewModel.onAppAction(BoardAction.OpenSettings)
        assertTrue(viewModel.ready.editStatus.confirmingClose)
        assertTrue(opened.isEmpty())
        viewModel.onDiscard()
        assertNull(viewModel.ready.edit)
        assertEquals(1, opened.size)
    }

    @Test
    fun switchingBoardsWaitsForUnsavedChangesToBeDiscarded() = runTest {
        val viewModel = viewModel()
        val actions = mutableListOf<BoardAction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.actions.toList(actions) }
        viewModel.editBWithLaterTime()
        viewModel.onAppAction(BoardAction.SwitchBoard(99))
        assertTrue(viewModel.ready.editStatus.confirmingClose)
        assertTrue(actions.isEmpty())
        viewModel.onDiscard()
        assertEquals(listOf(BoardAction.SwitchBoard(99)), actions)
    }

    @Test
    fun cancellingTheSettingsQuestionStaysOnTheBoard() = runTest {
        val viewModel = viewModel()
        val opened = mutableListOf<BoardAction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.actions.toList(opened) }
        viewModel.editBWithLaterTime()
        viewModel.onAppAction(BoardAction.OpenSettings)
        viewModel.onCancelDialog()
        // A later close must not open Settings either.
        viewModel.onEmptyTap()
        viewModel.onDiscard()
        assertTrue(opened.isEmpty())
        assertNull(viewModel.ready.edit)
    }

    @Test
    fun settingsWhileSavingWaitsAndUnchangedEditingJustEnds() = runTest {
        val saving = CompletableDeferred<Unit>()
        coEvery { taskRepository.saveTask(any()) } coAnswers { saving.await() }
        val viewModel = viewModel()
        val opened = mutableListOf<BoardAction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.actions.toList(opened) }
        viewModel.editBWithLaterTime()
        viewModel.onConfirm(TAIPEI)
        viewModel.onAppAction(BoardAction.OpenSettings)
        assertTrue(opened.isEmpty())
        saving.complete(Unit)
        viewModel.onTaskTap(A)
        viewModel.onAppAction(BoardAction.OpenSettings)
        assertNull(viewModel.ready.edit)
        assertEquals(1, opened.size)
    }

    @Test
    fun lockedBoardStartsNoEditingNewTaskOrReorder() {
        board.value = board.value!!.copy(locked = true)
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onNewTask(later)
        viewModel.onTaskMove(B, 0)
        assertNull(viewModel.ready.edit)
        coVerify(exactly = 0) { taskRepository.moveTask(any(), any(), any()) }
    }

    // The first-lock confirmation was already accepted, so locking does not ask.
    private fun lockConfirmedBefore() {
        settings.tryEmit(TimelineSettings.DEFAULT.copy(lockConfirmed = true))
    }

    @Test
    fun lockSavesTheOppositeStateRightAwayOnceConfirmedBefore() {
        lockConfirmedBefore()
        val viewModel = viewModel()
        viewModel.onToggleLock()
        coVerify { boardRepository.setLocked(BOARD, true) }
        assertFalse(viewModel.ready.confirmingLock)
        board.value = board.value!!.copy(locked = true)
        viewModel.onToggleLock()
        coVerify { boardRepository.setLocked(BOARD, false) }
    }

    @Test
    fun firstLockAsksAndLocksOnlyAfterLock() {
        val viewModel = viewModel()
        viewModel.onToggleLock()
        assertTrue(viewModel.ready.confirmingLock)
        coVerify(exactly = 0) { boardRepository.setLocked(any(), any()) }
        viewModel.onLockConfirm()
        assertFalse(viewModel.ready.confirmingLock)
        coVerify { boardRepository.setLocked(BOARD, true) }
        coVerify { settingsRepository.markLockConfirmed() }
    }

    @Test
    fun cancelledFirstLockStaysUnlockedAndAsksAgainNextTime() {
        val viewModel = viewModel()
        viewModel.onToggleLock()
        viewModel.onLockCancel()
        assertFalse(viewModel.ready.confirmingLock)
        // A stray confirm after cancelling (e.g. a double tap) must not lock.
        viewModel.onLockConfirm()
        viewModel.onToggleLock()
        assertTrue(viewModel.ready.confirmingLock)
        coVerify(exactly = 0) { boardRepository.setLocked(any(), any()) }
        coVerify(exactly = 0) { settingsRepository.markLockConfirmed() }
    }

    @Test
    fun unlockingNeverAsks() {
        board.value = board.value!!.copy(locked = true)
        val viewModel = viewModel()
        viewModel.onToggleLock()
        assertFalse(viewModel.ready.confirmingLock)
        coVerify { boardRepository.setLocked(BOARD, false) }
    }

    @Test
    fun lockWhileEditingWithoutChangesEndsEditingThenAsks() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onToggleLock()
        assertNull(viewModel.ready.edit)
        assertTrue(viewModel.ready.confirmingLock)
    }

    @Test
    fun unsavedChangesAreResolvedBeforeTheFirstLockQuestion() {
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onToggleLock()
        assertTrue(viewModel.ready.editStatus.confirmingClose)
        assertFalse(viewModel.ready.confirmingLock)
        viewModel.onDiscard()
        assertNull(viewModel.ready.edit)
        assertTrue(viewModel.ready.confirmingLock)
        // Cancel now leaves the board unlocked; the draft was already discarded.
        viewModel.onLockCancel()
        assertNull(viewModel.ready.edit)
        coVerify(exactly = 0) { boardRepository.setLocked(any(), any()) }
    }

    @Test
    fun switchingBoardsDropsAnOpenLockQuestion() {
        val viewModel = viewModel()
        viewModel.onToggleLock()
        every { boardRepository.observeBoard(99) } returns MutableStateFlow(Board(99, "Other", null, 0))
        every { taskRepository.observeTasks(99) } returns MutableStateFlow(emptyList())
        viewModel.openBoard(99)
        assertFalse(viewModel.ready.confirmingLock)
    }

    @Test
    fun lockWithUnsavedChangesAsksFirstAndLocksAfterSaveOrDiscard() {
        lockConfirmedBefore()
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onToggleLock()
        assertTrue(viewModel.ready.editStatus.confirmingClose)
        coVerify(exactly = 0) { boardRepository.setLocked(any(), any()) }
        viewModel.onSave(TAIPEI)
        coVerify { taskRepository.saveTask(any()) }
        coVerify(exactly = 1) { boardRepository.setLocked(BOARD, true) }

        board.value = board.value!!.copy(locked = false)
        viewModel.editBWithLaterTime()
        viewModel.onToggleLock()
        viewModel.onDiscard()
        coVerify(exactly = 2) { boardRepository.setLocked(BOARD, true) }
    }

    @Test
    fun cancellingTheLockQuestionKeepsEditingAndNeverLocks() {
        lockConfirmedBefore()
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onToggleLock()
        viewModel.onCancelDialog()
        assertEquals(B, viewModel.ready.selectedTaskId)
        // A later close must not lock either.
        viewModel.onEmptyTap()
        viewModel.onDiscard()
        coVerify(exactly = 0) { boardRepository.setLocked(any(), any()) }
    }

    @Test
    fun failedSaveFromTheLockQuestionKeepsEditingAndDoesNotLock() {
        coEvery { taskRepository.saveTask(any()) } throws IllegalStateException("disk full")
        lockConfirmedBefore()
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onToggleLock()
        viewModel.onSave(TAIPEI)
        assertTrue(viewModel.ready.editStatus.saveFailed)
        assertEquals(B, viewModel.ready.selectedTaskId)
        coVerify(exactly = 0) { boardRepository.setLocked(any(), any()) }
    }

    @Test
    fun newTaskStartsAnUnsavedDraftWithARandomColor() {
        val viewModel = viewModel()
        viewModel.onNewTask(later)
        val draft = viewModel.ready.edit!!.draft
        assertTrue(draft.isNew)
        assertEquals(later, draft.schedule)
        assertTrue(draft.colorIndex in 0 until 7)
        coVerify(exactly = 0) { taskRepository.createTask(any(), any(), any(), any(), any()) }
    }

    @Test
    fun untouchedNewTaskJustGoesAway() {
        val viewModel = viewModel()
        viewModel.onNewTask(later)
        viewModel.onEmptyTap()
        assertNull(viewModel.ready.edit)
        assertFalse(viewModel.ready.editStatus.confirmingClose)
    }

    @Test
    fun savingANewTaskCreatesItAfterTheLastRow() {
        val created = task(D, 3)
        coEvery { taskRepository.createTask(any(), any(), any(), any(), any()) } coAnswers {
            tasks.value += created
            D
        }
        val viewModel = viewModel()
        viewModel.onNewTask(later)
        val color = viewModel.ready.edit!!.draft.colorIndex
        viewModel.onTitleChange("  Design review  ")
        viewModel.onConfirm(TAIPEI)
        coVerify { taskRepository.createTask(BOARD, "Design review", later, color, "") }
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
        assertNull(viewModel.ready.edit)
    }

    @Test
    fun newTaskEditingEndsOnlyOnceTheSavedRowArrives() {
        coEvery { taskRepository.createTask(any(), any(), any(), any(), any()) } returns D
        val viewModel = viewModel()
        viewModel.onNewTask(later)
        viewModel.onTitleChange("Design review")
        viewModel.onConfirm(TAIPEI)
        assertTrue(viewModel.ready.editStatus.isSaving)
        tasks.value += task(D, 3)
        assertNull(viewModel.ready.edit)
    }

    @Test
    fun newTaskHasNoDeleteAndItsRowCannotMove() {
        val viewModel = viewModel()
        viewModel.onNewTask(later)
        viewModel.onDeleteRequest()
        viewModel.onTaskMove(TaskEdit.NEW_TASK_ID, 0)
        assertFalse(viewModel.ready.editStatus.confirmingDelete)
        coVerify(exactly = 0) { taskRepository.moveTask(any(), any(), any()) }
    }

    @Test
    fun newTaskWaitsWhileAnotherTaskIsEdited() {
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onNewTask(later)
        assertEquals(B, viewModel.ready.selectedTaskId)
    }

    @Test
    fun failedCreateKeepsTheNewDraft() {
        coEvery { taskRepository.createTask(any(), any(), any(), any(), any()) } throws IllegalStateException("disk")
        val viewModel = viewModel()
        viewModel.onNewTask(later)
        viewModel.onTitleChange("Design review")
        viewModel.onConfirm(TAIPEI)
        assertTrue(viewModel.ready.editStatus.saveFailed)
        assertTrue(viewModel.ready.edit!!.draft.isNew)
        assertEquals("Design review", viewModel.ready.edit!!.draft.title)
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
        every { boardRepository.observeBoard(BOARD) } throws IllegalStateException("disk") andThen board
        val viewModel = viewModel()
        assertEquals(BoardUiState.Error, viewModel.uiState.value)
        viewModel.retry()
        assertTrue(viewModel.uiState.value is BoardUiState.Ready)
    }

    @Test
    fun noBoardIsAnError() {
        board.value = null
        assertEquals(BoardUiState.Error, viewModel().uiState.value)
    }

    @Test
    fun selectionSurvivesNewTaskData() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        tasks.value = listOf(b.copy(sortOrder = 0), a.copy(sortOrder = 1), c)
        assertEquals(B, viewModel.ready.selectedTaskId)
    }

    @Test
    fun titleChangesOnlyTheDraft() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onTitleChange("Renamed")
        assertEquals("Renamed", viewModel.ready.edit!!.draft.title)
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
    }

    @Test
    fun confirmWithoutChangesJustEndsEditing() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onConfirm(TAIPEI)
        assertNull(viewModel.ready.edit)
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
    }

    @Test
    fun confirmSavesTheTrimmedName() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onTitleChange("  Renamed  ")
        viewModel.onConfirm(TAIPEI)
        coVerify { taskRepository.saveTask(TaskEdit(B, "Renamed", b.schedule, colorIndex = 1)) }
        assertNull(viewModel.ready.edit)
    }

    @Test
    fun blankNameCannotBeSaved() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onTitleChange("   ")
        assertFalse(viewModel.ready.canSave)
        viewModel.onConfirm(TAIPEI)
        viewModel.onSave(TAIPEI)
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
        assertEquals(B, viewModel.ready.selectedTaskId)
    }

    @Test
    fun nameCannotChangeWhileSaving() {
        val saving = CompletableDeferred<Unit>()
        coEvery { taskRepository.saveTask(any()) } coAnswers { saving.await() }
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onConfirm(TAIPEI)
        viewModel.onTitleChange("Too late")
        assertEquals("Task $B", viewModel.ready.edit!!.draft.title)
        assertFalse(viewModel.ready.canSave)
        saving.complete(Unit)
    }

    @Test
    fun colorChangesOnlyTheDraft() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onColorChange(6)
        assertEquals(6, viewModel.ready.edit!!.draft.colorIndex)
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
    }

    @Test
    fun newColorCountsAsAChange() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onColorChange(6)
        viewModel.onEmptyTap()
        assertTrue(viewModel.ready.editStatus.confirmingClose)
    }

    @Test
    fun pastedLineBreaksBecomeSpacesAndColorWaitsWhileSaving() {
        val saving = CompletableDeferred<Unit>()
        coEvery { taskRepository.saveTask(any()) } coAnswers { saving.await() }
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onTitleChange("Plan\nrelease")
        assertEquals("Plan release", viewModel.ready.edit!!.draft.title)
        viewModel.onConfirm(TAIPEI)
        viewModel.onColorChange(6)
        assertEquals(1, viewModel.ready.edit!!.draft.colorIndex)
        saving.complete(Unit)
    }

    @Test
    fun notesChangeOnlyTheDraftAndCountAsAChange() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onNotesChange("Bring laptops")
        assertEquals("Bring laptops", viewModel.ready.edit!!.draft.notes)
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
        viewModel.onEmptyTap()
        assertTrue(viewModel.ready.editStatus.confirmingClose)
    }

    @Test
    fun confirmSavesNotesWithoutTrailingBlanks() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onNotesChange("Bring laptops\n\n")
        viewModel.onConfirm(TAIPEI)
        coVerify {
            taskRepository.saveTask(TaskEdit(B, "Task $B", b.schedule, colorIndex = 1, notes = "Bring laptops"))
        }
    }

    @Test
    fun invalidTimesCannotBeSaved() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onSheetScheduleChange(
            TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T13:00"), TAIPEI.at("2026-10-31T10:30"))),
        )
        assertFalse(viewModel.ready.canSave)
        viewModel.onConfirm(TAIPEI)
        coVerify(exactly = 0) { taskRepository.saveTask(any()) }
    }

    @Test
    fun midnightToMidnightIsSavedAsAllDay() {
        val viewModel = viewModel()
        viewModel.onTaskTap(B)
        viewModel.onSheetScheduleChange(
            TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T00:00"), TAIPEI.at("2026-11-02T00:00"))),
        )
        viewModel.onConfirm(TAIPEI)
        val allDay = TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 2)))
        coVerify { taskRepository.saveTask(TaskEdit(B, "Task $B", allDay, colorIndex = 1)) }
    }

    @Test
    fun timesAndAllDayWaitWhileSaving() {
        val saving = CompletableDeferred<Unit>()
        coEvery { taskRepository.saveTask(any()) } coAnswers { saving.await() }
        val viewModel = viewModel()
        viewModel.editBWithLaterTime()
        viewModel.onConfirm(TAIPEI)
        viewModel.onSheetScheduleChange(b.schedule)
        viewModel.onAllDayChange(true, TAIPEI)
        assertEquals(later, viewModel.ready.edit!!.draft.schedule)
        saving.complete(Unit)
    }

    private companion object {
        const val BOARD = 1L
        const val A = 10L
        const val B = 11L
        const val C = 12L
        const val D = 13L
    }
}
