package com.lifuyue.kora.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifuyue.kora.core.network.FastGptApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppAnalyticsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val api: FastGptApi,
    ) : ViewModel() {
        private val appId: String = checkNotNull(savedStateHandle["appId"])
        private val mutableState = MutableStateFlow(AppAnalyticsUiState())
        val uiState: StateFlow<AppAnalyticsUiState> = mutableState.asStateFlow()

        init {
            refresh()
        }

        fun updateRange(range: AnalyticsRange) {
            mutableState.value = mutableState.value.copy(range = range, status = AnalyticsStatus.Loading)
            refresh()
        }

        private fun refresh() {
            viewModelScope.launch {
                runCatching { api.getAppAnalytics(appId = appId, range = mutableState.value.range.raw).data }
                    .onSuccess { data ->
                        if (data == null) {
                            mutableState.value = mutableState.value.copy(status = AnalyticsStatus.Empty)
                        } else {
                            mutableState.value =
                                mutableState.value.copy(
                                    requestCount = data.requestCount,
                                    conversationCount = data.conversationCount,
                                    inputTokens = data.inputTokens,
                                    outputTokens = data.outputTokens,
                                    status = AnalyticsStatus.Success,
                                    errorMessage = null,
                                )
                        }
                    }.onFailure { error ->
                        mutableState.value =
                            mutableState.value.copy(
                                status = AnalyticsStatus.Error,
                                errorMessage = error.message,
                            )
                    }
            }
        }
    }
