package org.torproject.android.service.circumvention

import android.net.ConnectivityManager
import android.net.Network

class SnowflakeNetworkCallbacks(private val snowflakeProxyService: SnowflakeProxyService) :
    ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        snowflakeProxyService.onNetworkEvent()
    }

    override fun onLost(network: Network) {
        snowflakeProxyService.onNetworkEvent()
    }
}