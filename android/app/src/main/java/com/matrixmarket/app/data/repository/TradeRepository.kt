package com.matrixmarket.app.data.repository

import com.matrixmarket.app.data.remote.ApiService
import com.matrixmarket.app.data.remote.dto.ConfirmTradeRequest
import com.matrixmarket.app.data.remote.dto.CreateOfferRequest
import com.matrixmarket.app.data.remote.dto.MyTradeMeetingResponse
import com.matrixmarket.app.data.remote.dto.OfferResponse
import com.matrixmarket.app.data.remote.dto.ScheduleTradeRequest
import com.matrixmarket.app.data.remote.dto.TradeMeetingResponse
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.apiCall

private const val TAG = "TradeRepository"

// User Defined Feature 2 (Trade Meeting Scheduler): offer -> schedule a safe campus
// meeting -> seller confirms the hand-over with the buyer's 6-digit code.
class TradeRepository(private val api: ApiService) {

    suspend fun makeOffer(listingId: Int, offeredPrice: Double): Resource<OfferResponse> =
        apiCall(TAG, "Could not send your offer.") { api.createOffer(CreateOfferRequest(listingId, offeredPrice)) }

    suspend fun getMyMeetings(): Resource<List<MyTradeMeetingResponse>> =
        apiCall(TAG, "Could not load your trade meetings.") { api.getMyTradeMeetings() }

    suspend fun scheduleMeeting(offerId: Int, campusSpot: String, meetingTimeIso: String): Resource<TradeMeetingResponse> =
        apiCall(TAG, "Could not schedule the meeting.") {
            api.scheduleTrade(ScheduleTradeRequest(offerId, campusSpot, meetingTimeIso))
        }

    suspend fun confirmTrade(meetingId: Int, code: String): Resource<String> =
        when (val result = apiCall(TAG, "Could not confirm the trade.") {
            api.confirmTrade(meetingId, ConfirmTradeRequest(code))
        }) {
            is Resource.Success -> Resource.Success(result.data.message ?: "Trade confirmed!")
            is Resource.Error -> result
            else -> Resource.Error("Could not confirm the trade.")
        }
}
