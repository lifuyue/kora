package com.lifuyue.kora.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object SettingsRoutes {
    const val OVERVIEW = "settings"
    const val CONNECTION = "settings/connection"
    const val THEME = "settings/theme"
    const val CHAT_PREFERENCES = "settings/chat-preferences"
    const val LANGUAGE = "settings/language"
    const val CACHE = "settings/cache"
    const val ABOUT = "settings/about"
}

fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onConnectionSaved: () -> Unit,
    currentAppId: String? = null,
    onOpenCurrentApp: ((String) -> Unit)? = null,
) {
    composable(SettingsRoutes.OVERVIEW) {
        SettingsOverviewRoute(
            onOpenConnection = { navController.navigate(SettingsRoutes.CONNECTION) },
            onOpenCurrentApp = {
                val appId = currentAppId
                if (!appId.isNullOrBlank()) {
                    onOpenCurrentApp?.invoke(appId)
                }
            },
            onOpenTheme = { navController.navigate(SettingsRoutes.THEME) },
            onOpenChatPreferences = { navController.navigate(SettingsRoutes.CHAT_PREFERENCES) },
            onOpenLanguage = { navController.navigate(SettingsRoutes.LANGUAGE) },
            onOpenCache = { navController.navigate(SettingsRoutes.CACHE) },
            onOpenAbout = { navController.navigate(SettingsRoutes.ABOUT) },
        )
    }
    composable(SettingsRoutes.CONNECTION) {
        ConnectionConfigRoute(
            viewModel = hiltViewModel(),
            onConnectionSaved = onConnectionSaved,
        )
    }
    composable(SettingsRoutes.THEME) {
        ThemeAppearanceRoute(
            viewModel = hiltViewModel(),
            onBack = { navController.popBackStack() },
        )
    }
    composable(SettingsRoutes.CHAT_PREFERENCES) {
        ChatPreferencesRoute(viewModel = hiltViewModel())
    }
    composable(SettingsRoutes.LANGUAGE) {
        LanguageSettingsRoute(viewModel = hiltViewModel())
    }
    composable(SettingsRoutes.CACHE) {
        CacheSettingsRoute(viewModel = hiltViewModel())
    }
    composable(SettingsRoutes.ABOUT) {
        AboutRoute(viewModel = hiltViewModel())
    }
}

@Composable
fun SettingsOverviewRoute(
    onOpenConnection: () -> Unit,
    onOpenCurrentApp: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenChatPreferences: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsOverviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsOverviewScreen(
        state = uiState,
        onOpenConnection = onOpenConnection,
        onOpenCurrentApp = onOpenCurrentApp,
        onOpenTheme = onOpenTheme,
        onOpenChatPreferences = onOpenChatPreferences,
        onOpenLanguage = onOpenLanguage,
        onOpenCache = onOpenCache,
        onOpenAbout = onOpenAbout,
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

@Composable
fun ChatPreferencesRoute(
    viewModel: ChatPreferencesViewModel =
        hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChatPreferencesScreen(
        state = uiState,
        onStreamEnabledChange = viewModel::updateStreamEnabled,
        onAutoScrollChange = viewModel::updateAutoScroll,
        onShowCitationsChange = viewModel::updateShowCitationsByDefault,
        onFontSizeScaleChange = viewModel::updateFontSizeScale,
    )
}

@Composable
fun LanguageSettingsRoute(
    viewModel: LanguageSettingsViewModel =
        hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LanguageSettingsScreen(
        state = uiState,
        onLanguageTagChange = viewModel::updateLanguageTag,
    )
}

@Composable
fun CacheSettingsRoute(
    viewModel: CacheSettingsViewModel =
        hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CacheSettingsScreen(
        state = uiState,
        onClearCache = viewModel::clearCache,
    )
}

@Composable
fun AboutRoute(
    viewModel: AboutViewModel =
        hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    AboutScreen(
        state = uiState,
        onOpenFeedback = { uriHandler.openUri(uiState.feedbackUrl) },
        onOpenLicenses = { uriHandler.openUri(uiState.licensesUrl) },
    )
}
