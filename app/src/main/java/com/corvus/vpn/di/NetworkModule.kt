package com.corvus.vpn.di

import com.corvus.vpn.data.IpInfoApi
import com.corvus.vpn.data.VpnApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val IP_API_URL = "http://ip-api.com/"
    private const val CDN_BASE_URL = "https://pub-cb24fe4df15e483d8cb39116dcff1f7a.r2.dev/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(40, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideIpInfoApi(okHttpClient: OkHttpClient, json: Json): IpInfoApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(IP_API_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(IpInfoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideVpnApi(okHttpClient: OkHttpClient, json: Json): VpnApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(CDN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(VpnApi::class.java)
    }
}
