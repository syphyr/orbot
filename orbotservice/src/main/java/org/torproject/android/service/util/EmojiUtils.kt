/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */ /* See LICENSE for licensing information */
package org.torproject.android.service.util

import java.util.Locale

object EmojiUtils {

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
