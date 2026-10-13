package com.vela.notes.data.attachment

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vela.notes.domain.model.Attachment
import com.vela.notes.domain.model.AttachmentType
import com.vela.notes.domain.model.Note
import com.vela.notes.domain.model.NoteBlock
import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.domain.model.NoteDocument
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AttachmentActionsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val root = File(context.filesDir, "attachments")
    private val staging = File(context.cacheDir, "attachment-action-staging")
    private val store = AppPrivateAttachmentStore(root, staging)
    private val handler = AttachmentActionHandler(store)

    @After
    fun tearDown() {
        root.deleteRecursively()
        staging.deleteRecursively()
    }

    @Test
    fun removalPlanRemovesUnsharedAttachmentAndReturnsCleanupPath() {
        val source = document(shared = false)

        val plan = AttachmentRemovalPlanner.create(source, "block-1")!!

        assertTrue(plan.document.blocks.isEmpty())
        assertTrue(plan.document.attachments.isEmpty())
        assertEquals("note/attachment.jpg", plan.relativePathToDelete)
    }

    @Test
    fun removalPlanKeepsAttachmentWhileAnotherBlockReferencesIt() {
        val source = document(shared = true)

        val plan = AttachmentRemovalPlanner.create(source, "block-1")!!

        assertEquals(listOf("block-2"), plan.document.blocks.map { it.id })
        assertEquals(1, plan.document.attachments.size)
        assertNull(plan.relativePathToDelete)
    }

    @Test
    fun deleteFileRemovesPrivateContentAfterMetadataSave() = runTest {
        val stored = store.import("note", "attachment", "photo.jpg") {
            "image".byteInputStream()
        }
        assertTrue(store.resolve(stored.relativePath).exists())

        handler.deleteFile(stored.relativePath)

        assertFalse(store.resolve(stored.relativePath).exists())
    }

    private fun document(shared: Boolean): NoteDocument {
        val attachment = Attachment(
            id = "attachment",
            noteId = "note",
            kind = AttachmentType.IMAGE,
            relativePath = "note/attachment.jpg",
            displayName = "photo.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 5,
            checksum = "checksum",
            createdAt = 1,
        )
        val first = NoteBlock(
            id = "block-1",
            noteId = "note",
            type = NoteBlockType.IMAGE,
            position = 0,
            attachmentId = attachment.id,
            createdAt = 1,
            updatedAt = 1,
        )
        val blocks = if (shared) {
            listOf(first, first.copy(id = "block-2", position = 1_024))
        } else {
            listOf(first)
        }
        return NoteDocument(
            note = Note("note", "", 1, 1),
            blocks = blocks,
            attachments = listOf(attachment),
        )
    }
}
