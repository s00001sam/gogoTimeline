package com.sam.gogotimeline.ui.board

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.gogotimeline.core.text.TaskTitle
import com.sam.gogotimeline.data.repository.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardFormState(
    val name: String = "",
    val originalName: String? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
) {
    val canSubmit: Boolean
        get() = TaskTitle.isValid(name) && originalName?.let { name.trim() != it } != false && !isSaving
}

sealed interface BoardFormResult {
    data class Created(val boardId: Long) : BoardFormResult

    data object Renamed : BoardFormResult
}

@HiltViewModel
class BoardFormViewModel @Inject constructor(
    private val repository: BoardRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        BoardFormState(
            name = savedStateHandle[NAME] ?: "",
            originalName = savedStateHandle[ORIGINAL_NAME],
        ),
    )
    val state: StateFlow<BoardFormState> = mutableState

    private val results = Channel<BoardFormResult>(Channel.CONFLATED)
    val result: Flow<BoardFormResult> = results.receiveAsFlow()

    fun initializeCreate() {
        if (!savedStateHandle.contains(MODE)) savedStateHandle[MODE] = CREATE
    }

    fun initializeRename(
        boardId: Long,
        name: String,
    ) {
        if (savedStateHandle.contains(MODE)) return
        savedStateHandle[MODE] = RENAME
        savedStateHandle[BOARD_ID] = boardId
        savedStateHandle[NAME] = name
        savedStateHandle[ORIGINAL_NAME] = name
        mutableState.value = BoardFormState(name = name, originalName = name)
    }

    fun onNameChange(value: String) {
        if (mutableState.value.isSaving) return
        val clean = TaskTitle.clean(value)
        savedStateHandle[NAME] = clean
        mutableState.update { it.copy(name = clean, saveFailed = false) }
    }

    fun submit() {
        val current = mutableState.value
        if (!current.canSubmit) return
        mutableState.value = current.copy(isSaving = true, saveFailed = false)
        viewModelScope.launch {
            try {
                when (savedStateHandle.get<String>(MODE) ?: CREATE) {
                    CREATE -> results.send(BoardFormResult.Created(repository.create(current.name)))

                    else -> {
                        repository.rename(checkNotNull(savedStateHandle[BOARD_ID]), current.name)
                        results.send(BoardFormResult.Renamed)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    private companion object {
        const val MODE = "boardFormMode"
        const val CREATE = "create"
        const val RENAME = "rename"
        const val BOARD_ID = "boardFormId"
        const val NAME = "boardFormName"
        const val ORIGINAL_NAME = "boardFormOriginalName"
    }
}
