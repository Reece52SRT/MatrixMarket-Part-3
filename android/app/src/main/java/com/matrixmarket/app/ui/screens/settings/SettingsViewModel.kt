package com.matrixmarket.app.ui.screens.settings

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrixmarket.app.R
import com.matrixmarket.app.data.local.SessionManager
import com.matrixmarket.app.data.repository.AuthRepository
import com.matrixmarket.app.data.repository.ProfileRepository
import com.matrixmarket.app.notifications.PushTokenManager
import com.matrixmarket.app.ui.components.UiText
import com.matrixmarket.app.util.InputValidator
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val fullName: String = "",
    val savedName: String = "",
    val email: String = "",
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val saveMessage: UiText? = null,
    val saveSucceeded: Boolean = false
)

// Settings: profile name, language, notifications, biometric unlock, offline sync, logout.
class SettingsViewModel(
    private val sessionManager: SessionManager,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val pushTokenManager: PushTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    // Reads the CURRENT cached session (updated after every successful save).
    fun reload() {
        viewModelScope.launch {
            val session = sessionManager.getSession()
            _uiState.update {
                it.copy(
                    fullName = session?.fullName.orEmpty(),
                    savedName = session?.fullName.orEmpty(),
                    email = session?.email.orEmpty(),
                    language = currentLanguage(),
                    notificationsEnabled = sessionManager.getNotificationsEnabled(),
                    biometricEnabled = sessionManager.isBiometricEnabled()
                )
            }
        }
    }

    private suspend fun currentLanguage(): String {
        val applied = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return when {
            applied.startsWith("af") -> "af"
            applied.startsWith("zu") -> "zu"
            applied.startsWith("en") -> "en"
            else -> sessionManager.getLanguage()
        }
    }

    fun onFullNameChanged(name: String) = _uiState.update { it.copy(fullName = name.take(80), saveMessage = null) }

    fun saveProfile() {
        val name = _uiState.value.fullName.trim()
        if (!InputValidator.isValidName(name)) {
            _uiState.update { it.copy(saveMessage = UiText.Res(R.string.error_invalid_name), saveSucceeded = false) }
            return
        }
        _uiState.update { it.copy(isSaving = true, saveMessage = null) }
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, saveMessage = UiText.Res(R.string.error_not_logged_in)) }
                return@launch
            }
            when (val result = profileRepository.updateSettings(userId, name, null)) {
                is Resource.Success -> {
                    // The repository already refreshed the cached session; mirror it here.
                    Log.i(TAG, "Name saved remotely and cached locally")
                    _uiState.update {
                        it.copy(
                            isSaving = false, fullName = result.data.fullName, savedName = result.data.fullName,
                            saveMessage = UiText.Res(R.string.settings_saved), saveSucceeded = true
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isSaving = false, saveMessage = UiText.from(result), saveSucceeded = false)
                }
                else -> _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun onLanguageSelected(languageCode: String) {
        if (_uiState.value.language == languageCode) return
        _uiState.update { it.copy(language = languageCode) }
        viewModelScope.launch { sessionManager.setLanguage(languageCode) }
        Log.i(TAG, "Language changed to $languageCode")
        // AppCompat stores the choice and recreates the activity with the new translations.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
    }

    fun onNotificationsToggled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch {
            sessionManager.setNotificationsEnabled(enabled)
            // Turning notifications off also tells the API to stop pushing to this phone.
            if (enabled) pushTokenManager.syncToken() else pushTokenManager.unregister()
        }
    }

    fun onBiometricChanged(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
        viewModelScope.launch {
            sessionManager.setBiometricEnabled(enabled)
            sessionManager.setBiometricOfferShown()
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
