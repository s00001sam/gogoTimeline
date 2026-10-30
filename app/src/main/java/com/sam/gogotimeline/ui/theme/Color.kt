package com.sam.gogotimeline.ui.theme

import androidx.compose.ui.graphics.Color

// Primary (PicCollage teal)
val PrimaryLight = Color(0xFF50C2C4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryDark = Color(0xFF3AA9AB)
val OnPrimaryDark = Color(0xFF000000)

// Backgrounds and surfaces
val BackgroundLight = Color(0xFFFAFAFA)
val BackgroundDark = Color(0xFF212121)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF2A2A2A)

// Text
val TextPrimaryLight = Color(0xFF1F1F1F)
val TextPrimaryDark = Color(0xFFECECEC)
val TextSecondaryLight = Color(0xFF5F6368)
val TextSecondaryDark = Color(0xFFA0A0A0)

// Selected tab / list item
val SelectedContainerLight = Color(0xFFD9EDF0)
val OnSelectedContainerLight = Color(0xFF00696B)
val SelectedContainerDark = Color(0xFF1F3E40)
val OnSelectedContainerDark = Color(0xFF7FD3D5)

// Selected task outline
val SelectionOutlineLight = Color(0xFF000000)
val SelectionOutlineDark = Color(0xFFFFFFFF)

// "Now" line
val NowLineLight = Color(0xFF00696B)
val NowLineDark = Color(0xFF7FD3D5)

// Grid lines (tentative)
val GridMinorLight = Color(0xFFE6E6E6)
val GridMinorDark = Color(0xFF333333)
val GridMajorLight = Color(0xFFCFCFCF)
val GridMajorDark = Color(0xFF4A4A4A)

/**
 * Task colors (tentative), indexed by the color index stored in the database:
 * red, orange, yellow, green, blue, indigo, violet. Light-mode colors are dark enough for white text.
 */
val TaskColorsLight = listOf(
    Color(0xFFD32F2F),
    Color(0xFFBF360C),
    Color(0xFF8F6A00),
    Color(0xFF2E7D32),
    Color(0xFF1976D2),
    Color(0xFF3F51B5),
    Color(0xFF9C27B0),
)

val TaskColorsDark = listOf(
    Color(0xFFEF5350),
    Color(0xFFFF9800),
    Color(0xFFFDD835),
    Color(0xFF66BB6A),
    Color(0xFF42A5F5),
    Color(0xFF7986CB),
    Color(0xFFBA68C8),
)

// 80% opaque backing behind the time text inside bars, so the primary text color reads on any task color.
val BarLabelBackgroundLight = Color(0xCCFFFFFF)
val BarLabelBackgroundDark = Color(0xCC2A2A2A)
