package com.xiaoquexing.app.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

fun createApiService(
    baseUrl: String,
    deviceId: String,
    appVersion: String,
    tokenProvider: () -> String?,
): ApiService {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    val headers = Interceptor { chain ->
        val b = chain.request().newBuilder()
            .header("X-Device-ID", deviceId)
            .header("X-Platform", "android")
            .header("X-App-Version", appVersion)
            .header("Accept", "application/json")
        tokenProvider()?.let { b.header("Authorization", "Bearer $it") }
        chain.proceed(b.build())
    }
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(headers)
        .build()
    return Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiService::class.java)
}
