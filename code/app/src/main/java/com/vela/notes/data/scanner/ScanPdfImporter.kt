package com.vela.notes.data.scanner

import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AttachmentSource
import com.vela.notes.data.attachment.AttachmentStore
import com.vela.notes.domain.model.Attachment
import com.vela.notes.domain.model.AttachmentType
import com.vela.notes.domain.model.NoteBlock
import com.vela.notes.domain.model.NoteBlockType
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

data class ImportedScan(
    val attachment: Attachment,
    val block: NoteBlock,
)

class ScanPdfImporter @Inject constructor(
    private val attachmentStore: AttachmentStore,
    private val idGenerator: IdGenerator,
    private val clock: VelaClock,
    private val exporter: ScanDocumentExporter,
) {
    suspend fun import(
        noteId: String,
        pages: List<ScanPage>,
        workingDirectory: File,
    ): ImportedScan {
        require(pages.isNotEmpty()) { "A scan document needs at least one page" }
        require(pages.size <= MAX_SCAN_PAGES) { "A scan document can contain at most $MAX_SCAN_PAGES pages" }
        val pdf = File.createTempFile("vela-scan-doc-", ".pdf", workingDirectory)
        var storedPath: String? = null
        try {
            exporter.export(pages, pdf)
            val attachmentId = idGenerator.newId()
            val blockId = idGenerator.newId()
            val now = clock.nowMillis()
            val stored = attachmentStore.import(
                noteId = noteId,
                attachmentId = attachmentId,
                displayName = "Tai-lieu-da-quet.pdf",
                source = AttachmentSource { FileInputStream(pdf) },
            )
            storedPath = stored.relativePath
            return ImportedScan(
                attachment = Attachment(
                    id = attachmentId,
                    noteId = noteId,
                    kind = AttachmentType.SCAN_PDF,
                    relativePath = stored.relativePath,
                    displayName = "Tài liệu đã quét.pdf",
                    mimeType = "application/pdf",
                    sizeBytes = stored.sizeBytes,
                    pageCount = pages.size,
                    checksum = stored.checksumSha256,
                    createdAt = now,
                ),
                block = NoteBlock(
                    id = blockId,
                    noteId = noteId,
                    type = NoteBlockType.SCAN,
                    position = 0,
                    attachmentId = attachmentId,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } catch (error: Throwable) {
            storedPath?.let { path -> runCatching { attachmentStore.delete(path) } }
            throw error
        } finally {
            pdf.delete()
        }
    }
}
