package com.vela.notes.data.audio

import android.Manifest
import android.net.Uri
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import java.io.File
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAudioPlaybackControllerTest {
    @get:Rule
    val microphonePermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private var recording: File? = null
    private var controller: AndroidAudioPlaybackController? = null

    @After
    fun tearDown() {
        controller?.let { active ->
            instrumentation.runOnMainSync { active.release() }
        }
        recording?.delete()
        AndroidAudioRecorderEngine(context).cleanupStaleFiles()
    }

    @Test
    fun preparePlaySeekSpeedPauseAndReleaseTwice() {
        val engine = AndroidAudioRecorderEngine(context)
        val file = try {
            engine.start {}
            Thread.sleep(RECORDING_MILLIS)
            engine.stop()
        } catch (error: Throwable) {
            engine.cancel()
            throw error
        }
        recording = file
        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        lateinit var playback: AndroidAudioPlaybackController
        instrumentation.runOnMainSync {
            playback = AndroidAudioPlaybackController(
                context,
                AudioUriResolver { Uri.fromFile(file) },
            )
        }
        controller = playback

        try {
            playback.toggle("rec", file.name)
            fail("ExoPlayer calls must run on the main thread")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains("main thread"))
        }

        instrumentation.runOnMainSync { playback.toggle("rec", file.name) }
        val playing = await(playback) { it.isPlaying && it.durationMs > 0L && it.attachmentId == "rec" }
        assertTrue(
            "durationMs=${playing.durationMs}",
            playing.durationMs in 1L..8_000L,
        )
        assertNull(playing.errorMessage)

        instrumentation.runOnMainSync {
            playback.setSpeed(4f)
            assertEquals(MAX_PLAYBACK_SPEED, playback.state.value.speed, 0f)
            assertTrue(playback.state.value.isPlaying)
            playback.setSpeed(0.1f)
            assertEquals(MIN_PLAYBACK_SPEED, playback.state.value.speed, 0f)
        }

        instrumentation.runOnMainSync { playback.toggle("rec", file.name) }
        val paused = await(playback) { !it.isPlaying && it.attachmentId == "rec" }
        assertEquals(MIN_PLAYBACK_SPEED, paused.speed, 0f)

        val target = paused.durationMs / 2
        instrumentation.runOnMainSync { playback.seekTo(target) }
        val sought = await(playback) { abs(it.positionMs - target) <= SEEK_TOLERANCE_MILLIS }
        assertEquals("rec", sought.attachmentId)
        assertTrue(!sought.isPlaying)

        instrumentation.runOnMainSync { playback.seekTo(-1_000L) }
        val rewound = await(playback) { it.positionMs <= SEEK_TOLERANCE_MILLIS }
        assertTrue(rewound.positionMs >= 0L)

        instrumentation.runOnMainSync {
            playback.release()
            assertEquals(AudioPlaybackState(), playback.state.value)
            playback.release()
            assertEquals(AudioPlaybackState(), playback.state.value)
        }
    }

    private fun await(
        playback: AndroidAudioPlaybackController,
        timeoutMs: Long = 8_000,
        predicate: (AudioPlaybackState) -> Boolean,
    ): AudioPlaybackState {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var latest = playback.state.value
        while (SystemClock.elapsedRealtime() < deadline) {
            latest = playback.state.value
            if (predicate(latest)) return latest
            SystemClock.sleep(50)
        }
        throw AssertionError("Timed out waiting for playback state: $latest")
    }

    private companion object {
        const val RECORDING_MILLIS = 4_000L
        const val SEEK_TOLERANCE_MILLIS = 500L
    }
}
