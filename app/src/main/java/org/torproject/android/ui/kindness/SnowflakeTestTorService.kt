package org.torproject.android.ui.kindness

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
    }
}