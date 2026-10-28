package com.matrixmarket.app.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// Generic factory so every ViewModel can take constructor-injected repositories
// from the AppContainer while still working with Compose's viewModel() helper.
class ViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
