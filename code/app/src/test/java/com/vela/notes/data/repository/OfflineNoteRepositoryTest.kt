package com.vela.notes.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AppPrivateAttachmentStore
import com.vela.notes.data.attachment.AttachmentSource
import com.vela.notes.data.local.VelaDatabase
import com.vela.notes.domain.model.Attachment
import com.vela.notes.domain.model.AttachmentType
import com.vela.notes.domain.model.NoteBlock
import com.vela.notes.domain.model.NoteBlockType
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineNoteRepositoryTest {
    private lateinit var database: VelaDatabase
    private lateinit var repository: OfflineNoteRepository
    private lateinit var store: AppPrivateAttachmentStore
    private lateinit var storageRoot: File
    private var now = 1_000L
    private val ids = ArrayDeque(listOf("note-id", "block-id", "note-b", "block-b"))

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VelaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storageRoot = File(checkNotNull(System.getProperty("java.io.tmpdir")), "vela-trash-${System.nanoTime()}")
        store = AppPrivateAttachmentStore(File(storageRoot, "attachments"), File(storageRoot, "staging"))
        repository = OfflineNoteRepository(
            noteDao = database.noteDao(),
            attachmentStore = store,
            clock = VelaClock { now },
            idGenerator = IdGenerator { ids.removeFirst() },
        )
    }

    @After
    fun tearDown() {
        database.close()
        storageRoot.deleteRecursively()
    }

    @Test
    fun createNote_usesInjectedIdAndClock_andCreatesTextBlock() = runTest {
        val id = repository.createNote()

        val document = repository.observeNote(id).first { it != null }!!
        assertEquals("note-id", document.note.id)
        assertEquals(1_000L, document.note.createdAt)
        assertEquals("block-id", document.blocks.single().id)
        assertEquals(NoteBlockType.TEXT, document.blocks.single().type)
    }

    @Test
    fun saveSearchAndSoftDelete_keepIndexInSync() = runTest {
        val id = repository.createNote()
        val created = repository.observeNote(id).first { it != null }!!
        now = 2_000L

        repository.saveNote(
            created.copy(
                note = created.note.copy(title = "Du an Vela"),
                blocks = created.blocks.map { it.copy(textContent = "Noi dung offline") },
            ),
        )

        assertEquals(id, repository.searchNotes("offline").single().id)
        assertEquals("Noi dung offline", repository.searchNoteSummaries("offline").single().preview)
        assertEquals(2_000L, repository.observeNote(id).first { it?.note?.updatedAt == now }!!.note.updatedAt)

        now = 3_000L
        repository.softDelete(id)

        assertTrue(repository.searchNotes("offline").isEmpty())
        assertTrue(repository.observeActiveNotes().first().isEmpty())
    }

    @Test
    fun scanOcrIsSearchableUntilTheNoteIsSoftDeleted() = runTest {
        val id = repository.createNote()
        val created = repository.observeNote(id).first { it != null }!!
        val scan = Attachment(
            id = "scan-1",
            noteId = id,
            kind = AttachmentType.SCAN_PDF,
            relativePath = "$id/scan-1.pdf",
            displayName = "Tài liệu đã quét.pdf",
            mimeType = "application/pdf",
            sizeBytes = 12,
            pageCount = 2,
            ocrText = "Xin chào Hà Nội",
            checksum = "checksum",
            createdAt = now,
        )
        val block = NoteBlock(
            id = "scan-block",
            noteId = id,
            type = NoteBlockType.SCAN,
            position = 2_048,
            attachmentId = scan.id,
            createdAt = now,
            updatedAt = now,
        )
        repository.saveNote(created.copy(attachments = listOf(scan), blocks = created.blocks + block))

        val found = repository.observeNote(id).first { document ->
            document?.attachments?.any { it.ocrText == "Xin chào Hà Nội" } == true
        }!!
        assertEquals(NoteBlockType.TEXT, found.blocks.first { it.type == NoteBlockType.TEXT }.type)
        assertEquals(id, repository.searchNotes("Hà Nội").single().id)

        repository.softDelete(id)
        assertTrue(repository.searchNotes("Hà Nội").isEmpty())
    }

    @Test
    fun saveNoteRemovesBlocksMissingFromReplacementDocument() = runTest {
        val id = repository.createNote()
        val created = repository.observeNote(id).first { it != null }!!
        val extra = created.blocks.single().copy(id = "extra-block", position = 1_024)
        repository.saveNote(created.copy(blocks = created.blocks + extra))

        repository.saveNote(created)

        val saved = repository.observeNote(id).first { it?.blocks?.size == 1 }!!
        assertEquals(listOf("block-id"), saved.blocks.map { it.id })
    }

    @Test
    fun undoRestoreAndPermanentDeleteKeepSharedFilesUntilUnreferenced() = runTest {
        val firstId = repository.createNote()
        val secondId = repository.createNote()
        val shared = store.import(
            firstId,
            "file-a",
            "a.pdf",
            AttachmentSource { ByteArrayInputStream(byteArrayOf(1, 2)) },
        )
        val owned = store.import(
            secondId,
            "file-b",
            "b.pdf",
            AttachmentSource { ByteArrayInputStream(byteArrayOf(3)) },
        )
        val first = repository.observeNote(firstId).first { it != null }!!
        val second = repository.observeNote(secondId).first { it != null }!!
        repository.saveNote(
            first.copy(
                note = first.note.copy(title = "Bien ban"),
                attachments = listOf(storedAttachment(firstId, "file-a", shared.relativePath)),
            ),
        )
        repository.saveNote(
            second.copy(
                attachments = listOf(
                    storedAttachment(
                        noteId = secondId,
                        attachmentId = "file-b",
                        path = owned.relativePath,
                        thumbnailPath = shared.relativePath,
                    ),
                ),
            ),
        )

        repository.softDelete(firstId)
        assertTrue(repository.observeActiveNotes().first().none { it.id == firstId })
        assertEquals(firstId, repository.observeDeletedNotes().first().single().id)
        assertTrue(repository.searchNotes("Bien").isEmpty())

        repository.restore(firstId)
        assertEquals(firstId, repository.searchNotes("Bien").single().id)
        assertTrue(repository.observeDeletedNotes().first().isEmpty())

        repository.softDelete(firstId)
        repository.deletePermanently(firstId)
        assertTrue(store.resolve(shared.relativePath).exists())
        assertTrue(store.resolve(owned.relativePath).exists())

        repository.softDelete(secondId)
        repository.emptyTrash()
        assertFalse(store.resolve(shared.relativePath).exists())
        assertFalse(store.resolve(owned.relativePath).exists())
        assertTrue(repository.observeNote(secondId).first { it == null } == null)
    }

    private fun storedAttachment(
        noteId: String,
        attachmentId: String,
        path: String,
        thumbnailPath: String? = null,
    ) = Attachment(
        id = attachmentId,
        noteId = noteId,
        kind = AttachmentType.FILE,
        relativePath = path,
        thumbnailRelativePath = thumbnailPath,
        displayName = "$attachmentId.pdf",
        mimeType = "application/pdf",
        sizeBytes = 2,
        checksum = "checksum",
        createdAt = now,
    )
}
