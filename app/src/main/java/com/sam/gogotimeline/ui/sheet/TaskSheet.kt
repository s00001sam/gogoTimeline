package com.sam.gogotimeline.ui.sheet

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
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
import com.sam.gogotimeline.core.text.TaskNotes
import com.sam.gogotimeline.core.text.TaskTitle
import com.sam.gogotimeline.core.time.TaskTimeFormat
import com.sam.gogotimeline.core.time.TimeError
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/** What the task sheet shows; dates and times are local to the current time zone. */
@Immutable
data class TaskSheetState(
    val title: String,
    val notes: String,
    val start: SheetMoment,
    val end: SheetMoment,
    val isAllDay: Boolean,
    val timeError: TimeError?,
    val colorIndex: Int,
    val canSave: Boolean,
    val isSaving: Boolean,
    val saveFailed: Boolean,
    // A task not saved yet: no Delete, and the sheet opens expanded with the name focused.
    val isNew: Boolean = false,
)

/** A start or end as the sheet shows it: the date, and the time unless the task is all-day. */
@Immutable
data class SheetMoment(
    val date: LocalDate,
    val time: LocalTime?,
)

/** Which end of the task a date or time was picked for. */
enum class Boundary { START, END }

private enum class OpenPicker(
    val boundary: Boundary,
    val isDate: Boolean,
) {
    START_DATE(Boundary.START, isDate = true),
    START_TIME(Boundary.START, isDate = false),
    END_DATE(Boundary.END, isDate = true),
    END_TIME(Boundary.END, isDate = false),
}

/**
 * The task sheet. The top part (× / ✓, name, times, All day, colors) shows while collapsed; dragging up opens the
 * sheet to full screen below the status bar and shows the notes, which grow with their text while the whole sheet
 * scrolls. The sheet can only expand by revealing content under the collapsed part, so it is as tall as the screen
 * allows. [onCollapsedHeightChange] reports the peek height in px. Everything follows the system font size.
 * Outside a bottom sheet ([inSheet] false: the floating card or the full page, spec 7.1) there is no drag handle, it
 * is as tall as its content, and the caller passes [expanded] true, so everything shows at once.
 */
