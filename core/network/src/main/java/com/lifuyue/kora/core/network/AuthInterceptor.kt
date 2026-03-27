package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.ConnectionSnapshotProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider,
) : Interceptor {
    constructor(connectionSnapshotProvider: ConnectionSnapshotProvider) : this(
        apiKeyProvider = ApiKeyProvider { connectionSnapshotProvider.getSnapshot().apiKey },
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val apiKey = apiKeyProvider.getApiKey().takeIf { !it.isNullOrBlank() }
        val authenticatedRequest =
            if (apiKey == null) {
                request
            } else {
                request.newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .build()
            }

        return chain.proceed(authenticatedRequest)
    }
}
