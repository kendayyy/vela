package com.vela.notes.domain.model

data class NoteDocument(
    val note: Note,
    val blocks: List<NoteBlock>,
    val attachments: List<Attachment>,
)

data class Note(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val folderId: String? = null,
    val color: String? = null,
)

data class NoteBlock(
    val id: String,
    val noteId: String,
    val type: NoteBlockType,
    val position: Long,
    val textContent: String? = null,
    val attachmentId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class NoteBlockType {
    TEXT,
    IMAGE,
    SCAN,
    AUDIO,
    FILE,
}

data class Attachment(
    val id: String,
    val noteId: String,
    val kind: AttachmentType,
    val relativePath: String,
    val thumbnailRelativePath: String? = null,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val waveform: ByteArray? = null,
    val pageCount: Int? = null,
    val ocrText: String? = null,
    val checksum: String,
    val createdAt: Long,
)

enum class AttachmentType {
    IMAGE,
    SCAN_PDF,
    AUDIO,
    FILE,
}
