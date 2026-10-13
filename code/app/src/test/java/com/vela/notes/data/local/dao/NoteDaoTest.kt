package com.vela.notes.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vela.notes.data.local.VelaDatabase
import com.vela.notes.data.local.entity.AttachmentEntity
import com.vela.notes.data.local.entity.AttachmentKind
import com.vela.notes.data.local.entity.BlockType
import com.vela.notes.data.local.entity.NoteBlockEntity
import com.vela.notes.data.local.entity.NoteEntity
import com.vela.notes.data.local.model.NoteDetail
import com.vela.notes.data.local.search.FtsQueryBuilder
import com.vela.notes.data.local.search.NoteSearchDocumentBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoteDaoTest {
    private lateinit var database: VelaDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VelaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun noteDetail_returnsBlocksInPositionOrder() = runTest {
        val note = note(id = "note-1", updatedAt = 10)
        val attachment = attachment(noteId = note.id)
        val blocks = listOf(
            block(id = "block-2", noteId = note.id, position = 2, attachmentId = attachment.id),
            block(id = "block-1", noteId = note.id, position = 1, text = "Hello"),
        )

        dao.upsertNoteDetail(NoteDetail(note, blocks, listOf(attachment)))

        val detail = dao.getNoteDetail(note.id)!!
        assertEquals(listOf("block-1", "block-2"), detail.blocks.map { it.id })
        assertEquals(attachment.id, detail.attachments.single().id)
    }

    @Test
    fun activeNotes_arePinnedThenUpdated_andExcludeArchivedOrDeleted() = runTest {
        dao.upsertNote(note("regular-new", updatedAt = 30))
        dao.upsertNote(note("pinned-old", updatedAt = 10, isPinned = true))
        dao.upsertNote(note("regular-old", updatedAt = 20))
        dao.upsertNote(note("archived", updatedAt = 40, isArchived = true))
        dao.upsertNote(note("deleted", updatedAt = 50, isDeleted = true))

        val ids = dao.observeActiveNotes().first().map { it.id }

        assertEquals(listOf("pinned-old", "regular-new", "regular-old"), ids)
    }

    @Test
    fun permanentDelete_cascadesBlocksAndAttachments() = runTest {
        val note = note("note-1", updatedAt = 10)
        val attachment = attachment(note.id)
        dao.upsertNoteDetail(
            NoteDetail(
                note = note,
                blocks = listOf(block("block-1", note.id, 1, attachmentId = attachment.id)),
                attachments = listOf(attachment),
            ),
        )

        dao.deletePermanently(note.id)

        assertNull(dao.getNoteDetail(note.id))
        assertEquals(emptyList<NoteBlockEntity>(), dao.observeBlocks(note.id).first())
        assertEquals(emptyList<AttachmentEntity>(), dao.observeAttachments(note.id).first())
    }

    @Test
    fun searchIndex_findsTitleMarkdownAndOcr_thenReplacesOldContent() = runTest {
        val note = note("note-1", updatedAt = 10).copy(title = "Ke hoach Vela")
        val scan = attachment(note.id).copy(
            displayName = "hoa-don.pdf",
            kind = AttachmentKind.SCAN_PDF,
            ocrText = "Tong thanh toan 250000 dong",
        )
        val detail = NoteDetail(
            note = note,
            blocks = listOf(block("block-1", note.id, 1, text = "Danh sach cong viec")),
            attachments = listOf(scan),
        )
        dao.upsertNoteDetail(detail)
        dao.replaceSearchIndex(note.id, NoteSearchDocumentBuilder.build(detail))

        assertEquals(listOf(note.id), dao.searchNoteIds(FtsQueryBuilder.build("Vela")!!))
        assertEquals(listOf(note.id), dao.searchNoteIds(FtsQueryBuilder.build("cong viec")!!))
        assertEquals(listOf(note.id), dao.searchNoteIds(FtsQueryBuilder.build("250000")!!))

        val updated = detail.copy(note = note.copy(title = "Noi dung moi"), blocks = emptyList())
        dao.replaceSearchIndex(note.id, NoteSearchDocumentBuilder.build(updated))

        assertEquals(emptyList<String>(), dao.searchNoteIds(FtsQueryBuilder.build("Vela")!!))
        assertEquals(listOf(note.id), dao.searchNoteIds(FtsQueryBuilder.build("Noi")!!))
    }

    private fun note(
        id: String,
        updatedAt: Long,
        isPinned: Boolean = false,
        isArchived: Boolean = false,
        isDeleted: Boolean = false,
    ) = NoteEntity(
        id = id,
        title = id,
        createdAt = 1,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isArchived = isArchived,
        isDeleted = isDeleted,
    )

    private fun attachment(noteId: String) = AttachmentEntity(
        id = "attachment-1",
        noteId = noteId,
        kind = AttachmentKind.IMAGE,
        relativePath = "$noteId/attachment-1.jpg",
        displayName = "photo.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 100,
        checksum = "checksum",
        createdAt = 2,
    )

    private fun block(
        id: String,
        noteId: String,
        position: Long,
        text: String? = null,
        attachmentId: String? = null,
    ) = NoteBlockEntity(
        id = id,
        noteId = noteId,
        type = if (attachmentId == null) BlockType.TEXT else BlockType.IMAGE,
        position = position,
        textContent = text,
        attachmentId = attachmentId,
        createdAt = 2,
        updatedAt = 2,
    )
}
