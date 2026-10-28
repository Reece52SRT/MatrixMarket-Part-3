package com.matrixmarket.app.data.remote.dto

data class UserProfileResponse(
    val userId: Int,
    val fullName: String,
    val studentEmail: String,
    val campus: String?,
    val karmaPoints: Int,
    val trustTier: String,
    val badges: List<String>?,
    val listingsCount: Int,
    val memberSince: String,
    val tradesCompleted: Int
)

data class UpdateSettingsRequest(
    val campus: String? = null,
    val fullName: String? = null
)

data class UpdateSettingsResponse(
    val userId: Int,
    val fullName: String,
    val campus: String?
)

data class DeviceTokenRequest(
    val token: String
)
