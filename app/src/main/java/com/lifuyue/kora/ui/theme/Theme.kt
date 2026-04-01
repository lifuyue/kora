package com.lifuyue.kora.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.lifuyue.kora.core.common.ThemeMode

private val LightColorScheme: ColorScheme =
    lightColorScheme(
        primary = mdThemeLightPrimary,
        onPrimary = mdThemeLightOnPrimary,
        secondary = mdThemeLightSecondary,
        tertiary = mdThemeLightTertiary,
        background = mdThemeLightBackground,
        surface = mdThemeLightSurface,
        surfaceVariant = mdThemeLightSurfaceVariant,
        outline = mdThemeLightOutline,
        onSurface = mdThemeLightOnSurface,
        onSurfaceVariant = mdThemeLightOnSurfaceVariant,
    )

private val DarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = mdThemeDarkPrimary,
        onPrimary = mdThemeDarkOnPrimary,
        secondary = mdThemeDarkSecondary,
        tertiary = mdThemeDarkTertiary,
        background = mdThemeDarkBackground,
        surface = mdThemeDarkSurface,
        surfaceVariant = mdThemeDarkSurfaceVariant,
        outline = mdThemeDarkOutline,
        onSurface = mdThemeDarkOnSurface,
        onSurfaceVariant = mdThemeDarkOnSurfaceVariant,
    )

@Composable
fun KoraTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KoraTypography,
        content = content,
    )
}
