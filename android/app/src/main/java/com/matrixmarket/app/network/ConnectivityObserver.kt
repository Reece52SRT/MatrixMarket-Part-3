package com.matrixmarket.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Watches the phone's connection so the app can show an "offline" banner and decide
// whether to save new listings locally (offline mode) instead of sending them.
class ConnectivityObserver(context: Context) {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val _isOnline = MutableStateFlow(checkNow())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        try {
            connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = update(true)
                override fun onLost(network: Network) = update(checkNow())
                override fun onUnavailable() = update(false)
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                    update(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            })
        } catch (e: Exception) {
            Log.e(TAG, "Could not register network callback", e)
        }
    }

    private fun update(online: Boolean) {
        if (_isOnline.value != online) Log.i(TAG, if (online) "Back online" else "Went offline")
        _isOnline.value = online
    }

    fun checkNow(): Boolean {
        val cm = connectivityManager ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object {
        const val TAG = "Connectivity"
    }
}
