package com.vela.notes.data.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AudioRecorderRecoveryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val directory = AndroidAudioRecorderEngine.recordingDirectory(context)

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun cleanupRemovesPartialAndUncommittedM4aButKeepsUnrelatedFiles() {
        directory.mkdirs()
        File(directory, "crashed.m4a.part").writeBytes(byteArrayOf(1))
        File(directory, "uncommitted.m4a").writeBytes(byteArrayOf(2))
        val unrelated = File(directory, "keep.txt").apply { writeText("keep") }

        val deleted = AndroidAudioRecorderEngine(context).cleanupStaleFiles()

        assertEquals(2, deleted)
        assertTrue(unrelated.exists())
    }
}
