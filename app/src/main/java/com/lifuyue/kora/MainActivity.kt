package com.lifuyue.kora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import com.lifuyue.kora.navigation.KoraNavGraph
import com.lifuyue.kora.testing.KoraTestOverrides
import com.lifuyue.kora.ui.theme.KoraTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var connectionRepository: ConnectionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoraApp(connectionRepository = connectionRepository)
        }
    }
}

@Composable
private fun KoraApp(connectionRepository: ConnectionRepository) {
    val snapshotFlow = KoraTestOverrides.snapshotOverride ?: connectionRepository.snapshot
    val snapshot by snapshotFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val connectionRouteOverride = KoraTestOverrides.connectionRouteOverride
    val shellRouteOverride = KoraTestOverrides.shellRouteOverride

    KoraTheme(
        themeMode = snapshot.appearancePreferences.themeMode,
        dynamicColor = snapshot.appearancePreferences.dynamicColorEnabled,
        oledEnabled = snapshot.appearancePreferences.oledEnabled,
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
                        onOnboardingCompleted = onOnboardingCompleted,
                        connectionRoute = { onConnectionSaved ->
                            connectionRouteOverride.Render(onConnectionSaved)
                        },
                    )
                }
                shellRouteOverride != null -> {
                    KoraNavGraph(
                        snapshot = snapshot,
                        onOnboardingCompleted = onOnboardingCompleted,
                        shellRoute = { shellSnapshot ->
                            shellRouteOverride.Render(shellSnapshot)
                        },
                    )
                }
                else -> {
                    KoraNavGraph(
                        snapshot = snapshot,
                        onOnboardingCompleted = onOnboardingCompleted,
                    )
                }
            }
        }
    }
}
