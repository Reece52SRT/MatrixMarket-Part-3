package com.matrixmarket.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ListingDao {

    @Query("SELECT * FROM cached_listings WHERE status = 'Active' ORDER BY createdAt DESC")
    abstract suspend fun getActive(): List<CachedListingEntity>

    @Query("SELECT * FROM cached_listings WHERE listingId = :id LIMIT 1")
    abstract suspend fun getById(id: Int): CachedListingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(listings: List<CachedListingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(listing: CachedListingEntity)

    @Query("DELETE FROM cached_listings")
    abstract suspend fun clear()

    @Query("DELETE FROM cached_listings WHERE listingId = :id")
    abstract suspend fun delete(id: Int)

    // Replaces the whole cache with the latest unfiltered feed from the API.
    @Transaction
    open suspend fun replaceAll(listings: List<CachedListingEntity>) {
        clear()
        upsertAll(listings)
    }
}

@Dao
interface PendingListingDao {

    @Query("SELECT * FROM pending_listings ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PendingListingEntity>>

    @Query("SELECT * FROM pending_listings WHERE syncState = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingListingEntity>

    @Query("SELECT COUNT(*) FROM pending_listings WHERE syncState = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Insert
    suspend fun insert(listing: PendingListingEntity): Long

    @Query("UPDATE pending_listings SET syncState = :state, lastError = :error, attempts = attempts + 1 WHERE localId = :localId")
    suspend fun markFailed(localId: Long, state: String, error: String?)

    @Query("UPDATE pending_listings SET syncState = 'PENDING', lastError = NULL WHERE localId = :localId")
    suspend fun retry(localId: Long)

    @Query("DELETE FROM pending_listings WHERE localId = :localId")
    suspend fun delete(localId: Long)

    @Query("DELETE FROM pending_listings")
    suspend fun clear()
}
