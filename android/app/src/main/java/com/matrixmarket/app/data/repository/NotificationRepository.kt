package com.matrixmarket.app.data.repository

import com.matrixmarket.app.data.remote.ApiService
import com.matrixmarket.app.data.remote.dto.NotificationResponse
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.apiCall
import com.matrixmarket.app.util.apiCallNoBody

private const val TAG = "NotificationRepository"

// The in-app notification inbox (stored in the API's NoSQL database).
class NotificationRepository(private val api: ApiService) {

    suspend fun getNotifications(): Resource<List<NotificationResponse>> =
        apiCall(TAG, "Could not load notifications.") { api.getNotifications() }

    suspend fun getUnreadCount(): Int {
        val result = apiCall(TAG, "Could not load unread count.") { api.getUnreadCount() }
        return if (result is Resource.Success) result.data.unread else 0
    }

    suspend fun markRead(id: String): Resource<Unit> =
        apiCallNoBody(TAG, "Could not update notification.") { api.markNotificationRead(id) }

    suspend fun markAllRead(): Resource<Unit> =
        apiCallNoBody(TAG, "Could not update notifications.") { api.markAllNotificationsRead() }
}
