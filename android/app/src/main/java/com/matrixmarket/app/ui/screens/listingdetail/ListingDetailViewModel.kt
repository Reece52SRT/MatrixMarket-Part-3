package com.matrixmarket.app.ui.screens.listingdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrixmarket.app.R
import com.matrixmarket.app.data.local.SessionManager
import com.matrixmarket.app.data.remote.dto.ListingResponse
import com.matrixmarket.app.data.repository.ListingRepository
import com.matrixmarket.app.data.repository.TradeRepository
import com.matrixmarket.app.data.repository.WatchlistRepository
import com.matrixmarket.app.ui.components.UiText
import com.matrixmarket.app.util.InputValidator
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OfferUiState(
    val isSending: Boolean = false,
    val error: UiText? = null,
    val createdOfferId: Int? = null
)

// One listing's details: make an offer (-> Trade Meeting), save it to the watchlist,
// or delete it if it's your own.
class ListingDetailViewModel(
    private val listingRepository: ListingRepository,
    private val tradeRepository: TradeRepository,
    private val watchlistRepository: WatchlistRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _listingState = MutableStateFlow<Resource<ListingResponse>>(Resource.Loading)
    val listingState: StateFlow<Resource<ListingResponse>> = _listingState.asStateFlow()

    private val _offerState = MutableStateFlow(OfferUiState())
    val offerState: StateFlow<OfferUiState> = _offerState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    private val _deleteState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val deleteState: StateFlow<Resource<Unit>> = _deleteState.asStateFlow()

    private var loadedId: Int? = null

    fun load(listingId: Int) {
        if (loadedId == listingId && _listingState.value is Resource.Success) return
        loadedId = listingId
        _listingState.value = Resource.Loading
        viewModelScope.launch {
            _currentUserId.value = sessionManager.getUserId()
            val result = listingRepository.getListingById(listingId)
            _listingState.value = result
            if (result is Resource.Success && !result.fromCache && result.data.sellerId != _currentUserId.value) {
                _isSaved.value = watchlistRepository.isSaved(listingId)
            }
        }
    }

    fun retry(listingId: Int) {
        loadedId = null
        load(listingId)
    }

    fun makeOffer(listing: ListingResponse, amountText: String) {
        val amount = InputValidator.parsePrice(amountText)
        val error = when {
            amount == null -> R.string.error_invalid_offer
            listing.listingType.equals("Sell", ignoreCase = true) && amount <= 0.0 -> R.string.error_offer_zero
            else -> null
        }
        if (error != null || amount == null) {
            _offerState.value = OfferUiState(error = UiText.Res(error ?: R.string.error_invalid_offer))
            return
        }

        _offerState.value = OfferUiState(isSending = true)
        viewModelScope.launch {
            _offerState.value = when (val result = tradeRepository.makeOffer(listing.listingId, amount)) {
                is Resource.Success -> {
                    Log.i(TAG, "Offer ${result.data.offerId} created for listing ${listing.listingId}")
                    OfferUiState(createdOfferId = result.data.offerId)
                }
                is Resource.Error -> OfferUiState(error = UiText.from(result))
                else -> OfferUiState()
            }
        }
    }

    fun onOfferNavigationHandled() {
        _offerState.value = OfferUiState()
    }

    fun toggleSaved(listingId: Int) {
        val wasSaved = _isSaved.value
        _isSaved.value = !wasSaved // optimistic update
        viewModelScope.launch {
            val result = if (wasSaved) watchlistRepository.remove(listingId) else watchlistRepository.save(listingId)
            if (result is Resource.Error) _isSaved.value = wasSaved
        }
    }

    fun delete(listingId: Int) {
        _deleteState.value = Resource.Loading
        viewModelScope.launch { _deleteState.value = listingRepository.deleteListing(listingId) }
    }

    private companion object {
        const val TAG = "ListingDetailVM"
    }
}
