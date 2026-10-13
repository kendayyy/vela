package com.vela.notes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vela.notes.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val notes: StateFlow<List<NoteRowUi>> = repository.observeActiveNoteSummaries()
        .map { notes -> notes.map(::toRow) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val deletedNotes: StateFlow<List<NoteRowUi>> = repository.observeDeletedNoteSummaries()
        .map { notes -> notes.map(::toRow) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<NoteRowUi>> = searchQuery
        .debounce(250)
        .mapLatest { query ->
            if (query.isBlank()) emptyList() else repository.searchNoteSummaries(query).map(::toRow)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun createNote(onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(repository.createNote()) }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { repository.softDelete(noteId) }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch { repository.restore(noteId) }
    }

    fun deleteNotePermanently(noteId: String) {
        viewModelScope.launch { repository.deletePermanently(noteId) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }

    private fun toRow(summary: com.vela.notes.domain.model.NoteSummary) = NoteRowUi(
        id = summary.note.id,
        title = summary.note.title.ifBlank { "Ghi chú" },
        preview = summary.preview,
        timestamp = DateFormat.getDateInstance(DateFormat.SHORT).format(Date(summary.note.updatedAt)),
        isPinned = summary.note.isPinned,
        primaryContentType = summary.primaryContentType,
        todoProgress = summary.todoProgress,
    )
}
