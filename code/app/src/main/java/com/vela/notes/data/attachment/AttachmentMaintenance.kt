package com.vela.notes.data.attachment

import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.local.dao.NoteDao
import javax.inject.Inject

class AttachmentMaintenance @Inject constructor(
    private val noteDao: NoteDao,
    private val attachmentStore: AttachmentStore,
    private val clock: VelaClock,
) {
    suspend fun clean(stagingMaxAgeMillis: Long = DEFAULT_STAGING_MAX_AGE_MILLIS): AttachmentCleanupResult {
        require(stagingMaxAgeMillis >= 0) { "Staging max age cannot be negative" }
        return attachmentStore.cleanup(
            referencedPaths = noteDao.getAllAttachmentPaths().toSet(),
            stagingOlderThanMillis = clock.nowMillis() - stagingMaxAgeMillis,
        )
    }

    companion object {
        const val DEFAULT_STAGING_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L
    }
}
