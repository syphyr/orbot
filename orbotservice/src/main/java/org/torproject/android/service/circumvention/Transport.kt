package org.torproject.android.service.circumvention

import IPtProxy.Controller
import IPtProxy.IPtProxy
import IPtProxy.OnTransportStopped
import android.util.Log
import org.torproject.android.service.util.Bridge
import org.torproject.android.service.util.Prefs
import java.lang.Exception

enum class Transport(val id: String) {

    NONE(Prefs.CONNECTION_PATHWAY_DIRECT),
    MEEK_AZURE(""), // TODO
    OBFS4(Prefs.CONNECTION_PATHWAY_OBFS4),
    SNOWFLAKE(Prefs.CONNECTION_PATHWAY_SNOWFLAKE),
    SNOWFLAKE_AMP(Prefs.CONNECTION_PATHWAY_SNOWFLAKE_AMP),
    SNOWFLAKE_SQS(Prefs.CONNECTION_PATHWAY_SNOWFLAKE_SQS),
    WEBTUNNEL(""), // TODO
    CUSTOM(""); // TODO

    companion object {
        var customBridges = emptyList<String>()

        var stateLocation = ""

        val controller: Controller by lazy {
            Controller(stateLocation, true, false, "INFO", statusCollector)
        }

        private val statusCollector = object : OnTransportStopped {
            override fun stopped(name: String?, exception: Exception?) {
                if (name == null) return

                Log.e(Transport::class.toString(), "$name stopped: ${exception?.localizedMessage}")
            }
        }

        /**
         * Seems more reliable in certain countries than the currently advertised one.
         */
        private val addFronts = listOf("github.githubassets.com")

        private val ampBroker = "https://snowflake-broker.torproject.net/"
        private val ampFronts = listOf("www.google.com")
        private val ampCache = "https://cdn.ampproject.org/"
        private val sqsQueue = "https://sqs.us-east-1.amazonaws.com/893902434899/snowflake-broker"
        private val sqsCreds = "eyJhd3MtYWNjZXNzLWtleS1pZCI6IkFLSUE1QUlGNFdKSlhTN1lIRUczIiwiYXdzLXNlY3JldC1rZXkiOiI3U0RNc0pBNHM1RitXZWJ1L3pMOHZrMFFXV0lsa1c2Y1dOZlVsQ0tRIn0="
    }

    val transportNames: Set<String>
        get() {
            return when (this) {
                NONE -> emptySet()
                MEEK_AZURE -> setOf(IPtProxy.MeekLite)
                OBFS4 -> setOf(IPtProxy.Obfs4)
                WEBTUNNEL -> setOf(IPtProxy.Webtunnel)
                CUSTOM -> customBridges.mapNotNull { Bridge(it).transport }.toSet()
                else -> setOf(IPtProxy.Snowflake)
            }
        }

    val port: Long
        get() {
            val transport = transportNames.firstOrNull() ?: return 0

            return controller.port(transport)
        }

    fun start() {
        when (this) {
            SNOWFLAKE -> {
                val snowflake = BuiltInBridges.getInstance()?.snowflake?.firstOrNull()

                // Seems more reliable in certain countries than the currently advertised one.
                val fronts = addFronts.toMutableSet()
                snowflake?.front?.let { fronts.add(it) }
                snowflake?.fronts?.let { fronts.addAll(it) }

                controller.snowflakeIceServers = snowflake?.ice ?: ""
                controller.snowflakeBrokerUrl = snowflake?.url ?: ""
                controller.snowflakeFrontDomains = fronts.joinToString(",")
                controller.snowflakeAmpCacheUrl = ""
                controller.snowflakeSqsUrl = ""
                controller.snowflakeSqsCreds = ""
            }
            SNOWFLAKE_AMP -> {
                controller.snowflakeIceServers = BuiltInBridges.getInstance()?.snowflake?.firstOrNull()?.ice ?: ""
                controller.snowflakeBrokerUrl = ampBroker
                controller.snowflakeFrontDomains = ampFronts.joinToString(",")
                controller.snowflakeAmpCacheUrl = ampCache
                controller.snowflakeSqsUrl = ""
                controller.snowflakeSqsCreds = ""
            }
            SNOWFLAKE_SQS -> {
                controller.snowflakeIceServers = BuiltInBridges.getInstance()?.snowflake?.firstOrNull()?.ice ?: ""
                controller.snowflakeBrokerUrl = ""
                controller.snowflakeFrontDomains = ""
                controller.snowflakeAmpCacheUrl = ""
                controller.snowflakeSqsUrl = sqsQueue
                controller.snowflakeSqsCreds = sqsCreds
            }
            else -> Unit
        }

        for (transport in transportNames) {
            controller.start(transport, "")
        }
    }

    fun stop() {
        for (transport in transportNames) {
            controller.stop(transport)
        }
    }
}