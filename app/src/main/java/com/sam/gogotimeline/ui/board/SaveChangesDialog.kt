package com.sam.gogotimeline.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sam.gogotimeline.R
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews

/** Save / Discard when closing the editor with changes; cancelling goes back to editing. */
@Composable
internal fun SaveChangesDialog(
    canSave: Boolean,
    isSaving: Boolean,
    saveFailed: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmDialogWindow(isBusy = isSaving, onCancel = onCancel) {
        SaveChangesContent(
            canSave = canSave,
            isSaving = isSaving,
            saveFailed = saveFailed,
            onSave = onSave,
            onDiscard = onDiscard,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun SaveChangesContent(
    canSave: Boolean,
    isSaving: Boolean,
    saveFailed: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmDialogContent(
        title = stringResource(R.string.save_changes_title),
        message = null,
        error = if (saveFailed) stringResource(R.string.save_failed) else null,
        isBusy = isSaving,
        onCancel = onCancel,
    ) {
        SecondaryDialogButton(text = stringResource(R.string.discard), enabled = !isSaving, onClick = onDiscard)
        PrimaryDialogButton(
            text = stringResource(R.string.save),
            enabled = canSave,
            isBusy = isSaving,
            onClick = onSave,
        )
    }
}

// Previews show the content without the Dialog window, which previews cannot render inline.
@ThemePreviews
@Composable
private fun SaveChangesDialogPreview() {
    PreviewSurface {
        SaveChangesContent(
            canSave = true,
            isSaving = false,
            saveFailed = false,
            onSave = {},
            onDiscard = {},
            onCancel = {},
        )
    }
}

@ThemePreviews
@Composable
private fun SaveChangesDialogSavingPreview() {
    PreviewSurface {
        SaveChangesContent(
            canSave = false,
            isSaving = true,
            saveFailed = false,
            onSave = {},
            onDiscard = {},
            onCancel = {},
        )
    }
}

@ThemePreviews
@Composable
private fun SaveChangesDialogFailedPreview() {
    PreviewSurface {
        SaveChangesContent(
            canSave = true,
            isSaving = false,
            saveFailed = true,
            onSave = {},
            onDiscard = {},
            onCancel = {},
        )
    }
}
