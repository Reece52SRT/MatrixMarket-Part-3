package com.matrixmarket.app.data.repository

import android.util.Log
import com.matrixmarket.app.data.local.SessionManager
import com.matrixmarket.app.data.remote.ApiService
import com.matrixmarket.app.data.remote.dto.AuthResponse
import com.matrixmarket.app.data.remote.dto.LoginRequest
import com.matrixmarket.app.data.remote.dto.RegisterRequest
import com.matrixmarket.app.data.remote.dto.SsoLoginRequest
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.apiCall

private const val TAG = "AuthRepository"

// Handles register / login / Google SSO and stores the resulting session.
open class AuthRepository(
    private val api: ApiService,
    private val sessionManager: SessionManager,
    private val onLoggedIn: suspend () -> Unit,
    private val onLoggingOut: suspend () -> Unit
) {
    open suspend fun register(email: String, password: String, fullName: String, campus: String?): Resource<AuthResponse> =
        finish("register", apiCall(TAG, "Could not create your account. Please try again.") {
            api.register(RegisterRequest(email, password, fullName, campus))
        })

    open suspend fun login(email: String, password: String): Resource<AuthResponse> =
        finish("login", apiCall(TAG, "Could not log in. Please try again.") {
            api.login(LoginRequest(email, password))
        })

    // Sends the Firebase ID token to the API, which verifies it server-side and
    // returns our own app token (lecturer feedback on the SSO feature).
    open suspend fun ssoLogin(firebaseIdToken: String): Resource<AuthResponse> =
        finish("ssoLogin", apiCall(TAG, "Google sign-in failed. Please try again.") {
            api.ssoLogin(SsoLoginRequest(firebaseIdToken))
        })

    private suspend fun finish(action: String, result: Resource<AuthResponse>): Resource<AuthResponse> {
        if (result is Resource.Success) {
            val body = result.data
            sessionManager.saveSession(body.token, body.userId, body.fullName, body.studentEmail, body.ssoProvider ?: "Email")
            Log.i(TAG, "$action succeeded for user ${body.userId}")
            try {
                onLoggedIn()
            } catch (e: Exception) {
                Log.w(TAG, "Post-login hook failed", e)
            }
        }
        return result
    }

    open suspend fun logout() {
        try {
            onLoggingOut() // e.g. unregister this phone from push notifications
        } catch (e: Exception) {
            Log.w(TAG, "Pre-logout hook failed", e)
        }
        sessionManager.logout()
        Log.i(TAG, "User logged out")
    }
}
