package com.vela.notes.data.photo

import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AttachmentStore
import com.vela.notes.domain.model.Attachment
import com.vela.notes.domain.model.AttachmentType
import com.vela.notes.domain.model.NoteBlock
import com.vela.notes.domain.model.NoteBlockType
import javax.inject.Inject

data class ImportedPhotos(
    val attachments: List<Attachment>,
    val blocks: List<NoteBlock>,
)

class PhotoImporter @Inject constructor(
    private val attachmentStore: AttachmentStore,
    private val idGenerator: IdGenerator,
    private val clock: VelaClock,
    private val imageNormalizer: ImageNormalizer,
) {
    suspend fun import(
        noteId: String,
        selections: List<PhotoSelection>,
        startingPosition: Long,
    ): ImportedPhotos {
        if (selections.isEmpty()) return ImportedPhotos(emptyList(), emptyList())
        selections.forEach { selection ->
            require(selection.mimeType.startsWith("image/")) {
                "Photo Picker returned a non-image MIME type"
            }
        }

        val importedAttachments = mutableListOf<Attachment>()
        val importedBlocks = mutableListOf<NoteBlock>()
        try {
            selections.forEachIndexed { index, selection ->
                val normalized = imageNormalizer.normalize(selection)
                val attachmentId = idGenerator.newId()
                val blockId = idGenerator.newId()
                val now = clock.nowMillis()
                val stored = attachmentStore.import(
                    noteId = noteId,
                    attachmentId = attachmentId,
                    displayName = normalized.displayName,
                    source = normalized.source,
                )
                importedAttachments += Attachment(
                    id = attachmentId,
                    noteId = noteId,
                    kind = AttachmentType.IMAGE,
                    relativePath = stored.relativePath,
                    displayName = normalized.displayName,
                    mimeType = normalized.mimeType,
                    sizeBytes = stored.sizeBytes,
                    width = normalized.width,
                    height = normalized.height,
                    checksum = stored.checksumSha256,
                    createdAt = now,
                )
                importedBlocks += NoteBlock(
                    id = blockId,
                    noteId = noteId,
                    type = NoteBlockType.IMAGE,
                    position = startingPosition + index * BLOCK_POSITION_STEP,
                    attachmentId = attachmentId,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        } catch (error: Throwable) {
            importedAttachments.forEach { attachment ->
                runCatching { attachmentStore.delete(attachment.relativePath) }
            }
            throw error
        }
        return ImportedPhotos(importedAttachments, importedBlocks)
    }

    private companion object {
        const val BLOCK_POSITION_STEP = 1_024L
    }
}
