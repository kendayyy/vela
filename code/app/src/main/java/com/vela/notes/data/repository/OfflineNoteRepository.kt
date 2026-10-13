package com.vela.notes.data.repository

import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AttachmentStore
import com.vela.notes.data.local.dao.NoteDao
import com.vela.notes.data.local.entity.BlockType
import com.vela.notes.data.local.entity.NoteBlockEntity
import com.vela.notes.data.local.entity.NoteEntity
import com.vela.notes.data.local.model.NoteDetail
import com.vela.notes.data.local.search.FtsQueryBuilder
import com.vela.notes.data.local.search.NoteSearchDocumentBuilder
import com.vela.notes.domain.model.Note
import com.vela.notes.domain.model.NoteDocument
import com.vela.notes.domain.model.NoteSummary
import com.vela.notes.domain.repository.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class OfflineNoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val attachmentStore: AttachmentStore,
    private val clock: VelaClock,
    private val idGenerator: IdGenerator,
) : NoteRepository {
    override fun observeActiveNotes(): Flow<List<Note>> =
        noteDao.observeActiveNotes().map { notes -> notes.map(NoteEntity::toDomain) }

    override fun observeDeletedNotes(): Flow<List<Note>> =
        noteDao.observeDeletedNotes().map { notes -> notes.map(NoteEntity::toDomain) }

    override fun observeActiveNoteSummaries(): Flow<List<NoteSummary>> = combine(
        noteDao.observeActiveNotes(),
        noteDao.observeActiveBlocks(),
        ::toSummaries,
    )

    override fun observeDeletedNoteSummaries(): Flow<List<NoteSummary>> = combine(
        noteDao.observeDeletedNotes(),
        noteDao.observeDeletedBlocks(),
        ::toSummaries,
    )

    override fun observeNote(noteId: String): Flow<NoteDocument?> = combine(
        noteDao.observeNote(noteId),
        noteDao.observeBlocks(noteId),
        noteDao.observeAttachments(noteId),
    ) { note, blocks, attachments ->
        note?.let { NoteDetail(it, blocks, attachments).toDomain() }
    }

    override suspend fun createNote(): String {
        val now = clock.nowMillis()
        val noteId = idGenerator.newId()
        val detail = NoteDetail(
            note = NoteEntity(
                id = noteId,
                title = "",
                createdAt = now,
                updatedAt = now,
            ),
            blocks = listOf(
                NoteBlockEntity(
                    id = idGenerator.newId(),
                    noteId = noteId,
                    type = BlockType.TEXT,
                    position = 0,
                    textContent = "",
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
            attachments = emptyList(),
        )
        noteDao.replaceNoteDetailAndSearch(detail, NoteSearchDocumentBuilder.build(detail))
        return noteId
    }

    override suspend fun saveNote(document: NoteDocument) {
        val now = clock.nowMillis()
        val updated = document.copy(
            note = document.note.copy(updatedAt = now),
            blocks = document.blocks.map { it.copy(updatedAt = now) },
        )
        val detail = updated.toEntity()
        noteDao.replaceNoteDetailAndSearch(detail, NoteSearchDocumentBuilder.build(detail))
    }

    override suspend fun searchNotes(query: String): List<Note> {
        val ftsQuery = FtsQueryBuilder.build(query) ?: return emptyList()
        val ids = noteDao.searchNoteIds(ftsQuery)
        if (ids.isEmpty()) return emptyList()
        val notesById = noteDao.getNotesByIds(ids).associateBy(NoteEntity::id)
        return ids.mapNotNull(notesById::get).map(NoteEntity::toDomain)
    }

    override suspend fun searchNoteSummaries(query: String): List<NoteSummary> {
        val ftsQuery = FtsQueryBuilder.build(query) ?: return emptyList()
        val ids = noteDao.searchNoteIds(ftsQuery)
        if (ids.isEmpty()) return emptyList()
        val notesById = noteDao.getNotesByIds(ids).associateBy(NoteEntity::id)
        val blocksByNote = noteDao.getBlocksByNoteIds(ids).groupBy(NoteBlockEntity::noteId)
        return ids.mapNotNull { id -> notesById[id]?.toSummary(blocksByNote[id].orEmpty()) }
    }

    override suspend fun softDelete(noteId: String) {
        noteDao.softDeleteAndRemoveIndex(noteId, clock.nowMillis())
    }

    override suspend fun restore(noteId: String) {
        noteDao.restore(noteId, clock.nowMillis())
        val detail = noteDao.getNoteDetail(noteId) ?: return
        noteDao.replaceSearchIndex(noteId, NoteSearchDocumentBuilder.build(detail))
    }

    override suspend fun deletePermanently(noteId: String) {
        val removedPaths = noteDao.permanentlyDeleteAndRemoveIndex(noteId)
        deleteUnreferenced(removedPaths)
    }

    override suspend fun emptyTrash() {
        noteDao.getDeletedNoteIds().forEach { noteId -> deletePermanently(noteId) }
    }

    private suspend fun deleteUnreferenced(paths: List<String>) {
        val referenced = (noteDao.getAllAttachmentPaths() + noteDao.getAllThumbnailPaths()).toSet()
        paths.filterNot(referenced::contains).forEach { path ->
            runCatching { attachmentStore.delete(path) }
        }
    }

    private fun toSummaries(
        notes: List<NoteEntity>,
        blocks: List<NoteBlockEntity>,
    ): List<NoteSummary> {
        val blocksByNote = blocks.groupBy(NoteBlockEntity::noteId)
        return notes.map { note -> note.toSummary(blocksByNote[note.id].orEmpty()) }
    }
}
