package com.vela.notes.ui.shell

import java.util.Locale

sealed interface FocusCapsuleState {
    data object Hidden : FocusCapsuleState
    data object Saved : FocusCapsuleState
    data object Saving : FocusCapsuleState
    data object SaveFailed : FocusCapsuleState
    data class Ocr(val completed: Int, val total: Int) : FocusCapsuleState
    data class Recording(val elapsedSeconds: Long, val isPaused: Boolean) : FocusCapsuleState
}

internal data class FocusCapsulePresentation(
    val label: String,
    val progress: Float?,
    val isError: Boolean = false,
)

internal fun FocusCapsuleState.toPresentation(): FocusCapsulePresentation? = when (this) {
    FocusCapsuleState.Hidden -> null
    FocusCapsuleState.Saved -> FocusCapsulePresentation(label = "Đã lưu", progress = 1f)
    FocusCapsuleState.Saving -> FocusCapsulePresentation(label = "Đang lưu", progress = null)
    FocusCapsuleState.SaveFailed -> FocusCapsulePresentation(label = "Chưa thể lưu", progress = null, isError = true)
    is FocusCapsuleState.Ocr -> {
        val safeTotal = total.coerceAtLeast(1)
        val safeCompleted = completed.coerceIn(0, safeTotal)
        FocusCapsulePresentation(
            label = "Đang nhận dạng $safeCompleted/$safeTotal",
            progress = safeCompleted.toFloat() / safeTotal,
        )
    }
    is FocusCapsuleState.Recording -> FocusCapsulePresentation(
        label = if (isPaused) "Tạm dừng ${elapsedSeconds.toTimer()}" else "Đang ghi ${elapsedSeconds.toTimer()}",
        progress = null,
    )
}

private fun Long.toTimer(): String {
    val safeSeconds = coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d", safeSeconds / 60, safeSeconds % 60)
}
