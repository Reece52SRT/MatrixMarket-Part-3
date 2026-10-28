package com.matrixmarket.app.di

import android.content.Context
import android.util.Log
import com.matrixmarket.app.auth.GoogleAuthManager
import com.matrixmarket.app.data.local.SessionManager
import com.matrixmarket.app.data.local.db.AppDatabase
import com.matrixmarket.app.data.remote.ApiClient
import com.matrixmarket.app.data.remote.ApiService
import com.matrixmarket.app.data.repository.AuthRepository
import com.matrixmarket.app.data.repository.ListingRepository
import com.matrixmarket.app.data.repository.NotificationRepository
import com.matrixmarket.app.data.repository.ProfileRepository
import com.matrixmarket.app.data.repository.TradeRepository
import com.matrixmarket.app.data.repository.WatchlistRepository
import com.matrixmarket.app.network.ConnectivityObserver
import com.matrixmarket.app.notifications.PushTokenManager
import com.matrixmarket.app.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

// A lightweight manual service locator (no DI framework needed for an app this size).
// Initialised once from MatrixMarketApp.onCreate (and defensively from the background
// services, which can start before any screen does).
object AppContainer {
    private const val TAG = "AppContainer"

    @Volatile
    private var initialized = false

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var appContext: Context
        private set
    lateinit var sessionManager: SessionManager
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var connectivity: ConnectivityObserver
        private set
    lateinit var api: ApiService
        private set

    // Emits when the API rejects our token, so the UI can return to the login screen.
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    // Emits when a push notification arrives, so open screens can refresh.
    private val _pushReceived = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pushReceived: SharedFlow<Unit> = _pushReceived.asSharedFlow()

    fun notifyPushReceived() {
        _pushReceived.tryEmit(Unit)
    }

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        sessionManager = SessionManager(appContext)
        database = AppDatabase.build(appContext)
        connectivity = ConnectivityObserver(appContext)
        api = ApiClient.create(sessionManager) {
            appScope.launch {
                sessionManager.logout()
                _sessionExpired.tryEmit(Unit)
            }
        }
        initialized = true
        Log.i(TAG, "Dependencies initialised")
    }

    val pushTokenManager: PushTokenManager by lazy { PushTokenManager(api, sessionManager) }

    val googleAuthManager: GoogleAuthManager by lazy { GoogleAuthManager(appContext) }

    val listingRepository: ListingRepository by lazy {
        ListingRepository(
            api = api,
            listingDao = database.listingDao(),
            pendingDao = database.pendingListingDao(),
            connectivity = connectivity,
            onPendingSaved = { SyncWorker.enqueue(appContext) }
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            api = api,
            sessionManager = sessionManager,
            onLoggedIn = { appScope.launch { pushTokenManager.syncToken() } },
            onLoggingOut = {
                pushTokenManager.unregister()
                googleAuthManager.signOut()
                listingRepository.clearLocalData()
            }
        )
    }

    val tradeRepository: TradeRepository by lazy { TradeRepository(api) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(api, sessionManager) }
    val notificationRepository: NotificationRepository by lazy { NotificationRepository(api) }
    val watchlistRepository: WatchlistRepository by lazy { WatchlistRepository(api) }
}
