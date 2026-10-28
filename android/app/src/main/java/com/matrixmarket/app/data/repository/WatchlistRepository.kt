package com.matrixmarket.app.data.repository

import com.matrixmarket.app.data.remote.ApiService
import com.matrixmarket.app.data.remote.dto.ListingResponse
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.apiCall
import com.matrixmarket.app.util.apiCallNoBody

private const val TAG = "WatchlistRepository"

// Saved Listings / Watchlist (User Defined Feature 5) - stored in the API's NoSQL database.
class WatchlistRepository(private val api: ApiService) {

    suspend fun getWatchlist(): Resource<List<ListingResponse>> =
        apiCall(TAG, "Could not load your saved listings.") { api.getWatchlist() }

    suspend fun isSaved(listingId: Int): Boolean {
        val result = apiCall(TAG, "Could not load saved listings.") { api.getWatchlistIds() }
        return result is Resource.Success && result.data.contains(listingId)
    }

    suspend fun save(listingId: Int): Resource<Unit> =
        apiCallNoBody(TAG, "Could not save this listing.") { api.addToWatchlist(listingId) }

    suspend fun remove(listingId: Int): Resource<Unit> =
        apiCallNoBody(TAG, "Could not remove this listing.") { api.removeFromWatchlist(listingId) }
}
