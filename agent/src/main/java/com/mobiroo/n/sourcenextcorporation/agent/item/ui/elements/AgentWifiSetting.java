package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.choosers.WifiChooserActivity;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.SettingSaver;
import com.mobiroo.n.sourcenextcorporation.agent.util.WifiWrapper;

import java.util.ArrayList;

public class AgentWifiSetting extends AgentUIElement implements
        SettingSaver {

    protected int mName;
    protected String mNetworkDataString;
    protected TextView mNetworkNameTextView;

    @Override
    public SettingType getType() {
        return SettingType.PICKLIST;
    }

    public AgentWifiSetting(AgentConfigurationProvider acp, int name,
                            String prefName, String networkName) {
        mPrefName = prefName;
        mName = name;
        mAgentConfigure = acp;
        mNetworkDataString = networkName;
    }


    @Override
    public View getView(Context context) {
        View wifiSettingView = View.inflate(context,
                R.layout.list_item_config_wifi, null);
        mNetworkNameTextView = (TextView) wifiSettingView
                .findViewById(R.id.wifiNetworkNameText);
        ((TextView) wifiSettingView.findViewById(R.id.title))
                .setText(getName());

        mNetworkNameTextView.setText(getFriendlyNetworkName());
        mNetworkNameTextView.setPaintFlags(mNetworkNameTextView.getPaintFlags()
                | Paint.UNDERLINE_TEXT_FLAG);
        mNetworkNameTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        AgentWifiSetting.this.mAgentConfigure
                                .getActivity(), WifiChooserActivity.class);

                String[] data = (mNetworkDataString != null) ? (mNetworkDataString
                        .split(AgentPreferences.STRING_SPLIT)) : (null);

                if (data == null) {
                    intent.putExtra(
                            WifiChooserActivity.WIFI_NETWORKS, "");
                }
                else {
                    intent.putExtra(
                            WifiChooserActivity.WIFI_NETWORKS,
                            mNetworkDataString);
                }

                AgentWifiSetting.this.mAgentConfigure
                        .startUpdateSettingActivityForResult(intent,
                                getActivityResultKey());
            }
        });

        Typeface font = Typeface.createFromAsset(mAgentConfigure.getActivity()
                .getAssets(), "Roboto-Light.ttf");
        mNetworkNameTextView.setTypeface(font);

        ((TextView) wifiSettingView.findViewById(R.id.title))
                .setTypeface(font);

        notifyChecksChanged();

        return wifiSettingView;
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


    protected String getFriendlyNetworkName() {
        if (!isNetworkSpecified(mNetworkDataString)) {
            return mAgentConfigure.getActivity().getResources()
                    .getString(R.string.config_wifi_network_unspecified);
        }

        int split = mNetworkDataString.indexOf(AgentPreferences.STRING_SPLIT);
        if (mNetworkDataString.contains(AgentPreferences.LIST_SPLIT)) {
            return mAgentConfigure.getActivity().getResources()
                    .getString(R.string.config_wifi_network_multiple);
        }

        return mNetworkDataString.substring(0, split);
    }

    public void setNetworkName(String networkName) {
        mNetworkDataString = networkName;

        notifyChecksChanged();
        mNetworkNameTextView.setText(getFriendlyNetworkName());
        mAgentConfigure.updateSetting(mPrefName, networkName);
    }

    @Override
    public void saveSetting(Intent data) {
        final String networks = data.getExtras().getString(
                WifiChooserActivity.WIFI_NETWORKS);

        setNetworkName(networks);
    }

    public static ArrayList<WifiWrapper> parseNetworkSsidNames(String networkPrefVal) {
        ArrayList<WifiWrapper> networks = new ArrayList<WifiWrapper>();

        if ((networkPrefVal == null) || (networkPrefVal.trim().length() == 0)) {
            return networks;
        }

        String[] listSplit = networkPrefVal.split(AgentPreferences.LIST_SPLIT);
        for (String network:listSplit) {
            String[] split = network.split(AgentPreferences.STRING_SPLIT);
            if ((split == null) || (split.length != 2)) {continue;}
            networks.add(new WifiWrapper(network));
        }

        return networks;
    }

    @Override
    public boolean getState() {
        return isEnabled()&&isNetworkSpecified(mNetworkDataString);
    }

    @Override
    public void disableElement() {  }

    @Override
    public void enableElement() {  }

    @Override
    public int getName() {
        return mName;
    }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public int getActivityResultKey() {
        return mPrefName.hashCode() & 32767;
    }



}
