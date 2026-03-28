package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ConversationListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinnedAndRegularSectionsAreGrouped() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.renderConversationList()

        composeRule.onNodeWithTag("${ChatTestTags.CONVERSATION_ITEM_PREFIX}chat-1").fetchSemanticsNode()
        composeRule.onNodeWithText(context.getString(R.string.conversation_list_clear_all)).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_FOLDER_FILTER).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_TAG_FILTER).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_FAB).assertIsDisplayed()
    }

    @Test
    fun longPressConversationOpensBottomSheetAndDispatchesActions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.renderConversationList()

        composeRule.onNodeWithTag("${ChatTestTags.CONVERSATION_ITEM_PREFIX}chat-1").performTouchInput {
            longClick()
        }
        composeRule.onNodeWithText(context.getString(R.string.conversation_list_action_sheet_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_ACTION_TOGGLE_PIN, useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_ACTION_RENAME, useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_ACTION_MOVE_FOLDER, useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_ACTION_EDIT_TAGS, useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_ACTION_ARCHIVE, useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_ACTION_DELETE, useUnmergedTree = true).fetchSemanticsNode()
    }

    @Test
    @Config(qualifiers = "en")
    fun conversationActionsAndDialogsLocalizeInEnglish() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var cleared = false
        composeRule.renderConversationList(
            uiState =
                ConversationListUiState(
                    items = sampleConversationItems,
                    folders = listOf(ConversationFolderUiModel(folderId = "folder-1", name = "Work")),
                    tags = listOf(ConversationTagUiModel(tagId = "tag-1", name = "Kotlin", colorToken = "sky")),
                ),
            onClearConversations = { cleared = true },
        )

        composeRule.onNodeWithTag("${ChatTestTags.CONVERSATION_ITEM_PREFIX}chat-1").performTouchInput {
            longClick()
        }
        composeRule.onNodeWithText(context.getString(R.string.conversation_list_action_sheet_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_ACTION_RENAME, useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_ACTION_MOVE_FOLDER, useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(context.getString(R.string.conversation_list_move_folder_sheet_title)).fetchSemanticsNode()

        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_TAG_FILTER).performClick()
        composeRule.onNodeWithText(context.getString(R.string.conversation_list_tag_sheet_title)).fetchSemanticsNode()

        composeRule.onNodeWithText(context.getString(R.string.conversation_list_clear_all)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.conversation_list_dialog_clear_all_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.conversation_list_confirm_clear)).performClick()

        assertTrue(cleared)
    }

    @Test
    fun conversationCardShowsFolderAndTagMeta() {
        composeRule.renderConversationList()

        composeRule.onNodeWithText("工作").assertIsDisplayed()
        composeRule.onNodeWithText("Kotlin").assertIsDisplayed()
    }

    @Test
    fun clearAllUsesExplicitConfirmation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var cleared = false

        composeRule.renderConversationList(onClearConversations = { cleared = true })

        composeRule.onNodeWithText(context.getString(R.string.conversation_list_clear_all)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.conversation_list_dialog_clear_all_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.conversation_list_confirm_clear)).performClick()

        assertTrue(cleared)
    }

    @Test
    fun emptyStateStillShowsSearchAndCreateEntry() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.renderConversationList(
            uiState = ConversationListUiState(items = emptyList()),
        )

        composeRule.onNodeWithText(context.getString(R.string.conversation_list_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_SEARCH).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_FAB).assertIsDisplayed()
    }

    @Test
    fun newConversationFabDispatchesCallback() {
        var invoked = false

        composeRule.renderConversationList(onNewConversation = { invoked = true })

        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_FAB).performClick()

        assertTrue(invoked)
    }

    @Test
    fun searchFieldShowsCurrentQuery() {
        composeRule.renderConversationList(
            uiState = ConversationListUiState(query = "架构", items = sampleConversationItems),
        )

        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_SEARCH).assertTextContains("架构")
        composeRule.onAllNodesWithText("架构讨论").assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = "en")
    fun filterChipsUseResourceFallbackLabelsInEnglish() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeRule.renderConversationList(
            uiState = ConversationListUiState(items = sampleConversationItems),
        )

        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_FOLDER_FILTER)
            .assertTextContains(context.getString(R.string.conversation_list_all_folders))
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_TAG_FILTER)
            .assertTextContains(context.getString(R.string.conversation_list_all_tags))
    }
}

private fun ComposeContentTestRule.renderConversationList(
    uiState: ConversationListUiState = ConversationListUiState(items = sampleConversationItems),
    onQueryChanged: (String) -> Unit = {},
    onSelectFolderFilter: (String?) -> Unit = {},
    onSelectTagFilter: (String?) -> Unit = {},
    onToggleShowArchived: (Boolean) -> Unit = {},
    onOpenConversation: (String) -> Unit = {},
    onNewConversation: () -> Unit = {},
    onDeleteConversation: (String) -> Unit = {},
    onRenameConversation: (String, String) -> Unit = { _, _ -> },
    onTogglePin: (String, Boolean) -> Unit = { _, _ -> },
    onClearConversations: () -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    onRenameFolder: (String, String) -> Unit = { _, _ -> },
    onDeleteFolder: (String) -> Unit = {},
    onCreateTag: (String) -> Unit = {},
    onRenameTag: (String, String) -> Unit = { _, _ -> },
    onDeleteTag: (String) -> Unit = {},
    onMoveConversationToFolder: (String, String?) -> Unit = { _, _ -> },
    onSetConversationTags: (String, List<String>) -> Unit = { _, _ -> },
    onSetArchived: (String, Boolean) -> Unit = { _, _ -> },
) {
    setContent {
        ConversationListScreen(
            uiState = uiState,
            onQueryChanged = onQueryChanged,
            onSelectFolderFilter = onSelectFolderFilter,
            onSelectTagFilter = onSelectTagFilter,
            onToggleShowArchived = onToggleShowArchived,
            onOpenConversation = onOpenConversation,
            onNewConversation = onNewConversation,
            onDeleteConversation = onDeleteConversation,
            onRenameConversation = onRenameConversation,
            onTogglePin = onTogglePin,
            onClearConversations = onClearConversations,
            onCreateFolder = onCreateFolder,
            onRenameFolder = onRenameFolder,
            onDeleteFolder = onDeleteFolder,
            onCreateTag = onCreateTag,
            onRenameTag = onRenameTag,
            onDeleteTag = onDeleteTag,
            onMoveConversationToFolder = onMoveConversationToFolder,
            onSetConversationTags = onSetConversationTags,
            onSetArchived = onSetArchived,
        )
    }
}

private val sampleConversationItems =
    listOf(
        ConversationListItemUiModel(
            chatId = "chat-1",
            appId = "app-1",
            title = "架构讨论",
            preview = "最新内容",
            folderId = "folder-1",
            folderName = "工作",
            tags = listOf(ConversationTagUiModel(tagId = "tag-1", name = "Kotlin", colorToken = "sky")),
            isPinned = true,
        ),
        ConversationListItemUiModel(
            chatId = "chat-2",
            appId = "app-1",
            title = "周报整理",
            preview = "待发送",
            isPinned = false,
        ),
    )
