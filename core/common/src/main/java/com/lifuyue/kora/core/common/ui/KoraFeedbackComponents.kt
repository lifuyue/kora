package com.lifuyue.kora.core.common.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifuyue.kora.core.common.KoraFeedbackPhase

@Composable
fun KoraInlineFeedbackCard(
    phase: KoraFeedbackPhase,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    actionHint: String? = null,
    testTag: String? = null,
) {
    AnimatedVisibility(
        visible = phase != KoraFeedbackPhase.Idle,
        enter = fadeIn(animationSpec = tween(durationMillis = 180)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(durationMillis = 120)) + slideOutVertically(targetOffsetY = { it / 8 }),
        modifier = modifier,
    ) {
        val style = feedbackVisualStyle(phase)
        val iconAlpha = if (phase == KoraFeedbackPhase.InFlightFirstByte || phase == KoraFeedbackPhase.InFlightStreaming || phase == KoraFeedbackPhase.Validating) 0.88f else 1f
        val emphasisScale = animateFloatAsState(
            targetValue = if (phase == KoraFeedbackPhase.SuccessTransient) 1.02f else 1f,
            animationSpec = tween(durationMillis = 220),
            label = "feedback_scale",
        ).value
        Surface(
            shape = MaterialTheme.shapes.large,
            color = style.containerColor,
            contentColor = style.contentColor,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .scale(emphasisScale)
                    .then(if (testTag == null) Modifier else Modifier.testTag(testTag)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.iconTint,
                        modifier =
                            Modifier
                                .size(20.dp)
                                .alpha(iconAlpha),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = style.contentColor.copy(alpha = 0.86f),
                        )
                    }
                }
                supportingText?.let {
                    FeedbackPill(
                        text = it,
                        containerColor = style.badgeColor,
                        contentColor = style.badgeContentColor,
                    )
                }
                actionHint?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = style.contentColor.copy(alpha = 0.74f),
                    )
                }
            }
        }
    }
}

@Composable
fun KoraFeedbackLabel(
    phase: KoraFeedbackPhase,
    text: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    AnimatedVisibility(
        visible = phase != KoraFeedbackPhase.Idle,
        enter = fadeIn(animationSpec = tween(durationMillis = 150)) + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut(animationSpec = tween(durationMillis = 100)),
        modifier = modifier,
    ) {
        val style = feedbackVisualStyle(phase)
        val iconAlpha = if (phase == KoraFeedbackPhase.InFlightFirstByte || phase == KoraFeedbackPhase.InFlightStreaming || phase == KoraFeedbackPhase.Validating) 0.88f else 1f
        Row(
            modifier =
                Modifier
                    .background(style.badgeColor, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .then(if (testTag == null) Modifier else Modifier.testTag(testTag)),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.badgeContentColor,
                modifier = Modifier.size(14.dp).alpha(iconAlpha),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = style.badgeContentColor,
            )
        }
    }
}

@Composable
private fun FeedbackPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Row(
        modifier = Modifier.background(containerColor, CircleShape).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

private data class FeedbackVisualStyle(
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val iconTint: Color,
    val badgeColor: Color,
    val badgeContentColor: Color,
)

@Composable
private fun feedbackVisualStyle(phase: KoraFeedbackPhase): FeedbackVisualStyle {
    val scheme = MaterialTheme.colorScheme
    return when (phase) {
        KoraFeedbackPhase.Validating,
        KoraFeedbackPhase.InFlightFirstByte,
        KoraFeedbackPhase.InFlightStreaming ->
            FeedbackVisualStyle(
                icon = Icons.Default.Info,
                containerColor = scheme.secondaryContainer.copy(alpha = 0.72f),
                contentColor = scheme.onSecondaryContainer,
                iconTint = scheme.secondary,
                badgeColor = scheme.surface.copy(alpha = 0.48f),
                badgeContentColor = scheme.onSurface,
            )
        KoraFeedbackPhase.SuccessTransient,
        KoraFeedbackPhase.SuccessStable ->
            FeedbackVisualStyle(
                icon = Icons.Default.CheckCircle,
                containerColor = scheme.primaryContainer.copy(alpha = if (phase == KoraFeedbackPhase.SuccessTransient) 0.92f else 0.72f),
                contentColor = scheme.onPrimaryContainer,
                iconTint = scheme.primary,
                badgeColor = scheme.surface.copy(alpha = 0.52f),
                badgeContentColor = scheme.onSurface,
            )
        KoraFeedbackPhase.ErrorRecoverable ->
            FeedbackVisualStyle(
                icon = Icons.Default.Close,
                containerColor = scheme.errorContainer,
                contentColor = scheme.onErrorContainer,
                iconTint = scheme.error,
                badgeColor = scheme.surface.copy(alpha = 0.4f),
                badgeContentColor = scheme.onSurface,
            )
        KoraFeedbackPhase.Stopped ->
            FeedbackVisualStyle(
                icon = Icons.Default.Info,
                containerColor = scheme.tertiaryContainer.copy(alpha = 0.72f),
                contentColor = scheme.onTertiaryContainer,
                iconTint = scheme.tertiary,
                badgeColor = scheme.surface.copy(alpha = 0.48f),
                badgeContentColor = scheme.onSurface,
            )
        KoraFeedbackPhase.Idle ->
            FeedbackVisualStyle(
                icon = Icons.Default.Info,
                containerColor = scheme.surfaceContainer,
                contentColor = scheme.onSurface,
                iconTint = scheme.onSurfaceVariant,
                badgeColor = scheme.surfaceVariant,
                badgeContentColor = scheme.onSurfaceVariant,
            )
    }
}
