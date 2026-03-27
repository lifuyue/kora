package com.lifuyue.kora.core.network

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {
    @Test
    fun addsBearerHeaderWhenApiKeyExists() {
        val interceptor = AuthInterceptor(StaticApiKeyProvider("fastgpt-secret"))
        val request =
            Request.Builder()
                .url("https://example.com/api/core/app/list")
                .build()

        val intercepted =
            interceptor.intercept(
                CapturingChain(request),
            )

        assertEquals("Bearer fastgpt-secret", intercepted.request.header("Authorization"))
    }

    @Test
    fun skipsHeaderWhenApiKeyMissing() {
        val interceptor = AuthInterceptor(StaticApiKeyProvider(null))
        val request =
            Request.Builder()
                .url("https://example.com/api/core/app/list")
                .build()

        val intercepted =
            interceptor.intercept(
                CapturingChain(request),
            )

        assertNull(intercepted.request.header("Authorization"))
    }

    private class CapturingChain(
        private val originalRequest: Request,
    ) : Interceptor.Chain {
        override fun request(): Request = originalRequest

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

        override fun withConnectTimeout(
            timeout: Int,
            unit: java.util.concurrent.TimeUnit,
        ) = this

        override fun readTimeoutMillis(): Int = 0

        override fun withReadTimeout(
            timeout: Int,
            unit: java.util.concurrent.TimeUnit,
        ) = this

        override fun writeTimeoutMillis(): Int = 0

        override fun withWriteTimeout(
            timeout: Int,
            unit: java.util.concurrent.TimeUnit,
        ) = this
    }
}
