package com.vela.notes.data.photo

import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AttachmentCleanupResult
import com.vela.notes.data.attachment.AttachmentSource
import com.vela.notes.data.attachment.AttachmentStore
import com.vela.notes.data.attachment.StoredAttachment
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoImporterTest {
    @Test
    fun emptySelectionIsCancellationWithoutSideEffects() = runTest {
        val store = FakeAttachmentStore()
        val importer = importer(store, emptyList())

        val result = importer.import("note", emptyList(), startingPosition = 0)

        assertEquals(emptyList<Any>(), result.attachments)
        assertEquals(emptyList<Any>(), result.blocks)
        assertEquals(0, store.importCount)
    }

    @Test
    fun importsMultiplePhotosWithStableOrderAndMetadata() = runTest {
        val store = FakeAttachmentStore()
        val importer = importer(
            store,
            listOf("attachment-1", "block-1", "attachment-2", "block-2"),
        )

        val result = importer.import(
            noteId = "note",
            selections = listOf(
                selection("first.jpg", "image/jpeg", byteArrayOf(1, 2)),
                selection("second.png", "image/png", byteArrayOf(3, 4, 5)),
            ),
            startingPosition = 100,
        )

        assertEquals(listOf("first.jpg", "second.png"), result.attachments.map { it.displayName })
        assertEquals(listOf(2L, 3L), result.attachments.map { it.sizeBytes })
        assertEquals(listOf("attachment-1", "attachment-2"), result.blocks.map { it.attachmentId })
        assertEquals(listOf(100L, 1_124L), result.blocks.map { it.position })
        assertEquals(2, store.importCount)
    }

    @Test
    fun failedBatchRollsBackFilesAlreadyImported() = runTest {
        val store = FakeAttachmentStore(failAtImport = 2)
        val importer = importer(
            store,
            listOf("attachment-1", "block-1", "attachment-2", "block-2"),
        )

        expectThrows<IOException> {
            importer.import(
                noteId = "note",
                selections = listOf(
                    selection("first.jpg", "image/jpeg", byteArrayOf(1)),
                    selection("second.jpg", "image/jpeg", byteArrayOf(2)),
                ),
                startingPosition = 0,
            )
        }

        assertEquals(listOf("note/attachment-1.jpg"), store.deletedPaths)
    }

    @Test
    fun rejectsNonImageBeforeCreatingFilesOrIds() = runTest {
        val store = FakeAttachmentStore()
        val importer = importer(store, emptyList())

        expectThrows<IllegalArgumentException> {
            importer.import(
                "note",
                listOf(selection("document.pdf", "application/pdf", byteArrayOf(1))),
                0,
            )
        }

        assertEquals(0, store.importCount)
    }

    private fun importer(store: AttachmentStore, ids: List<String>): PhotoImporter {
        val queue = ArrayDeque(ids)
        return PhotoImporter(
            attachmentStore = store,
            idGenerator = IdGenerator { queue.removeFirst() },
            clock = VelaClock { 1_000L },
            imageNormalizer = ImageNormalizer { selection ->
                NormalizedPhoto(
                    displayName = selection.displayName,
                    mimeType = selection.mimeType,
                    width = 10,
                    height = 20,
                    source = selection.source,
                )
            },
        )
    }

    private fun selection(name: String, mime: String, bytes: ByteArray) = PhotoSelection(
        displayName = name,
        mimeType = mime,
        source = AttachmentSource { ByteArrayInputStream(bytes) },
    )

    private suspend inline fun <reified T : Throwable> expectThrows(noinline block: suspend () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            if (error is T) return
            throw AssertionError("Expected ${T::class.java.simpleName}", error)
        }
        throw AssertionError("Expected ${T::class.java.simpleName}")
    }

    private class FakeAttachmentStore(
        private val failAtImport: Int? = null,
    ) : AttachmentStore {
        var importCount = 0
        val deletedPaths = mutableListOf<String>()

        override suspend fun import(
            noteId: String,
            attachmentId: String,
            displayName: String,
            source: AttachmentSource,
        ): StoredAttachment {
            importCount += 1
            if (importCount == failAtImport) throw IOException("provider failed")
            val bytes = source.open().use { it.readBytes() }
            val extension = displayName.substringAfterLast('.', "bin")
            return StoredAttachment(
                relativePath = "$noteId/$attachmentId.$extension",
                sizeBytes = bytes.size.toLong(),
                checksumSha256 = "checksum-$attachmentId",
            )
        }

        override suspend fun delete(relativePath: String) {
            deletedPaths += relativePath
        }

        override suspend fun cleanup(
            referencedPaths: Set<String>,
            stagingOlderThanMillis: Long,
        ) = AttachmentCleanupResult(0, 0)

        override fun resolve(relativePath: String) = File(relativePath)
    }
}
