package com.sam.gogotimeline.ui.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme

/** Wraps preview content in the app theme with the themed background. */
@Composable
fun PreviewSurface(content: @Composable () -> Unit) {
    GogoTimelineTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
