package com.sam.gogotimeline.ui.board

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.gogotimeline.core.text.TaskNotes
import com.sam.gogotimeline.core.text.TaskTitle
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.data.repository.BoardRepository
import com.sam.gogotimeline.data.repository.SettingsRepository
import com.sam.gogotimeline.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Opens the most recently used board (step 7 moves this choice to a start screen). Time changes go to a draft
 * that is written only on Save; reordering is saved right away.
 */
@HiltViewModel
class BoardViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val loadRequests = MutableStateFlow(0)
    private val edit = MutableStateFlow(EditSession.restore(savedStateHandle))
    private val editStatus = MutableStateFlow(EditStatus())
    private val topRowIndex = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BoardUiState> = loadRequests
        .flatMapLatest { load() }
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

    fun onTaskTap(taskId: Long) {
        if (edit.value != null) return
        val task = ready?.tasks?.firstOrNull { it.id == taskId } ?: return
        setEdit(EditSession.from(task))
    }

    fun onEmptyTap() = requestClose()

    fun onBack() = requestClose()

    fun onTaskScheduleChange(
        taskId: Long,
        schedule: TaskSchedule,
    ) {
        val session = edit.value ?: return
        // Nothing may change the draft while it is being saved.
        if (editStatus.value.isSaving || session.draft.taskId != taskId) return
        setEdit(session.withSchedule(schedule))
    }

    fun onTitleChange(title: String) {
        val session = edit.value ?: return
        if (!editStatus.value.isSaving) setEdit(session.withTitle(TaskTitle.clean(title)))
    }

    fun onNotesChange(notes: String) {
        val session = edit.value ?: return
        if (!editStatus.value.isSaving) setEdit(session.withNotes(TaskNotes.limit(notes)))
    }

    fun onColorChange(colorIndex: Int) {
        val session = edit.value ?: return
        if (!editStatus.value.isSaving) setEdit(session.withColor(colorIndex))
    }

    /** A date or time picked in the sheet; it may be invalid and then blocks saving. */
    fun onSheetScheduleChange(schedule: TaskSchedule) {
        val session = edit.value ?: return
        if (!editStatus.value.isSaving) setEdit(session.withSheetSchedule(schedule))
    }

    fun onAllDayChange(
        checked: Boolean,
        calendar: CalendarContext,
    ) {
        val session = edit.value ?: return
        if (!editStatus.value.isSaving) setEdit(session.withAllDay(checked, calendar))
    }

    /** ✓: nothing to save just ends editing; otherwise saves. */
    fun onConfirm(calendar: CalendarContext) {
        val session = edit.value ?: return
        if (!session.isValid) return
        if (session.isDirty) {
            onSave(calendar)
        } else if (!editStatus.value.isSaving) {
            endEditing()
        }
    }

    /**
     * Saves the draft. Timed drafts become all-day when both ends sit on local midnights (spec 7: decided on Save,
     * not while picking), which needs the current [calendar].
     */
    fun onSave(calendar: CalendarContext) {
        val session = edit.value ?: return
        if (editStatus.value.isSaving || !session.isValid) return
        editStatus.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val draft = session.draft
                val schedule = (draft.schedule as? TaskSchedule.Timed)?.let { TaskSchedule.from(it.range, calendar) }
                taskRepository.saveTask(
                    draft.copy(
                        title = draft.title.trim(),
                        notes = TaskNotes.normalize(draft.notes),
                        schedule = schedule ?: draft.schedule,
                    ),
                )
                endEditing()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the draft and edit mode so the user can try again or discard.
                editStatus.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    fun onDiscard() {
        if (!editStatus.value.isSaving) endEditing()
    }

    /** Closes the dialog and goes back to editing with the draft kept. */
    fun onCancelClose() {
        if (!editStatus.value.isSaving) editStatus.value = EditStatus()
    }

    fun onTaskMove(
        taskId: Long,
        toIndex: Int,
    ) {
        val boardId = ready?.board?.id ?: return
        write { taskRepository.moveTask(boardId, taskId, toIndex) }
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

    /** Ends editing right away when nothing changed; otherwise asks Save / Discard. */
    private fun requestClose() {
        val session = edit.value ?: return
        if (editStatus.value.isSaving) return
        if (session.isDirty) editStatus.value = EditStatus(confirmingClose = true) else endEditing()
    }

    private fun endEditing() {
        setEdit(null)
        editStatus.value = EditStatus()
    }

    private fun setEdit(session: EditSession?) {
        edit.value = session
        if (session == null) EditSession.clear(savedStateHandle) else session.save(savedStateHandle)
    }

    private fun load(): Flow<BoardUiState> = flow {
        val boardId = boardRepository.getStartBoardId()
        if (boardId == null) {
            emit(BoardUiState.Error)
            return@flow
        }
        ignoreFailure { boardRepository.markOpened(boardId) }
        emitAll(
            combine(
                boardRepository.observeBoard(boardId),
                taskRepository.observeTasks(boardId),
                settingsRepository.observeSettings(),
                edit,
                editStatus,
            ) { board, tasks, settings, edit, editStatus ->
                if (board == null) BoardUiState.Error else BoardUiState.Ready(board, tasks, settings, edit, editStatus)
            },
        )
    }.onStart { emit(BoardUiState.Loading) }
        .catch { emit(BoardUiState.Error) }

    // Reorder, zoom and pane writes do not surface failures; the screen keeps showing what the database has.
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
