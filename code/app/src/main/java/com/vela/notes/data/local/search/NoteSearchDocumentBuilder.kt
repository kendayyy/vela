package com.vela.notes.data.local.search

import com.vela.notes.data.local.entity.BlockType
import com.vela.notes.data.local.model.NoteDetail

object NoteSearchDocumentBuilder {
    fun build(detail: NoteDetail): String = buildList {
        add(detail.note.title)
        detail.blocks
            .asSequence()
            .filter { it.type == BlockType.TEXT }
            .mapNotNull { it.textContent }
            .filter { it.isNotBlank() }
            .forEach(::add)
        detail.attachments.forEach { attachment ->
            add(attachment.displayName)
            attachment.ocrText
                ?.takeIf(String::isNotBlank)
                ?.let(::add)
        }
    }.joinToString(separator = "\n")
}
