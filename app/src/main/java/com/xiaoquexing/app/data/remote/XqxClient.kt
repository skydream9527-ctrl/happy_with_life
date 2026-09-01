package com.xiaoquexing.app.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class TokenHolder {
    @Volatile var accessToken: String? = null
    @Volatile var deviceId: String = "android-unbound"
}

fun createApiService(
    baseUrl: String,
    deviceId: String,
    appVersion: String,
    tokenProvider: () -> String?,
    deviceIdProvider: () -> String = { deviceId },
): ApiService {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    val headers = Interceptor { chain ->
        val b = chain.request().newBuilder()
            .header("X-Device-ID", deviceIdProvider())
            .header("X-Platform", "android")
            .header("X-App-Version", appVersion)
            .header("Accept", "application/json")
        tokenProvider()?.let { b.header("Authorization", "Bearer $it") }
        val response = chain.proceed(b.build())
        val body = response.body
        val contentType = body?.contentType()?.toString().orEmpty()
        if (!response.isSuccessful && body != null && !contentType.contains("json", ignoreCase = true)) {
            val raw = runCatching { body.string() }.getOrDefault(response.message)
            body.close()
            val safe = raw.replace("\\", "\\\\").replace("\"", "\\\"").take(180).ifBlank { "请求失败 ${response.code}" }
            val json = """{"error":{"code":"HTTP_${response.code}","message":"$safe","retryable":false}}"""
            return response.newBuilder()
                .body(okhttp3.ResponseBody.create("application/json".toMediaType(), json))
                .build()
        }
        response
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
