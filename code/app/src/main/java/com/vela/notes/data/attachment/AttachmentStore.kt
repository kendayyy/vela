package com.vela.notes.data.attachment

import java.io.File
import java.io.InputStream

fun interface AttachmentSource {
    fun open(): InputStream
}

interface AttachmentStore {
    suspend fun import(
        noteId: String,
        attachmentId: String,
        displayName: String,
        source: AttachmentSource,
    ): StoredAttachment

    suspend fun delete(relativePath: String)

    suspend fun cleanup(
        referencedPaths: Set<String>,
        stagingOlderThanMillis: Long,
    ): AttachmentCleanupResult

    fun resolve(relativePath: String): File
}

data class StoredAttachment(
    val relativePath: String,
    val sizeBytes: Long,
    val checksumSha256: String,
)

data class AttachmentCleanupResult(
    val deletedOrphans: Int,
    val deletedStagingFiles: Int,
)

sealed class AttachmentStorageException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class InvalidIdentifier(value: String) :
        AttachmentStorageException("Unsafe attachment identifier: $value")

    class InvalidRelativePath(path: String) :
        AttachmentStorageException("Unsafe attachment path: $path")

    class FileTooLarge(val maximumBytes: Long) :
        AttachmentStorageException("Attachment exceeds the $maximumBytes byte limit")

    class AlreadyExists(path: String) :
        AttachmentStorageException("Attachment already exists: $path")

    class StorageFailure(cause: Throwable) :
        AttachmentStorageException("Unable to store attachment", cause)
}
