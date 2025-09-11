package org.torproject.android.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.torproject.android.service.circumvention.SnowflakeProxyService

class PowerConnectionReceiver(private val snowflakeProxyService: SnowflakeProxyService) :
    BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        snowflakeProxyService.powerConnectedCallback(intent.action == Intent.ACTION_POWER_CONNECTED)
    }
}