@Composable
fun TaskSheet(
    modifier: Modifier = Modifier,
    state: TaskSheetState,
    expanded: Boolean,
    focusName: Boolean,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDatePick: (Boundary, LocalDate) -> Unit,
    onTimePick: (Boundary, LocalTime) -> Unit,
    onAllDayChange: (Boolean) -> Unit,
    onColorChange: (Int) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit,
    onCollapsedHeightChange: (Int) -> Unit = {},
    inSheet: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    // The keyboard overlays the screen; the peek height is the larger of the usual collapsed height and "name field
    // bottom + gap + keyboard", so a rising keyboard lifts the sheet until the name sits just above it and the rows
    // below stay behind the keyboard. Read during composition: it follows the keyboard frame by frame.
    val usualBottomPx = WindowInsets.navigationBars.getBottom(density) +
        with(density) { LayoutConst.SHEET_PADDING_DP.dp.roundToPx() }
    val keyboardPx = WindowInsets.ime.getBottom(density)
    val gapPx = with(density) { LayoutConst.SHEET_SPACING_DP.dp.roundToPx() }
    var collapsedPx by remember { mutableIntStateOf(0) }
    var collapsedTopPx by remember { mutableFloatStateOf(0f) }
    var nameBottomPx by remember { mutableFloatStateOf(0f) }
    val usualPeekPx = collapsedPx + usualBottomPx
    val nameAbovePeekPx = (nameBottomPx - collapsedTopPx).roundToInt() + gapPx + keyboardPx
    // Computed here, not inside SideEffect: reads in composition make a new measurement recompose and report.
    val peekPx = if (collapsedPx > 0) maxOf(usualPeekPx, nameAbovePeekPx) else 0
    SideEffect { if (peekPx > 0) onCollapsedHeightChange(peekPx) }
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    // Long notes scroll the whole sheet; back at the collapsed size it always shows the top part.
    val scroll = rememberScrollState()
    LaunchedEffect(expanded) { if (!expanded) scroll.scrollTo(0) }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // A sheet is as tall as the screen allows below the status bar, so it can expand; the card and the page wrap
        // their content up to the height they are given, scrolling beyond it.
        val fullHeight = if (inSheet && constraints.hasBoundedHeight) {
            Modifier.height(with(density) { (constraints.maxHeight - statusBarPx).coerceAtLeast(0).toDp() })
        } else {
            Modifier
        }
        Column(
            modifier = fullHeight
                .fillMaxWidth()
                // The scrolling area ends at the keyboard, so a focused line is brought into view above it.
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom),
                )
                .verticalScroll(scroll)
                // A tap on any empty part of the sheet leaves the text fields and closes the keyboard.
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                .padding(horizontal = LayoutConst.SHEET_PADDING_DP.dp),
        ) {
            CollapsedPart(
                modifier = Modifier.onGloballyPositioned {
                    collapsedPx = it.size.height
                    collapsedTopPx = it.positionInRoot().y
                },
                state = state,
                focusName = focusName,
                showHandle = inSheet,
                onNameBottomInRoot = { nameBottomPx = it },
                onTitleChange = onTitleChange,
                onDatePick = onDatePick,
                onTimePick = onTimePick,
                onAllDayChange = onAllDayChange,
                onColorChange = onColorChange,
                onClose = onClose,
                onConfirm = onConfirm,
            )
            // Notes and Delete only while expanded (collapsed, their top would peek out through the bottom padding);
            // they fade in and out.
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(LayoutConst.NOTES_FADE_MILLIS)),
                exit = fadeOut(tween(LayoutConst.NOTES_FADE_MILLIS)),
            ) {
                Column(
                    modifier = Modifier.padding(top = LayoutConst.SHEET_SPACING_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(LayoutConst.SHEET_PADDING_DP.dp),
                ) {
                    // Starts three lines tall and keeps growing with its text.
                    NotesField(notes = state.notes, enabled = !state.isSaving, onNotesChange = onNotesChange)
                    if (!state.isNew) DeleteButton(enabled = !state.isSaving, onClick = onDelete)
                }
            }
            Spacer(modifier = Modifier.height(LayoutConst.SHEET_PADDING_DP.dp))
        }
    }
}

@Composable
private fun CollapsedPart(
    modifier: Modifier = Modifier,
    state: TaskSheetState,
    focusName: Boolean,
    showHandle: Boolean,
    onNameBottomInRoot: (Float) -> Unit,
    onTitleChange: (String) -> Unit,
    onDatePick: (Boundary, LocalDate) -> Unit,
    onTimePick: (Boundary, LocalTime) -> Unit,
    onAllDayChange: (Boolean) -> Unit,
    onColorChange: (Int) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    var openPicker by rememberSaveable { mutableStateOf<OpenPicker?>(null) }
    // A new task is typed right away: the name takes focus, which brings up the keyboard.
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(focusName) { if (focusName) nameFocus.requestFocus() }
    Column(
        modifier = modifier.padding(top = LayoutConst.SHEET_TOP_PADDING_DP.dp),
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
                if (showHandle) {
                    Box(
                        modifier = Modifier
                            .size(LayoutConst.SHEET_HANDLE_WIDTH_DP.dp, LayoutConst.SHEET_HANDLE_HEIGHT_DP.dp)
                            .background(
                                colorScheme.onSurfaceVariant.copy(alpha = LayoutConst.SHEET_HANDLE_ALPHA),
                                RoundedCornerShape(LayoutConst.SHEET_HANDLE_HEIGHT_DP.dp),
                            ),
                    )
                }
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
        NameField(
            modifier = Modifier
                .focusRequester(nameFocus)
                .onGloballyPositioned { onNameBottomInRoot(it.positionInRoot().y + it.size.height) },
            title = state.title,
            enabled = !state.isSaving,
            onTitleChange = onTitleChange,
        )
        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.save_failed),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.error,
            )
        }
        TimeLine(
            label = stringResource(R.string.start),
            moment = state.start,
            enabled = !state.isSaving,
            onDateClick = { openPicker = OpenPicker.START_DATE },
            onTimeClick = { openPicker = OpenPicker.START_TIME },
        )
        TimeLine(
            label = stringResource(R.string.end),
            moment = state.end,
            enabled = !state.isSaving,
            onDateClick = { openPicker = OpenPicker.END_DATE },
            onTimeClick = { openPicker = OpenPicker.END_TIME },
        )
        state.timeError?.let { error ->
            Text(
                text = stringResource(
                    when (error) {
                        TimeError.END_NOT_AFTER_START -> R.string.end_not_after_start
                        TimeError.TOO_SHORT -> R.string.too_short
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.error,
            )
        }
        AllDayLine(checked = state.isAllDay, enabled = !state.isSaving, onCheckedChange = onAllDayChange)
        ColorPicker(selected = state.colorIndex, enabled = !state.isSaving, onSelect = onColorChange)
    }
    openPicker?.let { picker ->
        val moment = if (picker.boundary == Boundary.START) state.start else state.end
        val close = { openPicker = null }
        if (picker.isDate) {
            SheetDatePickerDialog(
                initial = moment.date,
                onPick = { onDatePick(picker.boundary, it) },
                onDismiss = close,
            )
        } else {
            moment.time?.let { time ->
                SheetTimePickerDialog(
                    initial = time,
                    onPick = { onTimePick(picker.boundary, it) },
                    onDismiss = close,
                )
            }
        }
    }
}

