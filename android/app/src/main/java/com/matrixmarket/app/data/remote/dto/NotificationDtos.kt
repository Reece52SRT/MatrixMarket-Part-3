package com.matrixmarket.app.data.remote.dto

data class NotificationResponse(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val listingId: Int?,
    val meetingId: Int?,
    val isRead: Boolean,
    val createdAt: String
)

data class UnreadCountResponse(
    val unread: Int
)
