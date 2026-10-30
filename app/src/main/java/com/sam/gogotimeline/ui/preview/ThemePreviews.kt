package com.sam.gogotimeline.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders a preview in light/dark × phone/tablet.
 * Apply to the stateless content composable of every screen and component.
 */
@Preview(name = "Phone - Light", device = "spec:width=411dp,height=891dp", showBackground = true)
@Preview(
    name = "Phone - Dark",
    device = "spec:width=411dp,height=891dp",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Preview(name = "Tablet - Light", device = "spec:width=1280dp,height=800dp,dpi=240", showBackground = true)
@Preview(
    name = "Tablet - Dark",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
annotation class ThemePreviews
