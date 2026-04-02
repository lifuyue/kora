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
import com.lifuyue.kora.core.network.FastGptApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

object SettingsRoutes {
    const val OVERVIEW = "settings"
    const val CONNECTION = "settings/connection"
    const val THEME = "settings/theme"
    const val LANGUAGE = "settings/language"
    const val CACHE = "settings/cache"
    const val ABOUT = "settings/about"
    const val CURRENT_APP = "settings/current-app"
    const val CHAT_PREFERENCES = "settings/chat-preferences"
}

fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onConnectionSaved: () -> Unit,
    currentAppId: String? = null,
    onOpenCurrentApp: ((String) -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
) {
    composable(SettingsRoutes.OVERVIEW) {
        SettingsOverviewRoute(
            onOpenConnection = { navController.navigate(SettingsRoutes.CONNECTION) },
            onOpenCurrentApp = {
                navController.navigate(SettingsRoutes.CURRENT_APP)
            },
            onOpenTheme = { navController.navigate(SettingsRoutes.THEME) },
            onOpenLanguage = { navController.navigate(SettingsRoutes.LANGUAGE) },
            onOpenCache = { navController.navigate(SettingsRoutes.CACHE) },
            onOpenAbout = { navController.navigate(SettingsRoutes.ABOUT) },
            onOpenChatPreferences = { navController.navigate(SettingsRoutes.CHAT_PREFERENCES) },
            onOpenDrawer = onOpenDrawer,
        )
    }
    composable(SettingsRoutes.CONNECTION) {
        ConnectionConfigRoute(
            onConnectionSaved = onConnectionSaved,
            onBack = { navController.popBackStack() },
            onOpenDrawer = onOpenDrawer,
        )
    }
    composable(SettingsRoutes.THEME) {
        ThemeAppearanceRoute(onBack = { navController.popBackStack() }, onOpenDrawer = onOpenDrawer)
    }
    composable(SettingsRoutes.LANGUAGE) {
        LanguageSettingsRoute(onBack = { navController.popBackStack() }, onOpenDrawer = onOpenDrawer)
    }
    composable(SettingsRoutes.CACHE) {
        CacheSettingsRoute(onBack = { navController.popBackStack() }, onOpenDrawer = onOpenDrawer)
    }
    composable(SettingsRoutes.ABOUT) {
        AboutRoute(onBack = { navController.popBackStack() }, onOpenDrawer = onOpenDrawer)
    }
    composable(SettingsRoutes.CURRENT_APP) {
        CurrentAppSettingsRoute(
            onBack = { navController.popBackStack() },
            onOpenDrawer = onOpenDrawer,
            onOpenCurrentApp = { appId ->
                if (!appId.isNullOrBlank()) {
                    onOpenCurrentApp?.invoke(appId)
                }
            },
            currentAppId = currentAppId,
        )
    }
    composable(SettingsRoutes.CHAT_PREFERENCES) {
        ChatPreferencesRoute(onBack = { navController.popBackStack() }, onOpenDrawer = onOpenDrawer)
    }
}

@Composable
fun SettingsOverviewRoute(
    onOpenConnection: () -> Unit,
    onOpenCurrentApp: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenChatPreferences: () -> Unit,
    onOpenDrawer: () -> Unit = {},
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
                    onOpenLanguage = onOpenLanguage,
                    onOpenCache = onOpenCache,
                    onOpenAbout = onOpenAbout,
                    onOpenChatPreferences = onOpenChatPreferences,
                    onOpenDrawer = onOpenDrawer,
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
            onOpenLanguage = onOpenLanguage,
            onOpenCache = onOpenCache,
            onOpenAbout = onOpenAbout,
            onOpenChatPreferences = onOpenChatPreferences,
            onOpenDrawer = onOpenDrawer,
        )
    }
}

