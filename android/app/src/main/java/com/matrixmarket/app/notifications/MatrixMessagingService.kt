package com.matrixmarket.app.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.matrixmarket.app.di.AppContainer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Receives real-time push notifications from Firebase Cloud Messaging.
// The API sends DATA messages, so this runs even when the app is in the background,
// which lets us respect the user's Notifications setting every time.
class MatrixMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.i(TAG, "New FCM token issued")
        AppContainer.init(applicationContext)
        AppContainer.appScope.launch { AppContainer.pushTokenManager.syncToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        AppContainer.init(applicationContext)
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body ?: ""
        Log.i(TAG, "Push received: type=${data["type"]} title=$title")

        // onMessageReceived runs on a background thread, so a short blocking read is fine.
        val enabled = runBlocking { AppContainer.sessionManager.getNotificationsEnabled() }
        if (enabled) NotificationHelper.show(applicationContext, title, body)

        // Tell any open screen (e.g. the bell badge, Meet tab) to refresh.
        AppContainer.notifyPushReceived()
    }

    private companion object {
        const val TAG = "MessagingService"
    }
}
