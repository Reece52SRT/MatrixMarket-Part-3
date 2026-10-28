package com.matrixmarket.app.ui

import com.matrixmarket.app.R
import com.matrixmarket.app.data.repository.TradeRepository
import com.matrixmarket.app.ui.components.UiText
import com.matrixmarket.app.ui.screens.trademeeting.TradeMeetingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TradeMeetingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: TradeRepository = mock()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun meetingInThePastIsRejected() = runTest(dispatcher) {
        val vm = TradeMeetingViewModel(repository)
        val now = LocalDateTime.of(2026, 10, 22, 12, 0)

        vm.scheduleMeeting(5, "Library Foyer", now.minusHours(1), now)

        assertEquals(UiText.Res(R.string.error_meeting_past), vm.scheduleError.value)
        verify(repository, never()).scheduleMeeting(any(), any(), any())
    }

    @Test
    fun meetingNeedsASpot() = runTest(dispatcher) {
        val vm = TradeMeetingViewModel(repository)
        val now = LocalDateTime.of(2026, 10, 22, 12, 0)

        vm.scheduleMeeting(5, "", now.plusDays(1), now)

        assertEquals(UiText.Res(R.string.error_choose_spot), vm.scheduleError.value)
    }

    @Test
    fun confirmationCodeMustBeSixDigits() = runTest(dispatcher) {
        val vm = TradeMeetingViewModel(repository)

        vm.confirmTrade(9, "12a4")

        assertEquals(UiText.Res(R.string.error_code_length), vm.confirmState.value.error)
        verify(repository, never()).confirmTrade(any(), any())
    }
}
