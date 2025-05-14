package org.torproject.android.core.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import org.torproject.android.core.LocaleHelper
import org.torproject.android.service.util.Prefs

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resetSecureFlags()
    }

    // we need this for on the fly locale changes, especially for supported locales
    override fun attachBaseContext(newBase: Context) =
        super.attachBaseContext(LocaleHelper.onAttach(newBase))

    override fun onResume() {
        super.onResume()
        LocaleHelper.onAttach(this)
        resetSecureFlags()
    }

    open fun resetSecureFlags() {
        if (Prefs.isSecureWindow())
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    }
}