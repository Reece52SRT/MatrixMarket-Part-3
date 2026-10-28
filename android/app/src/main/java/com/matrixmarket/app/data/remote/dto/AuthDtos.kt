package com.matrixmarket.app.data.remote.dto

// Request/response bodies for /api/v1/auth/*

data class RegisterRequest(
    val studentEmail: String,
    val password: String,
    val fullName: String,
    val campus: String? = null
)

data class LoginRequest(
    val studentEmail: String,
    val password: String
)

// SSO: we send the Firebase ID token; the API verifies it server-side.
data class SsoLoginRequest(
    val idToken: String
)

data class AuthResponse(
    val token: String,
    val userId: Int,
    val fullName: String,
    val studentEmail: String,
    val karmaPoints: Int,
    val ssoProvider: String?
)
