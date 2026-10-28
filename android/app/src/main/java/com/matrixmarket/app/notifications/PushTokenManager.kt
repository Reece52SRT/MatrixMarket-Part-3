package com.matrixmarket.app.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.matrixmarket.app.data.local.SessionManager
import com.matrixmarket.app.data.remote.ApiService
import com.matrixmarket.app.data.remote.dto.DeviceTokenRequest
import kotlinx.coroutines.tasks.await

// Registers this phone's Firebase Cloud Messaging token with our API so the server
// can send real-time pushes (new offer, meeting scheduled, trade confirmed...).
class PushTokenManager(
    private val api: ApiService,
    private val sessionManager: SessionManager
) {
    suspend fun syncToken(knownToken: String? = null) {
        try {
            if (!sessionManager.isLoggedIn() || !sessionManager.getNotificationsEnabled()) return
            val token = knownToken ?: FirebaseMessaging.getInstance().token.await()
            val response = api.saveDeviceToken(DeviceTokenRequest(token))
            Log.i(TAG, "FCM token registered with API (HTTP ${response.code()})")
        } catch (e: Exception) {
            Log.w(TAG, "Could not register FCM token (will retry next launch)", e)
        }
    }

    suspend fun unregister() {
        try {
            if (sessionManager.getToken() == null) return
            val response = api.removeDeviceToken()
            Log.i(TAG, "FCM token removed from API (HTTP ${response.code()})")
        } catch (e: Exception) {
            Log.w(TAG, "Could not remove FCM token", e)
        }
    }

    private companion object {
        const val TAG = "PushTokenManager"
    }
}
