package com.sam.gogotimeline.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme

/** What the read-only sheet shows; [time] is already formatted. */
data class TaskViewState(
    val title: String,
    val colorIndex: Int,
    val time: String,
    val notes: String,
)

/**
 * A locked board's task, read only (spec 9): color dot, name (two lines at most) and × ([onClose]), then the time and
 * notes as plain text. As tall as its content up to [maxHeight]; long notes scroll inside. In a bottom sheet
 * ([inSheet]) it keeps clear of the navigation bar and reports its height in px through [onHeightChange] for the peek
 * height. Text follows the system font size.
 */
@Composable
fun TaskViewSheet(
    modifier: Modifier = Modifier,
    state: TaskViewState,
    maxHeight: Dp,
    onClose: () -> Unit,
    onHeightChange: (Int) -> Unit = {},
    inSheet: Boolean = true,
) {
    val bottomInsets = if (inSheet) WindowInsets.navigationBars.only(WindowInsetsSides.Bottom) else WindowInsets(0)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChange(it.height) }
            .windowInsetsPadding(bottomInsets)
            .heightIn(max = maxHeight)
            .padding(LayoutConst.SHEET_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(LayoutConst.SHEET_SPACING_DP.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LayoutConst.SHEET_SPACING_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(LayoutConst.VIEW_SHEET_COLOR_DOT_DP.dp)
                    .background(GogoTimelineTheme.extendedColors.taskColor(state.colorIndex), CircleShape),
            )
            Text(
                text = state.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = LayoutConst.NAME_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            // The same × as the task editor's.
            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier.size(LayoutConst.SHEET_BUTTON_SIZE_DP.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(LayoutConst.SHEET_BUTTON_ICON_DP.dp),
                )
            }
        }
        Text(
            text = state.time,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.notes.isNotEmpty()) {
            Text(
                text = state.notes,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ViewPreview(state: TaskViewState) {
    PreviewSurface {
        TaskViewSheet(state = state, maxHeight = 320.dp, onClose = {}, inSheet = false)
    }
}

private val previewState = TaskViewState(
    title = "Kickoff meeting",
    colorIndex = 0,
    time = "Fri, Oct 30  09:00 – 10:30",
    notes = "Agenda: goals, roles, timeline",
)

@ThemePreviews
@Composable
private fun TaskViewSheetPreview() = ViewPreview(previewState)

@ThemePreviews
@Composable
private fun TaskViewSheetNoNotesPreview() = ViewPreview(previewState.copy(notes = "", colorIndex = 4))

@ThemePreviews
@Composable
private fun TaskViewSheetLongPreview() = ViewPreview(
    previewState.copy(
        title = "Quarterly planning with design, sales and the whole support team",
        time = "Oct 30 – Nov 13",
        notes = (1..30).joinToString("\n") { "Line $it of the planning notes" },
        colorIndex = 5,
    ),
)
