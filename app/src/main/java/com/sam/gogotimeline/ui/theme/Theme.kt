package com.sam.gogotimeline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.sam.gogotimeline.core.ColorConst

// Dynamic color is intentionally off so the brand teal and task colors stay fixed.
private val LightColorScheme = lightColorScheme(
    primary = Color(ColorConst.PRIMARY_LIGHT),
    onPrimary = Color(ColorConst.ON_PRIMARY_LIGHT),
    background = Color(ColorConst.BACKGROUND_LIGHT),
    onBackground = Color(ColorConst.TEXT_PRIMARY_LIGHT),
    surface = Color(ColorConst.SURFACE_LIGHT),
    onSurface = Color(ColorConst.TEXT_PRIMARY_LIGHT),
    onSurfaceVariant = Color(ColorConst.TEXT_SECONDARY_LIGHT),
    secondaryContainer = Color(ColorConst.SELECTED_CONTAINER_LIGHT),
    onSecondaryContainer = Color(ColorConst.ON_SELECTED_CONTAINER_LIGHT),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(ColorConst.PRIMARY_DARK),
    onPrimary = Color(ColorConst.ON_PRIMARY_DARK),
    background = Color(ColorConst.BACKGROUND_DARK),
    onBackground = Color(ColorConst.TEXT_PRIMARY_DARK),
    surface = Color(ColorConst.SURFACE_DARK),
    onSurface = Color(ColorConst.TEXT_PRIMARY_DARK),
    onSurfaceVariant = Color(ColorConst.TEXT_SECONDARY_DARK),
    secondaryContainer = Color(ColorConst.SELECTED_CONTAINER_DARK),
    onSecondaryContainer = Color(ColorConst.ON_SELECTED_CONTAINER_DARK),
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
