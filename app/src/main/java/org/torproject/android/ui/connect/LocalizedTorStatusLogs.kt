package org.torproject.android.ui.connect

import android.widget.TextView
import org.torproject.android.R

object LocalizedTorStatusLogs {
    private val logsWePrint = mapOf(
        "Connected to tor control port" to R.string.status_connected_control_port,
        "(handshake)" to R.string.status_handshake,
        "(handshake_done)" to R.string.status_handshake_done,
        "(circuit_create)" to R.string.status_circuit_create,
        "(done)" to R.string.status_done,
        "(conn_pt)" to R.string.status_conn_pt,
        "(conn_done_pt)" to R.string.status_conn_done_pt,
        "(conn_done)" to R.string.status_conn_done,
        "(onehop_create)" to R.string.status_onehop_create,
        "(enough_dirinfo)" to R.string.status_enough_dirinfo,
        "(requesting_status)" to R.string.status_requesting_status,
        "(loading_status)" to R.string.status_loading_status,
        "(loading_keys)" to R.string.status_loading_status,
        "(requesting_descriptors)" to R.string.status_requesting_descriptors,
        "(loading_descriptors)" to R.string.status_loading_descriptors,
    )

    fun setTextviewOnFormattedLog(logline: String, label: TextView) {
        logsWePrint.keys.forEach { key ->
            if (logline.contains(key)) {
                label.setText(logsWePrint[key]!!)
                return
            }
        }
    }
}