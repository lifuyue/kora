package com.lifuyue.kora.feature.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.lifuyue.kora.core.common.AudioPreferences
import com.lifuyue.kora.core.common.SpeechToTextEngine
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface ChatAudioPreferencesSource {
    fun currentAudioPreferences(): AudioPreferences
}

@Singleton
class ConnectionRepositoryChatAudioPreferencesSource
    @Inject
    constructor(
        private val connectionRepository: ConnectionRepository,
    ) : ChatAudioPreferencesSource {
        override fun currentAudioPreferences(): AudioPreferences = connectionRepository.snapshot.value.audioPreferences
    }

interface SpeechRecognitionEngine {
    fun start(
        speechToTextEngine: SpeechToTextEngine,
        onPartialTranscript: (String) -> Unit,
        onFinalTranscript: (String) -> Unit,
        onError: (SpeechRecognitionError) -> Unit,
    ): SpeechRecognitionSession
}

interface SpeechRecognitionSession {
    fun stop()

    fun cancel()
}

sealed interface SpeechRecognitionError {
    data class PermissionDenied(
        val message: String,
    ) : SpeechRecognitionError

    data class Unavailable(
        val message: String,
    ) : SpeechRecognitionError

    data class RecognitionFailed(
        val message: String,
    ) : SpeechRecognitionError
}

@Singleton
class SystemSpeechRecognitionEngine
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SpeechRecognitionEngine {
        @Suppress("UNUSED_PARAMETER")
        override fun start(
            speechToTextEngine: SpeechToTextEngine,
            onPartialTranscript: (String) -> Unit,
            onFinalTranscript: (String) -> Unit,
            onError: (SpeechRecognitionError) -> Unit,
        ): SpeechRecognitionSession {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError(
                    SpeechRecognitionError.Unavailable(
                        context.appString("chat_error_speech_unavailable"),
                    ),
                )
                return NoOpSpeechRecognitionSession
            }

            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            var finished = false

            fun finish() {
                if (!finished) {
                    finished = true
                    recognizer.destroy()
                }
            }

            recognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit

                    override fun onBeginningOfSpeech() = Unit

                    override fun onRmsChanged(rmsdB: Float) = Unit

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() = Unit

                    override fun onError(error: Int) {
                        if (finished) {
                            return
                        }
                        val mappedError =
                            when (error) {
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                    SpeechRecognitionError.PermissionDenied(
                                        context.appString("chat_error_speech_permission_required"),
                                    )
                                else ->
                                    SpeechRecognitionError.RecognitionFailed(
                                        context.appString("chat_error_speech_failed"),
                                    )
                            }
                        onError(mappedError)
                        finish()
                    }

                    override fun onResults(results: Bundle?) {
                        if (finished) {
                            return
                        }
                        results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                            ?.let(onFinalTranscript)
                        finish()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                            ?.let(onPartialTranscript)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                },
            )

            val intent =
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

            try {
                recognizer.startListening(intent)
            } catch (_: SecurityException) {
                finish()
                onError(
                    SpeechRecognitionError.PermissionDenied(
                        context.appString("chat_error_speech_permission_required"),
                    ),
                )
                return NoOpSpeechRecognitionSession
            } catch (_: Throwable) {
                finish()
                onError(
                    SpeechRecognitionError.RecognitionFailed(
                        context.appString("chat_error_speech_failed"),
                    ),
                )
                return NoOpSpeechRecognitionSession
            }

            return object : SpeechRecognitionSession {
                override fun stop() {
                    if (!finished) {
                        recognizer.stopListening()
                    }
                }

                override fun cancel() {
                    if (!finished) {
                        recognizer.cancel()
                        finish()
                    }
                }
            }
        }
    }

private object NoOpSpeechRecognitionSession : SpeechRecognitionSession {
    override fun stop() = Unit

    override fun cancel() = Unit
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatSpeechSupportModule {
    @Binds
    abstract fun bindSpeechRecognitionEngine(engine: SystemSpeechRecognitionEngine): SpeechRecognitionEngine

    @Binds
    abstract fun bindChatAudioPreferencesSource(source: ConnectionRepositoryChatAudioPreferencesSource): ChatAudioPreferencesSource
}
