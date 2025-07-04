package org.torproject.android.service.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import IPtProxy.Controller;

public class Prefs {

    private final static String PREF_BRIDGES_LIST = "pref_bridges_list";
    private final static String PREF_DEFAULT_LOCALE = "pref_default_locale";
    private final static String PREF_DETECT_ROOT = "pref_detect_root";
    private final static String PREF_ENABLE_LOGGING = "pref_enable_logging";
    private final static String PREF_ENABLE_ROTATION = "pref_enable_rotation";
    private final static String PREF_START_ON_BOOT = "pref_start_boot";
    private final static String PREF_ALLOW_BACKGROUND_STARTS = "pref_allow_background_starts";
    private final static String PREF_OPEN_PROXY_ON_ALL_INTERFACES = "pref_open_proxy_on_all_interfaces";
    private final static String PREF_USE_VPN = "pref_vpn";
    private final static String PREF_EXIT_NODES = "pref_exit_nodes";
    private final static String PREF_BE_A_SNOWFLAKE = "pref_be_a_snowflake";
    private final static String PREF_SHOW_SNOWFLAKE_MSG = "pref_show_snowflake_proxy_msg";
    private final static String PREF_BE_A_SNOWFLAKE_LIMIT_WIFI = "pref_be_a_snowflake_limit_wifi";
    private final static String PREF_BE_A_SNOWFLAKE_LIMIT_CHARGING = "pref_be_a_snowflake_limit_charing";

    private final static String PREF_SMART_TRY_SNOWFLAKE = "pref_smart_try_snowflake";
    private final static String PREF_SMART_TRY_OBFS4 = "pref_smart_try_obfs";
    private static final String PREF_POWER_USER_MODE = "pref_power_user";


    private final static String PREF_HOST_ONION_SERVICES = "pref_host_onionservices";

    private final static String PREF_SNOWFLAKES_SERVED_COUNT = "pref_snowflakes_served";
    private final static String PREF_SNOWFLAKES_SERVED_COUNT_WEEKLY = "pref_snowflakes_served_weekly";

    private static final String PREF_CURRENT_VERSION = "pref_current_version";

    private static final String PREF_CAMO_APP_PACKAGE = "pref_key_camo_app";
    private static final String PREF_CAMO_APP_DISPLAY_NAME = "pref_key_camo_app_display_name";
    private static final String PREF_REQUIRE_PASSWORD = "pref_require_password";
    private static final String PREF_DISALLOW_BIOMETRIC_AUTH = "pref_auth_no_biometrics";

    private static final String PREF_CONNECTION_PATHWAY = "pref_connection_pathway";
    public static final String CONNECTION_PATHWAY_SMART = "smart";
    /**
     * Represents a direct connection to tor with no bridges
     */
    public static final String CONNECTION_PATHWAY_DIRECT = "direct";
    /**
     * Tor connection with snowflake, settable from ConfigConnectionBottomSheet
     */
    public static final String CONNECTION_PATHWAY_SNOWFLAKE = "snowflake";
    /**
     * Tor connection with snowflake using AMP brokers,
     * settable from ConfigConnectionBottomSheet
     */
    public static final String CONNECTION_PATHWAY_SNOWFLAKE_AMP = "snowflake_amp";
    /**
     * Use AMP brokers and start snowflake with SQS Rendezvous. Currently no way to
     * set SQS setting in app, if you force it, a runtime exception is thrown in
     * {@link org.torproject.android.service.circumvention.SnowflakeClient#startWithSqsRendezvous(Controller)}
     */
    public static final String CONNECTION_PATHWAY_SNOWFLAKE_SQS = "snowflake_sqs";
    /**
     * Start lyrebird with meek_lite bridges stored in @{link {@link #getBridgesList()}}
     * This can be set in manually via the CustomBridgeBottomSheet.
     */
    public static final String CONNECTION_PATHWAY_MEEK = "meek";
    /**
     * Start lyrebird with obfs4 bridges stored in @{link {@link #getBridgesList()}}
     * This can be set in manually via the CustomBridgeBottomSheet. This is also currently
     * set when the user "Gets a bridge from tor" successfully in ConfigConnectionBottomSheet.
     */
    public static final String CONNECTION_PATHWAY_OBFS4 = "custom";

    private static final List<String> SUPPORTED_PATHWAYS = Arrays.asList(new String[]{
            CONNECTION_PATHWAY_SMART,
            CONNECTION_PATHWAY_DIRECT,
            CONNECTION_PATHWAY_OBFS4,
            CONNECTION_PATHWAY_SNOWFLAKE,
            CONNECTION_PATHWAY_SNOWFLAKE_AMP,
            CONNECTION_PATHWAY_SNOWFLAKE_SQS,
            CONNECTION_PATHWAY_MEEK
    });

