package com.sam.gogotimeline.ui.board

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.BuildConfig
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.data.model.Board
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import kotlinx.coroutines.launch

/**
 * The board list around [content] (spec 9). Tablets: always shown on the left with a divider. Phones: a modal drawer
 * opened only by ☰ ([content]'s `openDrawer`); edge swipes stay off so they never fight the scheduler's scrolling.
 */
@Composable
fun BoardDrawerLayout(
    modifier: Modifier = Modifier,
    boards: List<Board>,
    currentBoardId: Long,
    tablet: Boolean,
    onBoardSelected: (Long) -> Unit,
    onNewBoard: () -> Unit,
    onRenameBoard: (Long) -> Unit,
    onDeleteBoard: (Long) -> Unit,
    content: @Composable (openDrawer: () -> Unit, drawerOpen: Boolean) -> Unit,
) {
    if (tablet) {
        Row(modifier.fillMaxSize()) {
            BoardDrawerContent(
                modifier = Modifier
                    .width(LayoutConst.DRAWER_WIDTH_DP.dp)
                    .fillMaxHeight(),
                boards = boards,
                currentBoardId = currentBoardId,
                onBoardSelected = { id -> if (id != currentBoardId) onBoardSelected(id) },
                onNewBoard = onNewBoard,
                onRenameBoard = onRenameBoard,
                onDeleteBoard = onDeleteBoard,
            )
            VerticalDivider()
            Box(Modifier.weight(1f)) { content({}, false) }
        }
        return
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var drawerActive by remember { mutableStateOf(drawerState.currentValue != DrawerValue.Closed) }
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue to drawerState.targetValue }.collect { (current, target) ->
            drawerActive = current != DrawerValue.Closed || target != DrawerValue.Closed
        }
    }
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        // Closed: no edge swipe, which would fight the timeline. Open: scrim taps and swipe-to-close work.
        gesturesEnabled = drawerActive,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(LayoutConst.DRAWER_WIDTH_DP.dp),
                windowInsets = WindowInsets(0),
            ) {
                BoardDrawerContent(
                    modifier = Modifier.fillMaxSize(),
                    boards = boards,
                    currentBoardId = currentBoardId,
                    onBoardSelected = { id ->
                        scope.launch { drawerState.close() }
                        if (id != currentBoardId) onBoardSelected(id)
                    },
                    onNewBoard = {
                        scope.launch { drawerState.close() }
                        onNewBoard()
                    },
                    onRenameBoard = onRenameBoard,
                    onDeleteBoard = onDeleteBoard,
                )
            }
        },
    ) {
        content(
            {
                drawerActive = true
                scope.launch { drawerState.open() }
            },
            drawerActive,
        )
    }
    // Registered after the drawer content so it wins over the root NavDisplay back handler.
    BackHandler(enabled = drawerActive) { scope.launch { drawerState.close() } }
}

/**
 * App name, the pill-shaped New board button, the board list (the only part that scrolls; opens on the current board)
 * and the version at the bottom. A long press on a board offers Rename and Delete.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoardDrawerContent(
    modifier: Modifier = Modifier,
    boards: List<Board>,
    currentBoardId: Long,
    onBoardSelected: (Long) -> Unit,
    onNewBoard: () -> Unit,
    onRenameBoard: (Long) -> Unit,
    onDeleteBoard: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentBoardId, boards) {
        val index = boards.indexOfFirst { it.id == currentBoardId }
        if (index >= 0) listState.scrollToItem(index)
    }
    Column(
        modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(LayoutConst.DRAWER_PADDING_DP.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Button(
            onClick = onNewBoard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LayoutConst.DRAWER_PADDING_DP.dp),
            shape = CircleShape,
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            Text(
                text = stringResource(R.string.new_board),
                modifier = Modifier.padding(start = LayoutConst.SHEET_SPACING_DP.dp),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LayoutConst.DRAWER_ITEM_SPACING_DP.dp),
        ) {
            items(boards, key = { it.id }) { board ->
                BoardItem(
                    board = board,
                    current = board.id == currentBoardId,
                    onClick = { onBoardSelected(board.id) },
                    onRename = { onRenameBoard(board.id) },
                    onDelete = { onDeleteBoard(board.id) },
                )
            }
        }
        Text(
            text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** One board, light teal when current; up to two lines. Long press vibrates and opens Rename / Delete beside it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoardItem(
    board: Board,
    current: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                ),
            shape = RoundedCornerShape(LayoutConst.SHEET_CHIP_CORNER_DP.dp),
            color = if (current) colorScheme.secondaryContainer else colorScheme.surface,
            contentColor = if (current) colorScheme.onSecondaryContainer else colorScheme.onSurface,
        ) {
            Text(
                text = board.name,
                modifier = Modifier.padding(LayoutConst.SHEET_SPACING_DP.dp),
                maxLines = LayoutConst.NAME_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename)) },
                onClick = {
                    menuOpen = false
                    onRename()
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
            )
        }
    }
}

@Composable
private fun DrawerPreview(boards: List<Board>) {
    PreviewSurface {
        BoardDrawerContent(
            modifier = Modifier
                .width(LayoutConst.DRAWER_WIDTH_DP.dp)
                .fillMaxHeight(),
            boards = boards,
            currentBoardId = boards.first().id,
            onBoardSelected = {},
            onNewBoard = {},
            onRenameBoard = {},
            onDeleteBoard = {},
        )
    }
}

@ThemePreviews
@Composable
private fun BoardDrawerFewPreview() = DrawerPreview(
    listOf(
        Board(2, "Work", null, 2),
        Board(1, "Personal Project for the second half of the year", null, 1),
    ),
)

@ThemePreviews
@Composable
private fun BoardDrawerManyPreview() = DrawerPreview(
    (1L..20L).map {
        Board(
            it,
            if (it % 3 ==
                0L
            ) {
                "Quarterly planning with a fairly long name $it"
            } else {
                "Board $it"
            },
            null,
            it,
        )
    },
)
