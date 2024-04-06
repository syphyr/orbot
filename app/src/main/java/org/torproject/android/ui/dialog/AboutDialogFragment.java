package org.torproject.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.BuildConfig;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.service.OrbotService;

import java.io.IOException;

import IPtProxy.IPtProxy;

public class AboutDialogFragment extends DialogFragment {

    public static final String TAG = AboutDialogFragment.class.getSimpleName();
    public static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String BUNDLE_KEY_TV_ABOUT_TEXT = "about_tv_txt";
    private TextView tvAbout;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.layout_about, null);

        TextView versionName = view.findViewById(R.id.versionName);
        versionName.setText(VERSION);

        tvAbout = view.findViewById(R.id.aboutother);

        TextView tvObfs4 = view.findViewById(R.id.tvObfs4);
        tvObfs4.setText(getString(R.string.obfs4_url, IPtProxy.lyrebirdVersion()));

        TextView tvTor = view.findViewById(R.id.tvTor);
        tvTor.setText(getString(R.string.tor_url, OrbotService.BINARY_TOR_VERSION));

        TextView tvSnowflake = view.findViewById(R.id.tvSnowflake);
        tvSnowflake.setText(getString(R.string.snowflake_url, IPtProxy.snowflakeVersion()));

        boolean buildAboutText = true;

        if (savedInstanceState != null) {
            String tvAboutText = savedInstanceState.getString(BUNDLE_KEY_TV_ABOUT_TEXT);
            if (tvAboutText != null) {
                buildAboutText = false;
                tvAbout.setText(tvAboutText);
            }
        }

        if (buildAboutText) {
            try {
                String aboutText = DiskUtils.readFileFromAssets("LICENSE", getContext());
                aboutText = aboutText.replace("\n", "<br/>");
                tvAbout.setText(Html.fromHtml(aboutText));
            } catch (IOException e) {
            }
        }
        return new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.button_about))
                .setView(view)
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_KEY_TV_ABOUT_TEXT, tvAbout.getText().toString());
    }
}
