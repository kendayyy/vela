package com.vela.notes.data.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.vela.notes.data.attachment.AttachmentSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject

data class DocumentSelection(
    val displayName: String,
    val mimeType: String,
    val declaredSizeBytes: Long?,
    val source: AttachmentSource,
)

fun interface DocumentSelectionReader {
    fun read(uri: Uri): DocumentSelection
}

class AndroidDocumentSelectionReader @Inject constructor(
    @ApplicationContext context: Context,
) : DocumentSelectionReader {
    private val resolver: ContentResolver = context.contentResolver

    override fun read(uri: Uri): DocumentSelection {
        var displayName: String? = null
        var size: Long? = null
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                displayName = cursor.getString(0)
                if (!cursor.isNull(1)) size = cursor.getLong(1)
            }
        }
        return DocumentSelection(
            displayName = displayName.orEmpty().ifBlank { "tep-dinh-kem.bin" },
            mimeType = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" },
            declaredSizeBytes = size?.takeIf { it >= 0 },
            source = AttachmentSource {
                resolver.openInputStream(uri)
                    ?: throw FileNotFoundException("Selected document is no longer available")
            },
        )
    }
}
