package com.vela.notes.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AudioPlaybackPolicyTest {
    @Test
    fun clampsSeekInsideKnownDuration() {
        assertEquals(0L, clampPlaybackPosition(-10, 5_000))
        assertEquals(1_250L, clampPlaybackPosition(1_250, 5_000))
        assertEquals(5_000L, clampPlaybackPosition(9_000, 5_000))
        assertEquals(0L, clampPlaybackPosition(20, -1))
    }

    @Test
    fun clampsSpeedToSupportedRange() {
        assertEquals(MIN_PLAYBACK_SPEED, clampPlaybackSpeed(0.1f), 0f)
        assertEquals(1.5f, clampPlaybackSpeed(1.5f), 0f)
        assertEquals(MAX_PLAYBACK_SPEED, clampPlaybackSpeed(4f), 0f)
        assertEquals(DEFAULT_PLAYBACK_SPEED, clampPlaybackSpeed(Float.NaN), 0f)
        assertEquals(DEFAULT_PLAYBACK_SPEED, clampPlaybackSpeed(Float.POSITIVE_INFINITY), 0f)
    }

    @Test
    fun speedOptionsStayInsideSupportedRange() {
        assertEquals(listOf(0.5f, 1f, 1.5f, 2f), PLAYBACK_SPEED_OPTIONS)
        PLAYBACK_SPEED_OPTIONS.forEach { speed ->
            assertEquals(speed, clampPlaybackSpeed(speed), 0f)
        }
    }

    @Test
    fun switchingAttachmentResetsTransportAndKeepsClampedSpeed() {
        val state = playbackStateForAttachment("audio-2", 3f)

        assertEquals("audio-2", state.attachmentId)
        assertEquals(MAX_PLAYBACK_SPEED, state.speed, 0f)
        assertEquals(0L, state.positionMs)
        assertEquals(0L, state.durationMs)
        assertFalse(state.isPlaying)
        assertNull(state.errorMessage)
    }

    @Test
    fun releaseClearsActivePlayback() {
        val active = AudioPlaybackState(
            attachmentId = "audio",
            isPlaying = true,
            positionMs = 1_500,
            durationMs = 10_000,
            speed = 1.5f,
            errorMessage = "Không thể phát bản ghi âm.",
        )

        assertEquals(AudioPlaybackState(), active.released())
    }
}
