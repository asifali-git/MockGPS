package com.mockgps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FD8EC),
    onPrimary = Color(0xFF00373E),
    primaryContainer = Color(0xFF00505A),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = Color(0xFFB5C9DA),
    onSecondary = Color(0xFF1F333E),
    secondaryContainer = Color(0xFF354955),
    onSecondaryContainer = Color(0xFFD1E6F5),
    tertiary = Color(0xFFCCC2DC),
    onTertiary = Color(0xFF322A43),
    tertiaryContainer = Color(0xFF4A425C),
    onTertiaryContainer = Color(0xFFE8DEF0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C1E),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF191C1E),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF3F484B),
    onSurfaceVariant = Color(0xFFBFC9CC),
    outline = Color(0xFF899295),
    outlineVariant = Color(0xFF3F484B),
    shadow = Color(0xFF000000),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E3E3),
    inverseOnSurface = Color(0xFF191C1E),
    inversePrimary = Color(0xFF006875),
    surfaceTint = Color(0xFF4FD8EC),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006875),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF002025),
    secondary = Color(0xFF4E626E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E6F5),
    onSecondaryContainer = Color(0xFF081E28),
    tertiary = Color(0xFF635B75),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8DEF0),
    onTertiaryContainer = Color(0xFF1E192B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8FDFF),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFF8FDFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDAE5E9),
    onSurfaceVariant = Color(0xFF3F484B),
    outline = Color(0xFF6F797C),
    outlineVariant = Color(0xFFBFC9CC),
    shadow = Color(0xFF000000),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2E3133),
    inverseOnSurface = Color(0xFFF0F0F0),
    inversePrimary = Color(0xFF4FD8EC),
    surfaceTint = Color(0xFF006875),
)

@Composable
fun Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.statusBarColor = colorScheme.background.toArgb()
                it.navigationBarColor = colorScheme.background.toArgb()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}