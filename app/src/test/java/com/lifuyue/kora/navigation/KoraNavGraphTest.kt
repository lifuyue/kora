package com.lifuyue.kora.navigation

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.KoraApplication
import com.lifuyue.kora.R
import com.lifuyue.kora.core.common.AppearancePreferences
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.database.store.ShareLinkPayload
import com.lifuyue.kora.core.common.ThemeMode
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class KoraNavGraphTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @After
    fun tearDown() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun onboardingIsShownWhenNotCompleted() {
        val context = ApplicationProvider.getApplicationContext<KoraApplication>()
        val title = context.getString(R.string.onboarding_page_1_title)
        val indicator = context.getString(R.string.onboarding_page_indicator, 1, 3)

        composeRule.setContent {
            KoraNavGraph(snapshot = ConnectionSnapshot())
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText(title).assertIsDisplayed()
            }.isSuccess
        }

        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithText(indicator).assertIsDisplayed()
    }

    @Test
    fun finishingOnboardingTriggersCompletionCallback() {
        var completed = false
        val context = ApplicationProvider.getApplicationContext<KoraApplication>()
        val next = context.getString(R.string.onboarding_cta_next)
        val finish = context.getString(R.string.onboarding_cta_finish)

        composeRule.setContent {
            KoraNavGraph(
                snapshot = ConnectionSnapshot(),
                onOnboardingCompleted = { completed = true },
                connectionRoute = {
                    Text("Fake Connection")
                },
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText(next).assertIsDisplayed()
            }.isSuccess
        }

        composeRule.onNodeWithText(next).performClick()
        composeRule.onNodeWithText(next).performClick()
        composeRule.onNodeWithText(finish).performClick()

        composeRule.onNodeWithText("Fake Connection").assertIsDisplayed()
        assertTrue(completed)
    }

    @Test
    fun connectionScreenIsShownWhenOnboardingCompleteButConnectionMissing() {
        composeRule.setContent {
            KoraNavGraph(
                snapshot =
                    ConnectionSnapshot(
                        onboardingCompleted = true,
                    ),
                connectionRoute = {
                    Text("Fake Connection")
                },
            )
        }

        composeRule.onNodeWithText("Fake Connection").assertIsDisplayed()
    }

    @Test
    fun shellIsShownWhenConnectionReady() {
        composeRule.setContent {
            KoraNavGraph(
                snapshot =
                    ConnectionSnapshot(
                        connectionType = ConnectionType.FAST_GPT,
                        serverBaseUrl = "https://api.fastgpt.in/",
                        apiKey = "fastgpt-secret",
                        selectedAppId = "app-1",
                        onboardingCompleted = true,
                        appearancePreferences =
                            AppearancePreferences(
                                themeMode = ThemeMode.DARK,
                            ),
                    ),
                shellRoute = { shellSnapshot ->
                    Text("Fake Shell ${shellSnapshot.selectedAppId}")
                },
            )
        }

        composeRule.onNodeWithText("Fake Shell app-1").assertIsDisplayed()
    }

    @Test
    fun shellIsShownWhenOpenAiConnectionReadyWithoutSelectedFastGptApp() {
        composeRule.setContent {
            KoraNavGraph(
                snapshot =
                    ConnectionSnapshot(
                        connectionType = ConnectionType.OPENAI_COMPATIBLE,
                        serverBaseUrl = "https://api.openai.com/v1",
                        apiKey = "openai-secret",
                        model = "gpt-4o-mini",
                        selectedAppId = "direct-openai",
                        onboardingCompleted = true,
                    ),
                shellRoute = { shellSnapshot ->
                    Text("OpenAI Shell ${shellSnapshot.model}")
                },
            )
        }

        composeRule.onNodeWithText("OpenAI Shell gpt-4o-mini").assertIsDisplayed()
    }

    @Test
    fun shareLinkPayloadBypassesBootstrapAndShowsShareRoute() {
        composeRule.setContent {
            KoraNavGraph(
                snapshot = ConnectionSnapshot(),
                shareLinkPayload = ShareLinkPayload("share-1", "uid-1", "chat-1"),
                shareRoute = { Text("Fake Share ${it.shareId}") },
            )
        }

        composeRule.onNodeWithText("Fake Share share-1").assertIsDisplayed()
    }

    @Test
    fun chatShellStartRouteDefaultsToNewConversationThread() {
        val snapshot =
            ConnectionSnapshot(
                connectionType = ConnectionType.FAST_GPT,
                serverBaseUrl = "https://api.fastgpt.in/",
                apiKey = "fastgpt-secret",
                selectedAppId = "app-1",
                onboardingCompleted = true,
            )

        val route = chatShellStartRoute(snapshot)

        assertTrue(route.startsWith("chat/thread/app-1?sessionKey="))
    }
}