    public static final String PREF_SECURE_WINDOW_FLAG = "pref_flag_secure";

    private static SharedPreferences prefs;

    public static int getCurrentVersionForUpdate() {
        return prefs.getInt(PREF_CURRENT_VERSION, 0);
    }

    public static void setCurrentVersionForUpdate(int version) {
        putInt(PREF_CURRENT_VERSION, version);
    }

    private static final String PREF_REINSTALL_GEOIP = "pref_geoip";

    public static boolean isGeoIpReinstallNeeded() {
        return prefs.getBoolean(PREF_REINSTALL_GEOIP, true);
    }

    public static void setIsGeoIpReinstallNeeded(boolean reinstallNeeded) {
        putBoolean(PREF_REINSTALL_GEOIP, reinstallNeeded);
    }

    public static void setContext(Context context) {
        if (prefs == null) {
            prefs = getSharedPrefs(context);
        }

    }

    public static void initWeeklyWorker() {
        PeriodicWorkRequest.Builder myWorkBuilder =
                new PeriodicWorkRequest.Builder(PrefsWeeklyWorker.class, 7, TimeUnit.DAYS);

        PeriodicWorkRequest myWork = myWorkBuilder.build();
        WorkManager.getInstance()
                .enqueueUniquePeriodicWork("prefsWeeklyWorker", ExistingPeriodicWorkPolicy.KEEP, myWork);
    }

    private static void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private static void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    private static void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public static boolean hostOnionServicesEnabled() {
        return prefs.getBoolean(PREF_HOST_ONION_SERVICES, true);
    }

    public static void putHostOnionServicesEnabled(boolean value) {
        putBoolean(PREF_HOST_ONION_SERVICES, value);
    }

    public static String getBridgesList() {
        // historically we did a check for Farsi here, and returned a different value
        // perhaps include hardcoded obfs4 defaults here for cold-start use case
        return prefs.getString(PREF_BRIDGES_LIST, "");
    }

    public static void setBridgesList(String value) {
        putString(PREF_BRIDGES_LIST, value);
    }

    public static String getDefaultLocale() {
        return prefs.getString(PREF_DEFAULT_LOCALE, Locale.getDefault().getLanguage());
    }

    public static void setDefaultLocale(String value) {
        putString(PREF_DEFAULT_LOCALE, value);
    }

    public static boolean detectRoot() {
        return prefs.getBoolean(PREF_DETECT_ROOT, false);
    }

    public static boolean beSnowflakeProxy() {
        return prefs.getBoolean(PREF_BE_A_SNOWFLAKE, false);
    }

    public static boolean showSnowflakeProxyMessage() {
        return prefs.getBoolean(PREF_SHOW_SNOWFLAKE_MSG, false);
    }

    public static void setBeSnowflakeProxy(boolean beSnowflakeProxy) {
        putBoolean(PREF_BE_A_SNOWFLAKE, beSnowflakeProxy);
    }

    public static void setBeSnowflakeProxyLimitWifi(boolean beSnowflakeProxy) {
        putBoolean(PREF_BE_A_SNOWFLAKE_LIMIT_WIFI, beSnowflakeProxy);
    }

    public static void setBeSnowflakeProxyLimitCharging(boolean beSnowflakeProxy) {
        putBoolean(PREF_BE_A_SNOWFLAKE_LIMIT_CHARGING, beSnowflakeProxy);
    }

    public static boolean limitSnowflakeProxyingWifi() {
        return prefs.getBoolean(PREF_BE_A_SNOWFLAKE_LIMIT_WIFI, false);
    }

    public static boolean limitSnowflakeProxyingCharging() {
        return prefs.getBoolean(PREF_BE_A_SNOWFLAKE_LIMIT_CHARGING, false);
    }

    public static boolean useDebugLogging() {
        return prefs.getBoolean(PREF_ENABLE_LOGGING, false);
    }

    public static boolean enableRotation() {
        return prefs.getBoolean(PREF_ENABLE_ROTATION, true);
    }

    public static boolean allowBackgroundStarts() {
        return prefs.getBoolean(PREF_ALLOW_BACKGROUND_STARTS, true);
    }

    public static boolean openProxyOnAllInterfaces() {
        return prefs.getBoolean(PREF_OPEN_PROXY_ON_ALL_INTERFACES, false);
    }

    public static boolean useVpn() {
        return prefs.getBoolean(PREF_USE_VPN, false);
    }

    public static void putUseVpn(boolean value) {
        putBoolean(PREF_USE_VPN, value);
    }

