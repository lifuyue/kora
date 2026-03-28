package com.lifuyue.kora.feature.chat

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
class AppAnalyticsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun analyticsScreenShowsSummaryCardsWhenDataExists() {
        composeRule.setContent {
            AppAnalyticsScreen(
                uiState =
                    AppAnalyticsUiState(
                        requestCount = 20,
                        conversationCount = 5,
                        inputTokens = 1000L,
                        outputTokens = 2000L,
                        status = AnalyticsStatus.Success,
                    ),
                onRangeChanged = {},
            )
        }

        composeRule.onNodeWithText("20").assertIsDisplayed()
        composeRule.onNodeWithText("5").assertIsDisplayed()
        composeRule.onNodeWithText("1000").assertIsDisplayed()
    }
}
