package com.vela.notes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_blocks",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("noteId"),
        Index("attachmentId"),
        Index(value = ["noteId", "position"], unique = true),
    ],
)
data class NoteBlockEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val type: BlockType,
    val position: Long,
    val textContent: String? = null,
    val attachmentId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class BlockType {
    TEXT,
    IMAGE,
    SCAN,
    AUDIO,
    FILE,
}
