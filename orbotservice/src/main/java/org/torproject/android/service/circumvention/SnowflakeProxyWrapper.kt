package org.torproject.android.service.circumvention

import IPtProxy.SnowflakeClientConnected
import IPtProxy.SnowflakeProxy
import android.content.Context
import android.os.Handler
import com.netzarchitekten.upnp.UPnP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.OrbotConstants.ONION_EMOJI
import org.torproject.android.service.OrbotService
import org.torproject.android.service.R
import org.torproject.android.service.util.Prefs
import org.torproject.android.service.util.showToast
import java.security.SecureRandom
import kotlin.random.Random

class SnowflakeProxyWrapper(private val context: Context) {

    private var proxy: SnowflakeProxy? = null

    private var mappedPorts = mutableListOf<Int>()

    @Synchronized
    fun enableProxy(
        hasWifi: Boolean = true,
        hasPower: Boolean = true
    ) {
        if (proxy != null) return
        if (Prefs.limitSnowflakeProxyingWifi() && !hasWifi) return

        CoroutineScope(Dispatchers.IO).launch {
            val start = Random.nextInt(49152, 65536 - 2)

            for (port in (start..start + 2)) {
                if (UPnP.openPortUDP(port, OrbotConstants.TAG)) {
                    mappedPorts.add(port)
                }
            }

            // Snowflake Proxy needs Capacity * 2 + 1 = 3 consecutive ports mapped for unrestricted mode.
            // If we can't get all of these, remove the ones we have and
            // rather have Snowflake Proxy run in restricted mode.
            if (mappedPorts.size < 3) {
                releaseMappedPorts()
            }

            val stunServers = BuiltInBridges.getInstance(context)?.snowflake?.firstOrNull()?.ice
                ?.split(",".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray() ?: emptyArray()
            val stunUrl = stunServers[SecureRandom().nextInt(stunServers.size)]

            proxy = SnowflakeProxy()
            with(proxy!!) {
                brokerUrl = OrbotService.getCdnFront("snowflake-target-direct")
                capacity = 1L
                pollInterval = 120L
                stunServer = stunUrl
                relayUrl = OrbotService.getCdnFront("snowflake-relay-url")
                natProbeUrl = OrbotService.getCdnFront("snowflake-nat-probe")
                clientConnected = SnowflakeClientConnected { onConnected() }

                // Setting these to 0 is equivalent to not setting them at all.
                ephemeralMinPort = (mappedPorts.firstOrNull() ?: 0).toLong()
                ephemeralMaxPort = (mappedPorts.lastOrNull() ?: 0).toLong()

                start()
            }

            if (Prefs.showSnowflakeProxyMessage()) {
                val message = context.getString(R.string.log_notice_snowflake_proxy_enabled)

                withContext(Dispatchers.Main) {
                    context.applicationContext.showToast(message)
                }
            }
        }
    }

    @Synchronized
    fun stopProxy() {
        if (proxy == null) return
        proxy?.stop()
        proxy = null

        releaseMappedPorts()

        if (Prefs.showSnowflakeProxyMessage()) {
            val message = context.getString(R.string.log_notice_snowflake_proxy_disabled)
            Handler(context.mainLooper).post {
                context.applicationContext.showToast(message)
            }
        }
    }

    private fun onConnected() {
        Prefs.addSnowflakeServed()
        if (!Prefs.showSnowflakeProxyMessage()) return
        val message: String = String.format(
            context.getString(R.string.snowflake_proxy_client_connected_msg),
            ONION_EMOJI,
            ONION_EMOJI
        )
        Handler(context.mainLooper).post {
            context.applicationContext.showToast(message)
        }
    }

    private fun releaseMappedPorts() {
        for (port in mappedPorts) {
            UPnP.closePortUDP(port)
        }

        mappedPorts = mutableListOf()
    }
}
