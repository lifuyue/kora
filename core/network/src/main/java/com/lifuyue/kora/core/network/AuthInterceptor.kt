package com.lifuyue.kora.core.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val apiKeyProvider: () -> String?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val apiKey = apiKeyProvider()?.takeIf { it.isNotBlank() }
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
