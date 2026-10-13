package com.vela.notes.data.file

import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AppPrivateAttachmentStore
import com.vela.notes.data.attachment.AttachmentCleanupResult
import com.vela.notes.data.attachment.AttachmentSource
import com.vela.notes.data.attachment.AttachmentStorageException
import com.vela.notes.data.attachment.AttachmentStore
import com.vela.notes.data.attachment.StoredAttachment
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileImporterTest {
    @Test
    fun importsDocumentAsFileAttachmentAndBlock() = runTest {
        val store = FakeStore()
        val importer = importer(store)

        val result = importer.import(
            "note",
            selection("ke-hoach.pdf", "application/pdf", declaredSize = 3, bytes = byteArrayOf(1, 2, 3)),
        )

        assertEquals("attachment", result.attachment.id)
        assertEquals("ke-hoach.pdf", result.attachment.displayName)
        assertEquals(3L, result.attachment.sizeBytes)
        assertEquals("attachment", result.block.attachmentId)
        assertEquals(1, store.importCount)
    }

    @Test
    fun rejectsDeclaredOversizeBeforeAllocatingIdsOrOpeningSource() = runTest {
        val store = FakeStore()
        var opened = false
        val importer = importer(store)
        val selection = DocumentSelection(
            displayName = "large.zip",
            mimeType = "application/zip",
            declaredSizeBytes = AppPrivateAttachmentStore.DEFAULT_MAX_FILE_BYTES + 1,
            source = AttachmentSource {
                opened = true
                ByteArrayInputStream(byteArrayOf())
            },
        )

        val error = runCatching { importer.import("note", selection) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(0, store.importCount)
        assertEquals(false, opened)
    }

    @Test
    fun rejectsVideoAttachments() = runTest {
        val store = FakeStore()

        val error = runCatching {
            importer(store).import(
                "note",
                selection("clip.mp4", "video/mp4", 1, byteArrayOf(1)),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(0, store.importCount)
    }

    @Test
    fun propagatesStreamedSizeLimitWhenProviderMetadataIsMissing() = runTest {
        val store = FakeStore(failWithSizeLimit = true)

        val error = runCatching {
            importer(store).import(
                "note",
                selection("unknown.bin", "application/octet-stream", null, byteArrayOf(1)),
            )
        }.exceptionOrNull()

        assertTrue(error is AttachmentStorageException.FileTooLarge)
    }

    private fun importer(store: AttachmentStore): FileImporter {
        val ids = ArrayDeque(listOf("attachment", "block"))
        return FileImporter(store, IdGenerator { ids.removeFirst() }, VelaClock { 123L })
    }

    private fun selection(
        name: String,
        mime: String,
        declaredSize: Long?,
        bytes: ByteArray,
    ) = DocumentSelection(
        displayName = name,
        mimeType = mime,
        declaredSizeBytes = declaredSize,
        source = AttachmentSource { ByteArrayInputStream(bytes) },
    )

    private class FakeStore(
        private val failWithSizeLimit: Boolean = false,
    ) : AttachmentStore {
        var importCount = 0

        override suspend fun import(
            noteId: String,
            attachmentId: String,
            displayName: String,
            source: AttachmentSource,
        ): StoredAttachment {
            importCount += 1
            if (failWithSizeLimit) {
                throw AttachmentStorageException.FileTooLarge(
                    AppPrivateAttachmentStore.DEFAULT_MAX_FILE_BYTES,
                )
            }
            val bytes = source.open().use { it.readBytes() }
            return StoredAttachment(
                relativePath = "$noteId/$attachmentId.${displayName.substringAfterLast('.', "bin")}",
                sizeBytes = bytes.size.toLong(),
                checksumSha256 = "checksum",
            )
        }

        override suspend fun delete(relativePath: String) = Unit

        override suspend fun cleanup(
            referencedPaths: Set<String>,
            stagingOlderThanMillis: Long,
        ) = AttachmentCleanupResult(0, 0)

        override fun resolve(relativePath: String) = File(relativePath)
    }
}
