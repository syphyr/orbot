package org.torproject.android.service.circumvention

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.torproject.android.service.R
import org.torproject.android.service.receivers.PowerConnectionReceiver
import org.torproject.android.service.util.Prefs

class SnowflakeProxyService : Service() {

    private lateinit var snowflakeProxyWrapper: SnowflakeProxyWrapper
    private lateinit var powerConnectionReceiver: PowerConnectionReceiver

    private lateinit var networkCallbacks: ConnectivityManager.NetworkCallback

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind: $intent")
        return null
    }

    override fun onCreate() {
        super.onCreate()
        snowflakeProxyWrapper = SnowflakeProxyWrapper(this)
        powerConnectionReceiver = PowerConnectionReceiver(this)
        val powerReceiverFilters = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        powerReceiverFilters.addAction(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(powerConnectionReceiver, powerReceiverFilters)
        initNetworkCallbacks()
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SNOWFLAKE_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_STICKY
    }

    private fun startForeground() {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else ""

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_tor)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle("Snowflake Proxy Service")
            .setSubText("Test...")
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initNetworkCallbacks() {
        val connectivityManager =
            getSystemService(ConnectivityManager::class.java) as ConnectivityManager

        networkCallbacks = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                stopSnowflakeProxy("lost network (limit wifi=${Prefs.limitSnowflakeProxyingWifi()}")
            }

            override fun onAvailable(network: Network) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val hasWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                if (Prefs.limitSnowflakeProxyingWifi() && !hasWifi) {
                    stopSnowflakeProxy("required wifi condition not met")
                } else {
                    startSnowflakeProxy("got network (wifi=${hasWifi}, limit wifi=${Prefs.limitSnowflakeProxyingWifi()}")
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallbacks)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Snowflake Service",
            NotificationManager.IMPORTANCE_HIGH
        )
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return CHANNEL_ID
    }


    private fun startSnowflakeProxy(logReason: String? = null) {
        Log.d(TAG, "Starting snowflake proxy - $logReason")
        snowflakeProxyWrapper.enableProxy(hasWifi = true)
    }

    private fun stopSnowflakeProxy(logMessage: String? = null) {
        Log.d(TAG, "Stopping snowflake proxy - reason: $logMessage")
        snowflakeProxyWrapper.stopProxy()
    }

    fun powerConnectedCallback(isPowerConnected: Boolean) {
        if (!Prefs.limitSnowflakeProxyingCharging()) return
        if (isPowerConnected) startSnowflakeProxy("power connected")
        else stopSnowflakeProxy("power disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(powerConnectionReceiver)
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkCallbacks)
        stopSnowflakeProxy("in onDestroy()")
    }

    companion object {
        const val TAG = "GoLog"//"SnowflakeProxyService"
        private const val NOTIFICATION_ID = 103
        private const val CHANNEL_ID = "snowflake"
        private const val ACTION_STOP_SNOWFLAKE_SERVICE = "ACTION_STOP_SNOWFLAKE_SERVICE"

        private fun getIntent(context: Context) = Intent(context, SnowflakeProxyService::class.java)

        // start this service, but not necessarily snowflake proxy from the app UI
        fun startSnowflakeProxyForegroundService(context: Context) {
            ContextCompat.startForegroundService(context, getIntent(context))
        }

        // stop this service, and snowflake proxy if its running, from the app UI

        fun stopSnowflakeProxyForegroundService(context: Context) {
            ContextCompat.startForegroundService(
                context,
                getIntent(context).setAction(ACTION_STOP_SNOWFLAKE_SERVICE)
            )
        }


    }
}