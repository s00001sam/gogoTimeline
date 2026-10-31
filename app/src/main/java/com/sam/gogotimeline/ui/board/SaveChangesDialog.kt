package com.sam.gogotimeline.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews

/**
 * A compact Save / Discard dialog. ×, tapping outside, and back cancel and return to editing; all of them,
 * and both buttons, wait while saving. Text follows the system font size; the height is never fixed.
 */
@Composable
internal fun SaveChangesDialog(
    canSave: Boolean,
    isSaving: Boolean,
    saveFailed: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!isSaving) onCancel() },
        properties = DialogProperties(dismissOnBackPress = !isSaving, dismissOnClickOutside = !isSaving),
    ) {
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
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.widthIn(max = LayoutConst.SAVE_DIALOG_MAX_WIDTH_DP.dp),
        shape = RoundedCornerShape(LayoutConst.SAVE_DIALOG_CORNER_DP.dp),
        color = colorScheme.surface,
        shadowElevation = LayoutConst.SAVE_DIALOG_ELEVATION_DP.dp,
    ) {
        Column(modifier = Modifier.padding(LayoutConst.SAVE_DIALOG_PADDING_DP.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.save_changes_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                )
                IconButton(onClick = onCancel, enabled = !isSaving) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(LayoutConst.SAVE_DIALOG_CLOSE_ICON_DP.dp),
                        tint = colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (saveFailed) {
                Text(
                    text = stringResource(R.string.save_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDiscard,
                    enabled = !isSaving,
                    colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.onSurfaceVariant),
                ) {
                    Text(stringResource(R.string.discard))
                }
                TextButton(onClick = onSave, enabled = canSave) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(LayoutConst.SAVE_PROGRESS_SIZE_DP.dp),
                            strokeWidth = LayoutConst.SAVE_PROGRESS_STROKE_DP.dp,
                        )
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

// Previews show the content without the Dialog window, which previews cannot render inline.
@ThemePreviews
@Composable
private fun SaveChangesDialogPreview() {
    PreviewSurface {
        SaveChangesContent(canSave = true, isSaving = false, saveFailed = false, onSave = {
        }, onDiscard = {}, onCancel = {})
    }
}

@ThemePreviews
@Composable
private fun SaveChangesDialogSavingPreview() {
    PreviewSurface {
        SaveChangesContent(canSave = false, isSaving = true, saveFailed = false, onSave = {
        }, onDiscard = {}, onCancel = {})
    }
}

@ThemePreviews
@Composable
private fun SaveChangesDialogFailedPreview() {
    PreviewSurface {
        SaveChangesContent(canSave = true, isSaving = false, saveFailed = true, onSave = {
        }, onDiscard = {}, onCancel = {})
    }
}
