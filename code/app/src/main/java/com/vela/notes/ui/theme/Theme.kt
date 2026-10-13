package com.vela.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val VelaLightColors = lightColorScheme(
    primary = VelaCobalt,
    onPrimary = VelaOnCobalt,
    primaryContainer = VelaCobaltContainer,
    onPrimaryContainer = VelaOnCobaltDark,
    secondary = VelaTeal,
    onSecondary = VelaOnTeal,
    secondaryContainer = VelaTeal.copy(alpha = 0.14f),
    onSecondaryContainer = VelaTeal,
    tertiary = VelaAmberOnLight,
    onTertiary = VelaOnCobalt,
    tertiaryContainer = VelaAmberContainer,
    onTertiaryContainer = VelaOnAmber,
    background = VelaBackground,
    onBackground = VelaInk,
    surface = VelaSurface,
    onSurface = VelaInk,
    surfaceVariant = VelaMutedSurface,
    onSurfaceVariant = VelaMutedInk,
    outline = VelaMutedInk,
    outlineVariant = VelaHairline,
    error = VelaError,
    onError = VelaOnTeal,
    errorContainer = VelaErrorContainer,
    onErrorContainer = VelaError,
)

internal val VelaDarkColors = darkColorScheme(
    primary = VelaCobaltDark,
    onPrimary = VelaOnCobaltDark,
    primaryContainer = VelaCobaltContainerDark,
    onPrimaryContainer = Color.White,
    secondary = VelaTealDark,
    onSecondary = VelaDarkOnTeal,
    secondaryContainer = VelaTealDark.copy(alpha = 0.18f),
    onSecondaryContainer = VelaTealDark,
    tertiary = VelaAmberDark,
    onTertiary = VelaDarkOnAmber,
    tertiaryContainer = VelaDarkAmberContainer,
    onTertiaryContainer = VelaAmberDark,
    background = VelaDarkBackground,
    onBackground = VelaDarkInk,
    surface = VelaDarkSurface,
    onSurface = VelaDarkInk,
    surfaceVariant = VelaDarkSurfaceVariant,
    onSurfaceVariant = VelaDarkMutedInk,
    outline = VelaDarkMutedInk,
    outlineVariant = VelaDarkHairline,
    error = VelaDarkError,
    onError = VelaDarkOnError,
    errorContainer = VelaDarkErrorContainer,
    onErrorContainer = VelaDarkError,
)

@Composable
fun VelaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) VelaDarkColors else VelaLightColors,
        typography = VelaTypography,
        shapes = VelaShapes,
        content = content,
    )
}
