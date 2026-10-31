package com.sam.gogotimeline.ui.timeline

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme

/** Width of the left pane's only column (task names) for phone/tablet and collapsed/expanded. */
fun leftPaneWidth(
    isTablet: Boolean,
    expanded: Boolean,
): Dp = when {
    isTablet && expanded -> LayoutConst.NAME_COLUMN_TABLET_EXPANDED_DP
    isTablet -> LayoutConst.NAME_COLUMN_TABLET_COLLAPSED_DP
    expanded -> LayoutConst.NAME_COLUMN_PHONE_EXPANDED_DP
    else -> LayoutConst.NAME_COLUMN_PHONE_COLLAPSED_DP
}.dp

/**
 * Top-left corner: only the round arrow, centered. The whole area toggles the pane. The arrow points right when
 * collapsed and rotates half a turn to point left when expanded, in step with the pane's width animation.
 */
@Composable
internal fun LeftPaneCorner(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val description = stringResource(if (expanded) R.string.collapse_task_list else R.string.expand_task_list)
    Box(
        modifier = modifier
            .background(colorScheme.surface)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        // Cross-fades in step with the pane's width animation.
        Crossfade(
            targetState = expanded,
            animationSpec = tween(LayoutConst.LEFT_PANE_ANIMATION_MILLIS),
            label = "leftPaneIcon",
        ) { isExpanded ->
            Icon(
                painter = painterResource(
                    if (isExpanded) R.drawable.ic_left_panel_close else R.drawable.ic_left_panel_open,
                ),
                contentDescription = null,
                modifier = Modifier.size(LayoutConst.CORNER_ICON_SIZE_DP.dp),
                tint = colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Full-height background of the left pane, so bars never show through below the last row. It carries the row lines
 * for the blank rows too; [scrollY] is read only while drawing, so scrolling just redraws.
 */
@Composable
internal fun LeftPaneBackground(
    modifier: Modifier = Modifier,
    scrollY: () -> Float,
    rowPx: Float,
) {
    val dividerColor = GogoTimelineTheme.extendedColors.gridMinor
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawRowLines(scrollY(), rowPx, dividerColor)
                val stroke = LayoutConst.GRID_LINE_WIDTH_DP.dp.toPx()
                drawLine(dividerColor, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
            },
    )
}

/** One left-pane row: the task name, up to two lines. Opaque, so bars scrolling left slide underneath it. */
@Composable
internal fun LeftPaneRow(
    modifier: Modifier = Modifier,
    title: String,
    styles: TimelineTextStyles,
    selected: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    val dividerColor = GogoTimelineTheme.extendedColors.gridMinor
    Box(
        modifier = modifier
            .background(if (selected) colorScheme.secondaryContainer else colorScheme.surface)
            .drawBehind {
                val stroke = LayoutConst.GRID_LINE_WIDTH_DP.dp.toPx()
                drawLine(dividerColor, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                drawLine(dividerColor, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
            }
            .padding(horizontal = LayoutConst.LEFT_PANE_PADDING_DP.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        // A new task's empty name shows the same gray placeholder as the sheet's name field.
        Text(
            text = title.ifEmpty { stringResource(R.string.task_name) },
            color = when {
                title.isEmpty() -> colorScheme.onSurfaceVariant
                selected -> colorScheme.onSecondaryContainer
                else -> colorScheme.onSurface
            },
            style = styles.taskName,
            maxLines = LayoutConst.NAME_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LeftPanePreview(expanded: Boolean) {
    PreviewSurface {
        val styles = rememberTimelineTextStyles()
        Column(Modifier.width(leftPaneWidth(isTablet = false, expanded = expanded))) {
            LeftPaneCorner(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((LayoutConst.HEADER_TOP_TIER_HEIGHT_DP + LayoutConst.HEADER_BOTTOM_TIER_HEIGHT_DP).dp),
                expanded = expanded,
                onToggle = {},
            )
            listOf("Design review" to false, "Quarterly planning with the whole product and design team" to true)
                .forEach { (title, selected) ->
                    LeftPaneRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LayoutConst.ROW_HEIGHT_DEFAULT_DP.dp),
                        title = title,
                        styles = styles,
                        selected = selected,
                    )
                }
            Box(Modifier.fillMaxSize())
        }
    }
}

@ThemePreviews
@Composable
private fun LeftPaneCollapsedPreview() = LeftPanePreview(expanded = false)

@ThemePreviews
@Composable
private fun LeftPaneExpandedPreview() = LeftPanePreview(expanded = true)
