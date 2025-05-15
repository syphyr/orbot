package org.torproject.android.service.circumvention

import IPtProxy.Controller
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
            iPtProxy.start(IPtProxy.IPtProxy.Snowflake, "")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun startWithAmpRendezvous(iPtProxy: IPtProxy.Controller) {
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
            iPtProxy.start(IPtProxy.IPtProxy.Snowflake, "")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun stop(iPtProxy: Controller) {
        iPtProxy.stop(IPtProxy.IPtProxy.Snowflake)
    }

    @JvmStatic
    fun getBrokers(context: Context) : List<String>{
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
}
