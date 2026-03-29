package com.lifuyue.kora.feature.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

object SettingsRoutes {
    const val OVERVIEW = "settings"
    const val CONNECTION = "settings/connection"
    const val THEME = "settings/theme"
    const val CHAT_PREFERENCES = "settings/chat-preferences"
    const val AUDIO = "settings/audio"
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
            onOpenAudio = { navController.navigate(SettingsRoutes.AUDIO) },
            onOpenLanguage = { navController.navigate(SettingsRoutes.LANGUAGE) },
            onOpenCache = { navController.navigate(SettingsRoutes.CACHE) },
            onOpenAbout = { navController.navigate(SettingsRoutes.ABOUT) },
        )
    }
    composable(SettingsRoutes.CONNECTION) {
        ConnectionConfigRoute(
            onConnectionSaved = onConnectionSaved,
            onBack = { navController.popBackStack() },
        )
    }
    composable(SettingsRoutes.THEME) {
        ThemeAppearanceRoute(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.CHAT_PREFERENCES) {
        ChatPreferencesRoute(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.AUDIO) {
        AudioSettingsRoute(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.LANGUAGE) {
        LanguageSettingsRoute(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.CACHE) {
        CacheSettingsRoute(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.ABOUT) {
        AboutRoute(onBack = { navController.popBackStack() })
    }
}

@Composable
fun SettingsOverviewRoute(
    onOpenConnection: () -> Unit,
    onOpenCurrentApp: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenChatPreferences: () -> Unit,
    onOpenAudio: () -> Unit = {},
    onOpenLanguage: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsOverviewViewModel = settingsViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useDualPane = rememberSettingsDualPaneEnabled()
    if (useDualPane) {
        AdaptiveSettingsScaffold(
            isExpanded = true,
            listPane = {
                SettingsOverviewScreen(
                    state = uiState,
                    onOpenConnection = onOpenConnection,
                    onOpenCurrentApp = onOpenCurrentApp,
                    onOpenTheme = onOpenTheme,
                    onOpenChatPreferences = onOpenChatPreferences,
                    onOpenAudio = onOpenAudio,
                    onOpenLanguage = onOpenLanguage,
                    onOpenCache = onOpenCache,
                    onOpenAbout = onOpenAbout,
                )
            },
            detailPane = { SettingsAdaptivePlaceholder() },
        )
    } else {
        SettingsOverviewScreen(
            state = uiState,
            onOpenConnection = onOpenConnection,
            onOpenCurrentApp = onOpenCurrentApp,
            onOpenTheme = onOpenTheme,
            onOpenChatPreferences = onOpenChatPreferences,
            onOpenAudio = onOpenAudio,
            onOpenLanguage = onOpenLanguage,
            onOpenCache = onOpenCache,
            onOpenAbout = onOpenAbout,
        )
    }
}

@Composable
fun ConnectionConfigRoute(
    viewModel: ConnectionConfigViewModel = settingsViewModel(),
    onConnectionSaved: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConnectionConfigScreen(
        state = uiState,
        onBaseUrlChange = viewModel::onBaseUrlChanged,
        onApiKeyChange = viewModel::onApiKeyChanged,
        onTestConnection = viewModel::testConnection,
        onSave = { viewModel.saveConnection(onConnectionSaved) },
        onClear = viewModel::clearConnection,
        onBack = onBack,
    )
}

@Composable
fun AudioSettingsRoute(
    viewModel: AudioSettingsViewModel = settingsViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AudioSettingsScreen(
        state = uiState,
        onSpeechToTextEngineChange = viewModel::updateSpeechToTextEngine,
        onAutoSendTranscriptsChange = viewModel::updateAutoSendTranscripts,
        onTextToSpeechEngineChange = viewModel::updateTextToSpeechEngine,
        onSpeechRateChange = viewModel::updateSpeechRate,
        onDefaultVoiceNameChange = viewModel::updateDefaultVoiceName,
        onBack = onBack,
    )
}

@Composable
fun ThemeAppearanceRoute(
    viewModel: ThemeAppearanceViewModel = settingsViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ThemeAppearanceScreen(
        state = uiState,
        onThemeModeChange = viewModel::updateThemeMode,
        onDynamicColorChange = viewModel::updateDynamicColorEnabled,
        onOledChange = viewModel::updateOledEnabled,
        onBack = onBack,
    )
}

@Composable
fun ChatPreferencesRoute(
    viewModel: ChatPreferencesViewModel =
        settingsViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChatPreferencesScreen(
        state = uiState,
        onStreamEnabledChange = viewModel::updateStreamEnabled,
        onAutoScrollChange = viewModel::updateAutoScroll,
        onShowCitationsChange = viewModel::updateShowCitationsByDefault,
        onFontSizeScaleChange = viewModel::updateFontSizeScale,
        onBack = onBack,
    )
}

@Composable
fun LanguageSettingsRoute(
    viewModel: LanguageSettingsViewModel =
        settingsViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LanguageSettingsScreen(
        state = uiState,
        onLanguageTagChange = viewModel::updateLanguageTag,
        onBack = onBack,
    )
}

@Composable
fun CacheSettingsRoute(
    viewModel: CacheSettingsViewModel =
        settingsViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CacheSettingsScreen(
        state = uiState,
        onClearCache = viewModel::clearCache,
        onBack = onBack,
    )
}

@Composable
fun AboutRoute(
    viewModel: AboutViewModel =
        settingsViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    AboutScreen(
        state = uiState,
        onOpenFeedback = { uriHandler.openUri(uiState.feedbackUrl) },
        onOpenLicenses = { uriHandler.openUri(uiState.licensesUrl) },
        onBack = onBack,
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface SettingsViewModelEntryPoint {
    fun connectionRepository(): ConnectionRepository

    @ApplicationContext
    fun applicationContext(): Context
}

@Composable
private inline fun <reified T : ViewModel> settingsViewModel(): T {
    val context = LocalContext.current.applicationContext
    val factory =
        remember(context) {
            val entryPoint =
                EntryPointAccessors.fromApplication(
                    context,
                    SettingsViewModelEntryPoint::class.java,
                )
            val settingsConnectionFacade = ConnectionRepositorySettingsFacade(entryPoint.connectionRepository())
            val settingsCacheManager = AndroidSettingsCacheManager(entryPoint.applicationContext())
            val appInfoProvider = AndroidAppInfoProvider(entryPoint.applicationContext())
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <VM : ViewModel> create(modelClass: Class<VM>): VM =
                    when (modelClass) {
                        ConnectionConfigViewModel::class.java ->
                            ConnectionConfigViewModel(settingsConnectionFacade) as VM
                        SettingsOverviewViewModel::class.java ->
                            SettingsOverviewViewModel(settingsConnectionFacade) as VM
                        ThemeAppearanceViewModel::class.java ->
                            ThemeAppearanceViewModel(settingsConnectionFacade) as VM
                        ChatPreferencesViewModel::class.java ->
                            ChatPreferencesViewModel(settingsConnectionFacade) as VM
                        AudioSettingsViewModel::class.java ->
                            AudioSettingsViewModel(settingsConnectionFacade) as VM
                        LanguageSettingsViewModel::class.java ->
                            LanguageSettingsViewModel(settingsConnectionFacade) as VM
                        CacheSettingsViewModel::class.java ->
                            CacheSettingsViewModel(settingsCacheManager) as VM
                        AboutViewModel::class.java ->
                            AboutViewModel(appInfoProvider) as VM
                        else -> throw IllegalArgumentException("Unsupported settings ViewModel: ${modelClass.name}")
                    }
            }
        }
    return viewModel(factory = factory)
}
