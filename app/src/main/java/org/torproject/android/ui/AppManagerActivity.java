/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.torproject.android.service.OrbotConstants;
import org.torproject.android.R;
import org.torproject.android.service.util.TorServiceUtils;
import org.torproject.android.service.vpn.TorifiedApp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class AppManagerActivity extends AppCompatActivity implements OnClickListener, OrbotConstants {

    private GridView listApps;
    private ListAdapter adapterApps;
    private final static String TAG = "Orbot";
    PackageManager pMgr = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pMgr = getPackageManager();

        this.setContentView(R.layout.layout_apps);
        setTitle(R.string.apps_mode);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listApps = (GridView) findViewById(R.id.applistview);
        mPrefs = TorServiceUtils.getSharedPrefs(getApplicationContext());


        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;

            protected void onPreExecute() {
                // Pre Code
                dialog = new ProgressDialog(AppManagerActivity.this, android.support.v4.app.DialogFragment.STYLE_NO_TITLE);
                dialog.show();
            }
            protected Void doInBackground(Void... unused) {
                loadApps(mPrefs);
                return null;
            }
            protected void onPostExecute(Void unused) {
                listApps.setAdapter(adapterApps);
                dialog.cancel();
            }
        }.execute();


    }

    SharedPreferences mPrefs = null;
    ArrayList<TorifiedApp> mApps = null;

    private void loadApps (SharedPreferences prefs)
    {

        mApps = getApps(getApplicationContext(), prefs);
        Collections.sort(mApps,new Comparator<TorifiedApp>() {
            public int compare(TorifiedApp o1, TorifiedApp o2) {
                if (o1.isTorified() == o2.isTorified()) return o1.getName().compareTo(o2.getName());
                if (o1.isTorified()) return -1;
                return 1;
            }
        });

        final LayoutInflater inflater = getLayoutInflater();

        adapterApps = new ArrayAdapter<TorifiedApp>(this, R.layout.layout_apps_item, R.id.itemtext,mApps) {

            public View getView(int position, View convertView, ViewGroup parent) {

                ListEntry entry = null;

                if (convertView == null)
                    convertView = inflater.inflate(R.layout.layout_apps_item, parent, false);
                else
                    entry = (ListEntry) convertView.getTag();

                if (entry == null) {
                    // Inflate a new view
                    entry = new ListEntry();
                    entry.icon = (ImageView) convertView.findViewById(R.id.itemicon);
                    entry.box = (CheckBox) convertView.findViewById(R.id.itemcheck);
                    entry.text = (TextView) convertView.findViewById(R.id.itemtext);
                    convertView.setTag(entry);
                }

                final TorifiedApp app = mApps.get(position);

                if (entry.icon != null) {

                    try {
                        entry.icon.setImageDrawable(pMgr.getApplicationIcon(app.getPackageName()));
                        entry.icon.setOnClickListener(AppManagerActivity.this);
                        entry.icon.setTag(entry.box);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                if (entry.text != null) {
                    entry.text.setText(app.getName());
                    entry.text.setOnClickListener(AppManagerActivity.this);
                    entry.text.setTag(entry.box);
                }

                if (entry.box != null) {
                    entry.box.setOnClickListener(AppManagerActivity.this);
                    entry.box.setChecked(app.isTorified());
                    entry.box.setTag(app);
                }

                return convertView;
            }
        };


    }

    private static class ListEntry {
        private CheckBox box;
        private TextView text;
        private ImageView icon;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop() {
        super.onStop();

    }


    public ArrayList<TorifiedApp> getApps (Context context, SharedPreferences prefs)
    {

        String tordAppString = prefs.getString(PREFS_KEY_TORIFIED, "");
        String[] tordApps;

        StringTokenizer st = new StringTokenizer(tordAppString,"|");
        tordApps = new String[st.countTokens()];
        int tordIdx = 0;
        while (st.hasMoreTokens())
        {
            tordApps[tordIdx++] = st.nextToken();
        }

        List<ApplicationInfo> lAppInfo = pMgr.getInstalledApplications(0);

        Iterator<ApplicationInfo> itAppInfo = lAppInfo.iterator();

        ArrayList<TorifiedApp> apps = new ArrayList<TorifiedApp>();

        ApplicationInfo aInfo = null;

        TorifiedApp app = null;

        while (itAppInfo.hasNext())
        {
            aInfo = itAppInfo.next();

            app = new TorifiedApp();

            try {
                PackageInfo pInfo = pMgr.getPackageInfo(aInfo.packageName, PackageManager.GET_PERMISSIONS);

                if (pInfo != null && pInfo.requestedPermissions != null)
                {
                    for (String permInfo:pInfo.requestedPermissions)
                    {
                        if (permInfo.equals("android.permission.INTERNET"))
                        {
                            app.setUsesInternet(true);

                        }
                    }

                }


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            /**
             if ((aInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
             {
             //System app
             app.setUsesInternet(true);
             }**/


            try
            {
                app.setName(pMgr.getApplicationLabel(aInfo).toString());
            }
            catch (Exception e)
            {
                // no name
                continue; //we only show apps with names
            }


            if (!app.usesInternet())
                continue;
            else
            {
                apps.add(app);
            }

            app.setEnabled(aInfo.enabled);
            app.setUid(aInfo.uid);
            app.setUsername(pMgr.getNameForUid(app.getUid()));
            app.setProcname(aInfo.processName);
            app.setPackageName(aInfo.packageName);


            // check if this application is allowed
            if (Arrays.binarySearch(tordApps, app.getUsername()) >= 0) {
                app.setTorified(true);
            }
            else
            {
                app.setTorified(false);
            }

        }

    //    Collections.sort(apps);

        return apps;
    }


    public void saveAppSettings (Context context)
    {

        StringBuilder tordApps = new StringBuilder();
        Intent response = new Intent();

        for (TorifiedApp tApp:mApps)
        {
            if (tApp.isTorified())
            {
                tordApps.append(tApp.getUsername());
                tordApps.append("|");
                response.putExtra(tApp.getUsername(),true);
            }
        }

        Editor edit = mPrefs.edit();
        edit.putString(PREFS_KEY_TORIFIED, tordApps.toString());
        edit.commit();

        setResult(RESULT_OK,response);
    }


    /**
     * Called an application is check/unchecked
     */
    /**
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final TorifiedApp app = (TorifiedApp) buttonView.getTag();
        if (app != null) {
            app.setTorified(isChecked);
        }

        saveAppSettings(this);

    }**/




    public void onClick(View v) {

        CheckBox cbox = null;

        if (v instanceof CheckBox)
            cbox = (CheckBox)v;
        else if (v.getTag() instanceof CheckBox)
            cbox = (CheckBox)v.getTag();

        if (cbox != null) {
            final TorifiedApp app = (TorifiedApp) cbox.getTag();
            if (app != null) {
                app.setTorified(!app.isTorified());
                cbox.setChecked(app.isTorified());
            }

            saveAppSettings(this);
        }
    }




}
