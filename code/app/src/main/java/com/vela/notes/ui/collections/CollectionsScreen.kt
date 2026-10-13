package com.vela.notes.ui.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vela.notes.ui.theme.VelaSize
import com.vela.notes.ui.theme.VelaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    collections: List<CollectionRowUi>,
    folders: List<FolderRowUi>,
    onCollectionClick: (CollectionKind) -> Unit,
    onFolderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                modifier = Modifier.testTag(COLLECTIONS_TOP_BAR_TEST_TAG),
                title = { Text("Thư mục") },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            item {
                CollectionSection(title = "Ghi chú", itemCount = collections.size) {
                    collections.forEachIndexed { index, collection ->
                        CollectionRow(
                            item = collection,
                            showDivider = index < collections.lastIndex,
                            onClick = { onCollectionClick(collection.kind) },
                        )
                    }
                }
            }

            if (folders.isNotEmpty()) {
                item {
                    CollectionSection(title = "Thư mục", itemCount = folders.size) {
                        folders.forEachIndexed { index, folder ->
                            FolderRow(
                                item = folder,
                                showDivider = index < folders.lastIndex,
                                onClick = { onFolderClick(folder.id) },
                            )
                        }
                    }
                }
            }

            item { Box(Modifier.navigationBarsPadding()) }
        }
    }
}

const val COLLECTIONS_TOP_BAR_TEST_TAG = "collections_top_bar"

@Composable
private fun CollectionSection(
    title: String,
    itemCount: Int,
    content: @Composable () -> Unit,
) {
    if (itemCount == 0) return
    Column(
        modifier = Modifier.padding(
            start = VelaSpacing.Lg,
            top = VelaSpacing.Xl,
            end = VelaSpacing.Lg,
            bottom = VelaSpacing.Sm,
        ),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(start = VelaSpacing.Xs, bottom = VelaSpacing.Sm),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(content = { content() })
        }
    }
}

@Composable
private fun CollectionRow(
    item: CollectionRowUi,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    GroupedRow(
        title = item.title,
        count = item.count,
        icon = item.icon,
        iconTint = when (item.kind) {
            CollectionKind.ALL -> MaterialTheme.colorScheme.primary
            CollectionKind.PINNED -> MaterialTheme.colorScheme.secondary
            CollectionKind.DELETED -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        showDivider = showDivider,
        onClick = onClick,
    )
}

@Composable
private fun FolderRow(
    item: FolderRowUi,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    GroupedRow(
        title = item.name,
        count = item.count,
        icon = Icons.Rounded.Folder,
        iconTint = MaterialTheme.colorScheme.primary,
        showDivider = showDivider,
        onClick = onClick,
    )
}

@Composable
private fun GroupedRow(
    title: String,
    count: Int,
    icon: ImageVector,
    iconTint: Color,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title, $count ghi chú"
                }
                .padding(horizontal = VelaSpacing.Md, vertical = VelaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Md),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(VelaSize.FolderIcon),
                tint = iconTint,
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = count.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(VelaSize.Icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                thickness = VelaSize.Hairline,
                color = DividerDefaults.color.copy(alpha = 0.7f),
            )
        }
    }
}
