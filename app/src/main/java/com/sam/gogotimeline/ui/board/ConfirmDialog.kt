package com.sam.gogotimeline.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst

/** The window of a compact confirm dialog: ×, tapping outside and back cancel, but all of them wait while busy. */
@Composable
internal fun ConfirmDialogWindow(
    isBusy: Boolean,
    onCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!isBusy) onCancel() },
        properties = DialogProperties(dismissOnBackPress = !isBusy, dismissOnClickOutside = !isBusy),
        content = content,
    )
}

/**
 * The card inside: a title with ×, an optional message and error, and the buttons at the bottom right. Text follows
 * the system font size; the height is never fixed.
 */
@Composable
internal fun ConfirmDialogContent(
    title: String,
    message: String?,
    error: String?,
    isBusy: Boolean,
    onCancel: () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
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
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    maxLines = LayoutConst.CONFIRM_TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onCancel, enabled = !isBusy) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(LayoutConst.SAVE_DIALOG_CLOSE_ICON_DP.dp),
                        tint = colorScheme.onSurfaceVariant,
                    )
                }
            }
            message?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = LayoutConst.CONFIRM_MESSAGE_GAP_DP.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            error?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = colorScheme.error)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                content = buttons,
            )
        }
    }
}

/** The gray button that backs out (Discard, Cancel). */
@Composable
internal fun SecondaryDialogButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    ) {
        Text(text)
    }
}

/** The button that does the work (Save, Delete); shows a spinner while busy. */
@Composable
internal fun PrimaryDialogButton(
    text: String,
    enabled: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(LayoutConst.SAVE_PROGRESS_SIZE_DP.dp),
                strokeWidth = LayoutConst.SAVE_PROGRESS_STROKE_DP.dp,
                color = color,
            )
        } else {
            Text(text)
        }
    }
}
