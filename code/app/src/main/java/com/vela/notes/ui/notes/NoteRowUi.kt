package com.vela.notes.ui.notes

import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.domain.model.TodoProgress

data class NoteRowUi(
    val id: String,
    val title: String,
    val preview: String,
    val timestamp: String,
    val isPinned: Boolean = false,
    val primaryContentType: NoteBlockType = NoteBlockType.TEXT,
    val todoProgress: TodoProgress? = null,
)
