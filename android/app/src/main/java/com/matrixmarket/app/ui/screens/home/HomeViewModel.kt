package com.matrixmarket.app.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrixmarket.app.data.local.db.PendingListingEntity
import com.matrixmarket.app.data.remote.dto.CategoryDto
import com.matrixmarket.app.data.remote.dto.ListingResponse
import com.matrixmarket.app.data.repository.ListingRepository
import com.matrixmarket.app.data.repository.NotificationRepository
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// "Current Listings" - the main REST API integration screen (GET /api/v1/listings),
// with a Room cache for offline browsing and the offline outbox shown on top.
class HomeViewModel(
    private val repository: ListingRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _listingsState = MutableStateFlow<Resource<List<ListingResponse>>>(Resource.Loading)
    val listingsState: StateFlow<Resource<List<ListingResponse>>> = _listingsState.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryDto>>(emptyList())
    val categories: StateFlow<List<CategoryDto>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    val pendingListings: StateFlow<List<PendingListingEntity>> =
        repository.pendingListings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch { _categories.value = repository.getCategories() }

        // Reload automatically when the connection comes back.
        viewModelScope.launch {
            AppContainer.connectivity.isOnline.drop(1).collect { online ->
                if (online) {
                    Log.i(TAG, "Back online - refreshing listings")
                    loadListings()
                    if (_categories.value.size <= 5) _categories.value = repository.getCategories()
                }
            }
        }

        // A push notification arrived - refresh the bell badge.
        viewModelScope.launch { AppContainer.pushReceived.collect { refreshUnread() } }
    }

    // Called every time the screen is shown.
    fun onScreenShown() {
        loadListings()
        refreshUnread()
    }

    fun loadListings() {
        loadJob?.cancel()
        if (_listingsState.value !is Resource.Success) _listingsState.value = Resource.Loading
        loadJob = viewModelScope.launch {
            _listingsState.value = repository.getListings(_selectedCategoryId.value, _searchQuery.value.ifBlank { null })
        }
    }

    fun refreshUnread() {
        viewModelScope.launch { _unreadCount.value = notificationRepository.getUnreadCount() }
    }

    fun onCategorySelected(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
        _listingsState.value = Resource.Loading
        loadListings()
    }

    // Waits until the user stops typing before calling the API.
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadListings()
        }
    }

    fun retryPending(item: PendingListingEntity) {
        viewModelScope.launch { repository.retryPending(item.localId) }
    }

    fun discardPending(item: PendingListingEntity) {
        viewModelScope.launch { repository.discardPending(item) }
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
