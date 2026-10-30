package com.sam.gogotimeline.ui.timeline

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.time.TimeScale
import com.sam.gogotimeline.core.time.TimelineTicks

/** Minor lines at bottom-tier ticks and major lines at top-tier ticks; off-screen lines are skipped. */
internal fun DrawScope.drawGridLines(
    ticks: TimelineTicks,
    scale: TimeScale,
    minorColor: Color,
    majorColor: Color,
) {
    val strokeWidth = LayoutConst.GRID_LINE_WIDTH_DP.dp.toPx()
    ticks.bottom.forEach { drawVerticalLine(scale.timeToX(it.time), minorColor, strokeWidth) }
    ticks.top.forEach { drawVerticalLine(scale.timeToX(it.time), majorColor, strokeWidth) }
}

/**
 * Top tier: left-aligned labels that stick to the left edge until the next label pushes them out.
 * Bottom tier: labels centered in their cell.
 */
internal fun DrawScope.drawHeader(
    ticks: TimelineTicks,
    scale: TimeScale,
    textMeasurer: TextMeasurer,
    topStyle: TextStyle,
    bottomStyle: TextStyle,
    dividerColor: Color,
) {
    val topHeight = LayoutConst.HEADER_TOP_TIER_HEIGHT_DP.dp.toPx()
    val bottomHeight = LayoutConst.HEADER_BOTTOM_TIER_HEIGHT_DP.dp.toPx()
    val padding = LayoutConst.TICK_LABEL_PADDING_DP.dp.toPx()

    ticks.top.forEachIndexed { index, tick ->
        val label = tick.label ?: return@forEachIndexed
        val x = scale.timeToX(tick.time)
        val nextX = ticks.top.getOrNull(index + 1)?.let { scale.timeToX(it.time) } ?: Float.POSITIVE_INFINITY
        if (x > size.width || nextX < 0f) return@forEachIndexed
        val layout = textMeasurer.measure(label, topStyle)
        val labelX = minOf(maxOf(x, 0f) + padding, nextX - padding - layout.size.width)
        drawText(layout, topLeft = Offset(labelX, (topHeight - layout.size.height) / 2))
    }

    ticks.bottom.zipWithNext { tick, next ->
        val label = tick.label ?: return@zipWithNext
        val x = scale.timeToX(tick.time)
        val nextX = scale.timeToX(next.time)
        if (x > size.width || nextX < 0f) return@zipWithNext
        val layout = textMeasurer.measure(label, bottomStyle)
        drawText(
            layout,
            topLeft = Offset((x + nextX - layout.size.width) / 2, topHeight + (bottomHeight - layout.size.height) / 2),
        )
    }

    val strokeWidth = LayoutConst.GRID_LINE_WIDTH_DP.dp.toPx()
    drawLine(dividerColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
}

internal fun DrawScope.drawNowLine(
    x: Float,
    color: Color,
) {
    drawVerticalLine(x, color, LayoutConst.NOW_LINE_WIDTH_DP.dp.toPx())
}

/** Small dot at the bottom of the header, on top of the "now" line. */
internal fun DrawScope.drawNowDot(
    x: Float,
    color: Color,
) {
    if (x < 0f || x > size.width) return
    drawCircle(color, LayoutConst.NOW_DOT_RADIUS_DP.dp.toPx(), Offset(x, size.height))
}

private fun DrawScope.drawVerticalLine(
    x: Float,
    color: Color,
    strokeWidth: Float,
) {
    if (x < 0f || x > size.width) return
    drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth)
}
