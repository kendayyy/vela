package com.vela.notes.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.ui.theme.VelaAmberOnLight
import com.vela.notes.ui.theme.VelaAudioIconContainer
import com.vela.notes.ui.theme.VelaFileIconContainer
import com.vela.notes.ui.theme.VelaImageIconContainer
import com.vela.notes.ui.theme.VelaScanIconContainer
import com.vela.notes.ui.theme.VelaSize
import com.vela.notes.ui.theme.VelaSpacing
import com.vela.notes.ui.theme.VelaTeal
import com.vela.notes.ui.theme.VelaTextIconContainer
import com.vela.notes.ui.theme.ensureTouchTarget
import kotlinx.coroutines.launch

enum class NotesCollection { ALL, PINNED, FOLDERS, DELETED }

private enum class NoteSort(val label: String) {
    UPDATED_DESC("Mới cập nhật"),
    UPDATED_ASC("Cũ nhất"),
    TITLE_ASC("Tên A-Z"),
}

private enum class ContentFilter(val label: String, val type: NoteBlockType?) {
    ALL("Tất cả nội dung", null),
    TEXT("Văn bản", NoteBlockType.TEXT),
    IMAGE("Ảnh", NoteBlockType.IMAGE),
    SCAN("Tài liệu quét", NoteBlockType.SCAN),
    AUDIO("Âm thanh", NoteBlockType.AUDIO),
    FILE("Tệp", NoteBlockType.FILE),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    title: String,
    notes: List<NoteRowUi>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCreateNote: () -> Unit,
    modifier: Modifier = Modifier,
    onUndo: (String) -> Unit = {},
    isTrash: Boolean = false,
    onRestore: (String) -> Unit = {},
    onDeletePermanently: (String) -> Unit = {},
    onEmptyTrash: () -> Unit = {},
    selectedCollection: NotesCollection = if (isTrash) NotesCollection.DELETED else NotesCollection.ALL,
    showBackButton: Boolean = true,
    onCollectionSelected: (NotesCollection) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    searchAlreadyApplied: Boolean = false,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingPermanentDelete by remember { mutableStateOf<String?>(null) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(NoteSort.UPDATED_DESC) }
    var contentFilter by remember { mutableStateOf(ContentFilter.ALL) }
    val visibleNotes = remember(notes, query, sort, contentFilter, searchAlreadyApplied) {
        notes.asSequence()
            .filter { note ->
                searchAlreadyApplied || query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.preview.contains(query, ignoreCase = true)
            }
            .filter { note -> contentFilter.type == null || note.primaryContentType == contentFilter.type }
            .let { sequence ->
                when (sort) {
                    NoteSort.UPDATED_DESC -> sequence
                    NoteSort.UPDATED_ASC -> sequence.toList().asReversed().asSequence()
                    NoteSort.TITLE_ASC -> sequence.sortedBy { it.title.lowercase() }
                }
            }
            .toList()
    }

    BoxWithConstraints(modifier) {
        val useLargeTitle = maxHeight >= 480.dp
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                NotesTopBar(title, showBackButton, useLargeTitle, onBack, onOpenSettings, scrollBehavior)
            },
            bottomBar = {
                if (isTrash) {
                    TrashBottomBar(notes.isNotEmpty()) { confirmEmptyTrash = true }
                } else {
                    NotesBottomBar(onCreateNote)
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(VelaSpacing.Md),
            ) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.fillMaxWidth().widthIn(max = 760.dp)) {
                            DashboardControls(
                                query = query,
                                onQueryChange = onQueryChange,
                                selectedCollection = selectedCollection,
                                onCollectionSelected = onCollectionSelected,
                                onShowFilters = { showFilters = true },
                                hasActiveFilter = contentFilter != ContentFilter.ALL || sort != NoteSort.UPDATED_DESC,
                            )
                        }
                    }
                }
                if (visibleNotes.isEmpty()) {
                    item { EmptyNotes(query.isNotBlank()) }
                } else {
                    items(visibleNotes, key = NoteRowUi::id) { note ->
                        Box(
                            Modifier.fillMaxWidth().padding(horizontal = VelaSpacing.Xl),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(Modifier.fillMaxWidth().widthIn(max = 720.dp)) {
                            if (isTrash) {
                                TrashNoteRow(
                                    note = note,
                                    onRestore = { onRestore(note.id) },
                                    onDeletePermanently = { pendingPermanentDelete = note.id },
                                )
                            } else {
                                SwipeableNoteRow(
                                    note = note,
                                    onClick = { onNoteClick(note.id) },
                                    onArchive = { onArchive(note.id) },
                                    onDelete = {
                                        onDelete(note.id)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Đã chuyển vào Đã xóa",
                                                actionLabel = "Hoàn tác",
                                            )
                                            if (result == SnackbarResult.ActionPerformed) onUndo(note.id)
                                        }
                                    },
                                )
                            }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.size(VelaSpacing.Sm)) }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            sort = sort,
            contentFilter = contentFilter,
            onSortChange = { sort = it },
            onContentFilterChange = { contentFilter = it },
            onDismiss = { showFilters = false },
        )
    }
    pendingPermanentDelete?.let { noteId ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDelete = null },
            title = { Text("Xóa vĩnh viễn ghi chú này?") },
            text = { Text("Không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePermanently(noteId)
                    pendingPermanentDelete = null
                }) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { pendingPermanentDelete = null }) { Text("Hủy") } },
        )
    }
    if (confirmEmptyTrash) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTrash = false },
            title = { Text("Dọn thùng rác?") },
            text = { Text("Mọi ghi chú trong thùng rác sẽ bị xóa vĩnh viễn.") },
            confirmButton = {
                TextButton(onClick = {
                    onEmptyTrash()
                    confirmEmptyTrash = false
                }) { Text("Dọn") }
            },
            dismissButton = { TextButton(onClick = { confirmEmptyTrash = false }) { Text("Hủy") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesTopBar(
    title: String,
    showBackButton: Boolean,
    useLargeTitle: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
) {
    val navigationIcon: @Composable () -> Unit = {
        if (showBackButton) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại Thư mục")
            }
        }
    }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
    )
    val actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.semantics { contentDescription = "Mở Cài đặt" },
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = null)
        }
    }
    if (useLargeTitle) {
        LargeTopAppBar(
            title = { Text(title, style = MaterialTheme.typography.headlineLarge) },
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
            colors = colors,
        )
    } else {
        TopAppBar(
            title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
            colors = colors,
        )
    }
}

