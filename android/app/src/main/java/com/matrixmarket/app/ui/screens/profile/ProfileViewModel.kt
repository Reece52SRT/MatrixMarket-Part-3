package com.matrixmarket.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrixmarket.app.data.local.SessionManager
import com.matrixmarket.app.data.remote.dto.UserProfileResponse
import com.matrixmarket.app.data.repository.ProfileRepository
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// A badge in the Trust Badges row: earned ones in colour, locked ones greyed out.
data class BadgeDisplay(val type: String, val earned: Boolean)

// User Defined Feature 3: Gamified Student Score profile.
class ProfileViewModel(
    private val repository: ProfileRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _profileState = MutableStateFlow<Resource<UserProfileResponse>>(Resource.Loading)
    val profileState: StateFlow<Resource<UserProfileResponse>> = _profileState.asStateFlow()

    fun loadProfile() {
        if (_profileState.value !is Resource.Success) _profileState.value = Resource.Loading
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            _profileState.value = if (userId == null) Resource.Error("Please log in again.")
            else repository.getProfile(userId)
        }
    }

    companion object {
        // Every badge the API can award (mirrors KarmaService.EvaluateBadgesAsync).
        val ALL_BADGES = listOf("First Trade", "Top Seller", "Trusted Trader")

        fun badges(earned: List<String>): List<BadgeDisplay> =
            ALL_BADGES.map { BadgeDisplay(it, earned.contains(it)) }
    }
}
