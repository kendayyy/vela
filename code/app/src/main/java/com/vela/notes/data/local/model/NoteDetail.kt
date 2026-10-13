package com.vela.notes.data.local.model

import com.vela.notes.data.local.entity.AttachmentEntity
import com.vela.notes.data.local.entity.NoteBlockEntity
import com.vela.notes.data.local.entity.NoteEntity

data class NoteDetail(
    val note: NoteEntity,
    val blocks: List<NoteBlockEntity>,
    val attachments: List<AttachmentEntity>,
)
