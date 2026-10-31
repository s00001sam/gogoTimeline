package com.sam.gogotimeline.ui.sheet

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.text.TaskTitle
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme

/** What the task sheet shows; times are already formatted for the current time zone. */
@Immutable
data class TaskSheetState(
    val title: String,
    val start: String,
    val end: String,
    val colorIndex: Int,
    val canSave: Boolean,
    val isSaving: Boolean,
    val saveFailed: Boolean,
)

/**
 * The collapsed task sheet: × at the top left, ✓ at the top right, then the name, times and colors. Everything
 * follows the system font size; the sheet measures its own height.
 */
@Composable
fun TaskSheet(
    modifier: Modifier = Modifier,
    state: TaskSheetState,
    onTitleChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            // A tap on any empty part of the sheet leaves the name field and closes the keyboard.
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .navigationBarsPadding()
            .padding(horizontal = LayoutConst.SHEET_PADDING_DP.dp)
            .padding(top = LayoutConst.SHEET_TOP_PADDING_DP.dp, bottom = LayoutConst.SHEET_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(LayoutConst.SHEET_SPACING_DP.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier.size(LayoutConst.SHEET_BUTTON_SIZE_DP.dp),
                enabled = !state.isSaving,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colorScheme.surfaceContainerHigh,
                    contentColor = colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_editor),
                    modifier = Modifier.size(LayoutConst.SHEET_BUTTON_ICON_DP.dp),
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(LayoutConst.SHEET_HANDLE_WIDTH_DP.dp, LayoutConst.SHEET_HANDLE_HEIGHT_DP.dp)
                        .background(
                            colorScheme.onSurfaceVariant.copy(alpha = LayoutConst.SHEET_HANDLE_ALPHA),
                            RoundedCornerShape(LayoutConst.SHEET_HANDLE_HEIGHT_DP.dp),
                        ),
                )
            }
            FilledIconButton(
                onClick = onConfirm,
                modifier = Modifier.size(LayoutConst.SHEET_BUTTON_SIZE_DP.dp),
                enabled = state.canSave,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(LayoutConst.SAVE_PROGRESS_SIZE_DP.dp),
                        strokeWidth = LayoutConst.SAVE_PROGRESS_STROKE_DP.dp,
                        color = colorScheme.onSurfaceVariant,
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.save),
                        modifier = Modifier.size(LayoutConst.SHEET_BUTTON_ICON_DP.dp),
                    )
                }
            }
        }
        NameField(title = state.title, enabled = !state.isSaving, onTitleChange = onTitleChange)
        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.save_failed),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.error,
            )
        }
        TimeLine(label = stringResource(R.string.start), value = state.start)
        TimeLine(label = stringResource(R.string.end), value = state.end)
        ColorPicker(selected = state.colorIndex, enabled = !state.isSaving, onSelect = onColorChange)
    }
}

/** A filled, borderless name field showing up to two lines; Done only closes the keyboard. */
@Composable
private fun NameField(
    title: String,
    enabled: Boolean,
    onTitleChange: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val placeholder = stringResource(R.string.task_name)
    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder },
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
        maxLines = LayoutConst.NAME_FIELD_MAX_LINES,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        cursorBrush = SolidColor(colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .background(
                        colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(LayoutConst.NAME_FIELD_CORNER_DP.dp),
                    )
                    .padding(
                        horizontal = LayoutConst.NAME_FIELD_PADDING_HORIZONTAL_DP.dp,
                        vertical = LayoutConst.NAME_FIELD_PADDING_VERTICAL_DP.dp,
                    ),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (title.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
                Text(
                    text = "${TaskTitle.length(title.trim())}/${LayoutConst.NAME_MAX_LENGTH}",
                    modifier = Modifier.padding(start = LayoutConst.SHEET_SPACING_DP.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun TimeLine(
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.widthIn(min = LayoutConst.SHEET_TIME_LABEL_MIN_WIDTH_DP.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * The seven task colors in one row; the selected one gets an outer ring with a gap. Scrolls sideways on narrow
 * screens instead of shrinking the touch targets (spec 7.2).
 */
@Composable
private fun ColorPicker(
    selected: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val taskColors = GogoTimelineTheme.extendedColors.taskColors
    val ringColor = MaterialTheme.colorScheme.onSurface
    // Centered when the row fits; scrolls when it does not.
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
        ) {
            taskColors.forEachIndexed { index, color ->
                val name = stringResource(colorNames[index])
                Box(
                    modifier = Modifier
                        .size(LayoutConst.COLOR_TOUCH_DP.dp)
                        .selectable(
                            selected = index == selected,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) },
                        )
                        .semantics { contentDescription = name },
                    contentAlignment = Alignment.Center,
                ) {
                    val ring = LayoutConst.COLOR_RING_WIDTH_DP + LayoutConst.COLOR_RING_GAP_DP
                    Box(
                        modifier = Modifier
                            .size((LayoutConst.COLOR_DOT_DP + 2 * ring).dp)
                            .then(
                                if (index == selected) {
                                    Modifier.border(LayoutConst.COLOR_RING_WIDTH_DP.dp, ringColor, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(LayoutConst.COLOR_DOT_DP.dp)
                                .background(color, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@StringRes
private val colorNames = listOf(
    R.string.color_red,
    R.string.color_orange,
    R.string.color_yellow,
    R.string.color_green,
    R.string.color_blue,
    R.string.color_indigo,
    R.string.color_violet,
)

private val previewState = TaskSheetState(
    title = "Kickoff meeting",
    start = "Fri, Oct 31  09:00",
    end = "Fri, Oct 31  10:30",
    colorIndex = 0,
    canSave = true,
    isSaving = false,
    saveFailed = false,
)

@Composable
private fun SheetPreview(state: TaskSheetState) {
    PreviewSurface {
        TaskSheet(state = state, onTitleChange = {}, onColorChange = {}, onClose = {}, onConfirm = {})
    }
}

@ThemePreviews
@Composable
private fun TaskSheetPreview() = SheetPreview(previewState)

@ThemePreviews
@Composable
private fun TaskSheetLongTitlePreview() =
    SheetPreview(previewState.copy(title = "Quarterly planning with design and sales", colorIndex = 4))

@ThemePreviews
@Composable
private fun TaskSheetSavingPreview() = SheetPreview(previewState.copy(canSave = false, isSaving = true))

@ThemePreviews
@Composable
private fun TaskSheetFailedPreview() = SheetPreview(previewState.copy(saveFailed = true))

@ThemePreviews
@Composable
private fun TaskSheetAllDayPreview() =
    SheetPreview(previewState.copy(start = "Sat, Nov 7", end = "Sat, Nov 7", colorIndex = 5))
