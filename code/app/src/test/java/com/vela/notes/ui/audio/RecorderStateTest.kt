package com.vela.notes.ui.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class RecorderStateTest {
    @Test
    fun permissionFlowMovesToReadyOnlyWhenGranted() {
        val waiting = RecorderState.Idle.reduce(RecorderEvent.StartRequested(permissionGranted = false))
        val denied = waiting.reduce(RecorderEvent.PermissionResult(granted = false))
        val ready = waiting.reduce(RecorderEvent.PermissionResult(granted = true))

        assertEquals(RecorderState.AwaitingPermission, waiting)
        assertEquals(RecorderState.Error("Quyền microphone đã bị từ chối"), denied)
        assertEquals(RecorderState.Ready, ready)
    }

    @Test
    fun grantedStartPauseResumeAndStopPreserveElapsedDuration() {
        val ready = RecorderState.Idle.reduce(RecorderEvent.StartRequested(permissionGranted = true))
        val recording = ready.reduce(RecorderEvent.StartSucceeded(nowMillis = 1_000))
        val paused = recording.reduce(RecorderEvent.PauseRequested(nowMillis = 2_500))
        val resumed = paused.reduce(RecorderEvent.ResumeSucceeded(nowMillis = 5_000))
        val stopping = resumed.reduce(RecorderEvent.StopRequested(nowMillis = 5_500))

        assertEquals(RecorderState.Paused(1_500), paused)
        assertEquals(RecorderState.Recording(5_000, 1_500), resumed)
        assertEquals(RecorderState.Stopping(2_000), stopping)
        assertEquals(RecorderState.Idle, stopping.reduce(RecorderEvent.Stopped))
    }

    @Test
    fun invalidTransitionsAreNoOpsAndFailuresAreRecoverable() {
        assertEquals(
            RecorderState.Idle,
            RecorderState.Idle.reduce(RecorderEvent.PauseRequested(nowMillis = 1_000)),
        )
        val failed = RecorderState.Ready.reduce(RecorderEvent.Failed("mic busy"))
        val retry = failed.reduce(RecorderEvent.StartRequested(permissionGranted = true))

        assertEquals(RecorderState.Error("mic busy"), failed)
        assertEquals(RecorderState.Ready, retry)
    }

    @Test
    fun elapsedTimeFreezesWhilePausedAndIncludesPreviousSegments() {
        assertEquals(3_500, RecorderState.Recording(2_000, 500).elapsedMillis(5_000))
        assertEquals(1_250, RecorderState.Paused(1_250).elapsedMillis(99_000))
        assertEquals(2_000, RecorderState.Stopping(2_000).elapsedMillis(99_000))
        assertEquals(0, RecorderState.Idle.elapsedMillis(99_000))
    }
}
