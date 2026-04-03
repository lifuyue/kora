package com.lifuyue.kora.feature.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ConnectionTestApp
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.KoraFeedbackPhase
import com.lifuyue.kora.core.common.ThemeMode
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionConfigViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun testSuccessEnablesSave() =
        runTest {
            val facade =
                ConnectionConfigFacadeStub(
                    testResult =
                        ConnectionTestResult.Success(
                            normalizedBaseUrl = "https://api.fastgpt.in/",
                            apps = listOf(ConnectionTestApp(id = "app-1", name = "Kora")),
                            latencyMs = 120,
                        ),
                )
            val viewModel = ConnectionConfigViewModel(facade)

            viewModel.onConnectionTypeChanged(ConnectionType.FAST_GPT)
            viewModel.testConnection()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.canSave)
            assertEquals(KoraFeedbackPhase.SuccessStable, viewModel.uiState.value.feedback.phase)
            assertEquals(ConnectionFeedbackSource.Test, viewModel.uiState.value.feedback.source)
        }

    @Test
    fun saveSuccessTransitionsFromTransientToStable() =
        runTest {
            val facade =
                ConnectionConfigFacadeStub(
                    testResult =
                        ConnectionTestResult.Success(
                            normalizedBaseUrl = "https://api.fastgpt.in/",
                            apps = listOf(ConnectionTestApp(id = "app-1", name = "Kora")),
                            latencyMs = 120,
                        ),
                )
            val viewModel = ConnectionConfigViewModel(facade)
            var savedCount = 0

            viewModel.onConnectionTypeChanged(ConnectionType.FAST_GPT)
            viewModel.testConnection()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            viewModel.saveConnection { savedCount += 1 }
            mainDispatcherRule.dispatcher.scheduler.runCurrent()

            assertEquals(KoraFeedbackPhase.SuccessTransient, viewModel.uiState.value.feedback.phase)
            mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(1200)
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(KoraFeedbackPhase.SuccessStable, viewModel.uiState.value.feedback.phase)
            assertEquals(ConnectionFeedbackSource.Save, viewModel.uiState.value.feedback.source)
            assertEquals(1, savedCount)
        }

    @Test
    fun clearRemovesSuccessFeedbackAfterFallback() =
        runTest {
            val facade = ConnectionConfigFacadeStub()
            val viewModel = ConnectionConfigViewModel(facade)

            viewModel.clearConnection()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(KoraFeedbackPhase.Idle, viewModel.uiState.value.feedback.phase)
            assertNull(viewModel.uiState.value.validationResult)
            assertEquals(false, viewModel.uiState.value.canSave)
        }
}

private class ConnectionConfigFacadeStub(
    private val testResult: ConnectionTestResult =
        ConnectionTestResult.Success(
            normalizedBaseUrl = "https://api.openai.com/v1/",
            apps = emptyList(),
            latencyMs = 80,
        ),
) : SettingsConnectionFacade {
    override val snapshot = MutableStateFlow(ConnectionSnapshot())

    override suspend fun testConnection(
        connectionType: ConnectionType,
        serverBaseUrl: String,
        apiKey: String,
        model: String?,
    ): ConnectionTestResult = testResult

    override suspend fun saveConnection(
        connectionType: ConnectionType,
        serverBaseUrl: String,
        apiKey: String,
        model: String?,
        selectedAppId: String?,
        onboardingCompleted: Boolean,
    ) = Unit

    override suspend fun updateSelectedAppId(selectedAppId: String) = Unit

    override suspend fun clearConnection() {
        snapshot.value = ConnectionSnapshot()
    }

    override suspend fun updateAppearance(themeMode: ThemeMode) = Unit

    override suspend fun updateLanguageTag(languageTag: String?) = Unit

    override suspend fun updateChatPreferences(
        showReasoningEntry: Boolean,
        streamResponses: Boolean,
    ) = Unit
}
