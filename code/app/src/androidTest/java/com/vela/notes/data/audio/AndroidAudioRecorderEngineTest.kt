package com.vela.notes.data.audio

import android.Manifest
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class AndroidAudioRecorderEngineTest {
    @get:Rule
    val microphonePermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val engine = AndroidAudioRecorderEngine(context)

    @After
    fun tearDown() {
        engine.cancel()
        engine.cleanupStaleFiles()
    }

    @Test
    fun recordsFinalizesAndExtractsWaveformOnDevice() {
        runBlocking {
            val partial = engine.start(onMaximumDurationReached = {})
            Thread.sleep(1_500)
            val completed = engine.stop()
            val waveform = AndroidAudioWaveformExtractor().extract(completed)

            assertTrue(partial.name.endsWith(".part"))
            assertTrue(completed.name.endsWith(".m4a"))
            assertTrue(completed.exists())
            assertTrue(completed.length() > 0)
            assertEquals(DEFAULT_WAVEFORM_PEAKS, waveform.size)
            assertTrue(waveform.all { it.toInt() in 0..100 })
            completed.delete()
        }
    }
}
