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
        /** The first-lock confirmation is showing (spec 9); editing has always ended by then. */
        val confirmingLock: Boolean = false,
        private val viewingTaskId: Long? = null,
    ) : BoardUiState {
        /** The task shown read only on a locked board; gone once the task is deleted. */
        val viewedTask: Task? get() = tasks.firstOrNull { it.id == viewingTaskId }

        /** The edited task, or the one shown read only. */
        val selectedTaskId: Long? get() = edit?.draft?.taskId ?: viewedTask?.id

        /** ✓ and the dialog's Save: a valid name and times, and not already saving. */
        val canSave: Boolean get() = edit?.isValid == true && !editStatus.isBusy
    }
}

/** The Save / Discard and Delete dialogs and their progress for the current edit. */
data class EditStatus(
    val confirmingClose: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val confirmingDelete: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteFailed: Boolean = false,
) {
    /** While saving or deleting, nothing may change the draft or close the editor. */
    val isBusy: Boolean get() = isSaving || isDeleting
}
