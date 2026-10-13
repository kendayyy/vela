package com.vela.notes.data.repository

import com.vela.notes.data.local.entity.BlockType
import com.vela.notes.data.local.entity.NoteBlockEntity
import com.vela.notes.data.local.entity.NoteEntity
import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.domain.model.TodoProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteSummaryMapperTest {
    private val note = NoteEntity(id = "note", title = "Title", createdAt = 1, updatedAt = 2)

    @Test
    fun summaryUsesFirstMeaningfulBlockAndFirstTextPreview() {
        val summary = note.toSummary(
            listOf(
                block("empty", BlockType.TEXT, 0, "  "),
                block("image", BlockType.IMAGE, 1),
                block("text", BlockType.TEXT, 2, "  hello\n  Vela  "),
            ),
        )

        assertEquals(NoteBlockType.IMAGE, summary.primaryContentType)
        assertEquals("hello Vela", summary.preview)
    }

    @Test
    fun summaryFallsBackToAttachmentLabel() {
        val summary = note.toSummary(listOf(block("audio", BlockType.AUDIO, 0)))

        assertEquals(NoteBlockType.AUDIO, summary.primaryContentType)
        assertEquals("Bản ghi âm", summary.preview)
    }

    @Test
    fun emptyNoteUsesTextDefaults() {
        val summary = note.toSummary(emptyList())

        assertEquals(NoteBlockType.TEXT, summary.primaryContentType)
        assertEquals("Chưa có nội dung", summary.preview)
        assertEquals(null, summary.todoProgress)
    }

    @Test
    fun summaryCountsChecklistAcrossTextBlocks() {
        val summary = note.toSummary(
            listOf(
                block("first", BlockType.TEXT, 0, "- [ ] Mua tài liệu\n- [x] Gửi email"),
                block("second", BlockType.TEXT, 1, "Nội dung\n- [X] Đã xong"),
            ),
        )

        assertEquals(TodoProgress(total = 3, completed = 2), summary.todoProgress)
    }

    @Test
    fun summaryDoesNotCountInlineOrOrdinaryBullets() {
        val summary = note.toSummary(
            listOf(block("text", BlockType.TEXT, 0, "- việc thường\ntext - [ ] inline")),
        )

        assertEquals(null, summary.todoProgress)
    }

    private fun block(
        id: String,
        type: BlockType,
        position: Long,
        text: String? = null,
    ) = NoteBlockEntity(
        id = id,
        noteId = note.id,
        type = type,
        position = position,
        textContent = text,
        createdAt = 1,
        updatedAt = 1,
    )
}
