package com.vela.notes.ui.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusCapsuleStateTest {
    @Test
    fun hiddenHasNoPresentation() {
        assertNull(FocusCapsuleState.Hidden.toPresentation())
    }

    @Test
    fun ocrProgressIsClamped() {
        val presentation = FocusCapsuleState.Ocr(completed = 7, total = 5).toPresentation()

        assertEquals("Đang nhận dạng 5/5", presentation?.label)
        assertEquals(1f, presentation?.progress)
    }

    @Test
    fun recordingUsesStableTimerText() {
        assertEquals(
            "Đang ghi 02:05",
            FocusCapsuleState.Recording(elapsedSeconds = 125, isPaused = false).toPresentation()?.label,
        )
        assertEquals(
            "Tạm dừng 00:00",
            FocusCapsuleState.Recording(elapsedSeconds = -1, isPaused = true).toPresentation()?.label,
        )
    }
}
