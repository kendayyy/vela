package com.vela.notes.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.vela.notes.domain.markdown.MarkdownChecklist
import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.domain.model.NoteDocument

data class EditorState(
    val noteId: String,
    val title: String,
    val timestamp: String,
    val textBlocks: List<EditorTextBlock>,
    val mediaBlocks: List<EditorMediaBlock> = emptyList(),
    val activeBlockId: String? = null,
) {
    companion object {
        fun from(
            document: NoteDocument,
            formatTimestamp: (Long) -> String,
        ): EditorState = EditorState(
            noteId = document.note.id,
            title = document.note.title,
            timestamp = formatTimestamp(document.note.updatedAt),
            textBlocks = document.blocks
                .asSequence()
                .filter { it.type == NoteBlockType.TEXT }
                .sortedBy { it.position }
                .map { block ->
                    EditorTextBlock(
                        id = block.id,
                        value = TextFieldValue(
                            text = block.textContent.orEmpty(),
                            selection = TextRange(block.textContent.orEmpty().length),
                        ),
                        position = block.position,
                    )
                }
                .toList(),
            mediaBlocks = document.blocks
                .asSequence()
                .filter { it.type != NoteBlockType.TEXT }
                .sortedBy { it.position }
                .map { block ->
                    val attachment = document.attachments.firstOrNull { it.id == block.attachmentId }
                    EditorMediaBlock(
                        id = block.id,
                        type = block.type,
                        position = block.position,
                        displayName = attachment?.displayName ?: defaultMediaLabel(block.type),
                        mimeType = attachment?.mimeType,
                        sizeBytes = attachment?.sizeBytes,
                        durationMs = attachment?.durationMs,
                        waveform = attachment?.waveform,
                        pageCount = attachment?.pageCount,
                        ocrText = attachment?.ocrText,
                        attachmentId = attachment?.id,
                        relativePath = attachment?.relativePath,
                        status = if (attachment == null) EditorMediaStatus.MISSING else EditorMediaStatus.READY,
                    )
                }
                .toList(),
        )
    }

    val orderedBlocks: List<EditorBlockItem>
        get() = (textBlocks + mediaBlocks).sortedWith(
            compareBy<EditorBlockItem> { it.position }.thenBy { it.id },
        )
}

data class EditorTextBlock(
    override val id: String,
    val value: TextFieldValue,
    override val position: Long,
) : EditorBlockItem {
    val text: String get() = value.text
}

sealed interface EditorBlockItem {
    val id: String
    val position: Long
}

data class EditorMediaBlock(
    override val id: String,
    val type: NoteBlockType,
    override val position: Long,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val durationMs: Long? = null,
    val waveform: ByteArray? = null,
    val pageCount: Int? = null,
    val ocrText: String? = null,
    val attachmentId: String? = null,
    val relativePath: String? = null,
    val status: EditorMediaStatus,
) : EditorBlockItem

enum class EditorMediaStatus {
    READY,
    MISSING,
    PROCESSING,
    FAILED,
}

sealed interface EditorAction {
    data class TitleChanged(val title: String) : EditorAction
    data class TextChanged(val blockId: String, val value: TextFieldValue) : EditorAction
    data class ToggleChecklistItem(val blockId: String, val lineStart: Int) : EditorAction
    data class BlockFocused(val blockId: String) : EditorAction
    data class FormatRequested(val style: MarkdownStyle) : EditorAction
    data class SplitBlock(val blockId: String, val newBlockId: String) : EditorAction
    data class MergeWithPrevious(val blockId: String) : EditorAction
    data class DeleteBlock(val blockId: String) : EditorAction
}

fun EditorState.reduce(action: EditorAction): EditorState = when (action) {
    is EditorAction.TitleChanged -> copy(title = action.title)
    is EditorAction.TextChanged -> copy(
        textBlocks = textBlocks.map { block ->
            if (block.id == action.blockId) block.copy(value = action.value) else block
        },
    )
    is EditorAction.ToggleChecklistItem -> copy(
        textBlocks = textBlocks.map { block ->
            if (block.id == action.blockId) {
                block.copy(value = MarkdownChecklist.toggle(block.value, action.lineStart))
            } else {
                block
            }
        },
    )
    is EditorAction.BlockFocused -> copy(activeBlockId = action.blockId)
    is EditorAction.FormatRequested -> copy(
        textBlocks = textBlocks.map { block ->
            if (block.id == activeBlockId) {
                block.copy(value = MarkdownFormatter.apply(block.value, action.style))
            } else {
                block
            }
        },
    )
    is EditorAction.SplitBlock -> splitBlock(action.blockId, action.newBlockId)
    is EditorAction.MergeWithPrevious -> mergeWithPrevious(action.blockId)
    is EditorAction.DeleteBlock -> deleteBlock(action.blockId)
}

