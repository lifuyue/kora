package com.lifuyue.kora.feature.chat

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTtsPlaybackController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : TtsPlaybackController {
        private val mutableState = MutableStateFlow(TtsPlaybackUiState())
        override val state: StateFlow<TtsPlaybackUiState> = mutableState

        private var textToSpeech: TextToSpeech? = null
        private var pendingRequest: TtsPlaybackRequest? = null
        private var initialized = false

        override fun play(request: TtsPlaybackRequest) {
            val sanitizedText = request.text.trim()
            if (sanitizedText.isBlank()) {
                mutableState.value =
                    TtsPlaybackUiState(
                        messageId = request.messageId,
                        status = TtsPlaybackStatus.Error,
                        errorMessage = context.appString("chat_error_tts_unavailable"),
                    )
                return
            }

            if (mutableState.value.messageId != null && mutableState.value.messageId != request.messageId) {
                stop()
            }

            pendingRequest = request.copy(text = sanitizedText)
            val engine = textToSpeech
            if (engine == null) {
                initializeEngine()
            } else {
                speak(engine, requireNotNull(pendingRequest))
            }
        }

        override fun pause() {
            val messageId = mutableState.value.messageId ?: return
            textToSpeech?.stop()
            mutableState.value =
                mutableState.value.copy(
                    messageId = messageId,
                    status = TtsPlaybackStatus.Paused,
                    progress = 0f,
                )
        }

        override fun stop() {
            val messageId = mutableState.value.messageId ?: return
            textToSpeech?.stop()
            mutableState.value =
                mutableState.value.copy(
                    messageId = messageId,
                    status = TtsPlaybackStatus.Stopped,
                    progress = 0f,
                )
        }

        private fun initializeEngine() {
            textToSpeech =
                TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        initialized = true
                        textToSpeech?.setOnUtteranceProgressListener(
                            object : UtteranceProgressListener() {
                                override fun onStart(utteranceId: String?) {
                                    val messageId = utteranceId ?: return
                                    mutableState.value =
                                        TtsPlaybackUiState(
                                            messageId = messageId,
                                            status = TtsPlaybackStatus.Playing,
                                            progress = 0f,
                                        )
                                }

                                override fun onDone(utteranceId: String?) {
                                    val messageId = utteranceId ?: return
                                    mutableState.value =
                                        TtsPlaybackUiState(
                                            messageId = messageId,
                                            status = TtsPlaybackStatus.Stopped,
                                            progress = 1f,
                                        )
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onError(utteranceId: String?) {
                                    val messageId = utteranceId ?: pendingRequest?.messageId
                                    mutableState.value =
                                        TtsPlaybackUiState(
                                            messageId = messageId,
                                            status = TtsPlaybackStatus.Error,
                                            errorMessage = context.appString("chat_error_tts_unavailable"),
                                        )
                                }
                            },
                        )
                        pendingRequest?.let { speak(requireNotNull(textToSpeech), it) }
                    } else {
                        mutableState.value =
                            TtsPlaybackUiState(
                                messageId = pendingRequest?.messageId,
                                status = TtsPlaybackStatus.Error,
                                errorMessage = context.appString("chat_error_tts_unavailable"),
                            )
                    }
                }
        }

        private fun speak(
            engine: TextToSpeech,
            request: TtsPlaybackRequest,
        ) {
            if (!initialized) {
                return
            }
            engine.language = Locale.getDefault()
            engine.setSpeechRate(request.audioPreferences.speechRate)
            mutableState.value =
                TtsPlaybackUiState(
                    messageId = request.messageId,
                    status = TtsPlaybackStatus.Playing,
                    progress = 0f,
                )
            val result = engine.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, request.messageId)
            if (result == TextToSpeech.ERROR) {
                mutableState.value =
                    TtsPlaybackUiState(
                        messageId = request.messageId,
                        status = TtsPlaybackStatus.Error,
                        errorMessage = context.appString("chat_error_tts_unavailable"),
                    )
            }
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsPlaybackModule {
    @Binds
    abstract fun bindTtsPlaybackController(controller: AndroidTtsPlaybackController): TtsPlaybackController
}
