package com.matrixmarket.app.ui.screens.login

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.matrixmarket.app.R
import com.matrixmarket.app.auth.GoogleAuthManager
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.theme.RoyalPurple
import kotlinx.coroutines.launch

// White text fields on the dark auth screens.
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    enabled: Boolean = true
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(if (visible) R.string.hide_password else R.string.show_password)
                    )
                }
            }
        } else null,
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.8f),
            focusedBorderColor = RoyalPurple
        )
    )
}

// "Continue with Google" - runs Google Sign-In -> Firebase -> sends the Firebase
// ID token to our API (which verifies it server-side).
@Composable
fun GoogleSsoButton(viewModel: LoginViewModel, enabled: Boolean, label: String) {
    val scope = rememberCoroutineScope()
    val googleAuth = AppContainer.googleAuthManager

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch {
            googleAuth.handleSignInResult(result.data)
                .onSuccess { idToken -> viewModel.completeSsoLogin(idToken) }
                .onFailure { error ->
                    if (error is GoogleAuthManager.SsoCancelledException) viewModel.onSsoCancelled()
                    else viewModel.reportSsoFailure(error.message ?: "Google sign-in failed.")
                }
        }
    }

    OutlinedButton(
        onClick = {
            viewModel.onSsoStarted()
            scope.launch {
                try {
                    launcher.launch(googleAuth.getSignInIntent())
                } catch (e: Exception) {
                    Log.e("GoogleSsoButton", "Could not start Google sign-in", e)
                    viewModel.reportSsoFailure("Google sign-in is not available on this device.")
                }
            }
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White)
    }
}
