package com.matrixmarket.app.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.matrixmarket.app.R
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.notifications.NotificationHelper
import java.util.concurrent.TimeUnit

// Offline mode with sync: WorkManager runs this as soon as the phone has a network
// connection again, and uploads every listing that was created while offline.
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AppContainer.init(applicationContext)
        if (!AppContainer.sessionManager.isLoggedIn()) {
            Log.i(TAG, "Not logged in - nothing to sync")
            return Result.success()
        }

        Log.i(TAG, "Sync started (attempt ${runAttemptCount + 1})")
        val outcome = AppContainer.listingRepository.syncPending()
        Log.i(TAG, "Sync finished: synced=${outcome.synced} failed=${outcome.failed} retry=${outcome.needsRetry}")

        if (outcome.synced > 0 && AppContainer.sessionManager.getNotificationsEnabled()) {
            NotificationHelper.show(
                applicationContext,
                applicationContext.getString(R.string.sync_done_title),
                applicationContext.resources.getQuantityString(R.plurals.sync_done_body, outcome.synced, outcome.synced)
            )
        }
        return if (outcome.needsRetry) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "pending-listings-sync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            Log.i(TAG, "Sync scheduled - runs when a connection is available")
        }
    }
}
