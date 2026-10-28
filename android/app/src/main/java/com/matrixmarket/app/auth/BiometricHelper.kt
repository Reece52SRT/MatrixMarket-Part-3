package com.matrixmarket.app.auth

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// Biometric authentication (fingerprint / face) using the AndroidX Biometric library.
object BiometricHelper {
    private const val TAG = "BiometricHelper"

    enum class Availability { AVAILABLE, NOT_ENROLLED, UNAVAILABLE }

    fun availability(context: Context): Availability =
        when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NOT_ENROLLED
            else -> Availability.UNAVAILABLE
        }

    fun findActivity(context: Context): FragmentActivity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is FragmentActivity) return current
            current = current.baseContext
        }
        return null
    }

    // Shows the system fingerprint/face dialog.
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onNegative: () -> Unit,
        onError: (String) -> Unit
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.i(TAG, "Biometric authentication succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.w(TAG, "Biometric error $errorCode: $errString")
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) onNegative()
                else onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                // A finger/face was not recognised - the system dialog stays open to retry.
                Log.i(TAG, "Biometric not recognised")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .setConfirmationRequired(false)
            .build()

        try {
            BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Could not show biometric prompt", e)
            onError(e.localizedMessage ?: "Biometric prompt unavailable")
        }
    }
}
