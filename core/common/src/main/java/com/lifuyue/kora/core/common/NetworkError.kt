package com.lifuyue.kora.core.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NetworkError(
    val code: Int,
    val statusText: String,
    val message: String,
    val data: JsonElement? = null,
)
