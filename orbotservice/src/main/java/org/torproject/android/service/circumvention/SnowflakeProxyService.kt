package org.torproject.android.service.circumvention

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import org.torproject.android.service.R
import org.torproject.android.service.receivers.PowerConnectionReceiver
import org.torproject.android.service.util.Prefs

class SnowflakeProxyService : Service() {

    private lateinit var snowflakeProxyWrapper: SnowflakeProxyWrapper
    private lateinit var powerConnectionReceiver: PowerConnectionReceiver
    private lateinit var snowflakeNetworkCallbacks: SnowflakeNetworkCallbacks

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
        snowflakeNetworkCallbacks = SnowflakeNetworkCallbacks(this)
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(snowflakeNetworkCallbacks)
        startForeground()
        startSnowflakeProxy("initializing in onCreate()")
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
        Log.d(TAG, "isPowerConnected=$isPowerConnected")
        if (isPowerConnected) snowflakeProxyWrapper.enableProxy()
        else snowflakeProxyWrapper.stopProxy()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(powerConnectionReceiver)
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(snowflakeNetworkCallbacks)
        stopSnowflakeProxy("in onDestroy()")
    }

    fun onNetworkEvent() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager.activeNetwork == null) {
            stopSnowflakeProxy("no active network, stopping")
        } else {
            if (!Prefs.limitSnowflakeProxyingWifi()) {
                if (snowflakeProxyWrapper.isProxyRunning()) return
                // we're online, and we don't care if it's wifi, so start it up
                startSnowflakeProxy("got an active network")
                return
            } else {
                var hasWifi: Boolean
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val capabilities =
                        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                    hasWifi =
                        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
                } else {
                    hasWifi =
                        connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
                }
                if (hasWifi)
                    startSnowflakeProxy("got onto metered network")
                else
                    stopSnowflakeProxy("metered network lost")
            }

        }
    }

    companion object {
        const val TAG = "GoLog"//"SnowflakeProxyService"
        private const val NOTIFICATION_ID = 103
        private const val CHANNEL_ID = "snowflake"
        const val ACTION_STOP_SNOWFLAKE_SERVICE = "ACTION_STOP_SNOWFLAKE_SERVICE"
    }
}