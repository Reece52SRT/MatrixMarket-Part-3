package com.matrixmarket.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.matrixmarket.app.R
import com.matrixmarket.app.util.Resource

// Text a ViewModel wants to show without needing a Context: either a translated
// string resource, or plain text that came back from the API.
sealed class UiText {
    data class Plain(val value: String) : UiText()
    data class Res(@StringRes val id: Int) : UiText()

    companion object {
        // Network failures get the translated "you're offline" message.
        fun from(error: Resource.Error): UiText =
            if (error.isNetworkError) Res(R.string.error_offline) else Plain(error.message)
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> value
    is UiText.Res -> stringResource(id)
}

@Composable
fun Resource.Error.displayText(): String = UiText.from(this).asString()
