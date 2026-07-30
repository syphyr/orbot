
package org.torproject.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.VpnService;
import android.os.Build;
import android.util.Log;

import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.util.Prefs;

import org.torproject.android.settings.Languages;
import org.torproject.android.settings.LocaleHelper;

import java.util.Locale;

public class OrbotApp extends Application implements OrbotConstants
{

    private Locale locale;

    @Override
    public void onCreate() {
        super.onCreate();

        Languages.setup(OrbotMainActivity.class, R.string.menu_settings);
        if (!Prefs.getDefaultLocale().equals(Locale.getDefault().getLanguage()))
	    Languages.setLanguage(this, Prefs.getDefaultLocale(), true);

    }

    @Override
    protected void attachBaseContext(Context base) {
        Prefs.setContext(base);
        super.attachBaseContext(LocaleHelper.onAttach(base, Prefs.getDefaultLocale()));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged " + newConfig.locale.getLanguage());
        if (!Prefs.getDefaultLocale().equals(Locale.getDefault().getLanguage()))
            Languages.setLanguage(this, Prefs.getDefaultLocale(), true);
    }
	
    public static Languages getLanguages(Activity activity) {
        return Languages.get(activity);
    }
}
