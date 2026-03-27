package com.lifuyue.kora.core.network

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test

class BaseUrlRewriteInterceptorTest {
    @Test
    fun rewritesPlaceholderHostToRuntimeBaseUrl() {
        val interceptor =
            BaseUrlRewriteInterceptor(
                StaticBaseUrlProvider("https://example.com/tenant/"),
            )
        val request =
            Request.Builder()
                .url("${RETROFIT_PLACEHOLDER_BASE_URL}api/core/app/list")
                .build()

        val response = interceptor.intercept(CapturingChain(request))

        assertEquals(
            "https://example.com/tenant/api/core/app/list",
            response.request.url.toString(),
        )
    }

    @Test
    fun keepsExistingUrlWhenHostIsNotPlaceholder() {
        val interceptor =
            BaseUrlRewriteInterceptor(
                StaticBaseUrlProvider("https://example.com/tenant/"),
            )
        val request =
            Request.Builder()
                .url("https://another.example.com/api/core/app/list")
                .build()

        val response = interceptor.intercept(CapturingChain(request))

        assertEquals("https://another.example.com/api/core/app/list", response.request.url.toString())
    }

    private class CapturingChain(
        private val request: Request,
    ) : Interceptor.Chain {
        override fun request(): Request = request

        override fun proceed(request: Request): Response =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()

        override fun connection() = null

        override fun call() = throw UnsupportedOperationException()

        override fun connectTimeoutMillis(): Int = 0

        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this

        override fun readTimeoutMillis(): Int = 0

        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this

        override fun writeTimeoutMillis(): Int = 0

        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
    }
}
