package org.torproject.android.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket

object NetworkUtils {
    fun isNetworkAvailable(context: Context, allowOtherVpnApps: Boolean = false): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        if (allowOtherVpnApps && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    /** Used for kindness mode connection test, returns true *if and only if* Orbot is the registered
     * VPN app. We can't use Prefs.useVpn() since this only tells us if Orbot is the registered VPN
     * app when Tor is on. When it's off, we don't know which, if any VPN, is configured.
     *
     * - first cheaply check Prefs.useVpn(), this is true when orbot is running
     * - if not, check to see if the system sees a VPN connection, return false if not
     * - ensure for certain that Orbot isn't the registered VPN, this can be done by seeing if the
     *      system gives Orbot an Intent to register to be the active VPN app. If it's non-null, we
     *      know for certain we have a non-Orbot VPN config on the system
     */
    fun isNonOrbotVpnActive(context: Context): Boolean {
        if (Prefs.useVpn()) {
            Log.wtf("bim", "orbot vpn is running, so this test is done")
            return false
        }

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        val deviceUsingVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        Log.wtf("bim", "VPN? $deviceUsingVpn")

        // we either don't have a VPN app running, if it is, check for certain it's not Orbot
        if (!deviceUsingVpn) return false
        val isOrbotRegisteredAsVpn = VpnService.prepare(context) != null
        Log.wtf("bim", "isOrbotRegisteredAsVpn: $isOrbotRegisteredAsVpn")
        return isOrbotRegisteredAsVpn
    }

    fun checkPortOrAuto(portString: String): String {
        if (!portString.equals("auto", ignoreCase = true)) {
            var isPortUsed = true
            var port = portString.toInt()
            while (isPortUsed) {
                isPortUsed = isPortOpen("127.0.0.1", port, 500)
                if (isPortUsed)  //the specified port is not available, so find one instead
                    port++
            }
            return port.toString()
        }
        return portString
    }

    fun isPortOpen(ip: String?, port: Int, timeout: Int): Boolean {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            socket.close()
            return true
        } catch (_: Exception) {
            return false
        }
    }
}