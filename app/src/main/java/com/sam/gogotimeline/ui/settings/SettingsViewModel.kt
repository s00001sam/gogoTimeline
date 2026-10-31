package com.sam.gogotimeline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.gogotimeline.core.RowHeight
import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.ZoomRules
import com.sam.gogotimeline.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Every change is saved right away (spec 8): a snap on tap, a slider on release. Values being dragged stay in this
 * ViewModel only; a failed save drops them, so the page falls back to what the database has.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val local = MutableStateFlow(LocalState())
    private val saveFailures = Channel<Unit>(Channel.CONFLATED)

    /** Null until the settings are read. */
    val uiState: StateFlow<SettingsUiState?> = combine(settingsRepository.observeSettings(), local) { settings, local ->
        val unit = settings.zoom.unit
        SettingsUiState(
            currentUnit = unit,
            selectedSnap = settings.snapFor(unit),
            widthMultiplier = settings.widthMultiplier,
            rowHeightDp = settings.rowHeightDp,
            pendingWidthMultiplier = local.pendingWidthMultiplier,
            pendingRowHeightMultiplier = local.pendingRowHeightMultiplier,
            showResetDialog = local.showResetDialog,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** One event per failed save, shown as `Couldn't save settings.` */
    val saveFailed: Flow<Unit> = saveFailures.receiveAsFlow()

    fun onSnapSelected(interval: SnapInterval) {
        val unit = uiState.value?.currentUnit ?: return
        save { settingsRepository.setSnap(unit, interval) }
    }

    fun onWidthDrag(value: Float) {
        local.update { it.copy(pendingWidthMultiplier = value) }
    }

    fun onWidthDragFinished() {
        val value = ZoomRules.roundWidthMultiplier(local.value.pendingWidthMultiplier ?: return)
        save(onDone = { local.update { it.copy(pendingWidthMultiplier = null) } }) {
            settingsRepository.setWidthMultiplier(value)
            uiState.first { it?.widthMultiplier == value }
        }
    }

    /** [multiplier] is the slider's value; 1x is the default row height. */
    fun onRowHeightDrag(multiplier: Float) {
        local.update { it.copy(pendingRowHeightMultiplier = multiplier) }
    }

    fun onRowHeightDragFinished() {
        val multiplier = local.value.pendingRowHeightMultiplier ?: return
        val dp = RowHeight.toDp(RowHeight.roundMultiplier(multiplier))
        save(onDone = { local.update { it.copy(pendingRowHeightMultiplier = null) } }) {
            settingsRepository.setRowHeight(dp)
            uiState.first { it?.rowHeightDp == dp }
        }
    }

    fun onResetClick() {
        local.update { it.copy(showResetDialog = true) }
    }

    fun onResetConfirm() {
        local.update { it.copy(showResetDialog = false) }
        save { settingsRepository.resetToDefaults() }
    }

    fun onResetDismiss() {
        local.update { it.copy(showResetDialog = false) }
    }

    // A slider's pending value is cleared once the saved value has come back from the database (or the save failed),
    // so the slider never jumps back to the old value for a frame.
    private fun save(
        onDone: () -> Unit = {},
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveFailures.trySend(Unit)
            } finally {
                onDone()
            }
        }
    }

    private data class LocalState(
        val pendingWidthMultiplier: Float? = null,
        val pendingRowHeightMultiplier: Float? = null,
        val showResetDialog: Boolean = false,
    )
}
