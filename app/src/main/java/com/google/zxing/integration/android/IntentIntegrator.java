/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.integration.android;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * <p>A utility class which helps ease integration with Barcode Scanner via {@link Intent}s. This is a simple
 * way to invoke barcode scanning and receive the result, without any need to integrate, modify, or learn the
 * project's source code.</p>
 *
 * <h1>Initiating a barcode scan</h1>
 *
 * <p>To integrate, create an instance of {@code IntentIntegrator} and call {@link #initiateScan(ActivityResultLauncher)} and wait
 * for the result in your app.</p>
 *
 * <p>It does require that the Barcode Scanner (or work-alike) application is installed. The
 * {@link #initiateScan(ActivityResultLauncher)} method will prompt the user to download the application, if needed.</p>
 *
 * <p>There are a few steps to using this integration. First, your {@link Activity} must implement
 * a {@link ActivityResultLauncher} and include a line of code like this:</p>
 *
 * <pre>{@code
 * { result ->
 *   IntentResult scanResult = IntentIntegrator.parseActivityResult(result.resultCode, result.intent);
 *   if (scanResult != null) {
 *     // handle scan result
 *   }
 *   // else continue with any other code you need in the method
 *   ...
 * }
 * }</pre>
 *
 * <p>This is where you will handle a scan result.</p>
 *
 * <p>Second, just call this in response to a user action somewhere to begin the scan process:</p>
 *
 * <pre>{@code
 * IntentIntegrator integrator = new IntentIntegrator(yourActivity);
 * integrator.initiateScan();
 * }</pre>
 *
 * <p>Note that {@link #initiateScan(ActivityResultLauncher)} returns an {@link AlertDialog} which is non-null if the
 * user was prompted to download the application. This lets the calling app potentially manage the dialog.
 * In particular, ideally, the app dismisses the dialog if it's still active in its {Activity#onPause()}
 * method.</p>
 *
 * <p>You can use {@link #setTitle(String)} to customize the title of this download prompt dialog (or, use
 * {@link #setTitleByID(int)} to set the title by string resource ID.) Likewise, the prompt message, and
 * yes/no button labels can be changed.</p>
 *
 * <p>Finally, you can use {@link #addExtra(String, Object)} to add more parameters to the Intent used
 * to invoke the scanner. This can be used to set additional options not directly exposed by this
 * simplified API.</p>
 *
 * <p>By default, this will only allow applications that are known to respond to this intent correctly
 * do so. The apps that are allowed to response can be set with {@link #setTargetApplications(List)}.
 * For example, set to {@link #TARGET_BARCODE_SCANNER_ONLY} to only target the Barcode Scanner app itself.</p>
 *
 * <h1>Sharing text via barcode</h1>
 *
 * <p>To share text, encoded as a QR Code on-screen, similarly, see {@link #shareText(CharSequence)}.</p>
 *
 * <p>Some code, particularly download integration, was contributed from the Anobiit application.</p>
 *
 * <h1>Enabling experimental barcode formats</h1>
 *
 * <p>Some formats are not enabled by default even when scanning with {@link #ALL_CODE_TYPES}, such as
 * PDF417. Use {@link #initiateScan(Collection, ActivityResultLauncher)} with
 * a collection containing the names of formats to scan for explicitly, like "PDF_417", to use such
 * formats.</p>
 *
 * @author Sean Owen
 * @author Fred Lin
 * @author Isaac Potoczny-Jones
 * @author Brad Drehmer
 * @author gcstang
 * @noinspection unused
 */
public class IntentIntegrator {

    private static final String TAG = IntentIntegrator.class.getSimpleName();

    public static final String DEFAULT_TITLE = "Install Barcode Scanner?";
    public static final String DEFAULT_MESSAGE =
            "This application requires Barcode Scanner. Would you like to install it?";
    public static final String DEFAULT_YES = "Yes";
    public static final String DEFAULT_NO = "No";

    private static final String BS_PACKAGE = "com.google.zxing.client.android";
    private static final String BSPLUS_PACKAGE = "com.srowen.bs.android";

    // supported barcode formats
    public static final Collection<String> PRODUCT_CODE_TYPES = list("UPC_A", "UPC_E", "EAN_8", "EAN_13", "RSS_14");
    public static final Collection<String> ONE_D_CODE_TYPES =
            list("UPC_A", "UPC_E", "EAN_8", "EAN_13", "CODE_39", "CODE_93", "CODE_128",
                    "ITF", "RSS_14", "RSS_EXPANDED");
    public static final Collection<String> QR_CODE_TYPES = Collections.singleton("QR_CODE");
    public static final Collection<String> DATA_MATRIX_TYPES = Collections.singleton("DATA_MATRIX");

    public static final Collection<String> ALL_CODE_TYPES = null;

    public static final List<String> TARGET_BARCODE_SCANNER_ONLY = Collections.singletonList(BS_PACKAGE);
    public static final List<String> TARGET_ALL_KNOWN = list(
            BSPLUS_PACKAGE,             // Barcode Scanner+
            BSPLUS_PACKAGE + ".simple", // Barcode Scanner+ Simple
            BS_PACKAGE                  // Barcode Scanner
            // What else supports this intent?
    );

    // Should be FLAG_ACTIVITY_NEW_DOCUMENT in API 21+.
    // Defined once here because the current value is deprecated, so generates just one warning
    @SuppressWarnings("deprecation")
    private static final int FLAG_NEW_DOC = Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;

    private final Activity activity;
    private final Fragment fragment;

    private String title;
    private String message;
    private String buttonYes;
    private String buttonNo;
    private List<String> targetApplications;
    private final Map<String, Object> moreExtras = new HashMap<>(3);

    /**
     * @param activity {@link Activity} invoking the integration
     */
    public IntentIntegrator(@NonNull Activity activity) {
        this.activity = activity;
        this.fragment = null;
        initializeConfiguration();
    }

    /**
     * @param fragment {@link Fragment} invoking the integration.
     */
    public IntentIntegrator(Fragment fragment) {
        this.activity = fragment.getActivity();
        this.fragment = fragment;
        initializeConfiguration();
    }

    private void initializeConfiguration() {
        title = DEFAULT_TITLE;
        message = DEFAULT_MESSAGE;
        buttonYes = DEFAULT_YES;
        buttonNo = DEFAULT_NO;
        targetApplications = TARGET_ALL_KNOWN;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public void setTitleByID(int titleID) {
        title = activity.getString(titleID);
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public void setMessage(@NonNull String message) {
        this.message = message;
    }

    public void setMessageByID(int messageID) {
        message = activity.getString(messageID);
    }

    @NonNull
    public String getButtonYes() {
        return buttonYes;
    }

    public void setButtonYes(@NonNull String buttonYes) {
        this.buttonYes = buttonYes;
    }

    public void setButtonYesByID(int buttonYesID) {
        buttonYes = activity.getString(buttonYesID);
    }

    @NonNull
    public String getButtonNo() {
        return buttonNo;
    }

    public void setButtonNo(@NonNull String buttonNo) {
        this.buttonNo = buttonNo;
    }

    public void setButtonNoByID(int buttonNoID) {
        buttonNo = activity.getString(buttonNoID);
    }

    @NonNull
    public Collection<String> getTargetApplications() {
        return targetApplications;
    }

    public final void setTargetApplications(@NonNull List<String> targetApplications) {
        if (targetApplications.isEmpty()) {
            throw new IllegalArgumentException("No target applications");
        }
        this.targetApplications = targetApplications;
    }

    public void setSingleTargetApplication(@NonNull String targetApplication) {
        this.targetApplications = Collections.singletonList(targetApplication);
    }

    @NonNull
    public Map<String, ?> getMoreExtras() {
        return moreExtras;
    }

    public final void addExtra(String key, Object value) {
        moreExtras.put(key, value);
    }

    /**
     * Initiates a scan for all known barcode types with the default camera.
     *
     * @param resultLauncher An ActivityResultLauncher to launch the intent with.
     * @return the {@link AlertDialog} that was shown to the user prompting them to download the app
     * if a prompt was needed, or null otherwise.
     */
    @Nullable
    public final AlertDialog initiateScan(@NonNull ActivityResultLauncher<Intent> resultLauncher) {
        return initiateScan(ALL_CODE_TYPES, -1, resultLauncher);
    }

    /**
     * Initiates a scan for all known barcode types with the specified camera.
     *
     * @param cameraId       camera ID of the camera to use. A negative value means "no preference".
     * @param resultLauncher An ActivityResultLauncher to launch the intent with.
     * @return the {@link AlertDialog} that was shown to the user prompting them to download the app
     * if a prompt was needed, or null otherwise.
     */
    @Nullable
    public final AlertDialog initiateScan(int cameraId, @NonNull ActivityResultLauncher<Intent> resultLauncher) {
        return initiateScan(ALL_CODE_TYPES, cameraId, resultLauncher);
    }

    /**
     * Initiates a scan, using the default camera, only for a certain set of barcode types, given as strings corresponding
     * to their names in ZXing's {@code BarcodeFormat} class like "UPC_A". You can supply constants
     * like {@link #PRODUCT_CODE_TYPES} for example.
     *
     * @param desiredBarcodeFormats names of {@code BarcodeFormat}s to scan for
     * @param resultLauncher        An ActivityResultLauncher to launch the intent with.
     * @return the {@link AlertDialog} that was shown to the user prompting them to download the app
     * if a prompt was needed, or null otherwise.
     */
    @Nullable
    public final AlertDialog initiateScan(@Nullable Collection<String> desiredBarcodeFormats, @NonNull ActivityResultLauncher<Intent> resultLauncher) {
        return initiateScan(desiredBarcodeFormats, -1, resultLauncher);
    }

    /**
     * Initiates a scan, using the specified camera, only for a certain set of barcode types, given as strings
     * corresponding to their names in ZXing's {@code BarcodeFormat} class like "UPC_A". You can supply constants
     * like {@link #PRODUCT_CODE_TYPES} for example.
     *
     * @param desiredBarcodeFormats names of {@code BarcodeFormat}s to scan for
     * @param cameraId              camera ID of the camera to use. A negative value means "no preference".
     * @param resultLauncher        An optional ActivityResultLauncher to launch the intent with.
     * @return the {@link AlertDialog} that was shown to the user prompting them to download the app
     * if a prompt was needed, or null otherwise
     */
    @Nullable
    public final AlertDialog initiateScan(@Nullable Collection<String> desiredBarcodeFormats, int cameraId, @NonNull ActivityResultLauncher<Intent> resultLauncher) {
        Intent intentScan = new Intent(BS_PACKAGE + ".SCAN");
        intentScan.addCategory(Intent.CATEGORY_DEFAULT);

        // check which types of codes to scan for
        if (desiredBarcodeFormats != null) {
            // set the desired barcode types
            StringBuilder joinedByComma = new StringBuilder();
            for (String format : desiredBarcodeFormats) {
                if (!joinedByComma.isEmpty()) {
                    joinedByComma.append(',');
                }
                joinedByComma.append(format);
            }
            intentScan.putExtra("SCAN_FORMATS", joinedByComma.toString());
        }

        // check requested camera ID
        if (cameraId >= 0) {
            intentScan.putExtra("SCAN_CAMERA_ID", cameraId);
        }

        String targetAppPackage = findTargetAppPackage(intentScan);
        if (targetAppPackage == null) {
            return showDownloadDialog();
        }
        intentScan.setPackage(targetAppPackage);
        intentScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentScan.addFlags(FLAG_NEW_DOC);
        attachMoreExtras(intentScan);
        startActivityForResult(intentScan, resultLauncher);
        return null;
    }

    /**
     * Start an activity. This method is defined to allow different methods of activity starting for
     * newer versions of Android and for compatibility library.
     *
     * @param intent         Intent to start.
     * @param resultLauncher An ActivityResultLauncher to launch the intent with.
     * @see Activity#startActivityForResult(Intent, int)
     * @see Fragment#startActivityForResult(Intent, int)
     */
    protected void startActivityForResult(@NonNull Intent intent, @NonNull ActivityResultLauncher<Intent> resultLauncher) {
        resultLauncher.launch(intent);
    }

    @Nullable
    private String findTargetAppPackage(@NonNull Intent intent) {
        PackageManager pm = activity.getPackageManager();
        List<ResolveInfo> availableApps = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (String targetApp : targetApplications) {
            if (contains(availableApps, targetApp)) {
                return targetApp;
            }
        }
        return null;
    }

    private static boolean contains(@NonNull Iterable<ResolveInfo> availableApps, @NonNull String targetApp) {
        for (ResolveInfo availableApp : availableApps) {
            String packageName = availableApp.activityInfo.packageName;
            if (targetApp.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private AlertDialog showDownloadDialog() {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        downloadDialog.setPositiveButton(buttonYes, (dialogInterface, i) -> {
            String packageName;
            if (targetApplications.contains(BS_PACKAGE)) {
                // Prefer to suggest download of BS if it's anywhere in the list
                packageName = BS_PACKAGE;
            } else {
                // Otherwise, first option:
                //noinspection SequencedCollectionMethodCanBeUsed
                packageName = targetApplications.get(0);
            }
            Uri uri = Uri.parse("market://details?id=" + packageName);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            try {
                if (fragment == null) {
                    activity.startActivity(intent);
                } else {
                    fragment.startActivity(intent);
                }
            } catch (ActivityNotFoundException anfe) {
                // Hmm, market is not installed
                Log.w(TAG, "Google Play is not installed; cannot install " + packageName);
            }
        });
        downloadDialog.setNegativeButton(buttonNo, null);
        downloadDialog.setCancelable(true);
        return downloadDialog.show();
    }


    /**
     * <p>Call this from your {@link ActivityResultLauncher} callback.</p>
     *
     * @param resultCode result code from {@code onActivityResult()}
     * @param intent     {@link Intent} from {@code onActivityResult()}
     * @return null if the event handled here was not related to this class, or
     * else an {@link IntentResult} containing the result of the scan. If the user cancelled scanning,
     * the fields will be null.
     */
    @Nullable
    public static IntentResult parseActivityResult(int resultCode, @Nullable Intent intent) {
        if (resultCode == Activity.RESULT_OK && intent != null) {
            String contents = intent.getStringExtra("SCAN_RESULT");
            String formatName = intent.getStringExtra("SCAN_RESULT_FORMAT");
            byte[] rawBytes = intent.getByteArrayExtra("SCAN_RESULT_BYTES");
            int intentOrientation = intent.getIntExtra("SCAN_RESULT_ORIENTATION", Integer.MIN_VALUE);
            Integer orientation = intentOrientation == Integer.MIN_VALUE ? null : intentOrientation;
            String errorCorrectionLevel = intent.getStringExtra("SCAN_RESULT_ERROR_CORRECTION_LEVEL");
            return new IntentResult(contents,
                    formatName,
                    rawBytes,
                    orientation,
                    errorCorrectionLevel);
        }
        return null;
    }


    /**
     * Defaults to type "TEXT_TYPE".
     *
     * @param text the text string to encode as a barcode
     * @return the {@link AlertDialog} that was shown to the user prompting them to download the app
     * if a prompt was needed, or null otherwise
     * @see #shareText(CharSequence, CharSequence)
     */
    @Nullable
    public final AlertDialog shareText(CharSequence text) {
        return shareText(text, "TEXT_TYPE");
    }

    /**
     * Shares the given text by encoding it as a barcode, such that another user can
     * scan the text off the screen of the device.
     *
     * @param text the text string to encode as a barcode
     * @param type type of data to encode. See {@code com.google.zxing.client.android.Contents.Type} constants.
     * @return the {@link AlertDialog} that was shown to the user prompting them to download the app
     * if a prompt was needed, or null otherwise
     */
    @Nullable
    public final AlertDialog shareText(@NonNull CharSequence text, @NonNull CharSequence type) {
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(BS_PACKAGE + ".ENCODE");
        intent.putExtra("ENCODE_TYPE", type);
        intent.putExtra("ENCODE_DATA", text);
        String targetAppPackage = findTargetAppPackage(intent);
        if (targetAppPackage == null) {
            return showDownloadDialog();
        }
        intent.setPackage(targetAppPackage);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(FLAG_NEW_DOC);
        attachMoreExtras(intent);
        if (fragment == null) {
            activity.startActivity(intent);
        } else {
            fragment.startActivity(intent);
        }
        return null;
    }

    @NonNull
    private static List<String> list(String... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    private void attachMoreExtras(@NonNull Intent intent) {
        for (Map.Entry<String, Object> entry : moreExtras.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // Kind of hacky
            switch (value) {
                case Integer i -> intent.putExtra(key, i);
                case Long l -> intent.putExtra(key, l);
                case Boolean b -> intent.putExtra(key, b);
                case Double v -> intent.putExtra(key, v);
                case Float v -> intent.putExtra(key, v);
                case Bundle bundle -> intent.putExtra(key, bundle);
                default -> intent.putExtra(key, value.toString());
            }
        }
    }

}
