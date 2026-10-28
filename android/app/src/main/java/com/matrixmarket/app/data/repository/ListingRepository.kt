package com.matrixmarket.app.data.repository

import android.util.Log
import com.matrixmarket.app.data.local.db.CachedListingEntity
import com.matrixmarket.app.data.local.db.ListingDao
import com.matrixmarket.app.data.local.db.PendingListingDao
import com.matrixmarket.app.data.local.db.PendingListingEntity
import com.matrixmarket.app.data.local.db.SyncState
import com.matrixmarket.app.data.remote.ApiService
import com.matrixmarket.app.data.remote.dto.BookInfoResponse
import com.matrixmarket.app.data.remote.dto.CategoryDto
import com.matrixmarket.app.data.remote.dto.CreateListingRequest
import com.matrixmarket.app.data.remote.dto.ListingResponse
import com.matrixmarket.app.network.ConnectivityObserver
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.apiCall
import com.matrixmarket.app.util.apiCallNoBody
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

private const val TAG = "ListingRepository"

// Result of tapping "Publish listing".
sealed class CreateListingResult {
    data class Published(val listing: ListingResponse, val photoUploadFailed: Boolean) : CreateListingResult()
    data object SavedOffline : CreateListingResult()
    data class Failed(val message: String) : CreateListingResult()
}

data class SyncOutcome(val synced: Int, val failed: Int, val needsRetry: Boolean)

