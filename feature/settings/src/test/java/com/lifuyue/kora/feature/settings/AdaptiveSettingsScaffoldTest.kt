package com.lifuyue.kora.feature.settings

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AdaptiveSettingsScaffoldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun expandedWidthShowsSettingsListAndDetailPaneTogether() {
        composeRule.setContent {
            AdaptiveSettingsScaffold(
                isExpanded = true,
                listPane = { Text("Settings List") },
                detailPane = { Text("Setting Detail") },
            )
        }

        composeRule.onNodeWithText("Settings List").assertIsDisplayed()
        composeRule.onNodeWithText("Setting Detail").assertIsDisplayed()
    }

    @Test
    fun compactWidthShowsOnlyDetailPane() {
        composeRule.setContent {
            AdaptiveSettingsScaffold(
                isExpanded = false,
                listPane = { Text("Settings List") },
                detailPane = { Text("Setting Detail") },
            )
        }

        composeRule.onAllNodesWithText("Settings List").assertCountEquals(0)
        composeRule.onNodeWithText("Setting Detail").assertIsDisplayed()
    }
}
