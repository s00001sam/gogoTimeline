package com.sam.gogotimeline.ui.board

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.text.TaskNotes
import com.sam.gogotimeline.core.text.TaskTitle
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
import com.sam.gogotimeline.data.model.TaskSortMode
import com.sam.gogotimeline.data.repository.BoardRepository
import com.sam.gogotimeline.data.repository.SettingsRepository
import com.sam.gogotimeline.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads the board selected by the app layer. Time changes go to a draft that is written only on Save; reordering is
 * saved right away.
 */
@HiltViewModel
class BoardViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val loadRequests = MutableStateFlow(0)
    private val boardId = MutableStateFlow<Long?>(null)
    private val edit = MutableStateFlow(EditSession.restore(savedStateHandle))
    private val editStatus = MutableStateFlow(EditStatus())
    private val confirmingLock = MutableStateFlow(false)
    private val viewingTaskId = MutableStateFlow<Long?>(null)
    private val topRowIndex = MutableStateFlow<Int?>(null)
    private val actionRequests = Channel<BoardAction>(Channel.CONFLATED)
    private val revealRequests = Channel<Long>(Channel.CONFLATED)

    // Runs once editing has ended through Save, Discard or a clean close; Cancel in the dialog drops it.
    private var afterEditing: (() -> Unit)? = null

    /** App-level action to perform after this board has safely finished editing. */
    val actions: Flow<BoardAction> = actionRequests.receiveAsFlow()

    /** A task the screen should scroll into view: one just saved on a sorted board, which may have moved. */
    val reveals: Flow<Long> = revealRequests.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BoardUiState> = combine(boardId.filterNotNull(), loadRequests) { id, _ -> id }
        .flatMapLatest(::load)
        .stateIn(viewModelScope, SharingStarted.Eagerly, BoardUiState.Loading)

    private val ready: BoardUiState.Ready? get() = uiState.value as? BoardUiState.Ready

    init {
        // Re-checks when the top row index or the task order changes, so a reorder updates the anchor too.
        viewModelScope.launch {
            combine(uiState.filterIsInstance<BoardUiState.Ready>(), topRowIndex.filterNotNull()) { state, index ->
                state.board to state.tasks.getOrNull(index)?.id
            }.collect { (board, taskId) ->
                if (taskId != null && taskId != board.anchorTaskId) {
                    ignoreFailure { boardRepository.saveAnchor(board.id, taskId) }
                }
            }
        }
    }

    /** Called by the route host; the id is data, while navigation stays outside this ViewModel. */
    fun openBoard(id: Long) {
        if (boardId.value == id) return
        boardId.value = id
        confirmingLock.value = false
        viewingTaskId.value = null
    }

    /** A tap on a task starts editing it; on a locked board it shows the task read only instead (spec 9). */
    fun onTaskTap(taskId: Long) {
        val state = ready ?: return
        if (edit.value != null) return
        if (state.board.locked) {
            viewingTaskId.value = taskId
            return
        }
        val row = state.tasks.indexOfFirst { it.id == taskId }
        if (row < 0) return
        setEdit(EditSession.from(state.tasks[row], row))
    }

    /** + or the empty state's New task: a draft with [schedule] and a random color, not saved until ✓. */
    fun onNewTask(schedule: TaskSchedule) {
        val state = ready ?: return
        if (edit.value != null || state.board.locked) return
        setEdit(EditSession.newTask(schedule, Random.nextInt(LayoutConst.TASK_COLOR_COUNT), state.tasks.size))
    }

    /** Leaving the board first resolves an unsaved task draft; the app layer performs [action]. */
    fun onAppAction(action: BoardAction) = runAfterEditing { actionRequests.trySend(action) }

    /** Shared by all boards; while editing, the draft is resolved first like leaving the board (spec 9). */
    fun onTaskSortModeChange(mode: TaskSortMode) {
        if (mode == ready?.settings?.taskSortMode) return
        runAfterEditing { write { settingsRepository.setTaskSortMode(mode) } }
    }

    /**
     * Unlocks right away; locks right away once the first-lock confirmation was accepted, otherwise asks it first.
     * While editing, the draft is resolved first like leaving the board, and only then may it ask (spec 9).
     */
    fun onToggleLock() = runAfterEditing {
        val state = ready ?: return@runAfterEditing
        when {
            state.board.locked -> {
                viewingTaskId.value = null
                write { boardRepository.setLocked(state.board.id, false) }
            }

            state.settings.lockConfirmed -> write { boardRepository.setLocked(state.board.id, true) }

            else -> confirmingLock.value = true
        }
    }

    /** Lock in the first-lock confirmation: locks, and never asks again in this app. */
    fun onLockConfirm() {
        if (!confirmingLock.value) return
        confirmingLock.value = false
        val boardId = ready?.board?.id ?: return
        write {
            boardRepository.setLocked(boardId, true)
            settingsRepository.markLockConfirmed()
        }
    }

    /** Cancel, outside or back: stays unlocked, and the next lock asks again. */
    fun onLockCancel() {
        confirmingLock.value = false
    }

    fun onEmptyTap() = requestClose()

    fun onBack() = requestClose()

    fun onTaskScheduleChange(
        taskId: Long,
        schedule: TaskSchedule,
    ) {
        val session = edit.value ?: return
        if (editStatus.value.isBusy || session.draft.taskId != taskId) return
        setEdit(session.withSchedule(schedule))
    }

    fun onTitleChange(title: String) {
        val session = edit.value ?: return
        if (!editStatus.value.isBusy) setEdit(session.withTitle(TaskTitle.clean(title)))
    }

    fun onNotesChange(notes: String) {
        val session = edit.value ?: return
        if (!editStatus.value.isBusy) setEdit(session.withNotes(TaskNotes.limit(notes)))
    }

    fun onColorChange(colorIndex: Int) {
        val session = edit.value ?: return
        if (!editStatus.value.isBusy) setEdit(session.withColor(colorIndex))
    }

    /** A date or time picked in the sheet; it may be invalid and then blocks saving. */
    fun onSheetScheduleChange(schedule: TaskSchedule) {
        val session = edit.value ?: return
        if (!editStatus.value.isBusy) setEdit(session.withSheetSchedule(schedule))
    }

    fun onAllDayChange(
        checked: Boolean,
        calendar: CalendarContext,
    ) {
        val session = edit.value ?: return
        if (!editStatus.value.isBusy) setEdit(session.withAllDay(checked, calendar))
    }

    /** ✓: nothing to save just ends editing; otherwise saves. */
    fun onConfirm(calendar: CalendarContext) {
        val session = edit.value ?: return
        if (!session.isValid) return
        if (session.isDirty) {
            onSave(calendar)
        } else if (!editStatus.value.isBusy) {
            endEditing()
        }
    }

    /**
     * Saves the draft. Timed drafts become all-day when both ends sit on local midnights (spec 7: decided on Save,
     * not while picking), which needs the current [calendar].
     */
    fun onSave(calendar: CalendarContext) {
        val session = edit.value ?: return
        if (editStatus.value.isBusy || !session.isValid) return
        editStatus.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val draft = session.draft
                val schedule = (draft.schedule as? TaskSchedule.Timed)?.let { TaskSchedule.from(it.range, calendar) }
                val toSave = draft.copy(
                    title = draft.title.trim(),
                    notes = TaskNotes.normalize(draft.notes),
                    schedule = schedule ?: draft.schedule,
                )
                val sorted = ready?.settings?.taskSortMode != TaskSortMode.MANUAL
                val id = if (toSave.isNew) createTask(toSave, session.draftRow) else saveTask(toSave, session)
                // Keep the draft over the database row until every saved field has come back from Room. Otherwise a
                // content-only edit can briefly show the old title/color/notes after the draft is removed.
                awaitSavedTask(id, toSave, expectedRow = session.draftRow.takeUnless { sorted })
                endEditing()
                if (sorted) revealRequests.trySend(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the draft and edit mode so the user can try again or discard.
                editStatus.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    fun onDiscard() {
        if (!editStatus.value.isBusy) endEditing()
    }

    /** Closes the Save / Discard or Delete dialog and goes back to editing with the draft kept. */
    fun onCancelDialog() {
        if (editStatus.value.isBusy) return
        afterEditing = null
        editStatus.value = EditStatus()
    }

    /** The sheet's Delete: asks first. A new task has no Delete; it is discarded instead. */
    fun onDeleteRequest() {
        val session = edit.value ?: return
        if (!session.draft.isNew && !editStatus.value.isBusy) editStatus.value = EditStatus(confirmingDelete = true)
    }

    /** Deletes the task; editing ends only when it worked, and the draft is dropped without asking. */
    fun onDeleteConfirm() {
        val session = edit.value ?: return
        val boardId = ready?.board?.id ?: return
        if (editStatus.value.isBusy) return
        editStatus.update { it.copy(isDeleting = true, deleteFailed = false) }
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(boardId, session.draft.taskId)
                endEditing()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                editStatus.update { it.copy(isDeleting = false, deleteFailed = true) }
            }
        }
    }

    fun onTaskMove(
        taskId: Long,
        toIndex: Int,
    ) {
        val state = ready ?: return
        val board = state.board
        // Only the manual order can be changed by hand (spec 9).
        if (board.locked || state.settings.taskSortMode != TaskSortMode.MANUAL) return
        val session = edit.value
        when {
            // Not editing: the new order is saved right away.
            session == null -> write { taskRepository.moveTask(board.id, taskId, toIndex) }

            // The task being edited: its new row is part of the draft, saved with ✓ together with its content, so
            // a draft order and a saved order never disagree (spec 6.5).
            session.draft.taskId == taskId && !editStatus.value.isBusy -> setEdit(session.withRow(toIndex))

            // Other rows stay put while a task is edited.
            else -> Unit
        }
    }

    fun onZoomChange(zoom: ZoomLevel) {
        write { settingsRepository.setZoom(zoom) }
    }

    fun onToggleLeftPane() {
        val expanded = ready?.settings?.leftPaneExpanded ?: return
        write { settingsRepository.setLeftPaneExpanded(!expanded) }
    }

    fun onVerticalScrollSettled(topRowIndex: Int) {
        this.topRowIndex.value = topRowIndex
    }

    fun retry() {
        loadRequests.update { it + 1 }
    }

    /**
     * Runs [block] now when not editing; after a clean close when nothing changed; or after Save / Discard, which are
     * asked first. Nothing happens while saving or deleting.
     */
    private fun runAfterEditing(block: () -> Unit) {
        val session = edit.value
        when {
            session == null -> block()

            editStatus.value.isBusy -> Unit

            session.isDirty -> {
                afterEditing = block
                editStatus.value = EditStatus(confirmingClose = true)
            }

            else -> {
                endEditing()
                block()
            }
        }
    }

    /** Closes the read-only sheet; ends editing right away when nothing changed, otherwise asks Save / Discard. */
    private fun requestClose() {
        viewingTaskId.value = null
        val session = edit.value ?: return
        if (editStatus.value.isBusy) return
        if (session.isDirty) editStatus.value = EditStatus(confirmingClose = true) else endEditing()
    }

    /** Adds the task at the draft's row of the manual order; returns its id. */
    private suspend fun createTask(
        edit: TaskEdit,
        row: Int,
    ): Long {
        val boardId = ready?.board?.id ?: error("No board")
        return taskRepository.createTask(boardId, edit.title, edit.schedule, edit.colorIndex, edit.notes, row)
    }

    /** Saves the content and, when the row changed, the new row too, in one transaction; returns the task's id. */
    private suspend fun saveTask(
        edit: TaskEdit,
        session: EditSession,
    ): Long {
        val moved = session.draftRow != session.originalRow
        taskRepository.saveTask(edit, toIndex = if (moved) session.draftRow else null)
        return edit.taskId
    }

    /** Waits for Room to return the full saved content and, in manual order, the saved row. */
    private suspend fun awaitSavedTask(
        taskId: Long,
        saved: TaskEdit,
        expectedRow: Int?,
    ) = awaitTasks { tasks ->
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index < 0) return@awaitTasks false
        val task = tasks[index]
        val contentMatches = task.title == saved.title &&
            task.notes == saved.notes &&
            task.colorIndex == saved.colorIndex &&
            task.schedule == saved.schedule
        val rowMatches = expectedRow == null || index == expectedRow.coerceIn(0, tasks.lastIndex)
        contentMatches && rowMatches
    }

    private suspend fun awaitTasks(matches: (List<Task>) -> Boolean) {
        uiState.first { state -> (state as? BoardUiState.Ready)?.tasks?.let(matches) == true }
    }

    private fun endEditing() {
        setEdit(null)
        editStatus.value = EditStatus()
        val block = afterEditing
        afterEditing = null
        block?.invoke()
    }

    private fun setEdit(session: EditSession?) {
        edit.value = session
        if (session == null) EditSession.clear(savedStateHandle) else session.save(savedStateHandle)
    }

    private fun load(boardId: Long): Flow<BoardUiState> = flow {
        ignoreFailure { boardRepository.markOpened(boardId) }
        emitAll(
            combine(
                boardRepository.observeBoard(boardId),
                taskRepository.observeTasks(boardId),
                settingsRepository.observeSettings(),
                combine(edit, editStatus, ::Pair),
                combine(confirmingLock, viewingTaskId, ::Pair),
            ) { board, tasks, settings, (edit, editStatus), (confirmingLock, viewingTaskId) ->
                if (board == null) {
                    BoardUiState.Error
                } else {
                    val shown = tasks.sortedFor(settings.taskSortMode, sortCalendar())
                    BoardUiState.Ready(board, shown, settings, edit, editStatus, confirmingLock, viewingTaskId)
                }
            },
        )
    }.onStart { emit(BoardUiState.Loading) }
        .catch { emit(BoardUiState.Error) }

    // Reorder, zoom, pane and lock writes do not surface failures; the screen keeps showing what the database has.
    // Start times and lengths need only the zone; the week start does not matter for sorting.
    private fun sortCalendar() = CalendarContext(ZoneId.systemDefault(), DayOfWeek.MONDAY)

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { ignoreFailure(block) }
    }

    private suspend fun ignoreFailure(block: suspend () -> Unit) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
        }
    }
}

sealed interface BoardAction {
    data object OpenSettings : BoardAction

    data object CreateBoard : BoardAction

    data object RenameBoard : BoardAction

    data object DeleteBoard : BoardAction

    data class SwitchBoard(val boardId: Long) : BoardAction
}
