package com.sam.gogotimeline.ui.board

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.text.TaskTitle
import com.sam.gogotimeline.data.model.Board
import com.sam.gogotimeline.ui.navigation.PageDialogFrame
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.sheet.FilledField

/**
 * Create a board ([board] null) or rename one (spec 9): a full page on phones, a dialog on tablets ([asDialog]). When
 * no board exists the page cannot be dismissed, and back leaves the app instead.
 */
@Composable
fun BoardFormScreen(
    modifier: Modifier = Modifier,
    board: Board?,
    dismissible: Boolean,
    asDialog: Boolean,
    onClose: () -> Unit,
    onCreated: (Long) -> Unit,
    onRenamed: () -> Unit,
    viewModel: BoardFormViewModel = hiltViewModel(),
) {
    val activity = LocalActivity.current
    if (!dismissible) BackHandler { activity?.finish() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, board) {
        if (board == null) viewModel.initializeCreate() else viewModel.initializeRename(board.id, board.name)
    }
    LaunchedEffect(viewModel) {
        viewModel.result.collect { result ->
            when (result) {
                is BoardFormResult.Created -> onCreated(result.boardId)
                BoardFormResult.Renamed -> onRenamed()
            }
        }
    }
    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        BoardFormContent(
            modifier = contentModifier,
            state = state,
            creating = board == null,
            dismissible = dismissible,
            asDialog = asDialog,
            onClose = onClose,
            onNameChange = viewModel::onNameChange,
            onSubmit = viewModel::submit,
        )
    }
    if (asDialog) PageDialogFrame(modifier, content) else content(modifier.fillMaxSize())
}

/** ← (or × in a dialog) and ✓ around one name field, focused with the keyboard up; a rename starts fully selected. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardFormContent(
    modifier: Modifier = Modifier,
    state: BoardFormState,
    creating: Boolean,
    dismissible: Boolean,
    asDialog: Boolean,
    onClose: () -> Unit,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Keyed on the original name, which a rename sets once loaded, so the field then starts fully selected.
    var fieldValue by remember(state.originalName) {
        val selection = if (state.originalName !=
            null
        ) {
            TextRange(0, state.name.length)
        } else {
            TextRange(state.name.length)
        }
        mutableStateOf(TextFieldValue(state.name, selection))
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    val insets = if (asDialog) WindowInsets(0) else null
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                // Same size as the board's own top bar title.
                title = {
                    Text(
                        text = stringResource(if (creating) R.string.new_board else R.string.rename_board),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    if (dismissible) {
                        IconButton(onClick = onClose, enabled = !state.isSaving) {
                            Icon(
                                painter = painterResource(
                                    if (asDialog) R.drawable.ic_close else R.drawable.ic_arrow_back,
                                ),
                                contentDescription = stringResource(if (asDialog) R.string.close else R.string.back),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSubmit, enabled = state.canSubmit) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(LayoutConst.SAVE_PROGRESS_SIZE_DP.dp),
                                strokeWidth = LayoutConst.SAVE_PROGRESS_STROKE_DP.dp,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = stringResource(R.string.save),
                            )
                        }
                    }
                },
                windowInsets = insets ?: TopAppBarDefaults.windowInsets,
            )
        },
        contentWindowInsets = insets ?: ScaffoldDefaults.contentWindowInsets,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(LayoutConst.SETTINGS_PADDING_DP.dp)
                .fillMaxSize(),
        ) {
            // Same filled style as the task name: up to two lines, the count at the bottom right.
            val placeholder = stringResource(R.string.board_name)
            BasicTextField(
                value = fieldValue,
                onValueChange = { value ->
                    // Cleaned here, not only in the ViewModel: the field keeps its own text, so a 41st character
                    // must be dropped from it too, or it would show while the saved name stays at 40.
                    val clean = TaskTitle.clean(value.text)
                    fieldValue = if (clean == value.text) {
                        value
                    } else {
                        TextFieldValue(clean, TextRange(minOf(value.selection.end, clean.length)))
                    }
                    onNameChange(clean)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = placeholder },
                enabled = !state.isSaving,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                maxLines = LayoutConst.NAME_FIELD_MAX_LINES,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    FilledField(
                        placeholder = placeholder.takeIf { fieldValue.text.isEmpty() },
                        counter = "${TaskTitle.length(state.name.trim())}/${LayoutConst.NAME_MAX_LENGTH}",
                        innerTextField = innerTextField,
                    )
                },
            )
            if (state.saveFailed) {
                Text(
                    text = stringResource(if (creating) R.string.create_board_failed else R.string.rename_board_failed),
                    modifier = Modifier.padding(top = LayoutConst.SHEET_SPACING_DP.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FormPreview(
    state: BoardFormState,
    creating: Boolean,
    dismissible: Boolean = true,
    asDialog: Boolean = false,
) {
    PreviewSurface {
        val content: @Composable (Modifier) -> Unit = { modifier ->
            BoardFormContent(
                modifier = modifier,
                state = state,
                creating = creating,
                dismissible = dismissible,
                asDialog = asDialog,
                onClose = {},
                onNameChange = {},
                onSubmit = {},
            )
        }
        if (asDialog) PageDialogFrame(content = content) else content(Modifier.fillMaxSize())
    }
}

@ThemePreviews
@Composable
private fun CreateBoardPreview() = FormPreview(BoardFormState(name = "New project"), creating = true)

@ThemePreviews
@Composable
private fun CreateFirstBoardPreview() = FormPreview(BoardFormState(), creating = true, dismissible = false)

@ThemePreviews
@Composable
private fun RenameBoardPreview() = FormPreview(
    BoardFormState(name = "Work", originalName = "Sample Project", saveFailed = true),
    creating = false,
)

@ThemePreviews
@Composable
private fun RenameBoardDialogPreview() = FormPreview(
    BoardFormState(name = "Work", originalName = "Work"),
    creating = false,
    asDialog = true,
)
