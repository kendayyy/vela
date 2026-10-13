package com.vela.notes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("noteId"),
        Index(value = ["relativePath"], unique = true),
    ],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val kind: AttachmentKind,
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

enum class AttachmentKind {
    IMAGE,
    SCAN_PDF,
    AUDIO,
    FILE,
}
