package com.matrixmarket.app.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrixmarket.app.data.remote.dto.ListingResponse
import com.matrixmarket.app.data.repository.WatchlistRepository
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// User Defined Feature 5: Saved Listings / Watchlist.
class WatchlistViewModel(private val repository: WatchlistRepository) : ViewModel() {

    private val _state = MutableStateFlow<Resource<List<ListingResponse>>>(Resource.Loading)
    val state: StateFlow<Resource<List<ListingResponse>>> = _state.asStateFlow()

    fun load() {
        if (_state.value !is Resource.Success) _state.value = Resource.Loading
        viewModelScope.launch { _state.value = repository.getWatchlist() }
    }

    fun remove(listingId: Int) {
        val current = _state.value
        if (current is Resource.Success) _state.value = Resource.Success(current.data.filterNot { it.listingId == listingId })
        viewModelScope.launch {
            if (repository.remove(listingId) is Resource.Error) load()
        }
    }
}