private fun EditorState.splitBlock(blockId: String, newBlockId: String): EditorState {
    if (textBlocks.any { it.id == newBlockId }) return this
    val index = textBlocks.indexOfFirst { it.id == blockId }
    if (index < 0) return this

    val block = textBlocks[index]
    val selectionStart = block.value.selection.min.coerceIn(0, block.text.length)
    val selectionEnd = block.value.selection.max.coerceIn(selectionStart, block.text.length)
    val left = block.text.substring(0, selectionStart)
    val right = block.text.substring(selectionEnd)
    val updatedBlocks = textBlocks.toMutableList().apply {
        this[index] = block.copy(value = TextFieldValue(left, TextRange(left.length)))
        add(
            index + 1,
            EditorTextBlock(
                id = newBlockId,
                value = TextFieldValue(right, TextRange.Zero),
                position = 0,
            ),
        )
    }
    return copy(
        textBlocks = updatedBlocks.withNormalizedPositions(),
        activeBlockId = newBlockId,
    )
}

private fun EditorState.mergeWithPrevious(blockId: String): EditorState {
    val index = textBlocks.indexOfFirst { it.id == blockId }
    if (index <= 0) return this

    val previous = textBlocks[index - 1]
    val current = textBlocks[index]
    val joinOffset = previous.text.length
    val merged = previous.copy(
        value = TextFieldValue(
            text = previous.text + current.text,
            selection = TextRange(joinOffset),
        ),
    )
    val updatedBlocks = textBlocks.toMutableList().apply {
        this[index - 1] = merged
        removeAt(index)
    }
    return copy(
        textBlocks = updatedBlocks.withNormalizedPositions(),
        activeBlockId = previous.id,
    )
}

private fun EditorState.deleteBlock(blockId: String): EditorState {
    val index = textBlocks.indexOfFirst { it.id == blockId }
    if (index < 0) return this
    if (textBlocks.size == 1) {
        val onlyBlock = textBlocks.single()
        return copy(
            textBlocks = listOf(onlyBlock.copy(value = TextFieldValue(""))),
            activeBlockId = onlyBlock.id,
        )
    }

    val updatedBlocks = textBlocks.toMutableList().apply { removeAt(index) }
    val nextActiveIndex = (index - 1).coerceAtLeast(0).coerceAtMost(updatedBlocks.lastIndex)
    val nextActive = updatedBlocks[nextActiveIndex]
    val blocksWithCursor = updatedBlocks.mapIndexed { blockIndex, block ->
        if (blockIndex == nextActiveIndex) {
            block.copy(value = block.value.copy(selection = TextRange(block.text.length)))
        } else {
            block
        }
    }
    return copy(
        textBlocks = blocksWithCursor.withNormalizedPositions(),
        activeBlockId = nextActive.id,
    )
}

private fun List<EditorTextBlock>.withNormalizedPositions(): List<EditorTextBlock> =
    mapIndexed { index, block -> block.copy(position = index * BLOCK_POSITION_STEP) }

private const val BLOCK_POSITION_STEP = 1_024L

private fun defaultMediaLabel(type: NoteBlockType): String = when (type) {
    NoteBlockType.IMAGE -> "Ảnh"
    NoteBlockType.SCAN -> "Tài liệu đã quét"
    NoteBlockType.AUDIO -> "Bản ghi âm"
    NoteBlockType.FILE -> "Tệp đính kèm"
    NoteBlockType.TEXT -> "Nội dung"
}

fun EditorState.toDocument(original: NoteDocument): NoteDocument {
    require(noteId == original.note.id) { "Editor state belongs to a different note" }
    val originalTextBlocks = original.blocks
        .filter { it.type == NoteBlockType.TEXT }
        .associateBy { it.id }
    val fallbackCreatedAt = original.note.updatedAt
    val updatedTextBlocks = textBlocks.map { editorBlock ->
        val source = originalTextBlocks[editorBlock.id]
        com.vela.notes.domain.model.NoteBlock(
            id = editorBlock.id,
            noteId = noteId,
            type = NoteBlockType.TEXT,
            position = editorBlock.position,
            textContent = editorBlock.text,
            createdAt = source?.createdAt ?: fallbackCreatedAt,
            updatedAt = source?.updatedAt ?: fallbackCreatedAt,
        )
    }
    return original.copy(
        note = original.note.copy(title = title),
        blocks = (original.blocks.filter { it.type != NoteBlockType.TEXT } + updatedTextBlocks)
            .sortedBy { it.position },
    )
}
