package com.github.rahmnathan.localmovies.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark theme with neutral grays
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF3D3D3D),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFB0B0B0),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFE8E8E8),
    tertiary = Color(0xFFCCA876),
    onTertiary = Color(0xFF2A1F0F),
    tertiaryContainer = Color(0xFF4A3A25),
    onTertiaryContainer = Color(0xFFF5E6D0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE5E5E5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE5E5E5),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFCACACA),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF444444)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF9C27B0),
    tertiary = Color(0xFF388E3C),
    background = Color.White,
    surface = Color(0xFFFAFAFA),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun LocalMoviesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
