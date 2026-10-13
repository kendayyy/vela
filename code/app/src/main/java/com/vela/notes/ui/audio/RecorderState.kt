package com.vela.notes.ui.audio

sealed interface RecorderState {
    data object Idle : RecorderState
    data object AwaitingPermission : RecorderState
    data object Ready : RecorderState
    data class Recording(val startedAtMillis: Long, val accumulatedMillis: Long = 0) : RecorderState
    data class Paused(val accumulatedMillis: Long) : RecorderState
    data class Stopping(val durationMillis: Long) : RecorderState
    data class Error(val message: String) : RecorderState
}

sealed interface RecorderEvent {
    data class StartRequested(val permissionGranted: Boolean) : RecorderEvent
    data class PermissionResult(val granted: Boolean) : RecorderEvent
    data class StartSucceeded(val nowMillis: Long) : RecorderEvent
    data class PauseRequested(val nowMillis: Long) : RecorderEvent
    data class ResumeSucceeded(val nowMillis: Long) : RecorderEvent
    data class StopRequested(val nowMillis: Long) : RecorderEvent
    data object Stopped : RecorderEvent
    data class Failed(val message: String) : RecorderEvent
}

fun RecorderState.elapsedMillis(nowMillis: Long): Long = when (this) {
    is RecorderState.Recording -> accumulatedMillis + (nowMillis - startedAtMillis).coerceAtLeast(0)
    is RecorderState.Paused -> accumulatedMillis
    is RecorderState.Stopping -> durationMillis
    else -> 0
}

fun RecorderState.reduce(event: RecorderEvent): RecorderState = when (event) {
    is RecorderEvent.StartRequested -> when (this) {
        RecorderState.Idle,
        is RecorderState.Error,
        -> if (event.permissionGranted) RecorderState.Ready else RecorderState.AwaitingPermission
        else -> this
    }
    is RecorderEvent.PermissionResult -> if (this == RecorderState.AwaitingPermission) {
        if (event.granted) RecorderState.Ready else RecorderState.Error("Quyền microphone đã bị từ chối")
    } else {
        this
    }
    is RecorderEvent.StartSucceeded -> if (this == RecorderState.Ready) {
        RecorderState.Recording(event.nowMillis)
    } else {
        this
    }
    is RecorderEvent.PauseRequested -> if (this is RecorderState.Recording) {
        RecorderState.Paused(accumulatedMillis + (event.nowMillis - startedAtMillis).coerceAtLeast(0))
    } else {
        this
    }
    is RecorderEvent.ResumeSucceeded -> if (this is RecorderState.Paused) {
        RecorderState.Recording(event.nowMillis, accumulatedMillis)
    } else {
        this
    }
    is RecorderEvent.StopRequested -> when (this) {
        is RecorderState.Recording -> RecorderState.Stopping(
            accumulatedMillis + (event.nowMillis - startedAtMillis).coerceAtLeast(0),
        )
        is RecorderState.Paused -> RecorderState.Stopping(accumulatedMillis)
        else -> this
    }
    RecorderEvent.Stopped -> if (this is RecorderState.Stopping) RecorderState.Idle else this
    is RecorderEvent.Failed -> RecorderState.Error(event.message)
}
