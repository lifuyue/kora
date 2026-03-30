package com.lifuyue.kora.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun KoraShellDrawer(
    currentAppId: String?,
    onOpenChat: () -> Unit,
    onOpenKnowledge: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val drawerSurface = if (isLightTheme) Color(0xFFF8FAFC) else Color(0xFF292A2D)
    val searchSurface = if (isLightTheme) Color.White else Color(0xFF1F2023)
    val primaryText = if (isLightTheme) Color(0xFF23262B) else Color.White
    val secondaryText = if (isLightTheme) Color(0xFF6C7280) else Color.White.copy(alpha = 0.58f)
    val iconContainer = if (isLightTheme) Color(0xFFF0F2F5) else Color.White.copy(alpha = 0.06f)

    Surface(
        modifier = modifier.fillMaxHeight().fillMaxWidth(0.84f),
        color = drawerSurface,
        contentColor = primaryText,
        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(color = searchSurface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    RowLikeSearch(primaryText = primaryText, secondaryText = secondaryText)
                }
            }
            DrawerActionRow(
                iconContainer = iconContainer,
                primaryText = primaryText,
                icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = primaryText) },
                title = "发起新对话",
                onClick = onOpenChat,
            )
            HorizontalDivider(color = secondaryText.copy(alpha = 0.2f))
            DrawerSectionLabel("工作区", primaryText)
            DrawerActionRow(
                iconContainer = iconContainer,
                primaryText = primaryText,
                icon = { Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = primaryText) },
                title = "聊天",
                subtitle = currentAppId?.let { "当前应用 $it" },
                onClick = onOpenChat,
            )
            DrawerActionRow(
                iconContainer = iconContainer,
                primaryText = primaryText,
                icon = { Icon(Icons.Filled.Search, contentDescription = null, tint = primaryText) },
                title = "知识库",
                onClick = onOpenKnowledge,
            )
            DrawerActionRow(
                iconContainer = iconContainer,
                primaryText = primaryText,
                icon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = primaryText) },
                title = "设置",
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun RowLikeSearch(
    primaryText: Color,
    secondaryText: Color,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = secondaryText)
        val value = remember { "" }
        BasicTextField(
            value = value,
            onValueChange = {},
            enabled = false,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = primaryText),
            decorationBox = {
                Text("搜索对话", color = secondaryText, style = MaterialTheme.typography.bodyLarge)
            },
        )
    }
}

@Composable
private fun DrawerSectionLabel(
    title: String,
    color: Color,
) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
}

@Composable
private fun DrawerActionRow(
    iconContainer: Color,
    primaryText: Color,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(iconContainer),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = primaryText)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryText.copy(alpha = 0.56f),
                )
            }
        }
    }
}
