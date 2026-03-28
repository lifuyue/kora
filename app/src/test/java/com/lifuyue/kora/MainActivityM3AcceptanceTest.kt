package com.lifuyue.kora

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.testing.MockWebServerRule
import com.lifuyue.kora.feature.chat.ChatTestTags
import com.lifuyue.kora.testing.AcceptanceAppHarnessRule
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = KoraApplication::class, sdk = [35])
class MainActivityM3AcceptanceTest {
    private val serverRule = MockWebServerRule()
    private val harnessRule = AcceptanceAppHarnessRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(serverRule).around(harnessRule).around(composeRule)

    @Before
    fun setUp() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @After
    fun tearDown() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun firstLaunchRunsOnboardingRealConnectionAndArrivesAtShell() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        enqueueConnectionSuccess()

        composeRule.onNodeWithText(context.getString(R.string.onboarding_page_1_title)).assertIsDisplayed()

        completeOnboardingAndSaveConnection()

        val request = serverRule.takeRequest()
        assertConnectionTestRequest(request)

        composeRule.onNodeWithText(context.getString(com.lifuyue.kora.feature.chat.R.string.conversation_list_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.nav_chat)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.nav_knowledge)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.nav_settings)).assertIsDisplayed()
    }

    @Test
    fun shellSupportsBottomNavigationAfterRealConnectionSave() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        enqueueConnectionSuccess()

        completeOnboardingAndSaveConnection()

        composeRule.onNodeWithText(context.getString(R.string.nav_knowledge)).performClick()
        composeRule
            .onNodeWithText(
                context.resources.getQuantityString(
                    com.lifuyue.kora.feature.knowledge.R.plurals.knowledge_overview_dataset_count,
                    0,
                    0,
                ),
            ).assertIsDisplayed()
        composeRule
            .onNodeWithText(
                context.getString(com.lifuyue.kora.feature.knowledge.R.string.knowledge_overview_open_datasets),
            ).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.nav_settings)).performClick()
        composeRule
            .onNodeWithText(
                context.getString(com.lifuyue.kora.feature.settings.R.string.settings_connection_title),
            ).assertIsDisplayed()
        composeRule
            .onNodeWithText(
                context.getString(com.lifuyue.kora.feature.settings.R.string.settings_connection_title),
            ).performClick()
        waitForText(context.getString(com.lifuyue.kora.feature.settings.R.string.settings_connection_api_key_summary, ""))

        composeRule.onNodeWithText(context.getString(R.string.nav_chat)).performClick()
        composeRule
            .onNodeWithText(
                context.getString(com.lifuyue.kora.feature.chat.R.string.conversation_list_empty_title),
            ).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_FAB).assertIsDisplayed()
    }

    @Test
    fun changingLanguageInSettingsAppliesEnglishLocaleAtRuntime() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        enqueueConnectionSuccess()

        completeOnboardingAndSaveConnection()

        composeRule.onNodeWithText(context.getString(R.string.nav_settings)).performClick()
        composeRule
            .onNodeWithTag("settings-overview-scroll")
            .performScrollToNode(hasText(context.getString(com.lifuyue.kora.feature.settings.R.string.settings_language_title)))
        composeRule.onNodeWithText(context.getString(com.lifuyue.kora.feature.settings.R.string.settings_language_title)).performClick()
        composeRule.onNodeWithTag("language-option-en").performClick()

        waitForText("English")

        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("Knowledge").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    private fun completeOnboardingAndSaveConnection() {
        val context = composeRule.activity

        composeRule.onNodeWithText(context.getString(R.string.onboarding_cta_next)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.onboarding_cta_next)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.onboarding_cta_finish)).performClick()

        waitForNodeTag("server-url")
        composeRule.onNodeWithTag("server-url").performTextClearance()
        composeRule.onNodeWithTag("server-url").performTextInput(serverRule.url("/api/").toString())
        composeRule.onNodeWithTag("api-key").performTextInput("fastgpt-secret")
        composeRule.onNodeWithTag("test-connection").performClick()

        waitForText(
            context.resources.getQuantityString(
                com.lifuyue.kora.feature.settings.R.plurals.settings_connection_success,
                2,
                2,
                0,
            ).substringBeforeLast("，耗时").substringBeforeLast(" in "),
        )

        composeRule.onNodeWithTag("save-connection").performClick()
        waitForText(context.getString(com.lifuyue.kora.feature.chat.R.string.conversation_list_title))
    }

    private fun enqueueConnectionSuccess() {
        serverRule.enqueueJson(
            """
            {
              "code": 200,
              "statusText": "success",
              "message": "",
              "data": [
                { "_id": "app-1", "name": "Alpha" },
                { "_id": "app-2", "name": "Beta" }
              ]
            }
            """.trimIndent(),
        )
    }

    private fun assertConnectionTestRequest(request: RecordedRequest) {
        assertEquals("POST", request.method)
        assertEquals("/api/core/app/list", request.path)
        assertEquals("Bearer fastgpt-secret", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("{}"))
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun waitForNodeTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
