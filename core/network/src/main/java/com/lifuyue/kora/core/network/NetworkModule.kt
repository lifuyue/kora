package com.lifuyue.kora.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideNetworkJson(): Json = NetworkJson.default

    @Provides
    @Singleton
    fun provideApiFactory(json: Json): FastGptApiFactory = FastGptApiFactory(json)

    @Provides
    @Singleton
    fun provideOpenAiCompatibleApiFactory(json: Json): OpenAiCompatibleApiFactory = OpenAiCompatibleApiFactory(json)

    @Provides
    @Singleton
    fun provideMutableConnectionProvider(): MutableConnectionProvider = MutableConnectionProvider()

    @Provides
    @Singleton
    fun provideBaseUrlProvider(connectionProvider: MutableConnectionProvider): BaseUrlProvider = connectionProvider

    @Provides
    @Singleton
    fun provideApiKeyProvider(connectionProvider: MutableConnectionProvider): ApiKeyProvider = connectionProvider

    @Provides
    @Singleton
    fun provideAuthInterceptor(apiKeyProvider: ApiKeyProvider): AuthInterceptor = AuthInterceptor(apiKeyProvider)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        baseUrlProvider: BaseUrlProvider,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(BaseUrlRewriteInterceptor(baseUrlProvider))
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    redactHeader("Authorization")
                    level = HttpLoggingInterceptor.Level.NONE
                },
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = createRetrofit(RETROFIT_PLACEHOLDER_BASE_URL, okHttpClient, json)

    @Provides
    @Singleton
    fun provideFastGptApi(retrofit: Retrofit): FastGptApi = retrofit.create(FastGptApi::class.java)

    @Provides
    @Singleton
    fun provideSseStreamClient(
        okHttpClient: OkHttpClient,
        json: Json,
        baseUrlProvider: BaseUrlProvider,
        connectionProvider: MutableConnectionProvider,
    ): SseStreamClient = SseStreamClient(okHttpClient, json, baseUrlProvider, connectionProvider)
}
