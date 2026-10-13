package com.vela.notes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.vela.notes.domain.markdown.MarkdownChecklist
import com.vela.notes.ui.theme.VelaSpacing
import com.vela.notes.ui.theme.VelaDarkBackground
import com.vela.notes.ui.theme.VelaPaper
import com.vela.notes.ui.theme.VelaSize
import com.vela.notes.ui.sheet.InsertAction
import com.vela.notes.ui.sheet.VelaInsertSheet
import com.vela.notes.ui.audio.RecorderState
import com.vela.notes.data.audio.AudioPlaybackState
import com.vela.notes.data.audio.PLAYBACK_SPEED_OPTIONS
import androidx.compose.foundation.Canvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorState,
    onAction: (EditorAction) -> Unit,
    onBack: () -> Unit,
    errorMessage: String? = null,
    onInsertAction: (InsertAction) -> Unit = {},
    onRetryMedia: (String) -> Unit = {},
    onMediaAction: (EditorMediaBlock, EditorMediaAction) -> Unit = { _, _ -> },
    recorderState: RecorderState = RecorderState.Idle,
    recordingElapsedSeconds: Long = 0,
    playbackState: AudioPlaybackState = AudioPlaybackState(),
    onToggleAudio: (EditorMediaBlock) -> Unit = {},
    onSeekAudio: (Long) -> Unit = {},
    onAudioSpeedChange: (Float) -> Unit = {},
    onPauseRecording: () -> Unit = {},
    onResumeRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var insertSheetVisible by rememberSaveable { mutableStateOf(false) }
    val editorBackground = if (isSystemInDarkTheme()) VelaDarkBackground else VelaPaper
    BackHandler(onBack = onBack)
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("editor_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Ghi chú") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Quay lại Ghi chú",
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = editorBackground,
                ),
            )
        },
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                RecordingControls(
                    state = recorderState,
                    elapsedSeconds = recordingElapsedSeconds,
                    onPause = onPauseRecording,
                    onResume = onResumeRecording,
                    onStop = onStopRecording,
                )
                FormattingBar(
                    enabled = state.activeBlockId != null,
                    onFormat = { onAction(EditorAction.FormatRequested(it)) },
                    onInsert = { insertSheetVisible = true },
                )
            }
        },
        containerColor = editorBackground,
    ) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val contentGutter = ((maxWidth - 760.dp) / 2).coerceAtLeast(0.dp)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(editorBackground)
                    .padding(horizontal = contentGutter),
                contentPadding = innerPadding,
            ) {
            item {
                Text(
                    text = state.timestamp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = VelaSpacing.Sm),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Xs),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                PlainEditorField(
                    value = state.title,
                    onValueChange = { onAction(EditorAction.TitleChanged(it)) },
                    placeholder = "Tiêu đề",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Md)
                        .testTag(EDITOR_TITLE_TEST_TAG),
                    textStyle = MaterialTheme.typography.headlineMedium,
                    singleLine = false,
                )
            }
            items(state.orderedBlocks, key = EditorBlockItem::id) { block ->
                when (block) {
                    is EditorTextBlock -> MarkdownChecklistTextField(
                        value = block.value,
                        onValueChange = { onAction(EditorAction.TextChanged(block.id, it)) },
                        onToggleChecklist = { lineStart ->
                            onAction(EditorAction.ToggleChecklistItem(block.id, lineStart))
                        },
                        onFocused = { onAction(EditorAction.BlockFocused(block.id)) },
                        testTag = "editor_block_${block.id}",
                        placeholder = "Bắt đầu viết",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Sm),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        singleLine = false,
                    )
                    is EditorMediaBlock -> MediaBlockSurface(
                        block = block,
                        playbackState = playbackState,
                        onToggleAudio = { onToggleAudio(block) },
                        onSeekAudio = onSeekAudio,
                        onSpeedChange = onAudioSpeedChange,
                        onRetry = { onRetryMedia(block.id) },
                        onAction = { onMediaAction(block, it) },
                    )
                }
            }
            }
        }
    }
    VelaInsertSheet(
        visible = insertSheetVisible,
        onDismiss = { insertSheetVisible = false },
        onAction = onInsertAction,
    )
}

@Composable
private fun RecordingControls(
    state: RecorderState,
    elapsedSeconds: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    if (state !is RecorderState.Recording && state !is RecorderState.Paused && state !is RecorderState.Stopping) {
        return
    }
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = VelaSpacing.Lg, vertical = VelaSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(VelaSpacing.Sm),
        ) {
            Text(
                text = when (state) {
                    is RecorderState.Paused -> "Đã tạm dừng ${elapsedSeconds.toAudioTimer()}"
                    is RecorderState.Stopping -> "Đang lưu bản ghi"
                    else -> "Đang ghi ${elapsedSeconds.toAudioTimer()}"
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
            )
            if (state !is RecorderState.Stopping) {
                IconButton(onClick = if (state is RecorderState.Paused) onResume else onPause) {
                    Icon(
                        if (state is RecorderState.Paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = if (state is RecorderState.Paused) "Tiếp tục ghi" else "Tạm dừng ghi",
                    )
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Rounded.Stop, contentDescription = "Dừng và lưu bản ghi")
                }
            }
        }
    }
}

