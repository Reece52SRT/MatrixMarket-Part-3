package com.matrixmarket.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.matrixmarket.app.data.remote.dto.ListingResponse

// ---------------------------------------------------------------------------
// Offline mode (RoomDB / SQLite).
//  * CachedListingEntity - copy of the latest listings so the feed works offline.
//  * PendingListingEntity - listings created while offline, waiting to be synced.
// ---------------------------------------------------------------------------

@Entity(tableName = "cached_listings")
data class CachedListingEntity(
    @PrimaryKey val listingId: Int,
    val title: String,
    val description: String,
    val price: Double,
    val listingType: String,
    val isbn: String?,
    val imageUrl: String?,
    val status: String,
    val categoryId: Int,
    val categoryName: String,
    val sellerName: String,
    val sellerId: Int,
    val sellerKarma: Int,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toResponse() = ListingResponse(
        listingId, title, description, price, listingType, isbn, imageUrl, status,
        categoryId, categoryName, sellerName, sellerId, sellerKarma, createdAt
    )

    companion object {
        fun from(r: ListingResponse) = CachedListingEntity(
            listingId = r.listingId,
            title = r.title,
            description = r.description,
            price = r.price,
            listingType = r.listingType,
            isbn = r.isbn,
            imageUrl = r.imageUrl,
            status = r.status,
            categoryId = r.categoryId,
            categoryName = r.categoryName,
            sellerName = r.sellerName,
            sellerId = r.sellerId,
            sellerKarma = r.sellerKarma,
            createdAt = r.createdAt
        )
    }
}

object SyncState {
    const val PENDING = "PENDING"
    const val FAILED = "FAILED" // the API rejected it - shown to the user, not retried forever
}

@Entity(tableName = "pending_listings")
data class PendingListingEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val categoryId: Int,
    val categoryName: String,
    val title: String,
    val description: String,
    val price: Double,
    val listingType: String,
    val isbn: String?,
    val localImagePath: String?,
    val syncState: String = SyncState.PENDING,
    val lastError: String? = null,
    val attempts: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
