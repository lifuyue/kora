package com.lifuyue.kora.core.common

sealed interface AppResult<out T> {
    data class Success<T>(
        val data: T,
    ) : AppResult<T>

    data class Error(
        val error: NetworkError,
    ) : AppResult<Nothing>

    data object Loading : AppResult<Nothing>
}
