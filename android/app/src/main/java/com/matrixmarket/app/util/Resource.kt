package com.matrixmarket.app.util

// Simple sealed wrapper so every screen renders Loading / Success / Error states
// the same way for every network call, keeping the UI robust against failures.
sealed class Resource<out T> {
    data class Success<T>(val data: T, val fromCache: Boolean = false) : Resource<T>()

    // isNetworkError = the phone could not reach the API at all (offline / timeout),
    // so the UI can show a translated "you're offline" message instead of raw text.
    data class Error(val message: String, val isNetworkError: Boolean = false) : Resource<Nothing>()

    data object Loading : Resource<Nothing>()
    data object Idle : Resource<Nothing>()
}
