package com.vela.notes.data.audio

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

data class ImportedAudio(val attachment: Attachment, val block: NoteBlock)

class AudioImporter @Inject constructor(
    private val attachmentStore: AttachmentStore,
    private val idGenerator: IdGenerator,
    private val clock: VelaClock,
) {
    suspend fun import(
        noteId: String,
        file: File,
        durationMillis: Long,
        waveform: ByteArray = byteArrayOf(),
    ): ImportedAudio {
        require(durationMillis >= 0) { "Duration cannot be negative" }
        val attachmentId = idGenerator.newId()
        val blockId = idGenerator.newId()
        val now = clock.nowMillis()
        val stored = attachmentStore.import(
            noteId = noteId,
            attachmentId = attachmentId,
            displayName = file.name,
            source = AttachmentSource { FileInputStream(file) },
        )
        return ImportedAudio(
            attachment = Attachment(
                id = attachmentId,
                noteId = noteId,
                kind = AttachmentType.AUDIO,
                relativePath = stored.relativePath,
                displayName = file.name,
                mimeType = "audio/mp4",
                sizeBytes = stored.sizeBytes,
                durationMs = durationMillis,
                waveform = waveform,
                checksum = stored.checksumSha256,
                createdAt = now,
            ),
            block = NoteBlock(
                id = blockId,
                noteId = noteId,
                type = NoteBlockType.AUDIO,
                position = 0,
                attachmentId = attachmentId,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}
