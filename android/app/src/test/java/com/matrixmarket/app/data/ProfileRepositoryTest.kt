package com.matrixmarket.app.data

import com.matrixmarket.app.data.local.SessionManager
import com.matrixmarket.app.data.remote.ApiService
import com.matrixmarket.app.data.remote.dto.UpdateSettingsResponse
import com.matrixmarket.app.data.repository.ProfileRepository
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

// Lecturer feedback (Settings): after a successful save the cached session must be
// updated, otherwise reopening Settings shows the old name.
class ProfileRepositoryTest {

    private val api: ApiService = mock()
    private val session: SessionManager = mock()
    private val repository = ProfileRepository(api, session)

    @Test
    fun successfulSaveUpdatesCachedName() = runTest {
        whenever(api.updateSettings(any(), any())).thenReturn(Response.success(UpdateSettingsResponse(7, "New Name", null)))

        val result = repository.updateSettings(7, "New Name", null)

        assertTrue(result is Resource.Success)
        verify(session).updateFullName("New Name")
    }

    @Test
    fun failedSaveLeavesCachedNameAlone() = runTest {
        whenever(api.updateSettings(any(), any())).thenReturn(
            Response.error(400, "{\"message\":\"Name must be 2-80 characters.\"}".toResponseBody("application/json".toMediaType()))
        )

        val result = repository.updateSettings(7, "X", null)

        assertTrue(result is Resource.Error)
        verify(session, never()).updateFullName(any())
    }
}
