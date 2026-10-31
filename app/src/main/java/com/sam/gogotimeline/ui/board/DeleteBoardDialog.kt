package com.sam.gogotimeline.ui.board

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sam.gogotimeline.R
import com.sam.gogotimeline.data.model.Board
import com.sam.gogotimeline.ui.navigation.DeleteBoardState
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews

/** `Delete "Work"?` with its tasks; stays open with an error when it fails (spec 9). */
@Composable
fun DeleteBoardDialog(
    state: DeleteBoardState,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmDialogWindow(isBusy = state.isDeleting, onCancel = onCancel) {
        DeleteBoardContent(state = state, onDelete = onDelete, onCancel = onCancel)
    }
}

@Composable
private fun DeleteBoardContent(
    state: DeleteBoardState,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmDialogContent(
        title = stringResource(R.string.delete_board_title, state.board.name),
        message = stringResource(R.string.delete_board_message),
        error = if (state.deleteFailed) stringResource(R.string.delete_board_failed) else null,
        isBusy = state.isDeleting,
        onCancel = onCancel,
    ) {
        SecondaryDialogButton(
            text = stringResource(R.string.cancel),
            enabled = !state.isDeleting,
            onClick = onCancel,
        )
        PrimaryDialogButton(
            text = stringResource(R.string.delete),
            enabled = !state.isDeleting,
            isBusy = state.isDeleting,
            onClick = onDelete,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// Previews show the content without the Dialog window, which previews cannot render inline.
@ThemePreviews
@Composable
private fun DeleteBoardDialogPreview() {
    PreviewSurface {
        DeleteBoardContent(state = DeleteBoardState(Board(1, "Work", null, 0)), onDelete = {}, onCancel = {})
    }
}

@ThemePreviews
@Composable
private fun DeleteBoardDialogFailedPreview() {
    PreviewSurface {
        DeleteBoardContent(
            state = DeleteBoardState(Board(1, "Work", null, 0), deleteFailed = true),
            onDelete = {},
            onCancel = {},
        )
    }
}
