package org.torproject.android

import java.util.Locale

object Regionalization {
    // converts a code like "ES" into "Spain", "Espagne", etc. based on the users current locale
    @JvmStatic
    fun getLocalizedNameForCountryCode(countryCode: String): String =
        Locale.Builder().setRegion(countryCode).build().displayCountry


    @JvmStatic
    fun getFlagEmojiForCountryCode(countryCode: String): String {
        val uppercaseCC = countryCode.uppercase(Locale.getDefault())
        val flagOffset = 0x1F1E6
        val asciiOffset = 0x41
        val firstChar = Character.codePointAt(uppercaseCC, 0) - asciiOffset + flagOffset
        val secondChar = Character.codePointAt(uppercaseCC, 1) - asciiOffset + flagOffset
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

}