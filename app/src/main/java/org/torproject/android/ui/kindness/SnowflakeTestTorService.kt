package org.torproject.android.ui.kindness

import android.app.Activity
import android.content.Intent
import android.util.Log
import org.torproject.jni.TorService

class SnowflakeTestTorService : TorService() {

    override fun onCreate() {
        super.onCreate()
        Log.wtf("bim", "in $TAG onCreate()")
        // TODO maybe do something with an time delay stopping feature
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.wtf("bim", "in $TAG onDestroy()")
    }


    companion object {
        const val TAG = "SnowflakeTestTorService"
        fun launchService(orbotActivity: Activity) {
//            ContextCompat.registerReceiver(orbotActivity, rece, IntentFilter(ACTION_STATUS), RECEIVER_NOT_EXPORTED)

        }

        fun killService(orbotActivity: Activity) {
            Log.wtf("bim", "killing test service")
            orbotActivity.startService(
                Intent(
                    orbotActivity,
                    SnowflakeTestTorService::class.java
                ).apply {
                    action = ACTION_STOP
                })
        }
    }
}