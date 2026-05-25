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

    fun getCountriesForExitNodeUi(): List<String> =
        listOf(
            GERMANY, AUSTRIA, SWEDEN,
            SWITZERLAND, ICELAND, CANADA,
            UNITED_STATES, SPAIN, FRANCE,
            BULGARIA, POLAND, AUSTRALIA,
            BRAZIL, CZECH_REPUBLIC, DENMARK,
            FINLAND, UNITED_KINGDOM, HUNGARY,
            NETHERLANDS, JAPAN, ROMANIA,
            SINGAPORE, SWEDEN
        )

    private const val GERMANY = "DE"
    private const val AUSTRIA = "AT"
    private const val SWEDEN = "SE"
    private const val SWITZERLAND = "CH"
    private const val ICELAND = "IS"
    private const val CANADA = "CA"
    private const val UNITED_STATES = "US"
    private const val SPAIN = "ES"
    private const val FRANCE = "FR"
    private const val BULGARIA = "BG"
    private const val POLAND = "PL"
    private const val AUSTRALIA = "AU"
    private const val BRAZIL = "BR"
    private const val CZECH_REPUBLIC = "CZ"
    private const val DENMARK = "DK"
    private const val FINLAND = "FI"
    private const val UNITED_KINGDOM = "UK"
    private const val HUNGARY = "HU"
    private const val NETHERLANDS = "NL"
    private const val JAPAN = "JP"
    private const val ROMANIA = "RO"
    private const val SINGAPORE = "SG"
    private const val SLOVAKIA = "SK"

}