// Listings with offline support: network first, Room cache as the fallback,
// and an outbox (pending_listings) for listings created without a connection.
class ListingRepository(
    private val api: ApiService,
    private val listingDao: ListingDao,
    private val pendingDao: PendingListingDao,
    private val connectivity: ConnectivityObserver,
    private val onPendingSaved: () -> Unit
) {
    // The 5 categories the API seeds - used when the app starts with no connection.
    private val defaultCategories = listOf(
        CategoryDto(1, "Textbooks", null),
        CategoryDto(2, "Electronics", null),
        CategoryDto(3, "Residence Essentials", null),
        CategoryDto(4, "Skills & Tutoring", null),
        CategoryDto(5, "Stationery", null)
    )

    @Volatile
    private var cachedCategories: List<CategoryDto>? = null

    val pendingListings: Flow<List<PendingListingEntity>> = pendingDao.observeAll()
    val pendingCount: Flow<Int> = pendingDao.observePendingCount()

    suspend fun getListings(categoryId: Int? = null, search: String? = null): Resource<List<ListingResponse>> {
        val result = apiCall(TAG, "Could not load listings.") { api.getListings(categoryId, search) }
        return when {
            result is Resource.Success -> {
                val entities = result.data.map { CachedListingEntity.from(it) }
                if (categoryId == null && search.isNullOrBlank()) listingDao.replaceAll(entities)
                else listingDao.upsertAll(entities)
                result
            }
            result is Resource.Error && result.isNetworkError -> {
                Log.i(TAG, "Offline - serving listings from the local cache")
                val term = search?.trim()?.lowercase().orEmpty()
                val cached = listingDao.getActive().map { it.toResponse() }.filter { listing ->
                    (categoryId == null || listing.categoryId == categoryId) &&
                        (term.isEmpty() || listing.title.lowercase().contains(term) ||
                            listing.description.lowercase().contains(term))
                }
                Resource.Success(cached, fromCache = true)
            }
            else -> result
        }
    }

    suspend fun getCategories(): List<CategoryDto> {
        cachedCategories?.let { return it }
        val result = apiCall(TAG, "Could not load categories.") { api.getCategories() }
        return if (result is Resource.Success && result.data.isNotEmpty()) {
            cachedCategories = result.data
            result.data
        } else {
            defaultCategories
        }
    }

    suspend fun getListingById(id: Int): Resource<ListingResponse> {
        val result = apiCall(TAG, "Could not load this listing.") { api.getListingById(id) }
        if (result is Resource.Success) {
            listingDao.upsert(CachedListingEntity.from(result.data))
            return result
        }
        if (result is Resource.Error && result.isNetworkError) {
            listingDao.getById(id)?.let { return Resource.Success(it.toResponse(), fromCache = true) }
        }
        return result
    }

    suspend fun lookupBook(isbn: String): Resource<BookInfoResponse> =
        apiCall(TAG, "Could not look up this book.") { api.lookupBook(isbn) }

    suspend fun deleteListing(id: Int): Resource<Unit> {
        val result = apiCallNoBody(TAG, "Could not delete this listing.") { api.deleteListing(id) }
        if (result is Resource.Success) listingDao.delete(id)
        return result
    }

    // Publishes straight away when online; otherwise keeps it in the offline outbox.
    suspend fun createListing(request: CreateListingRequest, categoryName: String, photo: File?): CreateListingResult {
        if (!connectivity.checkNow()) {
            savePending(request, categoryName, photo)
            return CreateListingResult.SavedOffline
        }

        return when (val result = apiCall(TAG, "Could not publish your listing.") { api.createListing(request) }) {
            is Resource.Success -> {
                var listing = result.data
                var photoFailed = false
                if (photo != null) {
                    when (val upload = uploadPhoto(listing.listingId, photo)) {
                        is Resource.Success -> {
                            listing = upload.data
                            photo.delete()
                        }
                        else -> photoFailed = true
                    }
                }
                listingDao.upsert(CachedListingEntity.from(listing))
                CreateListingResult.Published(listing, photoFailed)
            }
            is Resource.Error -> {
                if (result.isNetworkError) {
                    savePending(request, categoryName, photo)
                    CreateListingResult.SavedOffline
                } else {
                    CreateListingResult.Failed(result.message)
                }
            }
            else -> CreateListingResult.Failed("Could not publish your listing.")
        }
    }

    suspend fun uploadPhoto(listingId: Int, photo: File): Resource<ListingResponse> {
        if (!photo.exists()) return Resource.Error("Photo file is missing.")
        val part = MultipartBody.Part.createFormData(
            "file", photo.name, photo.asRequestBody("image/jpeg".toMediaType())
        )
        return apiCall(TAG, "Could not upload the photo.") { api.uploadListingImage(listingId, part) }
    }

    private suspend fun savePending(request: CreateListingRequest, categoryName: String, photo: File?) {
        val id = pendingDao.insert(
            PendingListingEntity(
                categoryId = request.categoryId,
                categoryName = categoryName,
                title = request.title,
                description = request.description,
                price = request.price,
                listingType = request.listingType,
                isbn = request.isbn,
                localImagePath = photo?.absolutePath
            )
        )
        Log.i(TAG, "Saved listing offline (local id $id) - will sync when back online")
        onPendingSaved()
    }

    suspend fun retryPending(localId: Long) {
        pendingDao.retry(localId)
        onPendingSaved()
    }

    suspend fun discardPending(item: PendingListingEntity) {
        item.localImagePath?.let { File(it).delete() }
        pendingDao.delete(item.localId)
    }

    // Called by SyncWorker when the phone is back online.
    suspend fun syncPending(): SyncOutcome {
        var synced = 0
        var failed = 0
        for (item in pendingDao.getPending()) {
            val request = CreateListingRequest(
                categoryId = item.categoryId,
                title = item.title,
                description = item.description,
                price = item.price,
                listingType = item.listingType,
                isbn = item.isbn
            )
            when (val result = apiCall(TAG, "Could not sync listing.") { api.createListing(request) }) {
                is Resource.Success -> {
                    var listing = result.data
                    item.localImagePath?.let { path ->
                        val file = File(path)
                        val upload = uploadPhoto(listing.listingId, file)
                        if (upload is Resource.Success) listing = upload.data
                        file.delete()
                    }
                    listingDao.upsert(CachedListingEntity.from(listing))
                    pendingDao.delete(item.localId)
                    synced++
                    Log.i(TAG, "Synced offline listing ${item.localId} -> ${listing.listingId}")
                }
                is Resource.Error -> {
                    if (result.isNetworkError) return SyncOutcome(synced, failed, needsRetry = true)
                    pendingDao.markFailed(item.localId, SyncState.FAILED, result.message)
                    failed++
                    Log.w(TAG, "Offline listing ${item.localId} rejected: ${result.message}")
                }
                else -> Unit
            }
        }
        return SyncOutcome(synced, failed, needsRetry = false)
    }

    suspend fun clearLocalData() {
        pendingDao.clear()
        listingDao.clear()
    }
}
