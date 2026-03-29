package com.lifuyue.kora.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifuyue.kora.R
import com.lifuyue.kora.core.database.store.ShareLinkPayload
import com.lifuyue.kora.core.database.store.ShareSessionStatus
import com.lifuyue.kora.feature.chat.ShareChatViewModel

internal const val ROUTE_SHARE = "share_entry"

fun NavGraphBuilder.shareGraph(payload: ShareLinkPayload) {
    composable(ROUTE_SHARE) {
        ShareRouteEntry(payload = payload)
    }
}

@Composable
fun ShareRouteEntry(
    payload: ShareLinkPayload,
    viewModel: ShareChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(payload) {
        if (uiState.status == ShareSessionStatus.Idle) {
            viewModel.bootstrap(payload)
        }
    }
    when (uiState.status) {
        ShareSessionStatus.Idle,
        ShareSessionStatus.Initializing,
        -> Text(stringResource(R.string.share_loading))
        ShareSessionStatus.Ready -> Text(stringResource(R.string.share_ready, uiState.appId ?: ""))
        ShareSessionStatus.Expired -> Text(stringResource(R.string.share_link_expired))
        ShareSessionStatus.Error -> Text(uiState.errorMessage ?: stringResource(R.string.share_error))
    }
}
