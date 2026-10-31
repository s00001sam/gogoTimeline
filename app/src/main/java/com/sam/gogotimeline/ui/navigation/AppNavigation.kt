package com.sam.gogotimeline.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.sam.gogotimeline.ui.board.BlankBoardScreen
import com.sam.gogotimeline.ui.board.BoardAction
import com.sam.gogotimeline.ui.board.BoardDrawerLayout
import com.sam.gogotimeline.ui.board.BoardFormScreen
import com.sam.gogotimeline.ui.board.BoardScreen
import com.sam.gogotimeline.ui.board.BoardViewModel
import com.sam.gogotimeline.ui.board.DeleteBoardDialog
import com.sam.gogotimeline.ui.isTabletLayout
import com.sam.gogotimeline.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

// Routes are serializable so the back stack survives the app being killed in the background.
@Serializable
data object StartRoute : NavKey

@Serializable
data class BoardRoute(
    val boardId: Long?,
    val openNewTask: Boolean = false,
) : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data class CreateBoardRoute(val dismissible: Boolean) : NavKey

@Serializable
data class RenameBoardRoute(val boardId: Long) : NavKey

/**
 * Routes (spec 5.3): the most recently opened board, or a blank board under a Create page that cannot be dismissed
 * when there is none. Settings, Create and Rename open over the board: full pages on phones, dialogs on tablets.
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel(),
) {
    val boards by viewModel.boards.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(StartRoute)
    val tablet = isTabletLayout()
    LaunchedEffect(boards) {
        val loaded = boards ?: return@LaunchedEffect
        if (backStack.singleOrNull() == StartRoute) {
            backStack.clear()
            val boardId = loaded.firstOrNull()?.id
            backStack.add(BoardRoute(boardId))
            if (boardId == null) backStack.add(CreateBoardRoute(dismissible = false))
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.boardDeleted.collect { result ->
            val current = backStack.filterIsInstance<BoardRoute>().lastOrNull()
            if (current?.boardId == result.boardId) {
                backStack.clear()
                backStack.add(BoardRoute(result.nextBoardId))
                if (result.nextBoardId == null) backStack.add(CreateBoardRoute(dismissible = false))
            }
        }
    }
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
            entry<StartRoute> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            entry<BoardRoute>(clazzContentKey = { route -> "board-${route.boardId}" }) { route ->
                if (route.boardId == null) {
                    BlankBoardScreen()
                } else {
                    val boardViewModel: BoardViewModel = hiltViewModel()
                    val performAction: (BoardAction) -> Unit = { action ->
                        when (action) {
                            BoardAction.OpenSettings -> backStack.add(SettingsRoute)

                            BoardAction.CreateBoard -> backStack.add(CreateBoardRoute(dismissible = true))

                            BoardAction.RenameBoard -> backStack.add(RenameBoardRoute(route.boardId))

                            BoardAction.DeleteBoard -> viewModel.requestDelete(route.boardId)

                            is BoardAction.SwitchBoard -> {
                                backStack[backStack.lastIndex] = BoardRoute(action.boardId)
                            }
                        }
                    }
                    BoardDrawerLayout(
                        boards = boards.orEmpty(),
                        currentBoardId = route.boardId,
                        tablet = tablet,
                        onBoardSelected = { boardViewModel.onAppAction(BoardAction.SwitchBoard(it)) },
                        onNewBoard = { boardViewModel.onAppAction(BoardAction.CreateBoard) },
                        onRenameBoard = { id ->
                            if (id == route.boardId) {
                                boardViewModel.onAppAction(BoardAction.RenameBoard)
                            } else {
                                backStack.add(RenameBoardRoute(id))
                            }
                        },
                        onDeleteBoard = { id ->
                            if (id == route.boardId) {
                                boardViewModel.onAppAction(BoardAction.DeleteBoard)
                            } else {
                                viewModel.requestDelete(id)
                            }
                        },
                    ) { openDrawer, drawerOpen ->
                        BoardScreen(
                            boardId = route.boardId,
                            openNewTask = route.openNewTask,
                            onNewTaskOpened = {
                                backStack[backStack.lastIndex] = route.copy(openNewTask = false)
                            },
                            onAppAction = performAction,
                            showNavigation = !tablet,
                            onOpenDrawer = openDrawer,
                            interactionBlocked = drawerOpen,
                            viewModel = boardViewModel,
                        )
                    }
                }
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
            entry<CreateBoardRoute>(
                metadata = { route ->
                    if (tablet) {
                        DialogSceneStrategy.dialog(
                            DialogProperties(
                                dismissOnBackPress = route.dismissible,
                                dismissOnClickOutside = route.dismissible,
                                usePlatformDefaultWidth = false,
                            ),
                        )
                    } else {
                        emptyMap()
                    }
                },
            ) { route ->
                BoardFormScreen(
                    board = null,
                    dismissible = route.dismissible,
                    asDialog = tablet,
                    onClose = { backStack.removeLastOrNull() },
                    onCreated = { id ->
                        backStack.clear()
                        backStack.add(BoardRoute(id, openNewTask = true))
                    },
                    onRenamed = {},
                )
            }
            entry<RenameBoardRoute>(
                metadata = if (tablet) {
                    DialogSceneStrategy.dialog(DialogProperties(usePlatformDefaultWidth = false))
                } else {
                    emptyMap()
                },
            ) { route ->
                val board = boards?.firstOrNull { it.id == route.boardId }
                if (board != null) {
                    BoardFormScreen(
                        board = board,
                        dismissible = true,
                        asDialog = tablet,
                        onClose = { backStack.removeLastOrNull() },
                        onCreated = {},
                        onRenamed = { backStack.removeLastOrNull() },
                    )
                }
            }
        },
    )
    deleteState?.let { state ->
        DeleteBoardDialog(state, onDelete = viewModel::confirmDelete, onCancel = viewModel::dismissDelete)
    }
}
