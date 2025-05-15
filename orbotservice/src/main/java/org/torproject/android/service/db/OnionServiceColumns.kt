package org.torproject.android.service.db

import android.provider.BaseColumns

object OnionServiceColumns : BaseColumns {
    const val NAME: String = "name"
    const val PORT: String = "port"
    const val ONION_PORT: String = "onion_port"
    const val DOMAIN: String = "domain"
    const val ENABLED: String = "enabled"
    const val PATH: String = "filepath"
    @JvmField
    val V3_ONION_SERVICE_PROJECTION: Array<String> =
        arrayOf(BaseColumns._ID, NAME, DOMAIN, PORT, ONION_PORT, ENABLED, PATH)
}
