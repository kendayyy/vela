package com.vela.notes.domain.repository

import com.vela.notes.domain.model.Note
import com.vela.notes.domain.model.NoteDocument
import com.vela.notes.domain.model.NoteSummary
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeActiveNotes(): Flow<List<Note>>

    fun observeDeletedNotes(): Flow<List<Note>>

    fun observeActiveNoteSummaries(): Flow<List<NoteSummary>>

    fun observeDeletedNoteSummaries(): Flow<List<NoteSummary>>

    fun observeNote(noteId: String): Flow<NoteDocument?>

    suspend fun createNote(): String

    suspend fun saveNote(document: NoteDocument)

    suspend fun searchNotes(query: String): List<Note>

    suspend fun searchNoteSummaries(query: String): List<NoteSummary>

    suspend fun softDelete(noteId: String)

    suspend fun restore(noteId: String)

    suspend fun deletePermanently(noteId: String)

    suspend fun emptyTrash()
}
