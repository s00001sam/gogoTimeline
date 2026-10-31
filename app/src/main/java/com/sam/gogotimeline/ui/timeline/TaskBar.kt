package com.sam.gogotimeline.ui.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme

/**
 * One task bar with its time label on a translucent backing. The label always shows, overflowing the bar
 * when it does not fit, and sticks to the visible left edge when the bar starts off screen. When the whole
 * bar is off screen, the label stays at the left edge of the time axis with ‹ (earlier) or › (later).
 * The selected task shows an outline and no label.
 */
@Composable
internal fun TaskBar(
    modifier: Modifier = Modifier,
    task: TimelineTask,
    text: String,
    selected: Boolean,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    barLeftPx: () -> Float,
    axisWidthPx: () -> Float,
) {
    val extendedColors = GogoTimelineTheme.extendedColors
    val textColor = MaterialTheme.colorScheme.onSurface
    val description = "${task.title}, $text"
    Box(
        modifier = modifier
            .testTag("taskBar-${task.id}")
            .semantics { contentDescription = description }
            .drawBehind {
                val corner = CornerRadius(LayoutConst.BAR_CORNER_RADIUS_DP.dp.toPx())
                drawRoundRect(extendedColors.taskColor(task.colorIndex), cornerRadius = corner)
                if (selected) {
                    val stroke = LayoutConst.SELECTION_OUTLINE_WIDTH_DP.dp.toPx()
                    drawRoundRect(
                        color = extendedColors.selectionOutline,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = corner,
                        style = Stroke(stroke),
                    )
                    // Handles straddle both edges, half inside and half outside the bar.
                    val handleSize = Size(
                        LayoutConst.HANDLE_VISUAL_WIDTH_DP.dp.toPx(),
                        LayoutConst.HANDLE_VISUAL_HEIGHT_DP.dp.toPx(),
                    )
                    val handleTop = (size.height - handleSize.height) / 2
                    listOf(0f, size.width).forEach { edge ->
                        drawRoundRect(
                            color = extendedColors.selectionOutline,
                            topLeft = Offset(edge - handleSize.width / 2, handleTop),
                            size = handleSize,
                            cornerRadius = CornerRadius(handleSize.width / 2),
                        )
                    }
                    return@drawBehind
                }
                val barLeft = barLeftPx()
                val side = barLabelSide(barLeft, size.width, axisWidthPx())
                val layout = textMeasurer.measure(barLabelText(text, side), style)
                val paddingX = LayoutConst.BAR_LABEL_PADDING_HORIZONTAL_DP.dp.toPx()
                val paddingY = LayoutConst.BAR_LABEL_PADDING_VERTICAL_DP.dp.toPx()
                val labelSize = Size(layout.size.width + 2 * paddingX, layout.size.height + 2 * paddingY)
                val labelTopLeft = Offset(
                    barLabelAxisLeft(barLeft, side, LayoutConst.TICK_LABEL_PADDING_DP.dp.toPx()) - barLeft,
                    (size.height - labelSize.height) / 2,
                )
                drawRoundRect(
                    color = extendedColors.barLabelBackground,
                    topLeft = labelTopLeft,
                    size = labelSize,
                    cornerRadius = CornerRadius(LayoutConst.BAR_LABEL_CORNER_DP.dp.toPx()),
                )
                drawText(layout, textColor, labelTopLeft + Offset(paddingX, paddingY))
            },
    )
}

/** Where a bar sits relative to the visible time axis, which decides where its label goes. */
internal enum class BarLabelSide {
    /** Any part of the bar is visible: the label sits in the bar, sticking to the left edge. */
    VISIBLE,

    /** The whole bar is left of the view: the label stays at the left edge with ‹. */
    EARLIER,

    /** The whole bar is right of the view: the label stays at the left edge with ›. */
    LATER,
}

internal fun barLabelSide(
    barLeft: Float,
    barWidth: Float,
    axisWidth: Float,
): BarLabelSide = when {
    barLeft + barWidth <= 0f -> BarLabelSide.EARLIER
    barLeft >= axisWidth -> BarLabelSide.LATER
    else -> BarLabelSide.VISIBLE
}

/** The label text, with ‹ or › when the whole bar is off screen. */
internal fun barLabelText(
    text: String,
    side: BarLabelSide,
): String = when (side) {
    BarLabelSide.EARLIER -> "${LayoutConst.BAR_LABEL_EARLIER_PREFIX}$text"
    BarLabelSide.LATER -> "${LayoutConst.BAR_LABEL_LATER_PREFIX}$text"
    BarLabelSide.VISIBLE -> text
}

/**
 * Left edge of the label (its backing) in px from the time axis' left edge: sticks to the visible part of a
 * visible bar, or to the axis' left edge when the bar is off screen.
 */
internal fun barLabelAxisLeft(
    barLeft: Float,
    side: BarLabelSide,
    gap: Float,
): Float = (if (side == BarLabelSide.VISIBLE) maxOf(barLeft, 0f) else 0f) + gap

@ThemePreviews
@Composable
private fun TaskBarPreview() {
    PreviewSurface {
        val textMeasurer = rememberTextMeasurer()
        val style = rememberTimelineTextStyles().barText
        val barHeight = (LayoutConst.ROW_HEIGHT_DEFAULT_DP - 2 * LayoutConst.BAR_VERTICAL_PADDING_DP).dp
        Column(Modifier.padding(16.dp)) {
            listOf(160.dp to false, 16.dp to false, 160.dp to true).forEachIndexed { index, (width, selected) ->
                TaskBar(
                    modifier = Modifier.width(width).height(barHeight),
                    task = TimelineTask(index.toLong(), "Design review", 0L, 0L, index + 2, isAllDay = false),
                    text = "Oct 30 09:00 – Oct 30 10:30",
                    selected = selected,
                    textMeasurer = textMeasurer,
                    style = style,
                    barLeftPx = { 0f },
                    axisWidthPx = { Float.MAX_VALUE },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
