package com.lifuyue.kora.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lifuyue.kora.R
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.database.store.ShareLinkPayload
import com.lifuyue.kora.feature.chat.ChatRoutes
import com.lifuyue.kora.feature.chat.chatGraph
import com.lifuyue.kora.feature.knowledge.KnowledgeRoutes
import com.lifuyue.kora.feature.settings.ChatQuickSettingsRoute
import com.lifuyue.kora.feature.settings.SettingsRoutes
import com.lifuyue.kora.feature.knowledge.knowledgeGraph
import com.lifuyue.kora.feature.settings.settingsGraph
import kotlinx.coroutines.launch

private const val ROUTE_BOOTSTRAP = "bootstrap"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_CONNECTION = "connection"
private const val ROUTE_SHELL = "shell"

internal fun chatShellStartRoute(snapshot: ConnectionSnapshot): String =
    snapshot.selectedAppId?.let { ChatRoutes.thread(it) } ?: SettingsRoutes.CONNECTION

@Composable
fun KoraNavGraph(
    snapshot: ConnectionSnapshot,
    shareLinkPayload: ShareLinkPayload? = null,
    modifier: Modifier = Modifier,
    onOnboardingCompleted: () -> Unit = {},
    connectionRoute: @Composable ((() -> Unit) -> Unit) = { onConnectionSaved ->
        com.lifuyue.kora.feature.settings.ConnectionConfigRoute(onConnectionSaved = onConnectionSaved)
    },
    shellRoute: @Composable ((ConnectionSnapshot) -> Unit) = { shellSnapshot ->
        KoraShell(snapshot = shellSnapshot)
    },
    shareRoute: @Composable ((ShareLinkPayload) -> Unit) = { payload ->
        ShareRouteEntry(payload = payload)
    },
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_BOOTSTRAP,
        modifier = modifier,
    ) {
        composable(ROUTE_BOOTSTRAP) {
            BootstrapRoute(snapshot = snapshot, shareLinkPayload = shareLinkPayload, navController = navController)
        }
        composable(ROUTE_ONBOARDING) {
            OnboardingRoute(
                onCompleted = onOnboardingCompleted,
                onContinueToConnection = {
                    navController.navigate(ROUTE_CONNECTION) {
                        popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(ROUTE_CONNECTION) {
            connectionRoute {
                navController.navigate(ROUTE_SHELL) {
                    popUpTo(ROUTE_CONNECTION) { inclusive = true }
                }
            }
        }
        composable(ROUTE_SHELL) {
            shellRoute(snapshot)
        }
        shareLinkPayload?.let { payload ->
            composable(ROUTE_SHARE) {
                shareRoute(payload)
            }
        }
    }
}

@Composable
private fun BootstrapRoute(
    snapshot: ConnectionSnapshot,
    shareLinkPayload: ShareLinkPayload?,
    navController: NavHostController,
) {
    LaunchedEffect(snapshot.onboardingCompleted, snapshot.hasValidConnection, snapshot.selectedAppId, shareLinkPayload) {
        when {
            shareLinkPayload != null -> {
                navController.navigate(ROUTE_SHARE) {
                    popUpTo(ROUTE_BOOTSTRAP) { inclusive = true }
                }
            }
            !snapshot.onboardingCompleted -> {
                navController.navigate(ROUTE_ONBOARDING) {
                    popUpTo(ROUTE_BOOTSTRAP) { inclusive = true }
                }
            }
            !snapshot.hasValidConnection || snapshot.selectedAppId.isNullOrBlank() -> {
                navController.navigate(ROUTE_CONNECTION) {
                    popUpTo(ROUTE_BOOTSTRAP) { inclusive = true }
                }
            }
            else -> {
                navController.navigate(ROUTE_SHELL) {
                    popUpTo(ROUTE_BOOTSTRAP) { inclusive = true }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.bootstrap_restoring), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun OnboardingRoute(
    onCompleted: () -> Unit,
    onContinueToConnection: () -> Unit,
) {
    val pages =
        listOf(
            stringResource(R.string.onboarding_page_1_title) to stringResource(R.string.onboarding_page_1_body),
            stringResource(R.string.onboarding_page_2_title) to stringResource(R.string.onboarding_page_2_body),
            stringResource(R.string.onboarding_page_3_title) to stringResource(R.string.onboarding_page_3_body),
        )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_page_indicator, pagerState.currentPage + 1, pages.size),
            style = MaterialTheme.typography.labelLarge,
        )
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(pages[page].first, style = MaterialTheme.typography.headlineMedium)
                    Text(pages[page].second, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Button(
            onClick = {
                scope.launch {
                    if (pagerState.currentPage == pages.lastIndex) {
                        onCompleted()
                        onContinueToConnection()
                    } else {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
        ) {
            Text(
                stringResource(
                    if (pagerState.currentPage == pages.lastIndex) {
                        R.string.onboarding_cta_finish
                    } else {
                        R.string.onboarding_cta_next
                    },
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KoraShell(snapshot: ConnectionSnapshot) {
    val navController = rememberNavController()
    var showChatQuickSettings by rememberSaveable { mutableStateOf(false) }
    val chatStartRoute = chatShellStartRoute(snapshot)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag("shell_workspace_container"),
        color = MaterialTheme.colorScheme.background,
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                KoraShellDrawer(
                    currentAppId = snapshot.selectedAppId,
                    onOpenChat = {
                        scope.launch {
                            drawerState.close()
                            snapshot.selectedAppId?.let { appId ->
                                navController.navigate(ChatRoutes.thread(appId)) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    },
                    onOpenKnowledge = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(KnowledgeRoutes.OVERVIEW) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onOpenSettings = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(SettingsRoutes.OVERVIEW) {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            },
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { innerPadding ->
                if (showChatQuickSettings) {
                    androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showChatQuickSettings = false }) {
                        ChatQuickSettingsRoute(
                            onOpenFullSettings = {
                                showChatQuickSettings = false
                                navController.navigate(SettingsRoutes.OVERVIEW) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = chatStartRoute,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    knowledgeGraph(
                        navController = navController,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                    )
                    settingsGraph(
                        navController = navController,
                        onConnectionSaved = {
                            val selectedAppId = snapshot.selectedAppId
                            if (!selectedAppId.isNullOrBlank()) {
                                navController.navigate(ChatRoutes.thread(selectedAppId)) {
                                    popUpTo(SettingsRoutes.CONNECTION) { inclusive = true }
                                }
                            }
                        },
                        currentAppId = snapshot.selectedAppId,
                        onOpenCurrentApp = { appId ->
                            navController.navigate(ChatRoutes.appDetail(appId))
                        },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                    )
                    if (!snapshot.selectedAppId.isNullOrBlank()) {
                        chatGraph(
                            navController = navController,
                            onOpenQuickSettings = { showChatQuickSettings = true },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                        )
                    }
                }
            }
        }
    }
}
