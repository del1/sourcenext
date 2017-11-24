package com.mobiroo.n.sourcenextcorporation.agent.activity.choosers;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.mobiroo.n.sourcenextcorporation.agent.R;

public class BluetoothChooserActivity extends FragmentActivity {

	public static String BLUETOOTH_NETWORKS = "bt_name";

	private static final int T_BLUETOOTH_LOAD_ACTIVITY = 0x1;

	protected String mBtNetworks;
	protected BluetoothChooserFragment mFragment;
    protected boolean mDirty;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mDirty = false;

        setTitle(R.string.bluetooth_chooser_description);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState != null) {
			mBtNetworks = savedInstanceState.getString("contacts");
            mDirty = (getIntent().getExtras().getString(BLUETOOTH_NETWORKS, "").equals(mBtNetworks));

		} else {
			Bundle extras = getIntent().getExtras();
			if (extras != null) {
				mBtNetworks = extras.getString(BLUETOOTH_NETWORKS, "");
			} else {
				mBtNetworks = "";
			}
		}

		setContentView(R.layout.activity_bluetooth_chooser);

		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter != null && btAdapter.isEnabled()) {
			addFragment();
		} else {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, T_BLUETOOTH_LOAD_ACTIVITY);
		}
	}



    protected void cancel() {
        mDirty = (mDirty || (!mBtNetworks.equals(mFragment.getBtNetworks())));
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
			savedInstanceState.putString("contacts", mFragment.getBtNetworks());
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		int requestCodeLowerBits = requestCode & 32767;
		switch (requestCodeLowerBits) {
		// special case of a choice result
		case T_BLUETOOTH_LOAD_ACTIVITY:
			if (resultCode == RESULT_OK) {
				addFragment();
			} else {
				Intent returnData = new Intent();
				setResult(RESULT_CANCELED, returnData);
				BluetoothChooserActivity.this.finish();
			}
			break;
		default:
		}
	}

	public void addFragment() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();

		final BluetoothChooserFragment fragment = new BluetoothChooserFragment();
		mFragment = fragment;
		fragment.setNetworkData(mBtNetworks);

		fragment.setCompletedListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                String btNames = fragment.getBtNetworks();

                Intent returnData = new Intent();
                returnData.putExtra(BLUETOOTH_NETWORKS, btNames);

                setResult(RESULT_OK, returnData);
                BluetoothChooserActivity.this.finish();
            }

        });

		fragmentTransaction.replace(R.id.fragment_container, fragment);
		fragmentTransaction.commitAllowingStateLoss();
	}

}
