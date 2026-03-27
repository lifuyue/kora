package com.lifuyue.kora.feature.settings

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle

object SettingsRoutes {
    const val overview = "settings"
    const val connection = "settings/connection"
    const val theme = "settings/theme"
}

fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onConnectionSaved: () -> Unit,
) {
    composable(SettingsRoutes.overview) {
        SettingsOverviewRoute(
            onOpenConnection = { navController.navigate(SettingsRoutes.connection) },
            onOpenTheme = { navController.navigate(SettingsRoutes.theme) },
        )
    }
    composable(SettingsRoutes.connection) {
        ConnectionConfigRoute(
            viewModel = hiltViewModel(),
            onConnectionSaved = onConnectionSaved,
        )
    }
    composable(SettingsRoutes.theme) {
        ThemeAppearanceRoute(
            viewModel = hiltViewModel(),
            onBack = { navController.popBackStack() },
        )
    }
}

@Composable
fun SettingsOverviewRoute(
    onOpenConnection: () -> Unit,
    onOpenTheme: () -> Unit,
    viewModel: SettingsOverviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsOverviewScreen(
        state = uiState,
        onOpenConnection = onOpenConnection,
        onOpenTheme = onOpenTheme,
    )
}

@Composable
fun ConnectionConfigRoute(
    viewModel: ConnectionConfigViewModel = hiltViewModel(),
    onConnectionSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConnectionConfigScreen(
        state = uiState,
        onBaseUrlChange = viewModel::onBaseUrlChanged,
        onApiKeyChange = viewModel::onApiKeyChanged,
        onTestConnection = viewModel::testConnection,
        onSave = { viewModel.saveConnection(onConnectionSaved) },
        onClear = viewModel::clearConnection,
    )
}

@Composable
fun ThemeAppearanceRoute(
    viewModel: ThemeAppearanceViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ThemeAppearanceScreen(
        state = uiState,
        onThemeModeChange = viewModel::updateThemeMode,
        onDynamicColorChange = viewModel::updateDynamicColorEnabled,
        onOledChange = viewModel::updateOledEnabled,
    )
}
