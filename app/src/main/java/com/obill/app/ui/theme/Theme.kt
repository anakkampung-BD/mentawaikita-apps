package com.obill.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = OrangeBrand,
    onPrimary = Color.White,
    primaryContainer = OrangeBrand.copy(alpha = 0.15f),
    secondary = BlueBrand,
    onSecondary = Color.White,
    tertiary = BlueBrand.copy(alpha = 0.85f),
    background = PageBackground,
    surface = CardSurface,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
)

private val DarkColors = darkColorScheme(
    primary = OrangeBrand,
    onPrimary = Color.Black,
    secondary = BlueBrand,
    onSecondary = Color.White,
    background = Color(0xFF121316),
    surface = Color(0xFF1E1F24),
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6),
)

@Composable
fun ObillTheme(
    darkTheme: Boolean = false,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = scheme,
        typography = obillTypography(textScale),
        content = content,
    )
}
