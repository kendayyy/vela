package com.vela.notes.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

class VelaContrastTest {
    @Test
    fun lightThemeTextAndIconsClearTheAppBackground() {
        assertTrue(contrast(VelaInk, VelaBackground) >= 4.5)
        assertTrue(contrast(VelaMutedInk, VelaBackground) >= 4.5)
        assertTrue(contrast(VelaTeal, VelaBackground) >= 4.5)
        assertTrue(contrast(VelaError, VelaBackground) >= 4.5)
        assertTrue(contrast(VelaLightColors.primary, VelaBackground) >= 4.5)
        assertTrue(contrast(VelaLightColors.onPrimary, VelaLightColors.primary) >= 4.5)
        assertTrue(contrast(VelaOnAmber, VelaAmber) >= 4.5)
    }

    @Test
    fun darkThemeTextAndIconsClearTheBackground() {
        assertTrue(contrast(VelaDarkInk, VelaDarkBackground) >= 4.5)
        assertTrue(contrast(VelaDarkMutedInk, VelaDarkBackground) >= 4.5)
        assertTrue(contrast(VelaAmberDark, VelaDarkBackground) >= 3.0)
        assertTrue(contrast(VelaTealDark, VelaDarkBackground) >= 3.0)
        assertTrue(contrast(VelaDarkError, VelaDarkBackground) >= 4.5)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val lighter = max(foreground.luminance(), background.luminance())
        val darker = min(foreground.luminance(), background.luminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun Color.luminance(): Double {
        fun channel(value: Float): Double {
            return if (value <= 0.04045f) {
                value / 12.92
            } else {
                ((value + 0.055) / 1.055).pow(2.4)
            }
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }
}
