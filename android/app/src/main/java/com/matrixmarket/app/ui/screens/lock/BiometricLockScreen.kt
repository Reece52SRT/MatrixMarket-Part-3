package com.matrixmarket.app.ui.screens.lock

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.matrixmarket.app.R
import com.matrixmarket.app.auth.BiometricHelper
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.theme.MatrixBlack
import com.matrixmarket.app.ui.theme.MatrixGold
import com.matrixmarket.app.ui.theme.RoyalPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Biometric Authentication: shown on app launch when fingerprint/face unlock is on.
// The saved session is only opened after the phone confirms the user's biometric.
@Composable
fun BiometricLockScreen(onUnlocked: () -> Unit, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }

    val title = stringResource(R.string.biometric_unlock_title)
    val subtitle = stringResource(R.string.biometric_unlock_subtitle)
    val usePassword = stringResource(R.string.use_password_instead)
    val unavailable = stringResource(R.string.biometric_unavailable)

    fun logoutToPassword() {
        scope.launch {
            AppContainer.authRepository.logout()
            onLoggedOut()
        }
    }

    fun prompt() {
        error = null
        if (BiometricHelper.availability(context) != BiometricHelper.Availability.AVAILABLE) {
            // e.g. fingerprints were removed from the phone - fall back to a password login.
            error = unavailable
            return
        }
        val activity = BiometricHelper.findActivity(context)
        if (activity == null) {
            error = unavailable
            return
        }
        BiometricHelper.authenticate(
            activity, title, subtitle, usePassword,
            onSuccess = {
                Log.i("BiometricLock", "Unlocked")
                onUnlocked()
            },
            onNegative = { logoutToPassword() },
            onError = { message -> error = message }
        )
    }

    LaunchedEffect(Unit) {
        name = AppContainer.sessionManager.getFullName().orEmpty()
        delay(350) // let the activity finish resuming before showing the system dialog
        prompt()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MatrixBlack).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MatrixGold, modifier = Modifier.size(88.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            if (name.isNotBlank()) stringResource(R.string.welcome_back_name, name) else stringResource(R.string.welcome_back),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)

        error?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = Color(0xFFFF8A8A), textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { prompt() },
            colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.unlock), color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { logoutToPassword() }) {
            Text(usePassword, color = Color.White.copy(alpha = 0.85f))
        }
    }
}
