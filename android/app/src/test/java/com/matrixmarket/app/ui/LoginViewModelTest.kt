package com.matrixmarket.app.ui

import com.matrixmarket.app.R
import com.matrixmarket.app.data.remote.dto.AuthResponse
import com.matrixmarket.app.data.repository.AuthRepository
import com.matrixmarket.app.ui.components.UiText
import com.matrixmarket.app.ui.screens.login.LoginViewModel
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: AuthRepository = mock()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun invalidEmailIsRejectedWithoutCallingTheApi() = runTest(dispatcher) {
        val vm = LoginViewModel(repository)
        vm.login("not-an-email", "secret1")

        assertEquals(UiText.Res(R.string.error_invalid_email), vm.state.value.error)
        verify(repository, never()).login(any(), any())
    }

    @Test
    fun shortPasswordIsRejectedOnRegister() = runTest(dispatcher) {
        val vm = LoginViewModel(repository)
        vm.register("caleb@test.ac.za", "123", "Caleb Naidoo", null)

        assertEquals(UiText.Res(R.string.error_password_short), vm.state.value.error)
        verify(repository, never()).register(any(), any(), any(), any())
    }

    @Test
    fun successfulLoginMarksStateSuccess() = runTest(dispatcher) {
        whenever(repository.login("caleb@test.ac.za", "secret1"))
            .thenReturn(Resource.Success(AuthResponse("jwt", 1, "Caleb", "caleb@test.ac.za", 0, "Email")))
        val vm = LoginViewModel(repository)

        vm.login("caleb@test.ac.za", "secret1")
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()

        assertTrue(vm.state.value.isSuccess)
    }

    @Test
    fun ssoSendsFirebaseTokenToApi() = runTest(dispatcher) {
        whenever(repository.ssoLogin("firebase-id-token"))
            .thenReturn(Resource.Error("Google sign-in could not be verified. Please try again."))
        val vm = LoginViewModel(repository)

        vm.completeSsoLogin("firebase-id-token")
        advanceUntilIdle()

        verify(repository).ssoLogin("firebase-id-token")
        assertEquals(UiText.Plain("Google sign-in could not be verified. Please try again."), vm.state.value.error)
    }
}