    public static boolean startOnBoot() {
        return prefs.getBoolean(PREF_START_ON_BOOT, true);
    }

    public static void putStartOnBoot(boolean value) {
        putBoolean(PREF_START_ON_BOOT, value);
    }

    public static String getExitNodes() {
        return prefs.getString(PREF_EXIT_NODES, "");
    }

    public static void setExitNodes(String country) {
        putString(PREF_EXIT_NODES, country);
    }

    public static SharedPreferences getSharedPrefs(Context context) {
        //   return context.getSharedPreferences(OrbotConstants.PREF_TOR_SHARED_PREFS, Context.MODE_MULTI_PROCESS);
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static int getSnowflakesServed() {
        return prefs.getInt(PREF_SNOWFLAKES_SERVED_COUNT, 0);
    }

    public static int getSnowflakesServedWeekly() {
        return prefs.getInt(PREF_SNOWFLAKES_SERVED_COUNT_WEEKLY, 0);
    }

    public static void addSnowflakeServed() {
        putInt(PREF_SNOWFLAKES_SERVED_COUNT, getSnowflakesServed() + 1);
        putInt(PREF_SNOWFLAKES_SERVED_COUNT_WEEKLY, getSnowflakesServedWeekly() + 1);
    }

    public static void resetSnowflakesServedWeekly() {
        putInt(PREF_SNOWFLAKES_SERVED_COUNT_WEEKLY, 0);

    }

    /**
     * @see #SUPPORTED_PATHWAYS
     * @return how Orbot is configured to attempt to connect to Tor
     */
    public static String getTorConnectionPathway() {
        // TODO since smart pathway was never fully implemented, default to DIRECT
        return prefs.getString(PREF_CONNECTION_PATHWAY, CONNECTION_PATHWAY_DIRECT);
    }

    /**
     * Set how Orbot should initialize a tor connection (direct, with a PT, etc)
     * @param pathway @see {@link #SUPPORTED_PATHWAYS}
     */
    public static void setTorConnectionPathway(String pathway) {
        if (!SUPPORTED_PATHWAYS.contains(pathway)) {
            throw new RuntimeException("Invalid pathway " + pathway + " - must be one of " + SUPPORTED_PATHWAYS.toString());
        }
        putString(PREF_CONNECTION_PATHWAY, pathway);
    }

    public static void putPrefSmartTrySnowflake(boolean trySnowflake) {
        putBoolean(PREF_SMART_TRY_SNOWFLAKE, trySnowflake);
    }

    public static boolean getPrefSmartTrySnowflake() {
        return prefs.getBoolean(PREF_SMART_TRY_SNOWFLAKE, false);
    }

    public static void putPrefSmartTryObfs4(String bridges) {
        putString(PREF_SMART_TRY_OBFS4, bridges);
    }

    public static String getPrefSmartTryObfs4() {
        return prefs.getString(PREF_SMART_TRY_OBFS4, null);
    }

    public static boolean isPowerUserMode() {
        return prefs.getBoolean(PREF_POWER_USER_MODE, true);
    }

    public static void setSecureWindow(boolean isFlagSecure) {
        putBoolean(PREF_SECURE_WINDOW_FLAG, isFlagSecure);
    }

    public static boolean isSecureWindow() {
        return prefs.getBoolean(PREF_SECURE_WINDOW_FLAG, true);
    }

    public static final String DEFAULT_CAMO_DISABLED_ACTIVITY = "org.torproject.android.OrbotActivity";

    public static boolean isCamoEnabled() {
        var app = prefs.getString(PREF_CAMO_APP_PACKAGE, DEFAULT_CAMO_DISABLED_ACTIVITY);
        return !app.equals(DEFAULT_CAMO_DISABLED_ACTIVITY);
    }

    public static String getSelectedCamoApp() {
        return prefs.getString(PREF_CAMO_APP_PACKAGE, DEFAULT_CAMO_DISABLED_ACTIVITY);
    }

    public static void setCamoAppPackage(String packageName) {
        putString(PREF_CAMO_APP_PACKAGE, packageName);
    }

    public static String getCamoAppDisplayName() {
        return prefs.getString(PREF_CAMO_APP_DISPLAY_NAME, "Android");
    }

    public static boolean requireDeviceAuthentication() {
        return prefs.getBoolean(PREF_REQUIRE_PASSWORD, false);
    }

    public static boolean disallowBiometricAuthentication() {
        return prefs.getBoolean(PREF_DISALLOW_BIOMETRIC_AUTH, false);
    }


    public static void setCamoAppDisplayName(String name) {
        putString(PREF_CAMO_APP_DISPLAY_NAME, name);
    }

}
