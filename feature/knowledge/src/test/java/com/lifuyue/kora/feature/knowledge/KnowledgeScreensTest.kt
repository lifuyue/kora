package com.lifuyue.kora.feature.knowledge

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class KnowledgeScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chunkViewerHighlightsCitationTarget() {
        composeRule.setContent {
            ChunkViewerScreen(
                state =
                    ChunkViewerUiState(
                        datasetId = "dataset-1",
                        collectionId = "collection-1",
                        highlightedDataId = "data-1",
                        items =
                            listOf(
                                ChunkItemUiModel(
                                    dataId = "data-1",
                                    chunkIndex = 1,
                                    question = "命中结果",
                                    answer = "片段内容",
                                    status = "active",
                                ),
                            ),
                    ),
                onBack = {},
                onStartEditing = {},
                onQuestionChanged = {},
                onAnswerChanged = {},
                onDisabledChanged = {},
                onSave = {},
                onDelete = {},
                onLoadMore = {},
            )
        }

        composeRule.onNodeWithText("当前引用命中").assertIsDisplayed()
        composeRule.onNodeWithText("命中结果").assertIsDisplayed()
        composeRule.onNodeWithText("片段内容").assertIsDisplayed()
    }
}
