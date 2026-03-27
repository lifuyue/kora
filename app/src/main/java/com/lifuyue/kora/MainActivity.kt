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
    val snapshot by connectionRepository.snapshot.collectAsState()
    val scope = rememberCoroutineScope()

    KoraTheme(
        themeMode = snapshot.appearancePreferences.themeMode,
        dynamicColor = snapshot.appearancePreferences.dynamicColorEnabled,
        oledEnabled = snapshot.appearancePreferences.oledEnabled,
    ) {
        Surface(modifier = Modifier) {
            KoraNavGraph(
                snapshot = snapshot,
                onOnboardingCompleted = {
                    scope.launch {
                        connectionRepository.updateOnboardingCompleted(true)
                    }
                },
            )
        }
    }
}
