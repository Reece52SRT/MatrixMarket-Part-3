package com.matrixmarket.app.data.remote.dto

data class CreateOfferRequest(
    val listingId: Int,
    val offeredPrice: Double
)

data class OfferResponse(
    val offerId: Int,
    val status: String
)

data class ScheduleTradeRequest(
    val offerId: Int,
    val campusSpot: String,
    val meetingTime: String // campus wall-clock time, ISO-8601 e.g. 2026-10-23T14:00:00
)

data class TradeMeetingResponse(
    val meetingId: Int,
    val offerId: Int,
    val campusSpot: String,
    val meetingTime: String,
    val confirmationCode: String, // only filled in for the buyer
    val isConfirmed: Boolean,
    val listingTitle: String
)

data class ConfirmTradeRequest(
    val confirmationCode: String
)

data class ConfirmTradeResponse(
    val isConfirmed: Boolean,
    val message: String?
)

// One row on the Meet tab - works for both sides of the trade.
data class MyTradeMeetingResponse(
    val meetingId: Int,
    val offerId: Int,
    val campusSpot: String,
    val meetingTime: String,
    val confirmationCode: String,
    val isConfirmed: Boolean,
    val canConfirm: Boolean,
    val listingTitle: String,
    val price: Double,
    val role: String, // "Buyer" or "Seller"
    val otherPartyName: String
)
