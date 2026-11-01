package com.sam.gogotimeline.ui.board

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sam.gogotimeline.R
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews

/** Confirms deleting [taskName], the name stored in the database rather than the half-edited draft name. */
@Composable
internal fun DeleteTaskDialog(
    taskName: String,
    isDeleting: Boolean,
    deleteFailed: Boolean,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmDialogWindow(isBusy = isDeleting, onCancel = onCancel) {
        DeleteTaskContent(
            taskName = taskName,
            isDeleting = isDeleting,
            deleteFailed = deleteFailed,
            onDelete = onDelete,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun DeleteTaskContent(
    taskName: String,
    isDeleting: Boolean,
    deleteFailed: Boolean,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmDialogContent(
        title = stringResource(R.string.delete_task_title, taskName),
        message = stringResource(R.string.delete_task_message),
        error = if (deleteFailed) stringResource(R.string.delete_failed) else null,
    ) {
        SecondaryDialogButton(text = stringResource(R.string.cancel), enabled = !isDeleting, onClick = onCancel)
        PrimaryDialogButton(
            text = stringResource(R.string.delete),
            enabled = !isDeleting,
            isBusy = isDeleting,
            onClick = onDelete,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// Previews show the content without the Dialog window, which previews cannot render inline.
@ThemePreviews
@Composable
private fun DeleteTaskDialogPreview() {
    PreviewSurface {
        DeleteTaskContent(
            taskName = "Kickoff meeting",
            isDeleting = false,
            deleteFailed = false,
            onDelete = {},
            onCancel = {},
        )
    }
}

@ThemePreviews
@Composable
private fun DeleteTaskDialogLongNamePreview() {
    PreviewSurface {
        DeleteTaskContent(
            taskName = "Quarterly planning review with the design and research teams",
            isDeleting = false,
            deleteFailed = false,
            onDelete = {},
            onCancel = {},
        )
    }
}

@ThemePreviews
@Composable
private fun DeleteTaskDialogDeletingPreview() {
    PreviewSurface {
        DeleteTaskContent(
            taskName = "Kickoff meeting",
            isDeleting = true,
            deleteFailed = false,
            onDelete = {},
            onCancel = {},
        )
    }
}

@ThemePreviews
@Composable
private fun DeleteTaskDialogFailedPreview() {
    PreviewSurface {
        DeleteTaskContent(
            taskName = "Kickoff meeting",
            isDeleting = false,
            deleteFailed = true,
            onDelete = {},
            onCancel = {},
        )
    }
}
