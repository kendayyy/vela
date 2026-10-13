package com.vela.notes.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index("updatedAt"),
        Index("isPinned"),
        Index("isFavorite"),
        Index("isArchived"),
        Index("isDeleted"),
        Index("folderId"),
    ],
)
data class NoteEntity(
    @PrimaryKey val id: String,
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
