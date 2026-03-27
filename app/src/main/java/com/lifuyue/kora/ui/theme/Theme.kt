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
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme: ColorScheme =
    lightColorScheme(
        primary = mdThemeLightPrimary,
        onPrimary = mdThemeLightOnPrimary,
        secondary = mdThemeLightSecondary,
        background = mdThemeLightBackground,
        surface = mdThemeLightSurface,
    )

private val DarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = mdThemeDarkPrimary,
        onPrimary = mdThemeDarkOnPrimary,
        secondary = mdThemeDarkSecondary,
        background = mdThemeDarkBackground,
        surface = mdThemeDarkSurface,
    )

@Composable
fun KoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KoraTypography,
        content = content,
    )
}
