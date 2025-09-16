package org.torproject.android.service.circumvention

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object ContentDeliveryNetworkFronts {
    @JvmStatic
    fun localFronts(context: Context): HashMap<String, String> {
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
}
