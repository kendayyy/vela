package com.vela.notes.data.attachment

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppPrivateAttachmentStore(
    private val attachmentsRoot: File,
    private val stagingRoot: File,
    private val maxFileBytes: Long = DEFAULT_MAX_FILE_BYTES,
) : AttachmentStore {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        attachmentsRoot = File(context.filesDir, ATTACHMENTS_DIRECTORY),
        stagingRoot = File(context.filesDir, STAGING_DIRECTORY),
    )

    override suspend fun import(
        noteId: String,
        attachmentId: String,
        displayName: String,
        source: AttachmentSource,
    ): StoredAttachment = withContext(Dispatchers.IO) {
        validateIdentifier(noteId)
        validateIdentifier(attachmentId)
        ensureDirectory(attachmentsRoot)
        ensureDirectory(stagingRoot)

        val extension = safeExtension(displayName)
        val relativePath = "$noteId/$attachmentId.$extension"
        val destination = resolve(relativePath)
        if (destination.exists()) throw AttachmentStorageException.AlreadyExists(relativePath)

        val staged = File.createTempFile("$attachmentId-", PART_SUFFIX, stagingRoot)
        try {
            val digest = MessageDigest.getInstance(SHA_256)
            var sizeBytes = 0L
            source.open().use { input ->
                FileOutputStream(staged).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        sizeBytes += read
                        if (sizeBytes > maxFileBytes) {
                            throw AttachmentStorageException.FileTooLarge(maxFileBytes)
                        }
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            }

            ensureDirectory(destination.parentFile ?: attachmentsRoot)
            moveIntoPlace(staged, destination)
            StoredAttachment(
                relativePath = relativePath,
                sizeBytes = sizeBytes,
                checksumSha256 = digest.digest().toHexString(),
            )
        } catch (error: AttachmentStorageException) {
            throw error
        } catch (error: Exception) {
            throw AttachmentStorageException.StorageFailure(error)
        } finally {
            staged.delete()
        }
    }

    override suspend fun delete(relativePath: String) = withContext(Dispatchers.IO) {
        val file = resolve(relativePath)
        if (file.exists() && !file.delete()) {
            throw AttachmentStorageException.StorageFailure(
                IOException("Unable to delete ${file.name}"),
            )
        }
        removeEmptyParents(file.parentFile)
    }

    override suspend fun cleanup(
        referencedPaths: Set<String>,
        stagingOlderThanMillis: Long,
    ): AttachmentCleanupResult = withContext(Dispatchers.IO) {
        val safeReferences = referencedPaths.mapTo(mutableSetOf()) { path ->
            normalizedRelativePath(resolve(path))
        }
        var deletedOrphans = 0
        if (attachmentsRoot.exists()) {
            attachmentsRoot.walkBottomUp().forEach { file ->
                when {
                    file.isFile && normalizedRelativePath(file) !in safeReferences -> {
                        if (file.delete()) deletedOrphans++
                    }
                    file.isDirectory && file != attachmentsRoot -> file.delete()
                }
            }
        }

        var deletedStagingFiles = 0
        if (stagingRoot.exists()) {
            stagingRoot.listFiles().orEmpty().forEach { file ->
                if (file.isFile && file.lastModified() <= stagingOlderThanMillis && file.delete()) {
                    deletedStagingFiles++
                }
            }
        }
        AttachmentCleanupResult(deletedOrphans, deletedStagingFiles)
    }

    override fun resolve(relativePath: String): File {
        val segments = relativePath.split('/')
        if (
            segments.size != 2 ||
            !SAFE_IDENTIFIER.matches(segments[0]) ||
            !SAFE_FILE_NAME.matches(segments[1]) ||
            relativePath.contains('\\')
        ) {
            throw AttachmentStorageException.InvalidRelativePath(relativePath)
        }
        val root = attachmentsRoot.canonicalFile
        val candidate = File(root, relativePath).canonicalFile
        if (!candidate.path.startsWith(root.path + File.separator)) {
            throw AttachmentStorageException.InvalidRelativePath(relativePath)
        }
        return candidate
    }

    private fun normalizedRelativePath(file: File): String =
        file.canonicalFile.relativeTo(attachmentsRoot.canonicalFile).invariantSeparatorsPath

    private fun removeEmptyParents(start: File?) {
        var directory = start
        val root = attachmentsRoot.canonicalFile
        while (directory != null && directory.canonicalFile != root) {
            if (!directory.delete()) return
            directory = directory.parentFile
        }
    }

    private fun moveIntoPlace(source: File, destination: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            moveWithNio(source, destination)
        } else if (!source.renameTo(destination)) {
            throw IOException("Unable to commit staged attachment")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun moveWithNio(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath())
        }
    }

    private fun ensureDirectory(directory: File) {
        if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
            throw IOException("Unable to create ${directory.name}")
        }
    }

    private fun validateIdentifier(value: String) {
        if (!SAFE_IDENTIFIER.matches(value)) {
            throw AttachmentStorageException.InvalidIdentifier(value)
        }
    }

    private fun safeExtension(displayName: String): String {
        val candidate = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.US)
            .filter(Char::isLetterOrDigit)
            .take(MAX_EXTENSION_LENGTH)
        return candidate.ifBlank { DEFAULT_EXTENSION }
    }

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte ->
        "%02x".format(Locale.US, byte.toInt() and 0xff)
    }

    companion object {
        const val DEFAULT_MAX_FILE_BYTES = 50L * 1024L * 1024L
        private const val ATTACHMENTS_DIRECTORY = "attachments"
        private const val STAGING_DIRECTORY = ".attachment-staging"
        private const val PART_SUFFIX = ".part"
        private const val DEFAULT_EXTENSION = "bin"
        private const val MAX_EXTENSION_LENGTH = 10
        private const val COPY_BUFFER_BYTES = 32 * 1024
        private const val SHA_256 = "SHA-256"
        private val SAFE_IDENTIFIER = Regex("[A-Za-z0-9_-]+")
        private val SAFE_FILE_NAME = Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9]{1,10}")
    }
}