@Composable
private fun DashboardControls(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCollection: NotesCollection,
    onCollectionSelected: (NotesCollection) -> Unit,
    onShowFilters: () -> Unit,
    hasActiveFilter: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VelaSpacing.Md)) {
        NoteSearchField(query, onQueryChange, onShowFilters, hasActiveFilter)
        LazyRow(
            contentPadding = PaddingValues(horizontal = VelaSpacing.Xl),
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm),
        ) {
            items(NotesCollection.entries) { collection ->
                val label = when (collection) {
                    NotesCollection.ALL -> "Tất cả"
                    NotesCollection.PINNED -> "Đã ghim"
                    NotesCollection.FOLDERS -> "Thư mục"
                    NotesCollection.DELETED -> "Đã xóa"
                }
                FilterChip(
                    selected = selectedCollection == collection,
                    onClick = { onCollectionSelected(collection) },
                    label = { Text(label) },
                    modifier = Modifier.semantics { selected = selectedCollection == collection },
                    colors = velaFilterChipColors(),
                )
            }
        }
    }
}

@Composable
private fun NoteSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onShowFilters: () -> Unit,
    hasActiveFilter: Boolean,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = VelaSpacing.Xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm),
    ) {
        Surface(
            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(VelaSize.Hairline, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = VelaSpacing.Md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm),
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused }
                        .semantics { contentDescription = "Tìm kiếm ghi chú" },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Tìm kiếm...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
        FilledTonalIconButton(
            onClick = onShowFilters,
            modifier = Modifier.size(52.dp).semantics { contentDescription = "Lọc và sắp xếp ghi chú" },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription = null,
                tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (isFocused || query.isNotEmpty()) {
            TextButton(
                onClick = {
                    onQueryChange("")
                    focusManager.clearFocus()
                },
                modifier = Modifier.ensureTouchTarget().testTag("search_cancel"),
            ) { Text("Hủy") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    sort: NoteSort,
    contentFilter: ContentFilter,
    onSortChange: (NoteSort) -> Unit,
    onContentFilterChange: (ContentFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(VelaSpacing.Md),
        ) {
            Text("Lọc và sắp xếp", style = MaterialTheme.typography.titleLarge)
            Text("Sắp xếp", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm)) {
                items(NoteSort.entries) { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { onSortChange(option) },
                        label = { Text(option.label) },
                        colors = velaFilterChipColors(),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Loại nội dung", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(
                modifier = Modifier.testTag("content_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm),
            ) {
                items(ContentFilter.entries) { option ->
                    FilterChip(
                        selected = contentFilter == option,
                        onClick = { onContentFilterChange(option) },
                        label = { Text(option.label) },
                        colors = velaFilterChipColors(),
                    )
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = VelaSize.MinimumTouchTarget),
            ) { Text("Xong") }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun velaFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNoteRow(
    note: NoteRowUi,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.settledValue) {
        when (dismissState.settledValue) {
            SwipeToDismissBoxValue.StartToEnd -> onArchive()
            SwipeToDismissBoxValue.EndToStart -> onDelete()
            SwipeToDismissBoxValue.Settled -> return@LaunchedEffect
        }
        dismissState.reset()
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.testTag("note_row_${note.id}"),
        backgroundContent = {
            val deleting = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (deleting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = VelaSpacing.Xl),
                contentAlignment = if (deleting) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Icon(
                    if (deleting) Icons.Rounded.DeleteOutline else Icons.Rounded.Archive,
                    contentDescription = if (deleting) "Xóa" else "Lưu trữ",
                    tint = if (deleting) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary,
                )
            }
        },
    ) {
        NoteCard(note, onClick, onArchive, onDelete)
    }
}

@Composable
private fun NoteCard(
    note: NoteRowUi,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(note.title)
                    if (note.isPinned) append(", đã ghim")
                    note.todoProgress?.let { progress ->
                        append(", ${progress.completed}/${progress.total} việc hoàn tất")
                    }
                    append(", ")
                    append(note.timestamp)
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(VelaSize.Hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = VelaSpacing.Lg, top = VelaSpacing.Md, bottom = VelaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Md),
        ) {
            ContentTypeIcon(note.primaryContentType)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = null,
                            modifier = Modifier.padding(end = VelaSpacing.Xs).size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(
                        note.title.ifBlank { "Ghi chú mới" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    note.preview,
                    modifier = Modifier.padding(top = VelaSpacing.Xs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                note.todoProgress?.let { progress ->
                    val allCompleted = progress.completed == progress.total
                    Text(
                        text = "${progress.completed}/${progress.total} việc hoàn tất",
                        modifier = Modifier
                            .padding(top = VelaSpacing.Xs)
                            .testTag("note_todo_progress_${note.id}"),
                        color = if (allCompleted) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    note.timestamp,
                    modifier = Modifier.padding(top = VelaSpacing.Xs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.semantics { contentDescription = "Tùy chọn ${note.title}" },
                ) { Icon(Icons.Rounded.MoreVert, contentDescription = null) }
                DropdownMenu(menuExpanded, { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Lưu trữ") },
                        leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onArchive()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa") },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentTypeIcon(type: NoteBlockType) {
    val darkTheme = isSystemInDarkTheme()
    val (icon, lightContainer, lightTint) = when (type) {
        NoteBlockType.TEXT -> Triple(Icons.Rounded.Description, VelaTextIconContainer, MaterialTheme.colorScheme.primary)
        NoteBlockType.IMAGE -> Triple(Icons.Rounded.Image, VelaImageIconContainer, Color(0xFFB52F68))
        NoteBlockType.SCAN -> Triple(Icons.Rounded.Description, VelaScanIconContainer, VelaAmberOnLight)
        NoteBlockType.AUDIO -> Triple(Icons.Rounded.AudioFile, VelaAudioIconContainer, Color(0xFF2868C7))
        NoteBlockType.FILE -> Triple(Icons.AutoMirrored.Rounded.InsertDriveFile, VelaFileIconContainer, VelaTeal)
    }
    val container = if (darkTheme) lightTint.copy(alpha = 0.22f) else lightContainer
    val tint = if (darkTheme) lightTint.copy(alpha = 0.95f) else lightTint
    Surface(shape = CircleShape, color = container, modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = type.accessibilityLabel(), tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

private fun NoteBlockType.accessibilityLabel(): String = when (this) {
    NoteBlockType.TEXT -> "Ghi chú văn bản"
    NoteBlockType.IMAGE -> "Ghi chú có ảnh"
    NoteBlockType.SCAN -> "Ghi chú có tài liệu quét"
    NoteBlockType.AUDIO -> "Ghi chú có âm thanh"
    NoteBlockType.FILE -> "Ghi chú có tệp"
}

@Composable
private fun TrashNoteRow(note: NoteRowUi, onRestore: () -> Unit, onDeletePermanently: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("trash_row_${note.id}"),
        verticalArrangement = Arrangement.spacedBy(VelaSpacing.Xs),
    ) {
        NoteCard(note, onRestore, onRestore, onDeletePermanently)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onRestore, Modifier.heightIn(min = VelaSize.MinimumTouchTarget)) { Text("Khôi phục") }
            TextButton(onDeletePermanently, Modifier.heightIn(min = VelaSize.MinimumTouchTarget)) {
                Text("Xóa vĩnh viễn")
            }
        }
    }
}

@Composable
private fun EmptyNotes(searching: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VelaSpacing.Xs),
    ) {
        Text(if (searching) "Không tìm thấy ghi chú" else "Chưa có ghi chú", style = MaterialTheme.typography.titleMedium)
        Text(
            if (searching) "Thử từ khóa hoặc bộ lọc khác" else "Chạm Ghi chú mới để bắt đầu",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun NotesBottomBar(onCreateNote: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Md),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onCreateNote,
            modifier = Modifier.widthIn(min = 176.dp).heightIn(min = 52.dp).testTag(CREATE_NOTE_TEST_TAG),
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(Icons.Rounded.EditNote, contentDescription = "Tạo ghi chú")
            Spacer(Modifier.size(VelaSpacing.Sm))
            Text("Ghi chú mới")
        }
    }
}

@Composable
private fun TrashBottomBar(enabled: Boolean, onEmptyTrash: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(min = 56.dp)
                .padding(horizontal = VelaSpacing.Xl),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onEmptyTrash, Modifier.heightIn(min = VelaSize.MinimumTouchTarget), enabled) {
                Text("Dọn thùng rác")
            }
        }
    }
}

const val CREATE_NOTE_TEST_TAG = "create_note"
