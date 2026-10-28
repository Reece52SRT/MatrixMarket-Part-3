package com.matrixmarket.app.data

import com.matrixmarket.app.data.local.db.CachedListingEntity
import com.matrixmarket.app.data.local.db.ListingDao
import com.matrixmarket.app.data.local.db.PendingListingDao
import com.matrixmarket.app.data.remote.ApiService
import com.matrixmarket.app.data.remote.dto.CreateListingRequest
import com.matrixmarket.app.data.repository.CreateListingResult
import com.matrixmarket.app.data.repository.ListingRepository
import com.matrixmarket.app.network.ConnectivityObserver
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

// Offline mode with sync.
class ListingRepositoryOfflineTest {

    private val api: ApiService = mock()
    private val listingDao: ListingDao = mock()
    private val pendingDao: PendingListingDao = mock()
    private val connectivity: ConnectivityObserver = mock()
    private var syncScheduled = 0
    private val repository = ListingRepository(api, listingDao, pendingDao, connectivity) { syncScheduled++ }

    private val request = CreateListingRequest(1, "Calculus textbook", "Barely used", 300.0, "Sell", null)

    @Test
    fun creatingAListingOfflineSavesItLocallyAndSchedulesSync() = runTest {
        whenever(connectivity.checkNow()).thenReturn(false)
        whenever(pendingDao.insert(any())).thenReturn(1L)

        val result = repository.createListing(request, "Textbooks", null)

        assertEquals(CreateListingResult.SavedOffline, result)
        verify(pendingDao).insert(any())
        verify(api, never()).createListing(any())
        assertEquals(1, syncScheduled)
    }

    @Test
    fun connectionDroppingMidRequestAlsoSavesOffline() = runTest {
        whenever(connectivity.checkNow()).thenReturn(true)
        whenever(api.createListing(any())).doSuspendableAnswer { throw IOException("timeout") }
        whenever(pendingDao.insert(any())).thenReturn(1L)

        val result = repository.createListing(request, "Textbooks", null)

        assertEquals(CreateListingResult.SavedOffline, result)
        verify(pendingDao).insert(any())
    }

    @Test
    fun browsingOfflineFallsBackToCachedListings() = runTest {
        whenever(api.getListings(anyOrNull(), anyOrNull(), anyOrNull())).doSuspendableAnswer { throw IOException("offline") }
        whenever(listingDao.getActive()).thenReturn(
            listOf(
                CachedListingEntity(1, "Calculus", "Stewart", 450.0, "Sell", null, null, "Active", 1, "Textbooks", "Thabo", 2, 42, "2026-10-01T10:00:00"),
                CachedListingEntity(2, "Desk lamp", "USB", 0.0, "Lend", null, null, "Active", 3, "Residence Essentials", "Zanele", 4, 7, "2026-10-02T10:00:00")
            )
        )

        val result = repository.getListings(categoryId = 1)

        if (result !is Resource.Success) throw AssertionError("Expected cached listings but got $result")
        assertTrue(result.fromCache)
        assertEquals(listOf(1), result.data.map { it.listingId })
    }
}
