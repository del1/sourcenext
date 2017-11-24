package com.mobiroo.n.sourcenextcorporation.agent.activity.choosers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.R;

/**
 * Created by omarseyal on 3/10/14.
 */
public class WifiChooserActivity extends FragmentActivity {

    public static String WIFI_NETWORKS = "wifi_name";

    protected WifiChooserFragment mFragment;

    protected String mWifiNetworks;
    protected boolean mDirty;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDirty = false;

        setTitle(R.string.wifi_chooser_description);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState != null) {
            mWifiNetworks = savedInstanceState.getString("networks");
            mDirty = (getIntent().getExtras().getString(WIFI_NETWORKS, "").equals(mWifiNetworks));

        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mWifiNetworks = getIntent().getExtras().getString(WIFI_NETWORKS, "");
            } else {
                mWifiNetworks = "";
            }
        }

        setContentView(R.layout.activity_bluetooth_chooser);

        WifiManager wm = (WifiManager) this.getSystemService(WIFI_SERVICE);
        boolean enabled = wm.isWifiEnabled();

        if (!enabled) {
            enabled = wm.setWifiEnabled(true);
        }

        if(!enabled) {
            Logger.i("ENABLING WIFI FAILED");
            Intent returnData = new Intent();
            setResult(RESULT_CANCELED, returnData);
            WifiChooserActivity.this.finish();
        }

        addFragment();

    }



    protected void cancel() {
        mDirty = (mDirty || (!mWifiNetworks.equals(mFragment.getWifiNetworks())));
        if(mDirty) {
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.agent_uninstall_confirm)
                    .setMessage(R.string.confirm_cancel_message)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    setResult(RESULT_CANCELED, null);
                                    finish();
                                }
                            })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            setResult(RESULT_CANCELED, null);
            finish();
        }

    }

    @Override
    public void onBackPressed() {
        cancel();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                cancel();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(mFragment != null) {
            savedInstanceState.putString("networks", mFragment.getWifiNetworks());
        }
    }

    public void addFragment() {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        final WifiChooserFragment fragment = new WifiChooserFragment();
        mFragment = fragment;
        fragment.setNetworkData(mWifiNetworks);

        fragment.setListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String btNames = fragment.getWifiNetworks();

                Intent returnData = new Intent();
                returnData.putExtra(WIFI_NETWORKS, btNames);

                setResult(RESULT_OK, returnData);
                WifiChooserActivity.this.finish();
            }

        });

        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

}
