package com.matrixmarket.app.util

import android.util.Log
import retrofit2.Response
import java.io.IOException

// Shared "call the API safely" helpers used by every repository: they never throw,
// they always return a Resource the UI can show.

suspend fun <T> apiCall(tag: String, fallbackError: String, block: suspend () -> Response<T>): Resource<T> {
    return try {
        val response = block()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Resource.Success(body)
        } else {
            val message = ErrorParser.parse(response.errorBody()?.string(), response.code(), fallbackError)
            Log.w(tag, "API error ${response.code()}: $message")
            Resource.Error(message)
        }
    } catch (e: IOException) {
        Log.w(tag, "Network unavailable", e)
        Resource.Error(fallbackError, isNetworkError = true)
    } catch (e: Exception) {
        Log.e(tag, "Unexpected error", e)
        Resource.Error(fallbackError)
    }
}

// For endpoints that return 204 No Content.
suspend fun apiCallNoBody(tag: String, fallbackError: String, block: suspend () -> Response<Unit>): Resource<Unit> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            Resource.Success(Unit)
        } else {
            val message = ErrorParser.parse(response.errorBody()?.string(), response.code(), fallbackError)
            Log.w(tag, "API error ${response.code()}: $message")
            Resource.Error(message)
        }
    } catch (e: IOException) {
        Log.w(tag, "Network unavailable", e)
        Resource.Error(fallbackError, isNetworkError = true)
    } catch (e: Exception) {
        Log.e(tag, "Unexpected error", e)
        Resource.Error(fallbackError)
    }
}