@Composable
fun ConnectionConfigRoute(
    viewModel: ConnectionConfigViewModel = settingsViewModel(),
    onConnectionSaved: () -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConnectionConfigScreen(
        state = uiState,
        onConnectionTypeChange = viewModel::onConnectionTypeChanged,
        onBaseUrlChange = viewModel::onBaseUrlChanged,
        onApiKeyChange = viewModel::onApiKeyChanged,
        onModelChange = viewModel::onModelChanged,
        onTestConnection = viewModel::testConnection,
        onSave = { viewModel.saveConnection(onConnectionSaved) },
        onClear = viewModel::clearConnection,
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
    )
}

@Composable
fun ThemeAppearanceRoute(
    viewModel: ThemeAppearanceViewModel = settingsViewModel(),
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ThemeAppearanceScreen(
        state = uiState,
        onThemeModeChange = viewModel::updateThemeMode,
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
    )
}

@Composable
fun LanguageSettingsRoute(
    viewModel: LanguageSettingsViewModel =
        settingsViewModel(),
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LanguageSettingsScreen(
        state = uiState,
        onLanguageTagChange = viewModel::updateLanguageTag,
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
    )
}

@Composable
fun CacheSettingsRoute(
    viewModel: CacheSettingsViewModel =
        settingsViewModel(),
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CacheSettingsScreen(
        state = uiState,
        onClearCache = viewModel::clearCache,
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
    )
}

@Composable
fun AboutRoute(
    viewModel: AboutViewModel =
        settingsViewModel(),
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    AboutScreen(
        state = uiState,
        onOpenFeedback = { uriHandler.openUri(uiState.feedbackUrl) },
        onOpenLicenses = { uriHandler.openUri(uiState.licensesUrl) },
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
    )
}

@Composable
fun ChatSettingsSheetRoute(
    onOpenRoute: (String) -> Unit,
    viewModel: SettingsOverviewViewModel = settingsViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChatSettingsSheetContent(
        state = uiState,
        onOpenConnection = { onOpenRoute(SettingsRoutes.CONNECTION) },
        onOpenCurrentApp = { onOpenRoute(SettingsRoutes.CURRENT_APP) },
        onOpenTheme = { onOpenRoute(SettingsRoutes.THEME) },
        onOpenLanguage = { onOpenRoute(SettingsRoutes.LANGUAGE) },
        onOpenCache = { onOpenRoute(SettingsRoutes.CACHE) },
        onOpenAbout = { onOpenRoute(SettingsRoutes.ABOUT) },
        onOpenChatPreferences = { onOpenRoute(SettingsRoutes.CHAT_PREFERENCES) },
    )
}

@Composable
fun ChatPreferencesRoute(
    viewModel: ChatPreferencesViewModel = settingsViewModel(),
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChatPreferencesScreen(
        state = uiState,
        onShowReasoningEntryChange = viewModel::updateShowReasoningEntry,
        onStreamResponsesChange = viewModel::updateStreamResponses,
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
    )
}

@Composable
fun CurrentAppSettingsRoute(
    viewModel: CurrentAppSettingsViewModel = settingsViewModel(),
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
    onOpenCurrentApp: (String) -> Unit = {},
    currentAppId: String? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CurrentAppSettingsScreen(
        state = uiState,
        onSwitchApp = viewModel::switchApp,
        onRefresh = viewModel::refresh,
        onOpenCurrentApp = {
            val appId = uiState.selectedAppId ?: currentAppId
            if (!appId.isNullOrBlank()) {
                onOpenCurrentApp(appId)
            }
        },
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface SettingsViewModelEntryPoint {
    fun connectionRepository(): ConnectionRepository
    fun fastGptApi(): FastGptApi

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
                        LanguageSettingsViewModel::class.java ->
                            LanguageSettingsViewModel(settingsConnectionFacade) as VM
                        ChatPreferencesViewModel::class.java ->
                            ChatPreferencesViewModel(settingsConnectionFacade) as VM
                        CurrentAppSettingsViewModel::class.java ->
                            CurrentAppSettingsViewModel(settingsConnectionFacade, entryPoint.fastGptApi()) as VM
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
