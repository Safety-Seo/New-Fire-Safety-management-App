package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9), // Light blue accent for dark mode
    onPrimary = Color(0xFF0D1E3A),
    primaryContainer = BoldPrimaryBlue,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFFB74D), // Amber accent for dark mode
    onSecondary = Color(0xFF2C1D00),
    tertiary = Color(0xFF81C784), // Green accent for dark mode
    onTertiary = Color(0xFF00270F),
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkText,
    error = PrimaryRed,
    onError = DarkText
)

private val LightColorScheme = lightColorScheme(
    primary = BoldPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = BoldPrimaryContainer,
    onPrimaryContainer = BoldPrimaryBlue,
    secondary = BoldSecondaryAmber,
    onSecondary = Color.White,
    secondaryContainer = BoldSecondaryContainer,
    tertiary = BoldTertiaryGreen,
    onTertiary = Color.White,
    tertiaryContainer = BoldTertiaryContainer,
    background = LightBackground,
    onBackground = BoldTextSlate,
    surface = LightSurface,
    onSurface = BoldTextSlate,
    surfaceVariant = BoldBorderSlate,
    onSurfaceVariant = BoldTextMuted,
    error = BoldErrorRed,
    onError = Color.White,
    errorContainer = BoldErrorContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false by default to preserve our customized corporate branding look
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
