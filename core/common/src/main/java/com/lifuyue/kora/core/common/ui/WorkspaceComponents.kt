package com.lifuyue.kora.core.common.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun KoraWorkspaceHeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    meta: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .background(
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                                ),
                        ),
                    ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                eyebrow?.let {
                    Text(
                        text = it.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                meta?.let {
                    WorkspaceMetaPill(text = it)
                }
            }
        }
    }
}

@Composable
fun KoraWorkspaceSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun WorkspaceMetaPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun KoraSectionCard(
    modifier: Modifier = Modifier,
    tag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.then(if (tag == null) Modifier else Modifier.testTag(tag)),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        border = null,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun KoraMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun KoraGeminiTopBar(
    title: String,
    onOpenDrawer: () -> Unit,
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, start = 8.dp, end = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clickable(onClick = onOpenDrawer)
                    .testTag("global_drawer_button"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Menu, contentDescription = "menu", tint = onSurface)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = onSurface,
            fontWeight = FontWeight.Medium,
        )
        GeminiAvatarButton(onClick = onOpenProfile)
    }
}

@Composable
fun GeminiAvatarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val avatarBackground = if (isLightTheme) Color.White else Color(0xFF121212)
    val avatarTint = if (isLightTheme) Color(0xFF5B616C) else Color(0xFFE1E1E1)
    val gradientColors =
        listOf(
            Color(0xFF4285F4),
            Color(0xFFEA4335),
            Color(0xFFFBBC05),
            Color(0xFF34A853),
        )
    Box(
        modifier =
            modifier
                .size(42.dp)
                .border(2.dp, Brush.sweepGradient(gradientColors), CircleShape)
                .background(avatarBackground, CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Person, contentDescription = "profile", tint = avatarTint)
    }
}
