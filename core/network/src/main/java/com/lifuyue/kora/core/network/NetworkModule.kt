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
    fun provideBaseUrlProvider(): BaseUrlProvider = StaticBaseUrlProvider("https://localhost/")

    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor = AuthInterceptor { null }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.NONE
                },
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        baseUrlProvider: BaseUrlProvider,
        json: Json,
    ): Retrofit = createRetrofit(baseUrlProvider.getBaseUrl(), okHttpClient, json)

    @Provides
    @Singleton
    fun provideFastGptApi(retrofit: Retrofit): FastGptApi = retrofit.create(FastGptApi::class.java)

    @Provides
    @Singleton
    fun provideSseStreamClient(
        okHttpClient: OkHttpClient,
        json: Json,
        baseUrlProvider: BaseUrlProvider,
    ): SseStreamClient = SseStreamClient(okHttpClient, json, baseUrlProvider)
}