/** A filled, borderless name field showing up to two lines; Done only closes the keyboard. */
@Composable
private fun NameField(
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder },
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
        maxLines = LayoutConst.NAME_FIELD_MAX_LINES,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        cursorBrush = SolidColor(colorScheme.primary),
        decorationBox = { innerTextField ->
            FilledField(
                placeholder = placeholder.takeIf { title.isEmpty() },
                counter = "${TaskTitle.length(title.trim())}/${LayoutConst.NAME_MAX_LENGTH}",
                innerTextField = innerTextField,
            )
        },
    )
}

/** Multi-line notes in the same filled style as the name; starts three lines tall and grows with its text. */
@Composable
private fun NotesField(
    modifier: Modifier = Modifier,
    notes: String,
    enabled: Boolean,
    onNotesChange: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val placeholder = stringResource(R.string.notes)
    BasicTextField(
        value = notes,
        onValueChange = onNotesChange,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder },
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
        minLines = LayoutConst.NOTES_FIELD_MIN_LINES,
        cursorBrush = SolidColor(colorScheme.primary),
        decorationBox = { innerTextField ->
            FilledField(
                placeholder = placeholder.takeIf { notes.isEmpty() },
                counter = "${TaskNotes.length(TaskNotes.normalize(notes))}/${LayoutConst.NOTES_MAX_LENGTH}",
                innerTextField = innerTextField,
            )
        },
    )
}

/**
 * The borderless filled box shared by the task name, notes and board name fields, with the character count at the
 * bottom right.
 */
@Composable
internal fun FilledField(
    placeholder: String?,
    counter: String,
    innerTextField: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    // Text starts at the top left; the count stays at the bottom right as the field grows.
    Row(
        modifier = Modifier
            .background(colorScheme.surfaceContainerHigh, RoundedCornerShape(LayoutConst.NAME_FIELD_CORNER_DP.dp))
            .padding(
                horizontal = LayoutConst.NAME_FIELD_PADDING_HORIZONTAL_DP.dp,
                vertical = LayoutConst.NAME_FIELD_PADDING_VERTICAL_DP.dp,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (placeholder != null) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            innerTextField()
        }
        Text(
            text = counter,
            modifier = Modifier
                .align(Alignment.Bottom)
                .padding(start = LayoutConst.SHEET_SPACING_DP.dp),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
        )
    }
}