private fun Long.toAudioTimer(): String {
    val safeSeconds = coerceAtLeast(0)
    return "%02d:%02d".format(safeSeconds / 60, safeSeconds % 60)
}

@Composable
private fun PlainEditorField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier,
    textStyle: androidx.compose.ui.text.TextStyle,
    singleLine: Boolean,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onBackground),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = singleLine,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.TopStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = textStyle,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun MarkdownChecklistTextField(
    value: androidx.compose.ui.text.input.TextFieldValue,
    onValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onToggleChecklist: (Int) -> Unit,
    onFocused: () -> Unit,
    testTag: String,
    placeholder: String,
    modifier: Modifier,
    textStyle: androidx.compose.ui.text.TextStyle,
    singleLine: Boolean,
) {
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val checklistItems = remember(value.text) { MarkdownChecklist.items(value.text) }

    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFocused() }
                .testTag(testTag)
                .semantics {
                    customActions = checklistItems.map { item ->
                        val state = if (item.checked) "Bỏ hoàn tất" else "Hoàn tất"
                        CustomAccessibilityAction("$state: ${item.label.ifBlank { "Việc cần làm" }}") {
                            onToggleChecklist(item.lineStart)
                            true
                        }
                    }
                }
                .pointerInput(value.text, textLayout) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                        val layout = textLayout ?: return@awaitEachGesture
                        val offset = layout.getOffsetForPosition(down.position)
                        val item = MarkdownChecklist.itemAtOffset(value.text, offset)
                            ?: return@awaitEachGesture
                        down.consume()
                        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial) ?: return@awaitEachGesture
                        up.consume()
                        onToggleChecklist(item.lineStart)
                    }
                },
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = singleLine,
            onTextLayout = { textLayout = it },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.TopStart) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = textStyle,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun PlainEditorField(
    value: androidx.compose.ui.text.input.TextFieldValue,
    onValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier,
    textStyle: androidx.compose.ui.text.TextStyle,
    singleLine: Boolean,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onBackground),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = singleLine,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.TopStart) {
                if (value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = textStyle,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun FormattingBar(
    enabled: Boolean,
    onFormat: (MarkdownStyle) -> Unit,
    onInsert: () -> Unit,
) {
    val actions = listOf(
        MarkdownStyle.HEADING to (Icons.Rounded.Title to "Tiêu đề Markdown"),
        MarkdownStyle.BOLD to (Icons.Rounded.FormatBold to "In đậm"),
        MarkdownStyle.ITALIC to (Icons.Rounded.FormatItalic to "In nghiêng"),
        MarkdownStyle.BULLET_LIST to (Icons.AutoMirrored.Rounded.FormatListBulleted to "Danh sách"),
        MarkdownStyle.CHECKLIST to (Icons.Rounded.Checklist to "Danh sách kiểm tra"),
        MarkdownStyle.QUOTE to (Icons.Rounded.FormatQuote to "Trích dẫn"),
        MarkdownStyle.CODE to (Icons.Rounded.Code to "Mã"),
    )
    androidx.compose.material3.Surface(
        modifier = Modifier.testTag("editor_formatting_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(VelaSize.Hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = VelaSpacing.Sm, vertical = VelaSpacing.Xs),
        ) {
            FilledTonalIconButton(onClick = onInsert) {
                Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Thêm nội dung")
            }
            actions.forEach { (style, visual) ->
                IconButton(
                    onClick = { onFormat(style) },
                    enabled = enabled,
                ) {
                    Icon(visual.first, contentDescription = visual.second)
                }
            }
        }
    }
}

@Composable
private fun MediaBlockSurface(
    block: EditorMediaBlock,
    playbackState: AudioPlaybackState,
    onToggleAudio: () -> Unit,
    onSeekAudio: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onRetry: () -> Unit,
    onAction: (EditorMediaAction) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val icon = when (block.type) {
        com.vela.notes.domain.model.NoteBlockType.IMAGE -> Icons.Rounded.Image
        com.vela.notes.domain.model.NoteBlockType.SCAN,
        com.vela.notes.domain.model.NoteBlockType.FILE,
        -> Icons.Rounded.Description
        com.vela.notes.domain.model.NoteBlockType.AUDIO -> Icons.Rounded.AudioFile
        com.vela.notes.domain.model.NoteBlockType.TEXT -> Icons.Rounded.Description
    }
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Sm)
            .testTag("editor_media_${block.id}"),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(VelaSize.Hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(VelaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(VelaSpacing.Md),
        ) {
            val isActiveAudio = block.type == com.vela.notes.domain.model.NoteBlockType.AUDIO &&
                playbackState.attachmentId == block.attachmentId
            if (block.type == com.vela.notes.domain.model.NoteBlockType.AUDIO && block.status == EditorMediaStatus.READY) {
                IconButton(onClick = onToggleAudio) {
                    Icon(
                        imageVector = if (isActiveAudio && playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isActiveAudio && playbackState.isPlaying) {
                            "Tạm dừng ${block.displayName}"
                        } else {
                            "Phát ${block.displayName}"
                        },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Icon(
                    imageVector = if (block.status == EditorMediaStatus.MISSING) Icons.Rounded.BrokenImage else icon,
                    contentDescription = if (block.type == com.vela.notes.domain.model.NoteBlockType.SCAN) {
                        "Ảnh bìa bản quét"
                    } else {
                        null
                    },
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(block.displayName, style = MaterialTheme.typography.titleSmall)
                val detail = when {
                    block.type == com.vela.notes.domain.model.NoteBlockType.SCAN &&
                        block.status == EditorMediaStatus.READY -> {
                        val recognition = if (block.ocrText.isNullOrBlank()) "Chưa nhận dạng" else "Đã nhận dạng"
                        "${block.pageCount ?: 1} trang · $recognition"
                    }
                    block.status == EditorMediaStatus.READY -> block.mimeType.orEmpty()
                    block.status == EditorMediaStatus.PROCESSING -> "Đang xử lý"
                    block.status == EditorMediaStatus.FAILED -> "Xử lý không thành công"
                    else -> "Không tìm thấy tệp"
                }
                if (detail.isNotEmpty()) {
                    Text(
                        detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (block.type == com.vela.notes.domain.model.NoteBlockType.AUDIO && block.status == EditorMediaStatus.READY) {
                    AudioPlaybackControls(
                        waveform = block.waveform ?: byteArrayOf(),
                        positionMs = if (isActiveAudio) playbackState.positionMs else 0,
                        durationMs = if (isActiveAudio && playbackState.durationMs > 0) {
                            playbackState.durationMs
                        } else {
                            block.durationMs ?: 0
                        },
                        speed = playbackState.speed,
                        enabled = isActiveAudio,
                        onSeek = onSeekAudio,
                        onSpeedChange = onSpeedChange,
                    )
                }
            }
            if (block.status == EditorMediaStatus.MISSING || block.status == EditorMediaStatus.FAILED) {
                androidx.compose.material3.TextButton(onClick = onRetry) { Text("Thử lại") }
            } else {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Tùy chọn ${block.displayName}")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        EditorMediaAction.entries.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.label) },
                                onClick = {
                                    menuExpanded = false
                                    onAction(action)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPlaybackControls(
    waveform: ByteArray,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }
    val safeDuration = durationMs.coerceAtLeast(0)
    Waveform(
        peaks = waveform,
        progress = if (safeDuration > 0) positionMs.toFloat() / safeDuration else 0f,
    )
    Slider(
        modifier = Modifier.semantics { contentDescription = "Vị trí phát" },
        value = positionMs.coerceIn(0, safeDuration).toFloat(),
        onValueChange = { onSeek(it.toLong()) },
        valueRange = 0f..safeDuration.coerceAtLeast(1).toFloat(),
        enabled = enabled && safeDuration > 0,
    )
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(
            "${positionMs.toPlaybackTimer()} / ${safeDuration.toPlaybackTimer()}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Box {
            TextButton(
                onClick = { speedMenuExpanded = true },
                enabled = enabled,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = "Tốc độ phát" },
            ) { Text("${speed.formatSpeed()}x") }
            DropdownMenu(
                expanded = speedMenuExpanded,
                onDismissRequest = { speedMenuExpanded = false },
            ) {
                PLAYBACK_SPEED_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("${option.formatSpeed()}x") },
                        onClick = {
                            speedMenuExpanded = false
                            onSpeedChange(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Waveform(peaks: ByteArray, progress: Float) {
    val playedColor = MaterialTheme.colorScheme.primary
    val remainingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(top = VelaSpacing.Xs)
            .semantics { contentDescription = "Dạng sóng âm" },
    ) {
        if (peaks.isEmpty()) return@Canvas
        val step = size.width / peaks.size
        val safeProgress = progress.coerceIn(0f, 1f)
        peaks.forEachIndexed { index, value ->
            val amplitude = (value.toInt().coerceIn(0, 100) / 100f).coerceAtLeast(0.08f)
            val halfHeight = size.height * amplitude / 2f
            val x = step * (index + 0.5f)
            drawLine(
                color = if (index.toFloat() / peaks.size <= safeProgress) playedColor else remainingColor,
                start = androidx.compose.ui.geometry.Offset(x, size.height / 2f - halfHeight),
                end = androidx.compose.ui.geometry.Offset(x, size.height / 2f + halfHeight),
                strokeWidth = minOf(3.dp.toPx(), step * 0.55f),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun Long.toPlaybackTimer(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun Float.formatSpeed(): String = if (this % 1f == 0f) toInt().toString() else toString()

enum class EditorMediaAction(val label: String) {
    OPEN("Mở"),
    SHARE("Chia sẻ"),
    REMOVE("Xóa khỏi ghi chú"),
}

const val EDITOR_TITLE_TEST_TAG = "editor_title"
