package com.vela.notes.data.repository

import com.vela.notes.data.local.entity.AttachmentEntity
import com.vela.notes.data.local.entity.AttachmentKind
import com.vela.notes.data.local.entity.BlockType
import com.vela.notes.data.local.entity.NoteBlockEntity
import com.vela.notes.data.local.entity.NoteEntity
import com.vela.notes.data.local.model.NoteDetail
import com.vela.notes.domain.model.Attachment
import com.vela.notes.domain.model.AttachmentType
import com.vela.notes.domain.model.Note
import com.vela.notes.domain.model.NoteBlock
import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.domain.model.NoteDocument
import com.vela.notes.domain.model.NoteSummary
import com.vela.notes.domain.model.TodoProgress
import com.vela.notes.domain.markdown.MarkdownChecklist

internal fun NoteEntity.toDomain() = Note(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isFavorite = isFavorite,
    isArchived = isArchived,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    folderId = folderId,
    color = color,
)

internal fun NoteEntity.toSummary(blocks: List<NoteBlockEntity>): NoteSummary {
    val ordered = blocks.sortedBy(NoteBlockEntity::position)
    val firstText = ordered.firstOrNull { block ->
        block.type == BlockType.TEXT && !block.textContent.isNullOrBlank()
    }
    val primaryBlock = ordered.firstOrNull { block ->
        block.type != BlockType.TEXT || !block.textContent.isNullOrBlank()
    }
    val primaryType = primaryBlock?.type?.let { NoteBlockType.valueOf(it.name) } ?: NoteBlockType.TEXT
    val preview = firstText?.textContent
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        .orEmpty()
        .ifBlank { primaryType.previewLabel() }
    val checklistItems = ordered
        .asSequence()
        .filter { it.type == BlockType.TEXT }
        .flatMap { MarkdownChecklist.items(it.textContent.orEmpty()).asSequence() }
        .toList()
    return NoteSummary(
        note = toDomain(),
        preview = preview,
        primaryContentType = primaryType,
        todoProgress = checklistItems
            .takeIf { it.isNotEmpty() }
            ?.let { items ->
                TodoProgress(
                    total = items.size,
                    completed = items.count { it.checked },
                )
            },
    )
}

private fun NoteBlockType.previewLabel(): String = when (this) {
    NoteBlockType.TEXT -> "Chưa có nội dung"
    NoteBlockType.IMAGE -> "Ảnh"
    NoteBlockType.SCAN -> "Tài liệu đã quét"
    NoteBlockType.AUDIO -> "Bản ghi âm"
    NoteBlockType.FILE -> "Tệp đính kèm"
}

internal fun Note.toEntity() = NoteEntity(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isFavorite = isFavorite,
    isArchived = isArchived,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    folderId = folderId,
    color = color,
)

internal fun NoteDetail.toDomain() = NoteDocument(
    note = note.toDomain(),
    blocks = blocks.map(NoteBlockEntity::toDomain),
    attachments = attachments.map(AttachmentEntity::toDomain),
)

internal fun NoteDocument.toEntity() = NoteDetail(
    note = note.toEntity(),
    blocks = blocks.map(NoteBlock::toEntity),
    attachments = attachments.map(Attachment::toEntity),
)

private fun NoteBlockEntity.toDomain() = NoteBlock(
    id = id,
    noteId = noteId,
    type = NoteBlockType.valueOf(type.name),
    position = position,
    textContent = textContent,
    attachmentId = attachmentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun NoteBlock.toEntity() = NoteBlockEntity(
    id = id,
    noteId = noteId,
    type = BlockType.valueOf(type.name),
    position = position,
    textContent = textContent,
    attachmentId = attachmentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun AttachmentEntity.toDomain() = Attachment(
    id = id,
    noteId = noteId,
    kind = AttachmentType.valueOf(kind.name),
    relativePath = relativePath,
    thumbnailRelativePath = thumbnailRelativePath,
    displayName = displayName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    durationMs = durationMs,
    waveform = waveform,
    pageCount = pageCount,
    ocrText = ocrText,
    checksum = checksum,
    createdAt = createdAt,
)

private fun Attachment.toEntity() = AttachmentEntity(
    id = id,
    noteId = noteId,
    kind = AttachmentKind.valueOf(kind.name),
    relativePath = relativePath,
    thumbnailRelativePath = thumbnailRelativePath,
    displayName = displayName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    durationMs = durationMs,
    waveform = waveform,
    pageCount = pageCount,
    ocrText = ocrText,
    checksum = checksum,
    createdAt = createdAt,
)
