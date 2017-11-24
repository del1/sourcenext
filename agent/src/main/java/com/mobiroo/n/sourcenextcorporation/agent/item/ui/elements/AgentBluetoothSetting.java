package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.activity.choosers.BluetoothChooserActivity;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.ConditionalElement;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.SettingSaver;
import com.mobiroo.n.sourcenextcorporation.agent.util.BluetoothWrapper;

import java.util.ArrayList;

public class AgentBluetoothSetting extends AgentUIElement implements
		SettingSaver, ConditionalElement {

	@Override
	public SettingType getType() {
		// TODO Auto-generated method stub
		return SettingType.PICKLIST;
	}

	public static final int BLUETOOTH_CHOOSER = 0x1;

    protected String mAfterSavePrompt;
    protected String mSecondaryAgent;
    protected String mSecondarySetting;

	protected int mName;
	protected String mNetworkDataString;

	protected TextView mNetworkNameTextView;

	public AgentBluetoothSetting(AgentConfigurationProvider acp, int name,
                                 String prefName, String networkName) {
		mPrefName = prefName;
		mName = name;
		mAgentConfigure = acp;
		mNetworkDataString = networkName;
	}

    public void addCascade(String afterSavePrompt, String secondaryAgent, String secondarySetting) {
        mAfterSavePrompt = afterSavePrompt;
        mSecondaryAgent = secondaryAgent;
        mSecondarySetting = secondarySetting;
    }

	@Override
	public View getView(Context context) {
		View bluetoothSettingView = View.inflate(context,
				R.layout.list_item_config_bluetooth, null);
		mNetworkNameTextView = (TextView) bluetoothSettingView
				.findViewById(R.id.bluetoothNetworkNameText);
		((TextView) bluetoothSettingView.findViewById(R.id.title))
				.setText(getName());

		mNetworkNameTextView.setText(getFriendlyNetworkName());
		mNetworkNameTextView.setPaintFlags(mNetworkNameTextView.getPaintFlags()
				| Paint.UNDERLINE_TEXT_FLAG);
		mNetworkNameTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(
						AgentBluetoothSetting.this.mAgentConfigure
								.getActivity(), BluetoothChooserActivity.class);

				String[] data = mNetworkDataString
						.split(AgentPreferences.STRING_SPLIT);

				// string is not set
				if (data == null) {
					intent.putExtra(
							BluetoothChooserActivity.BLUETOOTH_NETWORKS, "");
				}
				// all other cases it's fine
				else {
					intent.putExtra(
							BluetoothChooserActivity.BLUETOOTH_NETWORKS,
							mNetworkDataString);
				}

				AgentBluetoothSetting.this.mAgentConfigure
						.startUpdateSettingActivityForResult(intent,
								getActivityResultKey());
			}
		});

		Typeface font = Typeface.createFromAsset(mAgentConfigure.getActivity()
				.getAssets(), "Roboto-Light.ttf");
		mNetworkNameTextView.setTypeface(font);
		((TextView) bluetoothSettingView.findViewById(R.id.title))
				.setTypeface(font);

        notifyChecksChanged();

		return bluetoothSettingView;
	}

	@Override
	public int getName() {
		return mName;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}
	
	
	public static boolean isNetworkSpecified(String networkString) {
		if ((networkString == null) || (networkString.trim().length() == 0)) {
			return false;
		}
		int split = networkString.indexOf(AgentPreferences.STRING_SPLIT);
		if (split <= 0) {
			return false;
		}		
		return true;
	}
	
	protected boolean isNetworkUnspecified() {
		return !isNetworkSpecified(mNetworkDataString);
	}

	protected String getFriendlyNetworkName() {
		if (isNetworkUnspecified()) {
			return mAgentConfigure.getActivity().getResources()
					.getString(R.string.config_drive_bt_network_unspecified);
		} 

		int split = mNetworkDataString.indexOf(AgentPreferences.STRING_SPLIT);
		if (mNetworkDataString.contains(AgentPreferences.LIST_SPLIT)) {
			return mAgentConfigure.getActivity().getResources()
					.getString(R.string.config_drive_bt_network_multiple);
		} 

		return mNetworkDataString.substring(0, split);
	}

	public void setNetworkName(String networkName) {
		mNetworkDataString = networkName;

        mNetworkNameTextView.setText(getFriendlyNetworkName());
		mAgentConfigure.updateSetting(mPrefName, networkName);

        notifyChecksChanged();
	}

	@Override
	public int getActivityResultKey() {
		return mPrefName.hashCode() & 32767;
	}

	@Override
	public void saveSetting(Intent data) {
		final String networks = data.getExtras().getString(
				BluetoothChooserActivity.BLUETOOTH_NETWORKS);

		setNetworkName(networks);

        if(mAfterSavePrompt != null) {
            new AlertDialog.Builder(mAgentConfigure.getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.agent_confirm)
                    .setMessage(mAfterSavePrompt)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    mAgentConfigure.updateSetting(mSecondaryAgent, mSecondarySetting, networks);
                            }}).setNegativeButton(R.string.no, null).show();
        }

	}

	public static ArrayList<BluetoothWrapper> parseNetworkMacNames(String networkPrefVal) {
		ArrayList<BluetoothWrapper> networks = new ArrayList<BluetoothWrapper>();

		if ((networkPrefVal == null) || (networkPrefVal.trim().length() == 0)) {
			return networks;
		}
		
		String[] listSplit = networkPrefVal.split(AgentPreferences.LIST_SPLIT);
		for (String network:listSplit) {
			String[] split = network.split(AgentPreferences.STRING_SPLIT);
			if ((split == null) || (split.length != 2)) {continue;}
			networks.add(new BluetoothWrapper(network));
		}

		return networks;
	}

	@Override
	public void disableElement() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enableElement() {
		// TODO Auto-generated method stub
		
	}

    @Override
    public boolean getState() {
       return isEnabled()&&!isNetworkUnspecified();
    }

}
