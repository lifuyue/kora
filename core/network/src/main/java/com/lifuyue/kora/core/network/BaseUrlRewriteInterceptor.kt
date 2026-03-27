package com.lifuyue.kora.core.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class BaseUrlRewriteInterceptor(
    private val baseUrlProvider: BaseUrlProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val runtimeBaseUrl = baseUrlProvider.getBaseUrl().toHttpUrl()
        val requestUrl = request.url
        val placeholderBaseUrl = RETROFIT_PLACEHOLDER_BASE_URL.toHttpUrl()

        val rewrittenUrl =
            if (requestUrl.host == placeholderBaseUrl.host) {
                val runtimeSegments = runtimeBaseUrl.pathSegments.filter { it.isNotBlank() }
                val requestSegments = requestUrl.pathSegments.filter { it.isNotBlank() }
                val combinedSegments = runtimeSegments + requestSegments
                val encodedPath =
                    if (combinedSegments.isEmpty()) {
                        "/"
                    } else {
                        "/${combinedSegments.joinToString("/")}"
                    }

                requestUrl.newBuilder()
                    .scheme(runtimeBaseUrl.scheme)
                    .host(runtimeBaseUrl.host)
                    .port(runtimeBaseUrl.port)
                    .encodedPath(encodedPath)
                    .build()
            } else {
                requestUrl
            }

        return chain.proceed(
            request.newBuilder()
                .url(rewrittenUrl)
                .build(),
        )
    }
}
