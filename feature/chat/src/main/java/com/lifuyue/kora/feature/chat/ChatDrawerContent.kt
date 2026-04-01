package com.lifuyue.kora.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal data class ChatDrawerColors(
    val drawerBackground: Color,
    val searchBackground: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val iconBadge: Color,
    val dividerColor: Color,
)

internal fun chatDrawerColors(colorScheme: ColorScheme): ChatDrawerColors =
    ChatDrawerColors(
        drawerBackground = colorScheme.surfaceContainerHigh,
        searchBackground = colorScheme.surfaceContainer,
        primaryText = colorScheme.onSurface,
        secondaryText = colorScheme.onSurfaceVariant,
        iconBadge = colorScheme.onSurface.copy(alpha = 0.08f),
        dividerColor = colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
    )

@Composable
fun ChatDrawerContent(
    uiState: ConversationListUiState,
    onQueryChanged: (String) -> Unit,
    onNewConversation: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenKnowledge: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerColors = chatDrawerColors(MaterialTheme.colorScheme)

    Surface(
        modifier =
            modifier
                .fillMaxHeight()
                .fillMaxWidth(0.84f)
                .testTag(ChatTestTags.CHAT_DRAWER),
        color = drawerColors.drawerBackground,
        contentColor = drawerColors.primaryText,
        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DrawerSearchField(
                value = uiState.query,
                onValueChange = onQueryChanged,
                backgroundColor = drawerColors.searchBackground,
                primaryText = drawerColors.primaryText,
                secondaryText = drawerColors.secondaryText,
            )
            DrawerActionRow(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.chat_drawer_new_conversation),
                tag = ChatTestTags.CHAT_DRAWER_NEW_CHAT,
                onClick = onNewConversation,
                primaryText = drawerColors.primaryText,
                iconBadge = drawerColors.iconBadge,
            )
            DrawerSectionHeader(title = stringResource(R.string.chat_drawer_my_content), color = drawerColors.primaryText)
            DrawerActionRow(
                icon = Icons.Filled.Search,
                title = stringResource(R.string.chat_drawer_knowledge),
                tag = ChatTestTags.CHAT_DRAWER_KNOWLEDGE,
                onClick = onOpenKnowledge,
                primaryText = drawerColors.primaryText,
                iconBadge = drawerColors.iconBadge,
            )
            DrawerSectionHeader(title = stringResource(R.string.chat_drawer_gem_title), color = drawerColors.primaryText)
            DrawerActionRow(
                icon = Icons.Filled.Settings,
                title = stringResource(R.string.chat_drawer_settings),
                tag = ChatTestTags.CHAT_DRAWER_SETTINGS,
                onClick = onOpenSettings,
                primaryText = drawerColors.primaryText,
                iconBadge = drawerColors.iconBadge,
            )
            HorizontalDivider(color = drawerColors.dividerColor)
            DrawerSectionHeader(title = stringResource(R.string.chat_drawer_history_title), color = drawerColors.primaryText)
            if (uiState.items.isEmpty()) {
                Text(
                    text = stringResource(R.string.chat_drawer_empty_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = drawerColors.secondaryText,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    uiState.items.take(12).forEach { item ->
                        ConversationDrawerItem(
                            item = item,
                            onClick = { onOpenConversation(item.chatId) },
                            primaryText = drawerColors.primaryText,
                            secondaryText = drawerColors.secondaryText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    backgroundColor: Color,
    primaryText: Color,
    secondaryText: Color,
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.chat_drawer_search_placeholder),
                tint = secondaryText,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().testTag(ChatTestTags.CHAT_DRAWER_SEARCH),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = primaryText),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { innerField ->
                    if (value.isBlank()) {
                        Text(
                            stringResource(R.string.chat_drawer_search_placeholder),
                            color = secondaryText,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    innerField()
                },
            )
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun DrawerActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tag: String,
    onClick: () -> Unit,
    primaryText: Color,
    iconBadge: Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 8.dp)
                .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBadge),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = primaryText)
            }
            Text(title, style = MaterialTheme.typography.bodyLarge, color = primaryText)
        }
    }
}

@Composable
private fun ConversationDrawerItem(
    item: ConversationListItemUiModel,
    onClick: () -> Unit,
    primaryText: Color,
    secondaryText: Color,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.preview.isNotBlank()) {
            Text(
                text = item.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
