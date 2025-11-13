/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */ /* See LICENSE for licensing information */
package org.torproject.android.ui.more

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.navigation.fragment.findNavController
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import org.torproject.android.OrbotApp
import org.torproject.android.R
import org.torproject.android.localization.Languages
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.util.Prefs
import org.torproject.android.service.util.sendIntentToService
import org.torproject.android.ui.core.BaseActivity

class SettingsPreferenceFragment : PreferenceFragmentCompat() {
    private var prefLocale: ListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        isSubscreen = rootKey != null
        initPrefs()
    }

    private var title: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = view.findViewById(R.id.title)
    }

    private fun initPrefs() {
        setNoPersonalizedLearningOnEditTextPreferences()

        prefLocale = findPreference("pref_default_locale")
        val languages = Languages[requireActivity()]
        prefLocale?.entries = languages?.allNames
        prefLocale?.entryValues = languages?.supportedLocales
        prefLocale?.value = Prefs.defaultLocale
        prefLocale?.onPreferenceChangeListener =
            OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                val language = newValue as String?
                Prefs.defaultLocale = newValue ?: ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val newLocale = LocaleListCompat.forLanguageTags(language)
                    AppCompatDelegate.setApplicationLocales(newLocale)
                    title?.text = requireContext().getString(R.string.menu_settings)
                } else {
                    requireActivity().sendIntentToService(OrbotConstants.ACTION_LOCAL_LOCALE_SET)
                    (requireActivity().application as OrbotApp).setLocale()
                    requireActivity().finish()
                }
                false
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // if defined in XML, disable the persistent notification preference on Oreo+
            findPreference<Preference>("pref_persistent_notifications")?.let {
                it.parent?.removePreference(it)
            }
        }

        val prefFlagSecure = findPreference<CheckBoxPreference>("pref_flag_secure")
        prefFlagSecure?.onPreferenceChangeListener =
            OnPreferenceChangeListener { _: Preference?, newValue: Any? ->

                Prefs.isSecureWindow = newValue as Boolean
                (activity as BaseActivity).resetSecureFlags()

                true
            }

        val prefCamoDialog = findPreference<Preference>("pref_key_camo_dialog")
        prefCamoDialog?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            onBackPressedCallback.isEnabled = false
            findNavController().navigate(R.id.open_camo)
            true
        }

        val prefOrbotAuthentication = findPreference<CheckBoxPreference>("pref_require_password")
        val prefPasswordNoBiometrics = findPreference<CheckBoxPreference>("pref_auth_no_biometrics")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            prefPasswordNoBiometrics?.isVisible = false
        } else {
            prefPasswordNoBiometrics?.isEnabled = prefOrbotAuthentication?.isChecked == true
            prefOrbotAuthentication?.onPreferenceChangeListener =
                OnPreferenceChangeListener { preference, newValue ->
                    prefPasswordNoBiometrics?.isEnabled = newValue as Boolean
                    true
                }
        }
    }

    private var isSubscreen = false

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        setPreferencesFromResource(R.xml.preferences, preferenceScreen.key)
        initPrefs()
        isSubscreen = true
        title?.text = preferenceScreen.title
    }

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSubscreen) {
                    title?.text = requireContext().getString(R.string.menu_settings)
                    setPreferencesFromResource(R.xml.preferences, null)
                    isSubscreen = false
                } else {
                    remove()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    override fun onResume() {
        super.onResume()
        if (!onBackPressedCallback.isEnabled)
            onBackPressedCallback.isEnabled = true
    }

    private fun setNoPersonalizedLearningOnEditTextPreferences() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return
        val preferenceScreen = preferenceScreen
        val categoryCount = preferenceScreen.preferenceCount
        for (i in 0 until categoryCount) {
            var p = preferenceScreen.getPreference(i)
            if (p is PreferenceCategory) {
                val pc = p
                val preferenceCount = pc.preferenceCount
                for (j in 0 until preferenceCount) {
                    p = pc.getPreference(j)
                    if (p is EditTextPreference) {
                        p.setOnBindEditTextListener {
                            it.imeOptions =
                                it.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                        }
                    }
                }
            }
        }
    }
}
