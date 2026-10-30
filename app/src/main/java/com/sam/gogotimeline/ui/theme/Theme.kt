package com.sam.gogotimeline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Dynamic color is intentionally off so the brand teal and task colors stay fixed.
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    secondaryContainer = SelectedContainerLight,
    onSecondaryContainer = OnSelectedContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    secondaryContainer = SelectedContainerDark,
    onSecondaryContainer = OnSelectedContainerDark,
)

@Composable
fun GogoTimelineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}

/** Shortcut for colors that Material's ColorScheme has no slot for. */
object GogoTimelineTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
