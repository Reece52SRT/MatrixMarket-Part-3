package com.matrixmarket.app

import android.app.Application
import android.util.Log
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.notifications.NotificationHelper
import com.matrixmarket.app.sync.SyncWorker
import kotlinx.coroutines.launch

// Application class: runs once when the app process starts.
class MatrixMarketApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application starting (version ${BuildConfig.VERSION_NAME})")
        AppContainer.init(this)
        NotificationHelper.createChannel(this)

        AppContainer.appScope.launch {
            AppContainer.sessionManager.warmUp()
            if (AppContainer.sessionManager.isLoggedIn()) {
                SyncWorker.enqueue(this@MatrixMarketApp)      // push any offline listings
                AppContainer.pushTokenManager.syncToken()      // keep the FCM token current
            }
        }
    }

    private companion object {
        const val TAG = "MatrixMarketApp"
    }
}
