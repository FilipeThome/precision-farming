package com.precisionfarming.mobile.data.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Uses the default-network callback and NET_CAPABILITY_VALIDATED (captive portals count as offline). */
class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {
    private val manager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _online = MutableStateFlow(currentlyValidated())
    override val online: StateFlow<Boolean> = _online.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _online.value = manager.getNetworkCapabilities(network)?.validated() ?: false
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _online.value = networkCapabilities.validated()
        }

        override fun onLost(network: Network) {
            _online.value = false
        }

        override fun onUnavailable() {
            _online.value = false
        }
    }

    init {
        runCatching { manager.registerDefaultNetworkCallback(callback) }
    }

    private fun currentlyValidated(): Boolean =
        runCatching { manager.getNetworkCapabilities(manager.activeNetwork)?.validated() ?: false }.getOrDefault(false)

    private fun NetworkCapabilities.validated(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
