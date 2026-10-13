package com.vela.notes.data.attachment

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.vela.notes.domain.model.NoteDocument
import javax.inject.Inject

data class AttachmentRemovalPlan(
    val document: NoteDocument,
    val relativePathToDelete: String?,
)

object AttachmentRemovalPlanner {
    fun create(document: NoteDocument, blockId: String): AttachmentRemovalPlan? {
        val block = document.blocks.firstOrNull { it.id == blockId } ?: return null
        val attachmentId = block.attachmentId ?: return null
        val remainingBlocks = document.blocks.filterNot { it.id == blockId }
        val stillReferenced = remainingBlocks.any { it.attachmentId == attachmentId }
        val attachment = document.attachments.firstOrNull { it.id == attachmentId }
        return AttachmentRemovalPlan(
            document = document.copy(
                blocks = remainingBlocks,
                attachments = if (stillReferenced) {
                    document.attachments
                } else {
                    document.attachments.filterNot { it.id == attachmentId }
                },
            ),
            relativePathToDelete = if (stillReferenced) null else attachment?.relativePath,
        )
    }
}

class AttachmentActionHandler @Inject constructor(
    private val attachmentStore: AttachmentStore,
) {
    fun openIntent(context: Context, relativePath: String, mimeType: String): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri(context, relativePath), mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun shareIntent(context: Context, relativePath: String, mimeType: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri(context, relativePath))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    suspend fun deleteFile(relativePath: String?) {
        if (relativePath != null) attachmentStore.delete(relativePath)
    }

    private fun contentUri(context: Context, relativePath: String) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.files",
        attachmentStore.resolve(relativePath),
    )
}
