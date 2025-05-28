/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */ /* See LICENSE for licensing information */
package org.torproject.android.service.util

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

object Utils {

    @JvmStatic
    fun checkPortOrAuto(portString: String) : String {
        if (!portString.equals("auto", ignoreCase = true)) {
            var isPortUsed = true
            var port = portString.toInt()
            while (isPortUsed) {
                isPortUsed = isPortOpen("127.0.0.1", port, 500)
                if (isPortUsed)  //the specified port is not available, so find one instead
                    port++
            }
            return port.toString()
        }
        return portString
    }

    @JvmStatic
    fun isPortOpen(ip: String?, port: Int, timeout: Int): Boolean {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            socket.close()
            return true
        } catch (ex: Exception) {
            return false
        }
    }

    @JvmStatic
    fun readInputStreamAsString(stream: InputStream?): String {
        var line: String?
        val out = StringBuilder()

        try {
            val reader = BufferedReader(InputStreamReader(stream))

            while ((reader.readLine().also { line = it }) != null) {
                out.append(line)
                out.append('\n')
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return out.toString()
    }

    @JvmStatic
    fun convertCountryCodeToFlagEmoji(countryCode: String): String {
        val uppercaseCC = countryCode.uppercase(Locale.getDefault())
        val flagOffset = 0x1F1E6
        val asciiOffset = 0x41
        val firstChar = Character.codePointAt(uppercaseCC, 0) - asciiOffset + flagOffset
        val secondChar = Character.codePointAt(uppercaseCC, 1) - asciiOffset + flagOffset
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
}
