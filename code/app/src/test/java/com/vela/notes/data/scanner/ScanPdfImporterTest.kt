package com.vela.notes.data.scanner

import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AttachmentCleanupResult
import com.vela.notes.data.attachment.AttachmentSource
import com.vela.notes.data.attachment.AttachmentStorageException
import com.vela.notes.data.attachment.AttachmentStore
import com.vela.notes.data.attachment.StoredAttachment
import com.vela.notes.domain.model.AttachmentType
import com.vela.notes.domain.model.NoteBlockType
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanPdfImporterTest {
    @Test
    fun importsPdfThroughStagingAndRecordsPageCount() = runTest {
        val store = FakeStore()
        val directory = tempDirectory()
        val pdfs = mutableListOf<File>()
        val importer = importer(store) { _, destination ->
            destination.writeBytes(byteArrayOf(1, 2, 3, 4))
            pdfs += destination
        }

        val result = importer.import("note", listOf(page("a"), page("b")), directory)

        assertEquals(AttachmentType.SCAN_PDF, result.attachment.kind)
        assertEquals(NoteBlockType.SCAN, result.block.type)
        assertEquals(2, result.attachment.pageCount)
        assertEquals("application/pdf", result.attachment.mimeType)
        assertEquals(4L, result.attachment.sizeBytes)
        assertEquals("note/attachment.pdf", result.attachment.relativePath)
        assertTrue(pdfs.all { !it.exists() })
        assertTrue(pageFilesStillPresent())
    }

    @Test
    fun deletesTheTemporaryPdfWhenStagingFails() = runTest {
        val store = FakeStore(fail = true)
        val directory = tempDirectory()
        val pdfs = mutableListOf<File>()
        val importer = importer(store) { _, destination ->
            destination.writeBytes(byteArrayOf(9))
            pdfs += destination
        }

        val error = runCatching { importer.import("note", listOf(page("a")), directory) }.exceptionOrNull()

        assertTrue(error is AttachmentStorageException.StorageFailure)
        assertEquals(0, store.keptPaths.size)
        assertTrue(pdfs.single().let { !it.exists() })
    }

    @Test
    fun rejectsMoreThanFiftyPagesBeforeExport() = runTest {
        var exported = false
        val importer = importer(FakeStore()) { _, _ -> exported = true }
        val pages = List(MAX_SCAN_PAGES + 1) { index -> page("p$index") }

        val error = runCatching {
            importer.import("note", pages, tempDirectory())
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertFalse(exported)
    }

    private var keptPages = emptyList<ScanPage>()

    private fun page(id: String): ScanPage {
        val image = File.createTempFile("scan-$id-", ".jpg")
        image.writeBytes(byteArrayOf(1))
        val created = ScanPage(id, image, image)
        keptPages = keptPages + created
        return created
    }

    private fun pageFilesStillPresent(): Boolean = keptPages.all { it.imageFile.exists() }

    private fun tempDirectory(): File = File(checkNotNull(System.getProperty("java.io.tmpdir")))

    private fun importer(
        store: AttachmentStore,
        export: (List<ScanPage>, File) -> Unit,
    ): ScanPdfImporter {
        val ids = ArrayDeque(listOf("attachment", "block"))
        return ScanPdfImporter(
            attachmentStore = store,
            idGenerator = IdGenerator { ids.removeFirst() },
            clock = VelaClock { 50L },
            exporter = ScanDocumentExporter { pages, destination -> export(pages, destination) },
        )
    }

    private class FakeStore(private val fail: Boolean = false) : AttachmentStore {
        val keptPaths = mutableListOf<String>()
        var importCount = 0

        override suspend fun import(
            noteId: String,
            attachmentId: String,
            displayName: String,
            source: AttachmentSource,
        ): StoredAttachment {
            importCount += 1
            val bytes = source.open().use { it.readBytes() }
            if (fail) throw AttachmentStorageException.StorageFailure(IllegalStateException("disk"))
            val path = "$noteId/$attachmentId.${displayName.substringAfterLast('.', "bin")}"
            keptPaths += path
            return StoredAttachment(path, bytes.size.toLong(), "checksum")
        }

        override suspend fun delete(relativePath: String) {
            keptPaths.remove(relativePath)
        }

        override suspend fun cleanup(
            referencedPaths: Set<String>,
            stagingOlderThanMillis: Long,
        ) = AttachmentCleanupResult(0, 0)

        override fun resolve(relativePath: String) = File(relativePath)
    }
}