/** Full width, light red with red text, at the bottom of the expanded sheet; it asks before deleting. */
@Composable
private fun DeleteButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(LayoutConst.NAME_FIELD_CORNER_DP.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = colorScheme.errorContainer,
            contentColor = colorScheme.error,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = null,
            modifier = Modifier.size(LayoutConst.SHEET_BUTTON_ICON_DP.dp),
        )
        Spacer(modifier = Modifier.width(LayoutConst.SHEET_SPACING_DP.dp))
        Text(stringResource(R.string.delete_task))
    }
}

/** The All day checkbox; the whole row toggles, not just the box. */
@Composable
private fun AllDayLine(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(
            text = stringResource(R.string.all_day),
            modifier = Modifier.padding(start = LayoutConst.SHEET_SPACING_DP.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** A label with a date button and, for timed tasks, a time button (spec 7, decisions 1 and 6). */
@Composable
private fun TimeLine(
    label: String,
    moment: SheetMoment,
    enabled: Boolean,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LayoutConst.SHEET_SPACING_DP.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.widthIn(min = LayoutConst.SHEET_TIME_LABEL_MIN_WIDTH_DP.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Chip(text = TaskTimeFormat.sheetDate(moment.date), enabled = enabled, onClick = onDateClick)
        moment.time?.let { Chip(text = TaskTimeFormat.sheetTime(it), enabled = enabled, onClick = onTimeClick) }
    }
}

@Composable
private fun Chip(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(LayoutConst.SHEET_CHIP_CORNER_DP.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(
                horizontal = LayoutConst.SHEET_CHIP_PADDING_HORIZONTAL_DP.dp,
                vertical = LayoutConst.SHEET_CHIP_PADDING_VERTICAL_DP.dp,
            ),
        style = MaterialTheme.typography.bodyMedium,
    )
}

/**
 * The seven task colors in one row; the selected one gets an outer ring in its own color, with a gap. Scrolls sideways on narrow
 * screens instead of shrinking the touch targets (spec 7.2).
 */
@Composable
private fun ColorPicker(
    selected: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val taskColors = GogoTimelineTheme.extendedColors.taskColors
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
                        // No ripple: the ring moving to the tapped color is the feedback.
                        .selectable(
                            selected = index == selected,
                            interactionSource = null,
                            indication = null,
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
                                    Modifier.border(LayoutConst.COLOR_RING_WIDTH_DP.dp, color, CircleShape)
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
    notes = "Agenda: goals, roles, timeline",
    start = SheetMoment(LocalDate.of(2026, 10, 31), LocalTime.of(9, 0)),
    end = SheetMoment(LocalDate.of(2026, 10, 31), LocalTime.of(10, 30)),
    isAllDay = false,
    timeError = null,
    colorIndex = 0,
    canSave = true,
    isSaving = false,
    saveFailed = false,
)

@Composable
private fun SheetPreview(
    state: TaskSheetState,
    expanded: Boolean = true,
) {
    PreviewSurface {
        TaskSheet(
            state = state,
            expanded = expanded,
            focusName = false,
            onTitleChange = {},
            onNotesChange = {},
            onDatePick = { _, _ -> },
            onTimePick = { _, _ -> },
            onAllDayChange = {},
            onColorChange = {},
            onClose = {},
            onConfirm = {},
            onDelete = {},
        )
    }
}

@ThemePreviews
@Composable
private fun TaskSheetPreview() = SheetPreview(previewState)

@ThemePreviews
@Composable
private fun TaskSheetCollapsedPreview() = SheetPreview(previewState, expanded = false)

@ThemePreviews
@Composable
private fun TaskSheetNewTaskPreview() =
    SheetPreview(previewState.copy(title = "", notes = "", isNew = true, canSave = false, colorIndex = 3))

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
private fun TaskSheetInvalidTimesPreview() = SheetPreview(
    previewState.copy(
        end = SheetMoment(LocalDate.of(2026, 10, 31), LocalTime.of(8, 0)),
        timeError = TimeError.END_NOT_AFTER_START,
        canSave = false,
    ),
)

@ThemePreviews
@Composable
private fun TaskSheetAllDayPreview() {
    val day = SheetMoment(LocalDate.of(2026, 11, 7), time = null)
    SheetPreview(previewState.copy(start = day, end = day, isAllDay = true, colorIndex = 5))
}
