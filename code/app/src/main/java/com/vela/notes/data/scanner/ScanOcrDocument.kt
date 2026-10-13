package com.vela.notes.data.scanner

import com.vela.notes.domain.model.NoteDocument

fun NoteDocument.withAttachmentOcr(attachmentId: String, ocrText: String?): NoteDocument = copy(
    attachments = attachments.map { attachment ->
        if (attachment.id == attachmentId) attachment.copy(ocrText = ocrText) else attachment
    },
)
