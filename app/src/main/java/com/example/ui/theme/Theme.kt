package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkSkyPrimary,
    onPrimary = OnDarkSkyPrimary,
    primaryContainer = DarkNavyContainer,
    onPrimaryContainer = OnDarkNavyContainer,
    secondary = DarkAmberSecondary,
    onSecondary = OnDarkAmberSecondary,
    secondaryContainer = DarkCopperContainer,
    onSecondaryContainer = OnDarkCopperContainer,
    tertiary = DarkCyanTertiary,
    onTertiary = OnDarkCyanTertiary,
    tertiaryContainer = DarkBlueprintContainer,
    onTertiaryContainer = OnDarkBlueprintContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = OnNavyPrimary,
    primaryContainer = NavyContainer,
    onPrimaryContainer = OnNavyContainer,
    secondary = CopperSecondary,
    onSecondary = OnCopperSecondary,
    secondaryContainer = CopperContainer,
    onSecondaryContainer = OnCopperContainer,
    tertiary = BlueprintTertiary,
    onTertiary = OnBlueprintTertiary,
    tertiaryContainer = BlueprintContainer,
    onTertiaryContainer = OnBlueprintContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

@Composable
fun Bs7671CableSizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
