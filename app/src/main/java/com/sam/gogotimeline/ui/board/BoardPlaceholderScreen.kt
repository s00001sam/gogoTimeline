package com.sam.gogotimeline.ui.board

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews

/**
 * Temporary empty screen for step 1, used to confirm the app launches with the theme applied.
 * Replaced by the scheduler in step 3.
 */
@Composable
fun BoardPlaceholderScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { _ -> }
}

@ThemePreviews
@Composable
private fun BoardPlaceholderScreenPreview() {
    PreviewSurface {
        BoardPlaceholderScreen()
    }
}
