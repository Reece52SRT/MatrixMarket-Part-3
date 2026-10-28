package com.matrixmarket.app.ui.screens.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrixmarket.app.R
import com.matrixmarket.app.data.repository.AuthRepository
import com.matrixmarket.app.ui.components.UiText
import com.matrixmarket.app.util.InputValidator
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isSuccess: Boolean = false
)

// Shared by the Login and Register screens (email/password + Google SSO).
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        val error = when {
            email.isBlank() || password.isBlank() -> R.string.error_enter_email_password
            !InputValidator.isValidEmail(email) -> R.string.error_invalid_email
            else -> null
        }
        if (error != null) {
            _state.value = AuthUiState(error = UiText.Res(error))
            return
        }
        launchAuth { authRepository.login(email.trim(), password) }
    }

    fun register(email: String, password: String, fullName: String, campus: String?) {
        val error = when {
            !InputValidator.isValidName(fullName) -> R.string.error_invalid_name
            !InputValidator.isValidEmail(email) -> R.string.error_invalid_email
            !InputValidator.isValidPassword(password) -> R.string.error_password_short
            else -> null
        }
        if (error != null) {
            _state.value = AuthUiState(error = UiText.Res(error))
            return
        }
        launchAuth { authRepository.register(email.trim(), password, fullName.trim(), campus?.trim()?.ifBlank { null }) }
    }

    // Called after GoogleAuthManager has signed in with Firebase and produced an ID token.
    fun completeSsoLogin(firebaseIdToken: String) {
        launchAuth { authRepository.ssoLogin(firebaseIdToken) }
    }

    fun onSsoStarted() {
        _state.value = AuthUiState(isLoading = true)
    }

    fun onSsoCancelled() {
        _state.value = AuthUiState()
    }

    fun reportSsoFailure(message: String) {
        Log.w(TAG, "SSO failed: $message")
        _state.value = AuthUiState(error = UiText.Plain(message))
    }

    fun clearError() {
        if (_state.value.error != null) _state.value = _state.value.copy(error = null)
    }

    private fun launchAuth(block: suspend () -> Resource<*>) {
        _state.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            _state.value = when (val result = block()) {
                is Resource.Success -> AuthUiState(isSuccess = true)
                is Resource.Error -> AuthUiState(error = UiText.from(result))
                else -> AuthUiState()
            }
        }
    }

    private companion object {
        const val TAG = "LoginViewModel"
    }
}
