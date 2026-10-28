package com.matrixmarket.app.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matrixmarket.app.BuildConfig
import com.matrixmarket.app.R
import com.matrixmarket.app.auth.BiometricHelper
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.sync.SyncWorker
import com.matrixmarket.app.ui.components.ScreenHeader
import com.matrixmarket.app.ui.components.asString
import com.matrixmarket.app.ui.theme.ErrorRed
import com.matrixmarket.app.ui.theme.LavenderLight
import com.matrixmarket.app.ui.theme.RoyalPurple
import com.matrixmarket.app.ui.theme.SuccessGreen
import com.matrixmarket.app.ui.theme.TextMuted
import com.matrixmarket.app.util.ViewModelFactory

private val Languages = listOf("en" to "English", "af" to "Afrikaans", "zu" to "isiZulu")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = ViewModelFactory {
            SettingsViewModel(
                AppContainer.sessionManager, AppContainer.profileRepository,
                AppContainer.authRepository, AppContainer.pushTokenManager
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    val isOnline by AppContainer.connectivity.isOnline.collectAsState()
    val pendingCount by AppContainer.listingRepository.pendingCount.collectAsState(initial = 0)

    // Re-read the cached session every time Settings opens.
    LaunchedEffect(Unit) { viewModel.reload() }

    val biometricAvailability = remember { BiometricHelper.availability(context) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    val bioTitle = stringResource(R.string.biometric_enable_title)
    val bioSubtitle = stringResource(R.string.biometric_enable_subtitle)
    val cancel = stringResource(R.string.cancel)

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onNotificationsToggled(granted)
    }

    Surface(color = LavenderLight, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp)) {
            ScreenHeader(stringResource(R.string.settings))
            Spacer(Modifier.height(16.dp))

            SettingsCard(Icons.Filled.Person, stringResource(R.string.profile)) {
                if (state.email.isNotBlank()) {
                    Text(state.email, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = viewModel::onFullNameChanged,
                    label = { Text(stringResource(R.string.full_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.saveProfile() },
                    enabled = !state.isSaving && state.fullName.trim() != state.savedName,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple)
                ) {
                    Text(stringResource(if (state.isSaving) R.string.saving else R.string.save), color = Color.White)
                }
                state.saveMessage?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it.asString(), color = if (state.saveSucceeded) SuccessGreen else ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(12.dp))
            SettingsCard(Icons.Filled.Language, stringResource(R.string.language)) {
                Text(stringResource(R.string.language_desc), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Languages.forEach { (code, label) ->
                        FilterChip(
                            selected = state.language == code,
                            onClick = { viewModel.onLanguageSelected(code) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalPurple, selectedLabelColor = Color.White)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SettingsCard(Icons.Filled.Notifications, stringResource(R.string.notifications)) {
                SwitchRow(
                    text = stringResource(R.string.notifications_desc),
                    checked = state.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        val needsPermission = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else viewModel.onNotificationsToggled(enabled)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            SettingsCard(Icons.Filled.Fingerprint, stringResource(R.string.biometric_login)) {
                SwitchRow(
                    text = stringResource(R.string.biometric_login_desc),
                    checked = state.biometricEnabled,
                    enabled = biometricAvailability == BiometricHelper.Availability.AVAILABLE,
                    onCheckedChange = { enable ->
                        biometricError = null
                        if (!enable) {
                            viewModel.onBiometricChanged(false)
                        } else {
                            // Prove the biometric works before switching it on.
                            val activity = BiometricHelper.findActivity(context)
                            if (activity != null) {
                                BiometricHelper.authenticate(
                                    activity, bioTitle, bioSubtitle, cancel,
                                    onSuccess = { viewModel.onBiometricChanged(true) },
                                    onNegative = {},
                                    onError = { biometricError = it }
                                )
                            }
                        }
                    }
                )
                when (biometricAvailability) {
                    BiometricHelper.Availability.NOT_ENROLLED -> Text(stringResource(R.string.biometric_not_enrolled), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    BiometricHelper.Availability.UNAVAILABLE -> Text(stringResource(R.string.biometric_unavailable), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    BiometricHelper.Availability.AVAILABLE -> Unit
                }
                biometricError?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }
            }

            Spacer(Modifier.height(12.dp))
            SettingsCard(Icons.Filled.CloudSync, stringResource(R.string.offline_sync)) {
                Text(
                    if (pendingCount > 0) stringResource(R.string.pending_sync_count, pendingCount) else stringResource(R.string.all_synced),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(if (isOnline) R.string.status_online else R.string.status_offline),
                    color = if (isOnline) SuccessGreen else ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
                if (pendingCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { SyncWorker.enqueue(context) }, enabled = isOnline) {
                        Text(stringResource(R.string.sync_now))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SettingsCard(Icons.Filled.Info, stringResource(R.string.about)) {
                Text(stringResource(R.string.version_value, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.about_desc), color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = { viewModel.logout(onLoggedOut) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ErrorRed)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.log_out), color = ErrorRed)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = RoyalPurple)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoyalPurple)
        )
    }
}
