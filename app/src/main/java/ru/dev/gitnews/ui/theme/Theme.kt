package ru.dev.gitnews.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object GitNewsTheme {
    val colors: GitNewsColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGitNewsColors.current

    val typography: GitNewsTypography
        get() = Typography
}

@Composable
fun GitNewsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            background = colors.surfaceCanvas,
            surface = colors.surfacePanel,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            primary = colors.textPrimary,
            onPrimary = colors.textInverse,
            secondary = colors.surfaceElevated,
            onSecondary = colors.textSecondary
        )
    } else {
        lightColorScheme(
            background = colors.surfaceCanvas,
            surface = colors.surfacePanel,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            primary = colors.textPrimary,
            onPrimary = colors.textInverse,
            secondary = colors.surfaceElevated,
            onSecondary = colors.textSecondary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalGitNewsColors provides colors
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}
