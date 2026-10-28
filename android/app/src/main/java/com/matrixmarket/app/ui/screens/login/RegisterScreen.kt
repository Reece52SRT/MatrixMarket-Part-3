package com.matrixmarket.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matrixmarket.app.R
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.asString
import com.matrixmarket.app.ui.theme.MatrixBlack
import com.matrixmarket.app.ui.theme.RoyalPurple
import com.matrixmarket.app.util.ViewModelFactory

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onBackToLogin: () -> Unit) {
    val viewModel: LoginViewModel = viewModel(factory = ViewModelFactory { LoginViewModel(AppContainer.authRepository) })
    val state by viewModel.state.collectAsState()

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var campus by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.isSuccess) { if (state.isSuccess) onRegisterSuccess() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MatrixBlack)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.create_your_account), color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.register_subtitle), color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(24.dp))

        GoogleSsoButton(viewModel = viewModel, enabled = !state.isLoading, label = stringResource(R.string.sign_up_google))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.or_use_email), color = Color.White.copy(alpha = 0.6f))
        Spacer(Modifier.height(12.dp))

        AuthTextField(fullName, { fullName = it; viewModel.clearError() }, stringResource(R.string.full_name), Icons.Filled.Person, enabled = !state.isLoading)
        Spacer(Modifier.height(12.dp))
        AuthTextField(email, { email = it; viewModel.clearError() }, stringResource(R.string.student_email), Icons.Filled.Email, KeyboardType.Email, enabled = !state.isLoading)
        Spacer(Modifier.height(12.dp))
        AuthTextField(campus, { campus = it }, stringResource(R.string.campus_optional), Icons.Filled.School, enabled = !state.isLoading)
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            password, { password = it; viewModel.clearError() }, stringResource(R.string.password_hint),
            Icons.Filled.Lock, KeyboardType.Password, ImeAction.Done, isPassword = true, enabled = !state.isLoading
        )

        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it.asString(), color = Color(0xFFFF8A8A), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(22.dp))
        Button(
            onClick = { viewModel.register(email, password, fullName, campus) },
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.create_account), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBackToLogin) {
            Text(stringResource(R.string.have_account_login), color = Color.White.copy(alpha = 0.85f))
        }
    }
}
