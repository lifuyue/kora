package com.lifuyue.kora.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface OpenAiCompatibleApi {
    @GET("v1/models")
    suspend fun listModels(): OpenAiModelListResponse
}

@Serializable
data class OpenAiModelListResponse(
    val data: List<OpenAiModelDto> = emptyList(),
)

@Serializable
data class OpenAiModelDto(
    val id: String,
    @SerialName("owned_by")
    val ownedBy: String? = null,
)
