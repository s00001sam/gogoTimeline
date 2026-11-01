package com.sam.gogotimeline.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sam.gogotimeline.R
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews

/** `Lock "Work"?`, shown only the first time a board is locked in the app (spec 9). */
@Composable
fun LockBoardDialog(
    boardName: String,
    onLock: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmDialogWindow(isBusy = false, onCancel = onCancel) {
        LockBoardContent(boardName = boardName, onLock = onLock, onCancel = onCancel)
    }
}

@Composable
private fun LockBoardContent(
    boardName: String,
    onLock: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmDialogContent(
        title = stringResource(R.string.lock_board_title, boardName),
        message = stringResource(R.string.lock_board_message),
        error = null,
    ) {
        SecondaryDialogButton(
            text = stringResource(R.string.cancel),
            enabled = true,
            onClick = onCancel,
        )
        // Primary, not red: locking is easy to undo.
        PrimaryDialogButton(
            text = stringResource(R.string.lock),
            enabled = true,
            isBusy = false,
            onClick = onLock,
        )
    }
}

// Previews show the content without the Dialog window, which previews cannot render inline.
@ThemePreviews
@Composable
private fun LockBoardDialogPreview() {
    PreviewSurface {
        LockBoardContent(boardName = "Work", onLock = {}, onCancel = {})
    }
}

@ThemePreviews
@Composable
private fun LockBoardDialogLongNamePreview() {
    PreviewSurface {
        LockBoardContent(
            boardName = "Quarterly planning with a fairly long name that wraps",
            onLock = {},
            onCancel = {},
        )
    }
}
