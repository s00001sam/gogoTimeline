package com.sam.gogotimeline.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.data.model.TaskEdit
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
 * Opens the most recently used board (step 7 moves this choice to a start screen). Drag results are written
 * straight to the database for now; step 5 turns them into drafts saved on ✓.
 */
@HiltViewModel
class BoardViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val loadRequests = MutableStateFlow(0)
    private val selectedTaskId = MutableStateFlow<Long?>(null)
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
        selectedTaskId.value = taskId
    }

    fun onEmptyTap() {
        selectedTaskId.value = null
    }

    fun onTaskScheduleChange(
        taskId: Long,
        schedule: TaskSchedule,
    ) {
        val task = ready?.tasks?.firstOrNull { it.id == taskId } ?: return
        write { taskRepository.saveTask(TaskEdit(task.id, task.title, schedule, task.colorIndex)) }
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
                selectedTaskId,
            ) { board, tasks, settings, selectedTaskId ->
                if (board == null) BoardUiState.Error else BoardUiState.Ready(board, tasks, settings, selectedTaskId)
            },
        )
    }.onStart { emit(BoardUiState.Loading) }
        .catch { emit(BoardUiState.Error) }

    // Step 4 does not surface write failures yet; the screen keeps showing what the database has.
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
