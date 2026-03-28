package com.lifuyue.kora.feature.chat

import com.lifuyue.kora.core.common.AudioPreferences
import kotlinx.coroutines.flow.StateFlow

data class TtsPlaybackRequest(
    val messageId: String,
    val text: String,
    val audioPreferences: AudioPreferences,
)

interface TtsPlaybackController {
    val state: StateFlow<TtsPlaybackUiState>

    fun play(request: TtsPlaybackRequest)

    fun pause()

    fun stop()
}
