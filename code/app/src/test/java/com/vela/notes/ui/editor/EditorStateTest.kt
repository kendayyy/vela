package com.vela.notes.ui.editor

import com.vela.notes.domain.model.Note
import com.vela.notes.domain.model.NoteBlock
import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.domain.model.NoteDocument
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorStateTest {
    @Test
    fun fromDocumentKeepsOnlyTextBlocksInPositionOrder() {
        val state = EditorState.from(
            document = document(
                block("second", position = 20, text = "B"),
                block("media", position = 10, text = null, type = NoteBlockType.IMAGE),
                block("first", position = 0, text = "A"),
            ),
            formatTimestamp = { "Đã sửa lúc $it" },
        )

        assertEquals(listOf("first", "second"), state.textBlocks.map(EditorTextBlock::id))
        assertEquals(listOf("A", "B"), state.textBlocks.map(EditorTextBlock::text))
        assertEquals("Đã sửa lúc 200", state.timestamp)
    }

    @Test
    fun reducerUpdatesTitleWithoutChangingBlocks() {
        val original = EditorState.from(document(block("one", 0, "Body"))) { it.toString() }

        val updated = original.reduce(EditorAction.TitleChanged("Vela"))

        assertEquals("Vela", updated.title)
        assertEquals(original.textBlocks, updated.textBlocks)
    }

    @Test
    fun reducerUpdatesOnlyMatchingBlock() {
        val original = EditorState.from(
            document(block("one", 0, "A"), block("two", 10, "B")),
        ) { it.toString() }

        val updated = original.reduce(EditorAction.TextChanged("two", TextFieldValue("Changed")))

        assertEquals(listOf("A", "Changed"), updated.textBlocks.map(EditorTextBlock::text))
        assertEquals(listOf(0L, 10L), updated.textBlocks.map(EditorTextBlock::position))
    }

    @Test
    fun reducerTogglesOnlyRequestedChecklistLine() {
        val original = EditorState.from(
            document(block("one", 0, "- [ ] First\n- [x] Second")),
        ) { it.toString() }

        val updated = original.reduce(EditorAction.ToggleChecklistItem("one", lineStart = 0))

        assertEquals("- [x] First\n- [x] Second", updated.textBlocks.single().text)
    }

    @Test
    fun reducerIgnoresUnknownBlockId() {
        val original = EditorState.from(document(block("one", 0, "A"))) { it.toString() }

        assertEquals(original, original.reduce(EditorAction.TextChanged("missing", TextFieldValue("Changed"))))
    }

    @Test
    fun formattingAppliesToFocusedBlockSelection() {
        val original = EditorState.from(document(block("one", 0, "Vela"))) { it.toString() }
        val selected = original.reduce(
            EditorAction.TextChanged("one", TextFieldValue("Vela", TextRange(0, 4))),
        ).reduce(EditorAction.BlockFocused("one"))

        val updated = selected.reduce(EditorAction.FormatRequested(MarkdownStyle.BOLD))

        assertEquals("**Vela**", updated.textBlocks.single().text)
        assertEquals(TextRange(2, 6), updated.textBlocks.single().value.selection)
    }

    @Test
    fun splitBlockDeletesSelectionAndMovesTrailingTextToNewBlock() {
        val original = EditorState.from(document(block("one", 50, "Hello Vela"))) { it.toString() }
            .reduce(
                EditorAction.TextChanged(
                    "one",
                    TextFieldValue("Hello Vela", TextRange(5, 6)),
                ),
            )

        val updated = original.reduce(EditorAction.SplitBlock("one", "two"))

        assertEquals(listOf("Hello", "Vela"), updated.textBlocks.map(EditorTextBlock::text))
        assertEquals(listOf(0L, 1_024L), updated.textBlocks.map(EditorTextBlock::position))
        assertEquals(TextRange(5), updated.textBlocks[0].value.selection)
        assertEquals(TextRange.Zero, updated.textBlocks[1].value.selection)
        assertEquals("two", updated.activeBlockId)
    }

    @Test
    fun splitBlockRejectsDuplicateIdWithoutChangingState() {
        val original = EditorState.from(
            document(block("one", 0, "A"), block("two", 10, "B")),
        ) { it.toString() }

        assertEquals(original, original.reduce(EditorAction.SplitBlock("one", "two")))
    }

    @Test
    fun mergeWithPreviousAppendsTextAndPlacesCursorAtJoin() {
        val original = EditorState.from(
            document(block("one", 9, "Hello "), block("two", 99, "Vela")),
        ) { it.toString() }

        val updated = original.reduce(EditorAction.MergeWithPrevious("two"))

        assertEquals(listOf("Hello Vela"), updated.textBlocks.map(EditorTextBlock::text))
        assertEquals(listOf(0L), updated.textBlocks.map(EditorTextBlock::position))
        assertEquals(TextRange(6), updated.textBlocks.single().value.selection)
        assertEquals("one", updated.activeBlockId)
    }

    @Test
    fun mergeFirstBlockIsNoOp() {
        val original = EditorState.from(document(block("one", 0, "A"))) { it.toString() }

        assertEquals(original, original.reduce(EditorAction.MergeWithPrevious("one")))
    }

    @Test
    fun deleteBlockFocusesPreviousAndNormalizesPositions() {
        val original = EditorState.from(
            document(
                block("one", 10, "A"),
                block("two", 20, "B"),
                block("three", 30, "C"),
            ),
        ) { it.toString() }

        val updated = original.reduce(EditorAction.DeleteBlock("two"))

        assertEquals(listOf("one", "three"), updated.textBlocks.map(EditorTextBlock::id))
        assertEquals(listOf(0L, 1_024L), updated.textBlocks.map(EditorTextBlock::position))
        assertEquals("one", updated.activeBlockId)
        assertEquals(TextRange(1), updated.textBlocks.first().value.selection)
    }

    @Test
    fun deletingOnlyBlockKeepsOneEmptyEditableBlock() {
        val original = EditorState.from(document(block("one", 0, "Body"))) { it.toString() }

        val updated = original.reduce(EditorAction.DeleteBlock("one"))

        assertEquals(1, updated.textBlocks.size)
        assertEquals("", updated.textBlocks.single().text)
        assertEquals("one", updated.activeBlockId)
    }

    @Test
    fun toDocumentReplacesTextBlocksAndPreservesMedia() {
        val media = block("image", 512, null, NoteBlockType.IMAGE)
        val source = document(block("one", 0, "Old"), media)
        val editor = EditorState.from(source) { it.toString() }
            .reduce(EditorAction.TitleChanged("Updated"))
            .reduce(EditorAction.TextChanged("one", TextFieldValue("New", TextRange(3))))
            .reduce(EditorAction.SplitBlock("one", "two"))

        val result = editor.toDocument(source)

        assertEquals("Updated", result.note.title)
        assertEquals(listOf("one", "image", "two"), result.blocks.map { it.id })
        assertEquals("New", result.blocks.first().textContent)
        assertEquals(NoteBlockType.IMAGE, result.blocks[1].type)
    }

    private fun document(vararg blocks: NoteBlock) = NoteDocument(
        note = Note(
            id = "note",
            title = "Title",
            createdAt = 100,
            updatedAt = 200,
        ),
        blocks = blocks.toList(),
        attachments = emptyList(),
    )

    private fun block(
        id: String,
        position: Long,
        text: String?,
        type: NoteBlockType = NoteBlockType.TEXT,
    ) = NoteBlock(
        id = id,
        noteId = "note",
        type = type,
        position = position,
        textContent = text,
        createdAt = 100,
        updatedAt = 200,
    )
}
