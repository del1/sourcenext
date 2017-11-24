package com.mobiroo.n.sourcenextcorporation.agent.activity.choosers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.BluetoothWrapper;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class BluetoothChooserFragment extends Fragment {

	private static final int T_BLUETOOTH_LOAD_ACTIVITY = 0x1;

	private enum BT_STATE {OK, EMPTY, OFF}

	LayoutInflater mInflater;
	protected OnClickListener mListener;
	protected ListView mList;
	protected TextView mContinue;

	ArrayList<BluetoothDevice> mPairedDevicesList;
	ArrayList<BluetoothWrapper> mSelectedNetworks;
	

	public BluetoothChooserFragment() {
        mSelectedNetworks = new ArrayList<BluetoothWrapper>();
	}

	public void setNetworkData(String serializedInfo) {
        mSelectedNetworks = new ArrayList<BluetoothWrapper>();

        // Serialized data is being returned as null in some instances as well as the potential empty string.
        if ((serializedInfo == null) || serializedInfo.equals("")) { return; }

		String[] infos = serializedInfo.split(AgentPreferences.LIST_SPLIT);
		
		for(String info : infos) {
			mSelectedNetworks.add(new BluetoothWrapper(info));
		}
		
	}
	
	public String serializeList() {
		StringBuilder sb = new StringBuilder();

		Iterator<BluetoothWrapper> i = mSelectedNetworks.iterator();
		while(i.hasNext()) {
			BluetoothWrapper bt = i.next();
			sb.append(bt.toString());
			if (i.hasNext()) sb.append(AgentPreferences.LIST_SPLIT);
		}

		return sb.toString();
	}
	
	
	public String getBtNetworks() {
		return serializeList();
	}
	
	public void setCompletedListener(OnClickListener listener) {
		mListener = listener;
	}

	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		View chooserView = inflater.inflate(
				R.layout.fragment_chooser, container, false);
		mList = (ListView) chooserView
				.findViewById(R.id.chooser_list);
		mInflater = inflater;


		mContinue = (TextView) chooserView.findViewById(R.id.chooser_continue);

		setupBluetoothList();

		mContinue.setOnClickListener(mListener);

		mList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {

				// TODO Auto-generated method stub
				BluetoothDevice bt = mPairedDevicesList.get(index);
				
				BluetoothWrapper network = new BluetoothWrapper();
				network.name = bt.getName();
				network.mac = bt.getAddress();

				if(mSelectedNetworks.contains(network)) {
					mSelectedNetworks.remove(network);
				} else {
					mSelectedNetworks.add(network);
				}
			}
			
		});
		
		return chooserView;
	}

	
	
	public void refreshBluetoothData() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

		if (adapter.isEnabled()) {
			Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
			mPairedDevicesList = new ArrayList<BluetoothDevice>(pairedDevices);
		} else {
			mPairedDevicesList = new ArrayList<BluetoothDevice>();
		}

		mList.setAdapter(getAdapter());
	}

	protected void setupBluetoothList() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

		if (adapter.isEnabled()) {
			Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
			mPairedDevicesList = new ArrayList<BluetoothDevice>(pairedDevices);
		} else {
			mPairedDevicesList = new ArrayList<BluetoothDevice>();
		}

		mList.setAdapter(getAdapter());

	}



	protected ArrayAdapter<BluetoothDevice> getAdapter() {
		return new ArrayAdapter<BluetoothDevice>(
				BluetoothChooserFragment.this.getActivity(),
				R.layout.list_view_item_thin, mPairedDevicesList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (null == convertView) {
					convertView = mInflater.inflate(
							R.layout.list_view_item_thin, parent, false);
				}

				BluetoothDevice d = getItem(position);

				BluetoothWrapper network = new BluetoothWrapper();

                if(d == null) {
                    network.name = getActivity().getResources().getString(R.string.unknown);
                    network.mac = "unknown";
                } else {
				    network.name = (d.getName()==null)?(getActivity().getResources().getString(R.string.unknown)):(d.getName());
				    network.mac = (d.getAddress()==null)?(getActivity().getResources().getString(R.string.unknown)):(d.getAddress());
                }

				if(mSelectedNetworks.contains(network)) {
					((ListView) parent).setItemChecked(position, true);
				} else {
					((ListView) parent).setItemChecked(position, false);
				}

				String name = network.name;
				if (name.startsWith("\"")) {
					name = name.substring(1, name.length() - 1);
				}
				((TextView) convertView.findViewById(R.id.text)).setText(name);

				return convertView;
			}
		};
	}
}
