package com.example.hrnext.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = FrappeBlue,
    onPrimary = Color_White,
    primaryContainer = FrappeBlueLight,
    onPrimaryContainer = FrappeBlueDark,
    secondary = HrmsTeal,
    onSecondary = Color_White,
    secondaryContainer = HrmsTealLight,
    onSecondaryContainer = HrmsTeal,
    background = NeutralBackgroundLight,
    onBackground = Color_Ink,
    surface = NeutralSurfaceLight,
    onSurface = Color_Ink,
    surfaceVariant = NeutralSurfaceVariantLight,
    onSurfaceVariant = Color_Ink.copy(alpha = 0.7f),
    outlineVariant = Color(0xFFE1E6F2),
    error = ErrorRed,
    errorContainer = ErrorRedContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = FrappeBlue,
    onPrimary = Color_White,
    primaryContainer = FrappeBlueDark,
    onPrimaryContainer = FrappeBlueLight,
    secondary = HrmsTeal,
    onSecondary = Color_White,
    secondaryContainer = HrmsTeal,
    onSecondaryContainer = HrmsTealLight,
    background = NeutralBackgroundDark,
    onBackground = Color_White,
    surface = NeutralSurfaceDark,
    onSurface = Color_White,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = Color_White.copy(alpha = 0.75f),
    outlineVariant = Color(0xFF323A50),
    error = Color(0xFFFFB4AB),
)

@Composable
fun HRNextTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HRNextTypography,
        content = content,
    )
}
