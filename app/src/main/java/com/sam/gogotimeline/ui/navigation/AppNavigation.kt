package com.sam.gogotimeline.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.sam.gogotimeline.ui.board.BoardScreen
import com.sam.gogotimeline.ui.isTabletLayout
import com.sam.gogotimeline.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

// Routes are serializable so the back stack survives the app being killed in the background.
@Serializable
data object BoardRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

/** The board, and Settings on top of it: a full page on phones, a centered dialog on tablets (spec 8). */
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(BoardRoute)
    val tablet = isTabletLayout()
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategies = listOf(DialogSceneStrategy(), SinglePaneSceneStrategy()),
        entryProvider = entryProvider {
            entry<BoardRoute> {
                BoardScreen(onOpenSettings = { backStack.add(SettingsRoute) })
            }
            entry<SettingsRoute>(
                metadata = if (tablet) {
                    DialogSceneStrategy.dialog(DialogProperties(usePlatformDefaultWidth = false))
                } else {
                    emptyMap()
                },
            ) {
                SettingsScreen(asDialog = tablet, onClose = { backStack.removeLastOrNull() })
            }
        },
    )
}
