package com.lifuyue.kora.feature.chat

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatDrawerContentTest {
    @Test
    fun drawerUsesActiveLightThemeColors() {
        val colorScheme =
            lightColorScheme(
                surfaceContainerHigh = Color(0xFFF7F2FA),
                surfaceContainer = Color(0xFFEEE8F4),
                onSurface = Color(0xFF1D1B20),
                onSurfaceVariant = Color(0xFF49454F),
            )

        val colors = chatDrawerColors(colorScheme)

        assertEquals(colorScheme.surfaceContainerHigh, colors.drawerBackground)
        assertEquals(colorScheme.surfaceContainer, colors.searchBackground)
        assertEquals(colorScheme.onSurface, colors.primaryText)
        assertEquals(colorScheme.onSurfaceVariant, colors.secondaryText)
        assertEquals(colorScheme.onSurface.copy(alpha = 0.08f), colors.iconBadge)
        assertEquals(colorScheme.onSurfaceVariant.copy(alpha = 0.12f), colors.dividerColor)
    }
}
