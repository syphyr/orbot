package org.torproject.android.service.db

import android.provider.BaseColumns

object V3ClientAuthColumns : BaseColumns {
    const val DOMAIN: String = "domain"
    const val HASH: String = "hash"
    const val ENABLED: String = "enabled"
    @JvmField
    val V3_CLIENT_AUTH_PROJECTION: Array<String> = arrayOf(BaseColumns._ID, DOMAIN, HASH, ENABLED)
}
