package org.torproject.android.service.circumvention

import IPtProxy.SnowflakeClientConnected
import IPtProxy.SnowflakeProxy
import android.content.Context
import android.os.Handler
import android.widget.Toast
import org.torproject.android.service.OrbotService
import org.torproject.android.service.R
import org.torproject.android.service.util.Prefs
import java.security.SecureRandom

class SnowflakeProxyWrapper(private val context: Context) {
    private var proxy: SnowflakeProxy? = null

    @Synchronized
    fun enableProxy(
        hasWifi: Boolean,
        hasPower: Boolean,
        onProxyConnected: SnowflakeClientConnected?
    ) {
        if (proxy != null) return
        if (Prefs.limitSnowflakeProxyingWifi() && !hasWifi) return
        if (Prefs.limitSnowflakeProxyingCharging() && !hasPower) return
        proxy = SnowflakeProxy()
        val stunServers = OrbotService.getCdnFront("snowflake-stun").split(",".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()
        val stunUrl = stunServers[SecureRandom().nextInt(stunServers.size)]

        proxy = SnowflakeProxy()
        with(proxy!!) {
            brokerUrl = OrbotService.getCdnFront("snowflake-target-direct")
            capacity = 1L
            pollInterval = 120L
            stunServer = stunUrl
            relayUrl = OrbotService.getCdnFront("snowflake-relay-url")
            natProbeUrl = OrbotService.getCdnFront("snowflake-nat-probe")
            clientConnected = onProxyConnected
            start()
        }

        if (Prefs.showSnowflakeProxyMessage()) {
            val message = context.getString(R.string.log_notice_snowflake_proxy_enabled)
            Handler(context.mainLooper)
                .post(Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG)::show)
        }
    }

    @Synchronized
    fun stopProxy() {
        if (proxy == null) return
        proxy!!.stop()
        proxy = null

        if (Prefs.showSnowflakeProxyMessage()) {
            val message = context.getString(R.string.log_notice_snowflake_proxy_disabled)
            Handler(context.mainLooper).post {
                Toast.makeText(
                    context.applicationContext,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
