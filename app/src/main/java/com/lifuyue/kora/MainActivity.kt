package com.lifuyue.kora

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import com.lifuyue.kora.i18n.AppLocaleController
import com.lifuyue.kora.navigation.KoraNavGraph
import com.lifuyue.kora.navigation.parseShareLinkIntent
import com.lifuyue.kora.testing.KoraTestOverrides
import com.lifuyue.kora.ui.theme.KoraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var connectionRepository: ConnectionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installDebugDemoOverrides(intent)
        installDebugConnectionOverride(intent)
        AppLocaleController.apply(connectionRepository.snapshot.value.appearancePreferences.languageTag)
        enableEdgeToEdge()
        val shareLinkPayload = parseShareLinkIntent(intent)
        setContent {
            KoraApp(connectionRepository = connectionRepository, shareLinkPayload = shareLinkPayload)
        }
    }

    private fun installDebugConnectionOverride(intent: android.content.Intent?) {
        val override = readDebugConnectionOverride(intent) ?: return
        KoraTestOverrides.reset()
        runBlocking {
            connectionRepository.saveConnection(
                connectionType = ConnectionType.OPENAI_COMPATIBLE,
                serverBaseUrl = override.serverBaseUrl,
                apiKey = override.apiKey,
                model = override.model,
                onboardingCompleted = true,
            )
        }
    }
}

@Composable
private fun KoraApp(
    connectionRepository: ConnectionRepository,
    shareLinkPayload: com.lifuyue.kora.core.database.store.ShareLinkPayload? = null,
) {
    val repositorySnapshot by connectionRepository.snapshot.collectAsState()
    val snapshotFlow = KoraTestOverrides.snapshotOverride ?: connectionRepository.snapshot
    val snapshot by snapshotFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val connectionRouteOverride = KoraTestOverrides.connectionRouteOverride
    val shellRouteOverride = KoraTestOverrides.shellRouteOverride
    val languageTag = repositorySnapshot.appearancePreferences.languageTag

    LaunchedEffect(languageTag) {
        AppLocaleController.apply(languageTag)
    }

    KoraTheme(
        themeMode = repositorySnapshot.appearancePreferences.themeMode,
    ) {
        Surface(modifier = Modifier) {
            val onOnboardingCompleted = {
                if (KoraTestOverrides.snapshotOverride == null) {
                    scope.launch {
                        connectionRepository.updateOnboardingCompleted(true)
                    }
                }
            }

            when {
                connectionRouteOverride != null && shellRouteOverride != null -> {
                    KoraNavGraph(
                        snapshot = snapshot,
                        shareLinkPayload = shareLinkPayload,
                        onOnboardingCompleted = onOnboardingCompleted,
                        connectionRoute = { onConnectionSaved ->
                            connectionRouteOverride.Render(onConnectionSaved)
                        },
                        shellRoute = { shellSnapshot ->
                            shellRouteOverride.Render(shellSnapshot)
                        },
                    )
                }
                connectionRouteOverride != null -> {
                    KoraNavGraph(
                        snapshot = snapshot,
                        shareLinkPayload = shareLinkPayload,
                        onOnboardingCompleted = onOnboardingCompleted,
                        connectionRoute = { onConnectionSaved ->
                            connectionRouteOverride.Render(onConnectionSaved)
                        },
                    )
                }
                shellRouteOverride != null -> {
                    KoraNavGraph(
                        snapshot = snapshot,
                        shareLinkPayload = shareLinkPayload,
                        onOnboardingCompleted = onOnboardingCompleted,
                        shellRoute = { shellSnapshot ->
                            shellRouteOverride.Render(shellSnapshot)
                        },
                    )
                }
                else -> {
                    KoraNavGraph(
                        snapshot = snapshot,
                        shareLinkPayload = shareLinkPayload,
                        onOnboardingCompleted = onOnboardingCompleted,
                    )
                }
            }
        }
    }
}
