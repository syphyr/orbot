package org.torproject.android

import androidx.core.net.toUri

object Constants {

    val bridgesUri = "https://bridges.torproject.org/".toUri()

    const val emailRecipient = "bridges@torproject.org"

    const val emailSubjectAndBody = "get transport"

    val telegramBot = "https://t.me/GetBridgesBot".toUri()
}