package com.sam.gogotimeline.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.gogotimeline.data.model.Board
import com.sam.gogotimeline.data.model.TimelineSettings
import com.sam.gogotimeline.data.repository.BoardRepository
import com.sam.gogotimeline.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The Delete board dialog for [board]. */
data class DeleteBoardState(
    val board: Board,
    val isDeleting: Boolean = false,
    val deleteFailed: Boolean = false,
)

/** [nextBoardId] is the most recently opened board left, or null when none is. */
data class BoardDeleted(
    val boardId: Long,
    val nextBoardId: Long?,
)

/**
 * App-level data: the board list (most recently opened first) decides the first route and feeds the drawer; deleting
 * a board lives here because it can target any board, not just the one on screen.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    /** Null until the first read. */
    val boards: StateFlow<List<Board>?> = boardRepository.observeBoards()
        .stateIn<List<Board>?>(viewModelScope, SharingStarted.Eagerly, null)

    /** Tablets: whether the docked drawer is shown; saved so it stays across boards and launches. */
    val drawerExpanded: StateFlow<Boolean> = settingsRepository.observeSettings()
        .map { it.drawerExpanded }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TimelineSettings.DEFAULT.drawerExpanded)

    fun toggleDrawer() {
        val expanded = !drawerExpanded.value
        // A failed write leaves the drawer as the database has it, like the left pane's toggle.
        viewModelScope.launch {
            try {
                settingsRepository.setDrawerExpanded(expanded)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    private val mutableDeleteState = MutableStateFlow<DeleteBoardState?>(null)
    val deleteState: StateFlow<DeleteBoardState?> = mutableDeleteState

    private val deleteResults = Channel<BoardDeleted>(Channel.CONFLATED)
    val boardDeleted: Flow<BoardDeleted> = deleteResults.receiveAsFlow()

    fun requestDelete(boardId: Long) {
        if (mutableDeleteState.value != null) return
        val board = boards.value?.firstOrNull { it.id == boardId } ?: return
        mutableDeleteState.value = DeleteBoardState(board)
    }

    fun dismissDelete() {
        if (mutableDeleteState.value?.isDeleting != true) mutableDeleteState.value = null
    }

    fun confirmDelete() {
        val target = mutableDeleteState.value ?: return
        if (target.isDeleting) return
        mutableDeleteState.value = target.copy(isDeleting = true, deleteFailed = false)
        viewModelScope.launch {
            try {
                boardRepository.delete(target.board.id)
                val next = boardRepository.getStartBoardId()
                mutableDeleteState.value = null
                deleteResults.send(BoardDeleted(target.board.id, next))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableDeleteState.update { it?.copy(isDeleting = false, deleteFailed = true) }
            }
        }
    }
}
