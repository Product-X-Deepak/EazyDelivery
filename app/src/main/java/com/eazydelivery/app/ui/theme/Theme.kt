package com.eazydelivery.app.ui.theme

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

// Light color scheme with refined palette
private val LightColorScheme = lightColorScheme(
    primary = EazyBlue,
    onPrimary = White,
    primaryContainer = LightBlue,
    onPrimaryContainer = DarkBlue,
    secondary = EazyGreen,
    onSecondary = White,
    secondaryContainer = LightGreen,
    onSecondaryContainer = DarkGreen,
    tertiary = EazyOrange,
    onTertiary = White,
    tertiaryContainer = LightOrange,
    onTertiaryContainer = DarkOrange,
    error = ErrorRed,
    onError = White,
    errorContainer = LightErrorRed,
    onErrorContainer = DarkErrorRed,
    background = BackgroundLight,
    onBackground = TextDark,
    surface = SurfaceLight,
    onSurface = TextDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextMedium,
    outline = OutlineLight
)

// Dark color scheme with refined palette
private val DarkColorScheme = darkColorScheme(
    primary = EazyBlueDark,
    onPrimary = White,
    primaryContainer = DarkBlue,
    onPrimaryContainer = LightBlue,
    secondary = EazyGreenDark,
    onSecondary = White,
    secondaryContainer = DarkGreen,
    onSecondaryContainer = LightGreen,
    tertiary = EazyOrangeDark,
    onTertiary = White,
    tertiaryContainer = DarkOrange,
    onTertiaryContainer = LightOrange,
    error = ErrorRedDark,
    onError = White,
    errorContainer = DarkErrorRed,
    onErrorContainer = LightErrorRed,
    background = BackgroundDark,
    onBackground = TextLight,
    surface = SurfaceDark,
    onSurface = TextLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextLightMedium,
    outline = OutlineDark
)

@Composable
fun EazyDeliveryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
