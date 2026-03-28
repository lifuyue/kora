package com.lifuyue.kora.core.database.store

import com.lifuyue.kora.core.network.ShareSessionBootstrapDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ShareSessionStatus {
    Idle,
    Initializing,
    Ready,
    Expired,
    Error,
}

data class ShareLinkPayload(
    val shareId: String,
    val outLinkUid: String,
    val chatId: String? = null,
)

data class ShareSessionState(
    val status: ShareSessionStatus = ShareSessionStatus.Idle,
    val shareId: String? = null,
    val outLinkUid: String? = null,
    val appId: String? = null,
    val chatId: String? = null,
    val appName: String = "",
    val title: String? = null,
    val errorMessage: String? = null,
)

@Singleton
class ShareSessionStore
    @Inject
    constructor() {
        private val mutableState = MutableStateFlow(ShareSessionState())
        val state: StateFlow<ShareSessionState> = mutableState

        fun setInitializing(payload: ShareLinkPayload) {
            mutableState.value =
                ShareSessionState(
                    status = ShareSessionStatus.Initializing,
                    shareId = payload.shareId,
                    outLinkUid = payload.outLinkUid,
                    chatId = payload.chatId,
                )
        }

        fun setReady(
            payload: ShareLinkPayload,
            bootstrap: ShareSessionBootstrapDto,
        ) {
            mutableState.value =
                ShareSessionState(
                    status = ShareSessionStatus.Ready,
                    shareId = payload.shareId,
                    outLinkUid = payload.outLinkUid,
                    appId = bootstrap.appId,
                    chatId = bootstrap.chatId,
                    appName = bootstrap.appName,
                    title = bootstrap.title,
                )
        }

        fun setExpired(payload: ShareLinkPayload) {
            mutableState.value =
                ShareSessionState(
                    status = ShareSessionStatus.Expired,
                    shareId = payload.shareId,
                    outLinkUid = payload.outLinkUid,
                    chatId = payload.chatId,
                )
        }

        fun setError(
            payload: ShareLinkPayload,
            message: String,
        ) {
            mutableState.value =
                ShareSessionState(
                    status = ShareSessionStatus.Error,
                    shareId = payload.shareId,
                    outLinkUid = payload.outLinkUid,
                    chatId = payload.chatId,
                    errorMessage = message,
                )
        }

        fun clear() {
            mutableState.value = ShareSessionState()
        }
    }
