package com.matrixmarket.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrixmarket.app.data.remote.dto.NotificationResponse
import com.matrixmarket.app.data.repository.NotificationRepository
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// In-app notification inbox (read from the API's NoSQL database).
class NotificationsViewModel(private val repository: NotificationRepository) : ViewModel() {

    private val _state = MutableStateFlow<Resource<List<NotificationResponse>>>(Resource.Loading)
    val state: StateFlow<Resource<List<NotificationResponse>>> = _state.asStateFlow()

    init {
        // A new push arrived while the inbox is open - show it straight away.
        viewModelScope.launch { AppContainer.pushReceived.collect { load() } }
    }

    fun load() {
        if (_state.value !is Resource.Success) _state.value = Resource.Loading
        viewModelScope.launch { _state.value = repository.getNotifications() }
    }

    fun markRead(item: NotificationResponse) {
        if (item.isRead) return
        updateLocal { list -> list.map { if (it.id == item.id) it.copy(isRead = true) else it } }
        viewModelScope.launch { repository.markRead(item.id) }
    }

    fun markAllRead() {
        updateLocal { list -> list.map { it.copy(isRead = true) } }
        viewModelScope.launch { repository.markAllRead() }
    }

    private fun updateLocal(transform: (List<NotificationResponse>) -> List<NotificationResponse>) {
        val current = _state.value
        if (current is Resource.Success) _state.value = Resource.Success(transform(current.data))
    }
}
