package com.matrixmarket.app.ui.screens.trademeeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrixmarket.app.R
import com.matrixmarket.app.data.remote.dto.MyTradeMeetingResponse
import com.matrixmarket.app.data.remote.dto.TradeMeetingResponse
import com.matrixmarket.app.data.repository.TradeRepository
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.UiText
import com.matrixmarket.app.util.DateFormats
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

// Result of the seller typing in the buyer's code for one meeting.
data class ConfirmUiState(
    val meetingId: Int? = null,
    val isSending: Boolean = false,
    val error: UiText? = null,
    val successMeetingId: Int? = null
)

// User Defined Feature 2 - schedule a safe campus meeting, and (for the seller)
// confirm the hand-over with the buyer's 6-digit code.
class TradeMeetingViewModel(private val repository: TradeRepository) : ViewModel() {

    private val _myMeetings = MutableStateFlow<Resource<List<MyTradeMeetingResponse>>>(Resource.Loading)
    val myMeetings: StateFlow<Resource<List<MyTradeMeetingResponse>>> = _myMeetings.asStateFlow()

    private val _scheduleState = MutableStateFlow<Resource<TradeMeetingResponse>>(Resource.Idle)
    val scheduleState: StateFlow<Resource<TradeMeetingResponse>> = _scheduleState.asStateFlow()

    private val _scheduleError = MutableStateFlow<UiText?>(null)
    val scheduleError: StateFlow<UiText?> = _scheduleError.asStateFlow()

    private val _confirmState = MutableStateFlow(ConfirmUiState())
    val confirmState: StateFlow<ConfirmUiState> = _confirmState.asStateFlow()

    init {
        // Refresh the list when a push (e.g. "meeting scheduled") arrives.
        viewModelScope.launch { AppContainer.pushReceived.collect { if (_myMeetings.value !is Resource.Loading) loadMyMeetings() } }
    }

    fun loadMyMeetings() {
        if (_myMeetings.value !is Resource.Success) _myMeetings.value = Resource.Loading
        viewModelScope.launch { _myMeetings.value = repository.getMyMeetings() }
    }

    fun scheduleMeeting(offerId: Int, campusSpot: String, dateTime: LocalDateTime, now: LocalDateTime = LocalDateTime.now()) {
        val error = when {
            campusSpot.isBlank() -> R.string.error_choose_spot
            !dateTime.isAfter(now) -> R.string.error_meeting_past
            else -> null
        }
        if (error != null) {
            _scheduleError.value = UiText.Res(error)
            return
        }
        _scheduleError.value = null
        _scheduleState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.scheduleMeeting(offerId, campusSpot, DateFormats.toApi(dateTime))
            _scheduleState.value = result
            if (result is Resource.Error) _scheduleError.value = UiText.from(result)
        }
    }

    fun confirmTrade(meetingId: Int, code: String) {
        if (code.length != 6 || !code.all { it.isDigit() }) {
            _confirmState.value = ConfirmUiState(meetingId = meetingId, error = UiText.Res(R.string.error_code_length))
            return
        }
        _confirmState.value = ConfirmUiState(meetingId = meetingId, isSending = true)
        viewModelScope.launch {
            when (val result = repository.confirmTrade(meetingId, code)) {
                is Resource.Success -> {
                    _confirmState.value = ConfirmUiState(successMeetingId = meetingId)
                    loadMyMeetings()
                }
                is Resource.Error -> _confirmState.value = ConfirmUiState(meetingId = meetingId, error = UiText.from(result))
                else -> _confirmState.value = ConfirmUiState()
            }
        }
    }
}
