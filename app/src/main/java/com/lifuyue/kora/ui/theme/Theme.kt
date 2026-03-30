package com.lifuyue.kora.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

private val OledDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = mdThemeDarkPrimary,
        onPrimary = mdThemeDarkOnPrimary,
        secondary = mdThemeDarkSecondary,
        tertiary = mdThemeDarkTertiary,
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = mdThemeDarkSurfaceVariant,
        outline = mdThemeDarkOutline,
        onSurface = mdThemeDarkOnSurface,
        onSurfaceVariant = mdThemeDarkOnSurfaceVariant,
    )

@Composable
fun KoraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    oledEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.OLED_DARK -> true
        }

    val colorScheme =
        when {
            dynamicColor && themeMode == ThemeMode.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            oledEnabled || themeMode == ThemeMode.OLED_DARK -> OledDarkColorScheme
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KoraTypography,
        content = content,
    )
}
