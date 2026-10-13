package com.vela.notes.domain.model

data class NoteSummary(
    val note: Note,
    val preview: String,
    val primaryContentType: NoteBlockType,
    val todoProgress: TodoProgress? = null,
)

data class TodoProgress(
    val total: Int,
    val completed: Int,
)
