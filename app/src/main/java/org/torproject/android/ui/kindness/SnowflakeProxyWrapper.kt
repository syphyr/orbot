package org.torproject.android.ui.kindness

import IPtProxy.SnowflakeClientEvents
import IPtProxy.SnowflakeProxy
import android.content.Context
import android.os.Handler
import android.util.Log
import com.netzarchitekten.upnp.UPnP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.torproject.android.R
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.circumvention.BuiltInBridges
import org.torproject.android.util.NetworkUtils
import org.torproject.android.util.Prefs
import org.torproject.android.util.showToast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.SecureRandom
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages configuration, state and static methods of Snowflake Proxy via ipt-proxy
 */
class SnowflakeProxyWrapper(private val service: SnowflakeProxyService) {

    private var proxy: SnowflakeProxy? = null

    private var mappedPorts = mutableListOf<Int>()

    // Bumped under lock by every enableProxy() and stopProxy() call. The slow
    // part of a start runs on IO with no lock held, so by the time it is ready
    // to bring the proxy up, a stop (or a newer start) may have won; comparing
    // generations is how it finds out it must back off instead of leaving a
    // proxy running that nothing owns anymore (#1183).
    private var startGeneration = 0

    @Synchronized
    fun enableProxy() {
        if (proxy != null) return
        val generation = ++startGeneration
        CoroutineScope(Dispatchers.IO).launch {
            val ports = mapPorts()
            val stunServers =
                BuiltInBridges.getInstance(service)?.snowflake?.firstOrNull()?.ice?.split(",".toRegex())
                    ?.dropLastWhile { it.isEmpty() }?.toTypedArray() ?: emptyArray()
            val stunUrl = stunServers[SecureRandom().nextInt(stunServers.size)]

            val built = SnowflakeProxy()
            val fronts = localFronts(service)
            with(built) {
                proxyTypeIdentifier = "orbot-android"
                brokerUrl = fronts["snowflake-target-direct"]
                capacity = 1L
                pollInterval = 120L
                stunServer = stunUrl
                relayUrl = fronts["snowflake-relay-url"]
                natProbeUrl = fronts["snowflake-nat-probe"]
                clientEvents = object : SnowflakeClientEvents {
                    override fun connected() = onConnected()
                    override fun connectionFailed() {}
                    override fun disconnected(country: String?) {}
                    override fun stats(
                        connectionCount: Long,
                        failedConnectionCount: Long,
                        inboundBytes: Long,
                        outboundBytes: Long,
                        inboundUnit: String?,
                        outboundUnit: String?,
                        summaryInterval: Long
                    ) {
                    }

                    override fun natTypeUpdated(natType: String) {
                        Prefs.lastSnowflakeNatType = natType
                    }
                }

                // Setting these to 0 is equivalent to not setting them at all.
                ephemeralMinPort = (ports.firstOrNull() ?: 0).toLong()
                ephemeralMaxPort = (ports.lastOrNull() ?: 0).toLong()
            }

            synchronized(this@SnowflakeProxyWrapper) {
                if (!shouldCommitStart(generation, startGeneration, proxy != null)) {
                    // A stop or a newer start arrived while ports were being
                    // mapped. This start lost; it cleans up its own mappings
                    // and never calls start().
                    closePorts(ports)
                    return@launch
                }
                mappedPorts = ports.toMutableList()
                if (ports.isNotEmpty()) {
                    // Written down before the proxy comes up, so a run that
                    // dies without reaching stopProxy() leaves a record the
                    // next launch can clean up after (#1795).
                    Prefs.snowflakeUpnpPorts = encodePorts(ports)
                }
                proxy = built
                built.start()
                Prefs.snowflakeProxyRunning = true
            }
            service.refreshNotification()
        }
    }

    @Synchronized
    fun stopProxy() {
        startGeneration++
        val p = proxy ?: return

        p.stop()
        proxy = null
        Prefs.snowflakeProxyRunning = false
        releaseMappedPorts()

        // IPtProxy's start() flips its isRunning flag inside a goroutine of
        // its own, so a stop() that outruns that goroutine is a silent no-op
        // and the proxy comes up anyway, unstoppable once this reference is
        // dropped (#1183). Watching it for a moment catches the late arrival
        // and puts it down. Upstream's start() returns right after spawning
        // that goroutine, which is what makes calling it under this lock safe.
        CoroutineScope(Dispatchers.IO).launch {
            repeat(10) {
                delay(200.milliseconds)
                if (p.isRunning) {
                    p.stop()
                    return@launch
                }
            }
        }
    }

    fun isProxyRunning(): Boolean = proxy != null

    // Ports a previous run mapped but never released, most likely because the
    // process died before stopProxy() could run (#1795). The record is cleared
    // synchronously so a start racing this cleanup cannot have its own fresh
    // mappings closed out from under it.
    fun releaseStalePorts() {
        val stale = decodePorts(Prefs.snowflakeUpnpPorts)
        if (stale.isEmpty()) return
        Prefs.snowflakeUpnpPorts = ""
        CoroutineScope(Dispatchers.IO).launch {
            for (port in stale) {
                UPnP.closePortUDP(port)
            }
        }
    }

    internal fun onConnected() {
        Prefs.addSnowflakeServed()
        service.refreshNotification()
        if (!Prefs.showSnowflakeProxyToast()) return
        val message: String = String.format(
            service.getString(R.string.snowflake_proxy_client_connected_msg),
            ONION_EMOJI,
            ONION_EMOJI
        )
        Handler(service.mainLooper).post {
            service.applicationContext.showToast(message)
        }
    }

    private fun mapPorts(): List<Int> {
        if (NetworkUtils.needsAccessLocalNetworkPermission(service) == true) return emptyList()
        val start = Random.nextInt(49152, 65536 - 2)
        val ports = mutableListOf<Int>()
        for (port in (start..start + 2)) {
            if (UPnP.openPortUDP(port, OrbotConstants.TAG)) {
                ports.add(port)
            }
        }

        // Snowflake Proxy needs Capacity * 2 + 1 = 3 consecutive ports mapped for unrestricted mode.
        // If we can't get all of these, remove the ones we have and
        // rather have Snowflake Proxy run in restricted mode.
        if (ports.size < 3) {
            closePorts(ports)
            return emptyList()
        }
        return ports
    }

    private fun closePorts(ports: List<Int>) {
        for (port in ports) {
            UPnP.closePortUDP(port)
        }
    }

    private fun releaseMappedPorts() {
        closePorts(mappedPorts)
        mappedPorts = mutableListOf()
        Prefs.snowflakeUpnpPorts = ""
    }

    @Synchronized
    private fun localFronts(context: Context): HashMap<String, String> {
        val map = HashMap<String, String>()
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open("fronts")))
            reader.forEachLine {
                val kv = it.split(" ")
                map[kv[0]] = kv[1]
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("CDNFronts", "error loading fronts from assets $e")
        }
        return map
    }

    companion object {
        private const val ONION_EMOJI: String = "\uD83E\uDDC5"

        // The one rule that decides whether a start that did its slow work on
        // IO still owns the right to bring the proxy up (#1183).
        fun shouldCommitStart(
            generationAtLaunch: Int, currentGeneration: Int, proxyPresent: Boolean
        ): Boolean = generationAtLaunch == currentGeneration && !proxyPresent

        fun encodePorts(ports: List<Int>): String = ports.joinToString(",")

        fun decodePorts(value: String): List<Int> =
            value.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..65535 }
    }
}
