package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode(val key: String, val label: String) {
    LIGHT("light", "Pure Light Mode");

    companion object {
        fun fromKey(@Suppress("UNUSED_PARAMETER") key: String?): ThemeMode = LIGHT
    }
}

private val LightColorScheme = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = Color.White,
    primaryContainer = SkySoft,
    onPrimaryContainer = SkyDeep,
    secondary = SkyBright,
    onSecondary = Color.White,
    secondaryContainer = SkyMist,
    onSecondaryContainer = SkyDeep,
    tertiary = CyanGlow,
    onTertiary = LightOnSurface,
    tertiaryContainer = SkyMist,
    onTertiaryContainer = SkyDeep,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceBright = Color.White,
    surfaceDim = LightSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF6FAFF),
    surfaceContainerHigh = Color(0xFFEFF6FD),
    surfaceContainerHighest = Color(0xFFE6F0FA),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = Color(0xFFDC2626),
    onError = Color.White
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ChattingAppTheme(
    @Suppress("UNUSED_PARAMETER") themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
