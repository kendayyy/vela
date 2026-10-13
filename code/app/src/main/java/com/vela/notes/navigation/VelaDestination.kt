package com.vela.notes.navigation

import com.vela.notes.ui.editor.NOTE_ID_ARGUMENT

enum class VelaDestination(val route: String) {
    Folders("folders"),
    AllNotes("notes/all"),
    PinnedNotes("notes/pinned"),
    DeletedNotes("notes/deleted"),
    Settings("settings"),
    Editor("editor/{$NOTE_ID_ARGUMENT}"),
}

fun editorRoute(noteId: String): String = "editor/$noteId"
