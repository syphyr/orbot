package org.torproject.android.service.circumvention

import IPtProxy.Controller
import IPtProxy.IPtProxy
import android.content.Context
import android.util.Log
import org.torproject.android.service.OrbotService
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList

object SnowflakeClient {
    @JvmStatic
    fun startWithDomainFronting(iPtProxy: Controller) {
        //this is using the current, default Tor snowflake infrastructure
        val target = OrbotService.getCdnFront("snowflake-target")
        val front = OrbotService.getCdnFront("snowflake-front")
        val stunServer = OrbotService.getCdnFront("snowflake-stun")
        try {
            iPtProxy.snowflakeBrokerUrl = target
            iPtProxy.snowflakeFrontDomains = front
            iPtProxy.snowflakeIceServers = stunServer
            iPtProxy.snowflakeMaxPeers = 1
            iPtProxy.start(IPtProxy.Snowflake, "")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun startWithAmpRendezvous(iPtProxy: Controller) {
        val stunServers = OrbotService.getCdnFront("snowflake-stun")
        val target = OrbotService.getCdnFront("snowflake-target-direct")
        val front = OrbotService.getCdnFront("snowflake-amp-front")
        val ampCache = OrbotService.getCdnFront("snowflake-amp-cache")
        try {
            iPtProxy.snowflakeBrokerUrl = target
            iPtProxy.snowflakeFrontDomains = front
            iPtProxy.snowflakeIceServers = stunServers
            iPtProxy.snowflakeAmpCacheUrl = ampCache
            iPtProxy.snowflakeMaxPeers = 1
            iPtProxy.start(IPtProxy.Snowflake, "")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @Throws(RuntimeException::class)
    @JvmStatic
    fun startWithSqsRendezvous(iPtProxy: Controller) {
        /* TODO
         make sure SQS queue and credentials are up to date in assets/fronts when re-enabling
         this feature. also remove android:visibility="gone" from the SQS container in
         app project's layout/config_connection_bottom_sheet.xml
         */
        throw RuntimeException("Snowflake SQS Not supported right now https://github.com/guardianproject/orbot-android/issues/1320")
        val stunServers = OrbotService.getCdnFront("snowflake-stun")
        val sqsqueue = OrbotService.getCdnFront("snowflake-sqsqueue")
        val sqscreds = OrbotService.getCdnFront("snowflake-sqscreds")
        try {
            iPtProxy.snowflakeIceServers = stunServers
            iPtProxy.snowflakeSqsUrl = sqsqueue
            iPtProxy.snowflakeSqsCreds = sqscreds
            iPtProxy.snowflakeMaxPeers = 1
            iPtProxy.start(IPtProxy.Snowflake, "")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun stop(iPtProxy: Controller) {
        iPtProxy.stop(IPtProxy.Snowflake)
    }

    @JvmStatic
    fun getClientTransportPluginTorrcLine(iPtProxy: Controller) : String{
        return "ClientTransportPlugin snowflake socks5 127.0.0.1:${iPtProxy.port(IPtProxy.Snowflake)}\n"
    }

    @JvmStatic
    fun getLocalBrokers(context: Context) : List<String>{
        val brokers = ArrayList<String>()
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open("snowflake-brokers")))
            reader.forEachLine { brokers.add(it) }
            reader.close()
        } catch (e: IOException) {
            Log.e("SnowflakeClient", "$e")
        }
        return brokers
    }

    @JvmStatic
    fun getLocalBrokersAmp(context: Context) : List<String>{
        val brokers = ArrayList<String>()
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open("snowflake-brokers-amp")))
            reader.forEachLine { brokers.add(it) }
            reader.close()
        } catch (e: IOException) {
            Log.e("SnowflakeClient", "$e")
        }
        return brokers
    }
}
