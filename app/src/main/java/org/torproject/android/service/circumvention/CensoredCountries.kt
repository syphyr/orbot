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


    private val dnsCountries = listOf(
        "ae", "af", "bd", "cn", "co", "id", "ir", "kw", "pk", "qa", "ru", "sy", "tr", "ug", "uz"
    )

    fun isDnsttEnabledForCountry(countryCode: String?): Boolean =
        dnsCountries.contains(countryCode?.lowercase())


    // Returns true if we prevent the user from activating kindness mode in their country
    // for now, we disable  kindness mode in the same countries that we offer users that we
    // provide an easy way in the UI to connect to tor via DNSTT
    // TODO perhaps add other countries where we know it's not a good idea to run kindness mode
    fun isKindnessModeAvailableForCountry(): Boolean {
        return isDnsttEnabledForCountry(Prefs.bridgeCountry)
    }
}