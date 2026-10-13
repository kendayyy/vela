package com.vela.notes.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class VelaThemeTokensTest {
    @Test
    fun brandColorsMatchUiSpecification() {
        assertEquals(Color(0xFF315EFB), VelaLightColors.primary)
        assertEquals(Color(0xFFF5A623), VelaAmber)
        assertEquals(Color(0xFF8EA8FF), VelaDarkColors.primary)
        assertEquals(Color(0xFF0F6B68), VelaLightColors.secondary)
        assertEquals(Color(0xFF5AC8C5), VelaDarkColors.secondary)
        assertEquals(Color(0xFFF7F8FC), VelaLightColors.background)
        assertEquals(Color(0xFF111318), VelaDarkColors.background)
    }

    @Test
    fun dimensionsAndTypographyMatchUiRules() {
        assertEquals(48.dp, VelaSize.MinimumTouchTarget)
        assertEquals(8.dp, VelaSpacing.Sm)
        assertEquals(0.sp, VelaTypography.displaySmall.letterSpacing)
        assertEquals(0.sp, VelaTypography.bodyLarge.letterSpacing)
    }
}
