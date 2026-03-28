package com.lifuyue.kora.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifuyue.kora.core.database.store.ShareLinkPayload
import com.lifuyue.kora.core.database.store.ShareSessionState
import com.lifuyue.kora.core.database.store.ShareSessionStatus
import com.lifuyue.kora.core.database.store.ShareSessionStore
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.ShareAuthInitRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareChatViewModel
    @Inject
    constructor(
        private val api: FastGptApi,
        private val shareSessionStore: ShareSessionStore,
    ) : ViewModel() {
        val uiState: StateFlow<ShareSessionState> = shareSessionStore.state

        fun bootstrap(payload: ShareLinkPayload) {
            viewModelScope.launch {
                shareSessionStore.setInitializing(payload)
                runCatching {
                    val normalizedUid = api.shareAuthInit(ShareAuthInitRequest(token = payload.outLinkUid)).data?.uid
                    val uid = normalizedUid?.takeIf { it.isNotBlank() } ?: payload.outLinkUid
                    payload.copy(outLinkUid = uid) to api.initShareSession(
                        shareId = payload.shareId,
                        outLinkUid = uid,
                        chatId = payload.chatId,
                    ).data
                }.onSuccess { (normalizedPayload, bootstrap) ->
                    if (bootstrap == null) {
                        shareSessionStore.setExpired(payload)
                    } else {
                        shareSessionStore.setReady(normalizedPayload, bootstrap)
                    }
                }.onFailure { error ->
                    val message = error.message.orEmpty()
                    if (message.contains("expired", ignoreCase = true) || message.contains("invalid", ignoreCase = true)) {
                        shareSessionStore.setExpired(payload)
                    } else {
                        shareSessionStore.setError(payload, message.ifBlank { "Share bootstrap failed" })
                    }
                }
            }
        }

        fun clearSession() {
            shareSessionStore.clear()
        }
    }

val ShareSessionState.isTerminal: Boolean
    get() = status == ShareSessionStatus.Ready || status == ShareSessionStatus.Expired || status == ShareSessionStatus.Error
