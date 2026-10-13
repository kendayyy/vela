package com.vela.notes.data.file

import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AppPrivateAttachmentStore
import com.vela.notes.data.attachment.AttachmentStore
import com.vela.notes.domain.model.Attachment
import com.vela.notes.domain.model.AttachmentType
import com.vela.notes.domain.model.NoteBlock
import com.vela.notes.domain.model.NoteBlockType
import javax.inject.Inject

data class ImportedFile(
    val attachment: Attachment,
    val block: NoteBlock,
)

class FileImporter @Inject constructor(
    private val attachmentStore: AttachmentStore,
    private val idGenerator: IdGenerator,
    private val clock: VelaClock,
) {
    suspend fun import(noteId: String, selection: DocumentSelection): ImportedFile {
        require(!selection.mimeType.startsWith("video/")) { "Video attachments are not supported" }
        require(
            selection.declaredSizeBytes == null ||
                selection.declaredSizeBytes <= AppPrivateAttachmentStore.DEFAULT_MAX_FILE_BYTES,
        ) { "Attachment exceeds the 50 MB limit" }

        val attachmentId = idGenerator.newId()
        val blockId = idGenerator.newId()
        val now = clock.nowMillis()
        val stored = attachmentStore.import(
            noteId = noteId,
            attachmentId = attachmentId,
            displayName = selection.displayName,
            source = selection.source,
        )
        return ImportedFile(
            attachment = Attachment(
                id = attachmentId,
                noteId = noteId,
                kind = AttachmentType.FILE,
                relativePath = stored.relativePath,
                displayName = selection.displayName,
                mimeType = selection.mimeType,
                sizeBytes = stored.sizeBytes,
                checksum = stored.checksumSha256,
                createdAt = now,
            ),
            block = NoteBlock(
                id = blockId,
                noteId = noteId,
                type = NoteBlockType.FILE,
                position = 0,
                attachmentId = attachmentId,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}
