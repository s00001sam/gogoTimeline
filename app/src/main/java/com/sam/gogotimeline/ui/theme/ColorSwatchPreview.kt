package com.sam.gogotimeline.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews

/** Shows the 7 task colors with sample bar text and the "now" line on the themed background to check contrast. */
@ThemePreviews
@Composable
private fun TaskColorSwatchPreview() {
    PreviewSurface {
        val colors = GogoTimelineTheme.extendedColors
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(colors.taskColors.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 24.dp)
                            .background(
                                colors.taskColor(index),
                                RoundedCornerShape(LayoutConst.BAR_CORNER_RADIUS_DP.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "09:00",
                            color = colors.textOnTask,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(48.dp)
                    .background(colors.nowLine),
            )
        }
    }
}
