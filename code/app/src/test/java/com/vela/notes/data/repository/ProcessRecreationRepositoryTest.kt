package com.vela.notes.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AppPrivateAttachmentStore
import com.vela.notes.data.local.VelaDatabase
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProcessRecreationRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "vela-process-${System.nanoTime()}.db"
    private val storageRoot = File(context.cacheDir, "vela-process-files-${System.nanoTime()}")

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
        storageRoot.deleteRecursively()
    }

    @Test
    fun flushedTextSurvivesANewDatabaseConnection() = runTest {
        val first = repository()
        val id = first.createNote()
        val created = first.observeNote(id).first { it != null }!!
        first.saveNote(
            created.copy(
                note = created.note.copy(title = "Bien ban"),
                blocks = created.blocks.map { block ->
                    block.copy(textContent = "Noi dung da luu")
                },
            ),
        )
        first.database.close()

        val reopened = repository()
        val restored = reopened.observeNote(id).first { it != null }!!
        assertEquals("Bien ban", restored.note.title)
        assertEquals("Noi dung da luu", restored.blocks.single().textContent)
        assertTrue(reopened.searchNotes("Bien").any { it.id == id })
        reopened.database.close()
    }

    private fun repository(): OpenRepository {
        val database = Room.databaseBuilder(context, VelaDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        val store = AppPrivateAttachmentStore(
            File(storageRoot, "attachments"),
            File(storageRoot, "staging"),
        )
        val ids = ArrayDeque(listOf("note-id", "block-id"))
        return OpenRepository(
            database = database,
            repository = OfflineNoteRepository(
                noteDao = database.noteDao(),
                attachmentStore = store,
                clock = VelaClock { 1_000L },
                idGenerator = IdGenerator { ids.removeFirst() },
            ),
        )
    }

    private class OpenRepository(
        val database: VelaDatabase,
        private val repository: OfflineNoteRepository,
    ) : com.vela.notes.domain.repository.NoteRepository by repository
}
