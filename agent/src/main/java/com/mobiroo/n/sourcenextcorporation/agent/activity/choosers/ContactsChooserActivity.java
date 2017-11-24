package com.mobiroo.n.sourcenextcorporation.agent.activity.choosers;


import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

import java.util.List;

public class ContactsChooserActivity extends FragmentActivity {

	public static final String CONTACTS_LIST = "contacts_list";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contacts_chooser);
        setTitle(R.string.contacts_title);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        findViewById(R.id.continue_button).setOnClickListener(mContinueListener);

        if(savedInstanceState == null) {
            if(Utils.isPermissionGranted(this, Manifest.permission.READ_CONTACTS)) {
                addFragment();
            } else {
                Utils.requestPermission(this, Manifest.permission.READ_CONTACTS,
                        Constants.PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }
	}

    @Override
    public void onBackPressed() {
        cancel();
    }

    protected void cancel() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ContactsChooserFragment fragment = (ContactsChooserFragment) fragmentManager.findFragmentById(R.id.fragment_container);

        if(fragment.isDirty()) {
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

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                cancel();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        // No call for super(). Bug on API Level > 11.
    }

    public void addFragment() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        ContactsChooserFragment mFragment = ContactsChooserFragment.newInstance(getSerializedListFromExtras());
		fragmentTransaction.add(R.id.fragment_container, mFragment);
        fragmentTransaction.commitAllowingStateLoss();
	}

    private String getSerializedListFromExtras() {
        Bundle extras = getIntent().getExtras();

        if(extras != null) {
            return extras.getString(CONTACTS_LIST);
        }

        return null;
    }

    protected OnClickListener mContinueListener = new OnClickListener() {
		@Override
		public void onClick(View v) {

            FragmentManager fragmentManager = getSupportFragmentManager();
            ContactsChooserFragment fragment = (ContactsChooserFragment) fragmentManager.findFragmentById(R.id.fragment_container);

            Intent returnData = new Intent();
			returnData.putExtra(CONTACTS_LIST, fragment.serializeContactsList());
			setResult(RESULT_OK, returnData);

			ContactsChooserActivity.this.finish();
		}
	};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Constants.PERMISSIONS_REQUEST_READ_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addFragment();
                } else {
                    finish();
                }
                break;
        }

        // Forward permission request result to the fragments
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }
}
