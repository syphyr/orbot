package org.torproject.android.service.circumvention

import android.util.Log
import org.torproject.android.util.Prefs

/**
 * Utility functions for streamlining the UI around users who explicitly set in Orbot
 * they're country code. Users don't have to set any country and by default non are set.
 * Currently, this is ued for:
 * - Showing/Hiding the option to use DNSTT to connect to Tor
 * - Preventing the user from using Kindness Mode instead of having them take and fail a connectivity test
 */
object CensoredCountries {

    fun isDnsttEnabledForCountry(countryCode: String?): Boolean =
        listOf(
            UNITED_ARAB_EMIRATES,
            AFGHANISTAN,
            BANGLADESH,
            CHINA,
            COLUMBIA,
            INDONESIA,
            IRAN,
            KUWAIT,
            PAKISTAN,
            QATAR,
            RUSSIA,
            SYRIA,
            TURKEY,
            UGANDA,
            UZBEKISTAN
        ).contains(countryCode?.lowercase())


    // Returns true if we prevent the user from even undertaking the kindness mode tor connection
    fun isKindnessModeAvailableForCountry(): Boolean =
        listOf(
            AFGHANISTAN,
            IRAN
        ).contains(Prefs.bridgeCountry?.lowercase())

    private const val UNITED_ARAB_EMIRATES = "ae"
    private const val AFGHANISTAN = "af"
    private const val IRAN = "ir"
    private const val BANGLADESH = "bd"
    private const val CHINA = "cn"
    private const val COLUMBIA = "co"
    private const val INDONESIA = "id"
    private const val KUWAIT = "kw"
    private const val PAKISTAN = "pk"
    private const val QATAR = "qa"
    private const val RUSSIA = "ru"
    private const val SYRIA = "sy"
    private const val TURKEY = "ty"
    private const val UGANDA = "ug"
    private const val UZBEKISTAN = "uz"
}