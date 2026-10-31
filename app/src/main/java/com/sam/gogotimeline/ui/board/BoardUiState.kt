package com.sam.gogotimeline.ui.board

import com.sam.gogotimeline.data.model.Board
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TimelineSettings

sealed interface BoardUiState {
    data object Loading : BoardUiState

    data object Error : BoardUiState

    data class Ready(
        val board: Board,
        val tasks: List<Task>,
        val settings: TimelineSettings,
        val selectedTaskId: Long?,
    ) : BoardUiState
}
