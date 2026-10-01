package org.torproject.android.ui.settings

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import org.torproject.android.util.Settings

class SecureEditTextPreference(context: Context, attrs: AttributeSet?) :
    EditTextPreference(context, attrs) {

    init {
        preferenceDataStore = SecurePreferenceDataStore()
    }
}