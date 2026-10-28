package com.matrixmarket.app.data.repository

import com.matrixmarket.app.data.local.SessionManager
import com.matrixmarket.app.data.remote.ApiService
import com.matrixmarket.app.data.remote.dto.UpdateSettingsRequest
import com.matrixmarket.app.data.remote.dto.UpdateSettingsResponse
import com.matrixmarket.app.data.remote.dto.UserProfileResponse
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.apiCall

private const val TAG = "ProfileRepository"

class ProfileRepository(
    private val api: ApiService,
    private val sessionManager: SessionManager
) {
    suspend fun getProfile(userId: Int): Resource<UserProfileResponse> =
        apiCall(TAG, "Could not load your profile.") { api.getProfile(userId) }

    // Saves remotely, THEN refreshes the cached session with what the server stored
    // (fixes "reopening settings restores the old name").
    suspend fun updateSettings(userId: Int, fullName: String?, campus: String?): Resource<UpdateSettingsResponse> {
        val result = apiCall(TAG, "Could not save your settings.") {
            api.updateSettings(userId, UpdateSettingsRequest(campus = campus, fullName = fullName))
        }
        if (result is Resource.Success) sessionManager.updateFullName(result.data.fullName)
        return result
    }
}
