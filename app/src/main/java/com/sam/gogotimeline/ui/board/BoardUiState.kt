package com.sam.gogotimeline.ui.board

import com.sam.gogotimeline.data.model.Board
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TimelineSettings

sealed interface BoardUiState {
    data object Loading : BoardUiState

    data object Error : BoardUiState

    /** [tasks] are what the database has; the screen lays [edit]'s draft over them. Editing = selected. */
    data class Ready(
        val board: Board,
        val tasks: List<Task>,
        val settings: TimelineSettings,
        val edit: EditSession?,
        val editStatus: EditStatus = EditStatus(),
    ) : BoardUiState {
        val selectedTaskId: Long? get() = edit?.draft?.taskId
    }
}

/** The Save / Discard dialog and saving progress for the current edit. */
data class EditStatus(
    val confirmingClose: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
)
