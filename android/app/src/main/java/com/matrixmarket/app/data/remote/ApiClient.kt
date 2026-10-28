package com.matrixmarket.app.data.remote

import android.util.Log
import com.matrixmarket.app.BuildConfig
import com.matrixmarket.app.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Builds the Retrofit client: attaches the JWT to every request and logs the user
// out cleanly if the API says the token is no longer valid (HTTP 401).
object ApiClient {
    private const val TAG = "ApiClient"

    fun create(sessionManager: SessionManager, onSessionExpired: () -> Unit): ApiService {
        val authInterceptor = Interceptor { chain ->
            val token = sessionManager.currentToken()
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
            }.build()

            val response = chain.proceed(request)
            if (response.code == 401 && !token.isNullOrBlank()) {
                Log.w(TAG, "API returned 401 for an authenticated request - session expired")
                onSessionExpired()
            }
            response
        }

        val logging = HttpLoggingInterceptor { message -> Log.d("OkHttp", message) }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            // Azure App Service can take a few seconds to wake up from idle.
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
