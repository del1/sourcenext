package com.mobiroo.n.sourcenextcorporation.agent.activity.choosers;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
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
import com.mobiroo.n.sourcenextcorporation.agent.util.WifiWrapper;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class WifiChooserFragment extends Fragment {

    protected OnClickListener mListener;
    protected ListView mList;
    protected TextView mContinue;

    protected View mError;
    protected View mMain;

    ArrayList<WifiConfiguration> mConfigurationsList;
    ArrayList<WifiWrapper> mSelectedNetworks;

    HashSet<String> mAssociatedNetworkIds;

    public void setNetworkData(String serializedInfo) {
        mSelectedNetworks = new ArrayList<WifiWrapper>();

        if((serializedInfo == null) || (serializedInfo.equals(""))) {
            return; // leave it empty
        }

        String[] infoArray = serializedInfo.split(AgentPreferences.LIST_SPLIT);

        for(String info : infoArray) {
            mSelectedNetworks.add(new WifiWrapper(info));
        }

    }

    public String serializeList() {
        StringBuilder sb = new StringBuilder();

        Iterator<WifiWrapper> i = mSelectedNetworks.iterator();
        while(i.hasNext()) {
            WifiWrapper wifiWrapper = i.next();

            if(!mAssociatedNetworkIds.contains(wifiWrapper.ssid))
                continue;

            sb.append(wifiWrapper.toString());
            if (i.hasNext()) sb.append(AgentPreferences.LIST_SPLIT);
        }

        return sb.toString();
    }


    public String getWifiNetworks() {
        return serializeList();
    }

    public void setListener(OnClickListener listener) {
        mListener = listener;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View chooserView = inflater.inflate(
                R.layout.fragment_chooser, container, false);

        mMain = chooserView.findViewById(R.id.main);
        mError = chooserView.findViewById(R.id.error);

        mList = (ListView) chooserView.findViewById(R.id.chooser_list);

        mContinue = (TextView) chooserView.findViewById(R.id.chooser_continue);
        mContinue.setOnClickListener(mListener);

        setupWifiList();

        mList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int index,
                                    long arg3) {

                WifiConfiguration wifiConfiguration = mConfigurationsList.get(index);

                WifiWrapper network = new WifiWrapper();
                network.name = wifiConfiguration.SSID.replace("\"", "");
                network.ssid = wifiConfiguration.SSID;

                if(mSelectedNetworks.contains(network)) {
                    mSelectedNetworks.remove(network);
                } else {
                    mSelectedNetworks.add(network);
                }
            }

        });

        return chooserView;
    }

    protected void showError() {
        mMain.setVisibility(View.GONE);
        mError.setVisibility(View.VISIBLE);
    }


    protected void setupWifiList() {

        WifiManager wm = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);

        if (wm.isWifiEnabled()) {
            List<WifiConfiguration> wifiConfigurations = wm.getConfiguredNetworks();
            if (wifiConfigurations == null) {
                mConfigurationsList = null;
            } else {
                mConfigurationsList = new ArrayList<WifiConfiguration>(wifiConfigurations);
            }

            if(mConfigurationsList == null) {
                showError();
                return;
            }
        } else {
            showError();
            return;
        }

        mAssociatedNetworkIds = new HashSet<String>();

        for(WifiConfiguration wifiConfig : mConfigurationsList) {
            mAssociatedNetworkIds.add(getSSIDFromWifiConfiguration(wifiConfig));
        }

        mList.setAdapter(getAdapter());
    }


    protected ArrayAdapter<WifiConfiguration> getAdapter() {
        return new ArrayAdapter<WifiConfiguration>(
                WifiChooserFragment.this.getActivity(),
                R.layout.list_view_item_thin, mConfigurationsList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (null == convertView) {
                    convertView = View.inflate(getActivity(), R.layout.list_view_item_thin, null);
                }

                WifiConfiguration wifiConfiguration = getItem(position);

                WifiWrapper network = new WifiWrapper();

                network.name = getNameFromWifiConfiguration(wifiConfiguration);
                network.ssid = getSSIDFromWifiConfiguration(wifiConfiguration);

                if(mSelectedNetworks.contains(network)) {
                    ((ListView) parent).setItemChecked(position, true);
                } else {
                    ((ListView) parent).setItemChecked(position, false);
                }

                ((TextView) convertView.findViewById(R.id.text)).setText(network.name);

                return convertView;
            }
        };
    }

    protected String getNameFromWifiConfiguration(WifiConfiguration wifiConfiguration) {
        if(wifiConfiguration == null) {
            return getActivity().getResources().getString(R.string.unknown);
        } else if((wifiConfiguration.SSID == null) || (wifiConfiguration.SSID.equals("")))  {
            return getActivity().getResources().getString(R.string.unknown);
        } else {
            return (wifiConfiguration.SSID.replace("\"", ""));
        }
    }

    protected String getSSIDFromWifiConfiguration(WifiConfiguration wifiConfiguration) {
        if(wifiConfiguration == null) {
            return  "unknown";
        } else if((wifiConfiguration.SSID == null) || (wifiConfiguration.SSID.equals("")))  {
            return "unknown";
        } else {
            return (wifiConfiguration.SSID);
        }
    }

}
