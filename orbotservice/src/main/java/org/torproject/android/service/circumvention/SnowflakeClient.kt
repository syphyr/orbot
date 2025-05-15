package org.torproject.android.service.circumvention

import IPtProxy.Controller
import org.torproject.android.service.OrbotService

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
}
