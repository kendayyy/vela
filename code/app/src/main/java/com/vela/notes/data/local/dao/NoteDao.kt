package com.vela.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vela.notes.data.local.entity.AttachmentEntity
import com.vela.notes.data.local.entity.NoteBlockEntity
import com.vela.notes.data.local.entity.NoteEntity
import com.vela.notes.data.local.entity.NoteSearchEntity
import com.vela.notes.data.local.model.NoteDetail
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NoteDao {
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0 AND isArchived = 0
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    abstract fun observeActiveNotes(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 1
        ORDER BY deletedAt DESC
        """,
    )
    abstract fun observeDeletedNotes(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT note_blocks.* FROM note_blocks
        INNER JOIN notes ON notes.id = note_blocks.noteId
        WHERE notes.isDeleted = 0 AND notes.isArchived = 0
        ORDER BY note_blocks.noteId, note_blocks.position
        """,
    )
    abstract fun observeActiveBlocks(): Flow<List<NoteBlockEntity>>

    @Query(
        """
        SELECT note_blocks.* FROM note_blocks
        INNER JOIN notes ON notes.id = note_blocks.noteId
        WHERE notes.isDeleted = 1
        ORDER BY note_blocks.noteId, note_blocks.position
        """,
    )
    abstract fun observeDeletedBlocks(): Flow<List<NoteBlockEntity>>

    @Query("SELECT id FROM notes WHERE isDeleted = 1")
    abstract suspend fun getDeletedNoteIds(): List<String>

    @Query("SELECT thumbnailRelativePath FROM attachments WHERE thumbnailRelativePath IS NOT NULL")
    abstract suspend fun getAllThumbnailPaths(): List<String>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    abstract fun observeNote(noteId: String): Flow<NoteEntity?>

    @Query("SELECT * FROM note_blocks WHERE noteId = :noteId ORDER BY position ASC")
    abstract fun observeBlocks(noteId: String): Flow<List<NoteBlockEntity>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC")
    abstract fun observeAttachments(noteId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT relativePath FROM attachments")
    abstract suspend fun getAllAttachmentPaths(): List<String>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    protected abstract suspend fun getNote(noteId: String): NoteEntity?

    @Query("SELECT * FROM note_blocks WHERE noteId = :noteId ORDER BY position ASC")
    protected abstract suspend fun getBlocks(noteId: String): List<NoteBlockEntity>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC")
    protected abstract suspend fun getAttachments(noteId: String): List<AttachmentEntity>

    @Upsert
    abstract suspend fun upsertNote(note: NoteEntity)

    @Upsert
    abstract suspend fun upsertBlocks(blocks: List<NoteBlockEntity>)

    @Upsert
    abstract suspend fun upsertAttachments(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM note_blocks WHERE id = :blockId")
    abstract suspend fun deleteBlock(blockId: String)

    @Query("DELETE FROM note_blocks WHERE noteId = :noteId")
    protected abstract suspend fun deleteBlocksForNote(noteId: String)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    abstract suspend fun deleteAttachment(attachmentId: String)

    @Query(
        """
        UPDATE notes
        SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt
        WHERE id = :noteId
        """,
    )
    abstract suspend fun softDelete(noteId: String, deletedAt: Long)

    @Query(
        """
        UPDATE notes
        SET isDeleted = 0, deletedAt = NULL, updatedAt = :restoredAt
        WHERE id = :noteId
        """,
    )
    abstract suspend fun restore(noteId: String, restoredAt: Long)

    @Query("DELETE FROM notes WHERE id = :noteId")
    abstract suspend fun deletePermanently(noteId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSearchEntry(entry: NoteSearchEntity)

    @Query("DELETE FROM note_search WHERE noteId = :noteId")
    protected abstract suspend fun deleteSearchEntry(noteId: String)

    @Query("SELECT noteId FROM note_search WHERE note_search MATCH :ftsQuery")
    abstract suspend fun searchNoteIds(ftsQuery: String): List<String>

    @Query("SELECT * FROM notes WHERE id IN (:noteIds) AND isDeleted = 0")
    abstract suspend fun getNotesByIds(noteIds: List<String>): List<NoteEntity>

    @Query("SELECT * FROM note_blocks WHERE noteId IN (:noteIds) ORDER BY noteId, position")
    abstract suspend fun getBlocksByNoteIds(noteIds: List<String>): List<NoteBlockEntity>

    @Transaction
    open suspend fun upsertNoteDetail(detail: NoteDetail) {
        upsertNote(detail.note)
        upsertAttachments(detail.attachments)
        upsertBlocks(detail.blocks)
    }

    @Transaction
    open suspend fun replaceNoteDetailAndSearch(detail: NoteDetail, searchableText: String) {
        upsertNote(detail.note)
        upsertAttachments(detail.attachments)
        deleteBlocksForNote(detail.note.id)
        upsertBlocks(detail.blocks)
        replaceSearchIndex(detail.note.id, searchableText)
    }

    @Transaction
    open suspend fun getNoteDetail(noteId: String): NoteDetail? {
        val note = getNote(noteId) ?: return null
        return NoteDetail(
            note = note,
            blocks = getBlocks(noteId),
            attachments = getAttachments(noteId),
        )
    }

    @Transaction
    open suspend fun replaceSearchIndex(noteId: String, searchableText: String) {
        deleteSearchEntry(noteId)
        insertSearchEntry(
            NoteSearchEntity(
                noteId = noteId,
                searchableText = searchableText,
            ),
        )
    }

    @Transaction
    open suspend fun softDeleteAndRemoveIndex(noteId: String, deletedAt: Long) {
        softDelete(noteId, deletedAt)
        deleteSearchEntry(noteId)
    }

    @Transaction
    open suspend fun permanentlyDeleteAndRemoveIndex(noteId: String): List<String> {
        val removedPaths = getAttachments(noteId).flatMap { attachment ->
            listOfNotNull(attachment.relativePath, attachment.thumbnailRelativePath)
        }
        deleteSearchEntry(noteId)
        deletePermanently(noteId)
        return removedPaths
    }
}
