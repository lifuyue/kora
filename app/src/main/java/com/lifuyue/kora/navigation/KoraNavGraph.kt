package com.lifuyue.kora.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifuyue.kora.R
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.database.store.ShareLinkPayload
import com.lifuyue.kora.feature.chat.ChatRoutes
import com.lifuyue.kora.feature.chat.chatGraph
import com.lifuyue.kora.feature.knowledge.KnowledgeRoutes
import com.lifuyue.kora.feature.knowledge.knowledgeGraph
import com.lifuyue.kora.feature.settings.SettingsRoutes
import com.lifuyue.kora.feature.settings.settingsGraph
import kotlinx.coroutines.launch

private const val ROUTE_BOOTSTRAP = "bootstrap"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_CONNECTION = "connection"
private const val ROUTE_SHELL = "shell"

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

internal enum class ShellDestination(
    val labelRes: Int,
) {
    Chat(R.string.nav_chat),
    Knowledge(R.string.nav_knowledge),
    Settings(R.string.nav_settings),
}

@Composable
private fun KoraShell(snapshot: ConnectionSnapshot) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var selectedTab by rememberSaveable { mutableStateOf(ShellDestination.Chat) }
    val chatStartRoute =
        snapshot.selectedAppId?.let { ChatRoutes.conversations(it) } ?: SettingsRoutes.CONNECTION

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag("shell_workspace_container"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                KoraShellNavigationBar(
                    selectedTab = selectedTab,
                    onSelect = { destination ->
                        selectedTab = destination
                        val route =
                            when (destination) {
                                ShellDestination.Chat -> chatStartRoute
                                ShellDestination.Knowledge -> KnowledgeRoutes.OVERVIEW
                                ShellDestination.Settings -> SettingsRoutes.OVERVIEW
                            }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = chatStartRoute,
                modifier = Modifier.padding(innerPadding),
            ) {
                knowledgeGraph(navController = navController)
                settingsGraph(
                    navController = navController,
                    onConnectionSaved = {
                        val selectedAppId = snapshot.selectedAppId
                        if (!selectedAppId.isNullOrBlank()) {
                            navController.navigate(ChatRoutes.conversations(selectedAppId)) {
                                popUpTo(SettingsRoutes.CONNECTION) { inclusive = true }
                            }
                        }
                    },
                    currentAppId = snapshot.selectedAppId,
                    onOpenCurrentApp = { appId ->
                        navController.navigate(ChatRoutes.appDetail(appId))
                    },
                )
                if (!snapshot.selectedAppId.isNullOrBlank()) {
                    chatGraph(navController = navController)
                }
            }

            LaunchedEffect(currentRoute) {
                selectedTab =
                    when {
                        currentRoute?.startsWith("knowledge") == true -> ShellDestination.Knowledge
                        currentRoute?.startsWith("settings") == true -> ShellDestination.Settings
                        else -> ShellDestination.Chat
                    }
            }
        }
    }
}

@Composable
internal fun KoraShellNavigationBar(
    selectedTab: ShellDestination,
    onSelect: (ShellDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ShellDestination.entries.forEach { destination ->
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = selectedTab == destination,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector =
                            when (destination) {
                                ShellDestination.Chat -> Icons.Outlined.ChatBubbleOutline
                                ShellDestination.Knowledge -> Icons.Outlined.AutoStories
                                ShellDestination.Settings -> Icons.Outlined.Settings
                            },
                        contentDescription = label,
                    )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(shellNavTag(destination)),
            )
        }
    }
}

private fun shellNavTag(destination: ShellDestination): String =
    when (destination) {
        ShellDestination.Chat -> "shell_nav_chat"
        ShellDestination.Knowledge -> "shell_nav_knowledge"
        ShellDestination.Settings -> "shell_nav_settings"
    }
