package com.sam.gogotimeline.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.core.LayoutConst

/** Shared tablet frame for Settings and board-name pages. */
@Composable
fun PageDialogFrame(
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val height = minOf(maxHeight * LayoutConst.DIALOG_HEIGHT_FRACTION, LayoutConst.DIALOG_MAX_HEIGHT_DP.dp)
        Surface(
            modifier = Modifier.size(LayoutConst.DIALOG_WIDTH_DP.dp, height),
            shape = RoundedCornerShape(LayoutConst.DIALOG_CORNER_RADIUS_DP.dp),
        ) {
            content(Modifier.fillMaxSize())
        }
    }
}
