package com.vela.notes.data.photo

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.vela.notes.data.attachment.AttachmentSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject

data class PhotoSelection(
    val displayName: String,
    val mimeType: String,
    val source: AttachmentSource,
)

fun interface PhotoSelectionReader {
    fun read(uri: Uri): PhotoSelection
}

class AndroidPhotoSelectionReader @Inject constructor(
    @ApplicationContext context: Context,
) : PhotoSelectionReader {
    private val resolver: ContentResolver = context.contentResolver

    override fun read(uri: Uri): PhotoSelection {
        val displayName = resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }.orEmpty().ifBlank { "anh-vela.jpg" }
        val mimeType = resolver.getType(uri).orEmpty().ifBlank { "image/*" }
        return PhotoSelection(
            displayName = displayName,
            mimeType = mimeType,
            source = AttachmentSource {
                resolver.openInputStream(uri)
                    ?: throw FileNotFoundException("Photo Picker URI is no longer available")
            },
        )
    }
}
