package com.sam.gogotimeline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sam.gogotimeline.ui.board.BoardPlaceholderScreen
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme
import dagger.hilt.android.AndroidEntryPoint

/** The app's only activity. Edge-to-edge: content must handle system bar insets itself. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GogoTimelineTheme {
                BoardPlaceholderScreen()
            }
        }
    }
}
