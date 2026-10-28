package com.matrixmarket.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "matrix_market_session")

// Everything we remember about the logged-in user between app launches.
data class Session(
    val token: String,
    val userId: Int,
    val fullName: String,
    val email: String,
    val provider: String
)

// Persists the JWT, the cached profile basics and the user's preferences with DataStore.
class SessionManager(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = intPreferencesKey("user_id")
        val FULL_NAME = stringPreferencesKey("full_name")
        val EMAIL = stringPreferencesKey("student_email")
        val PROVIDER = stringPreferencesKey("sso_provider")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled_v2")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val BIOMETRIC_ASKED = booleanPreferencesKey("biometric_offer_shown")
    }

    // In-memory copy of the token so the OkHttp interceptor never blocks on disk.
    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var tokenLoaded = false

    // Called from OkHttp's background thread. Reads from disk only the very first time.
    fun currentToken(): String? {
        if (!tokenLoaded) {
            cachedToken = runBlocking { context.dataStore.data.first()[Keys.TOKEN] }
            tokenLoaded = true
        }
        return cachedToken
    }

    // Call once at start-up so the interceptor has the token before the first request.
    suspend fun warmUp() {
        cachedToken = context.dataStore.data.first()[Keys.TOKEN]
        tokenLoaded = true
    }

    private fun Preferences.toSession(): Session? {
        val token = this[Keys.TOKEN] ?: return null
        val userId = this[Keys.USER_ID] ?: return null
        return Session(
            token = token,
            userId = userId,
            fullName = this[Keys.FULL_NAME].orEmpty(),
            email = this[Keys.EMAIL].orEmpty(),
            provider = this[Keys.PROVIDER] ?: "Email"
        )
    }

    val sessionFlow: Flow<Session?> = context.dataStore.data.map { it.toSession() }

    suspend fun getSession(): Session? = context.dataStore.data.first().toSession()

    suspend fun saveSession(token: String, userId: Int, fullName: String, email: String, provider: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = userId
            prefs[Keys.FULL_NAME] = fullName
            prefs[Keys.EMAIL] = email
            prefs[Keys.PROVIDER] = provider
        }
        cachedToken = token
        tokenLoaded = true
    }

    // Lecturer feedback (Settings): after the name is saved on the server we update the
    // cached copy too, so reopening Settings shows the NEW name instead of the old one.
    suspend fun updateFullName(fullName: String) {
        context.dataStore.edit { it[Keys.FULL_NAME] = fullName }
    }

    suspend fun getToken(): String? = context.dataStore.data.first()[Keys.TOKEN]

    suspend fun getUserId(): Int? = context.dataStore.data.first()[Keys.USER_ID]

    suspend fun getFullName(): String? = context.dataStore.data.first()[Keys.FULL_NAME]

    suspend fun isLoggedIn(): Boolean = getSession() != null

    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = languageCode }
    }

    suspend fun getLanguage(): String = context.dataStore.data.first()[Keys.LANGUAGE] ?: "en"

    val notificationsEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun getNotificationsEnabled(): Boolean = notificationsEnabledFlow.first()

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun isBiometricEnabled(): Boolean = context.dataStore.data.first()[Keys.BIOMETRIC_ENABLED] ?: false

    suspend fun setBiometricOfferShown() {
        context.dataStore.edit { it[Keys.BIOMETRIC_ASKED] = true }
    }

    suspend fun wasBiometricOfferShown(): Boolean = context.dataStore.data.first()[Keys.BIOMETRIC_ASKED] ?: false

    // Clears the login but keeps device preferences (language, notifications, biometrics choice).
    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.FULL_NAME)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.PROVIDER)
            prefs.remove(Keys.BIOMETRIC_ENABLED)
            prefs.remove(Keys.BIOMETRIC_ASKED)
        }
        cachedToken = null
        tokenLoaded = true
    }
}
