package com.sam.gogotimeline.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews

/** Shown over an empty board: an icon and a "New task" button, no explanatory sentence (spec 6.6). */
@Composable
internal fun EmptyState(
    modifier: Modifier = Modifier,
    onNewTask: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LayoutConst.EMPTY_STATE_SPACING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_date_range),
            contentDescription = null,
            modifier = Modifier.size(LayoutConst.EMPTY_STATE_ICON_DP.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onNewTask) {
            Text(stringResource(R.string.new_task))
        }
    }
}

@ThemePreviews
@Composable
private fun EmptyStatePreview() {
    PreviewSurface {
        EmptyState(onNewTask = {})
    }
}
