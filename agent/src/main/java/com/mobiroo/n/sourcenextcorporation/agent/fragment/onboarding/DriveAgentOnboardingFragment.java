package com.mobiroo.n.sourcenextcorporation.agent.fragment.onboarding;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.item.onboarding.OnboardingCallbackInterface;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.DriveAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.ParkingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.onboarding.ConfigurationDataInterface;

import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by krohnjw on 4/23/2014.
 */
public class DriveAgentOnboardingFragment extends BaseOnboardingFragment implements View.OnClickListener {

    public static class DriveAgentConfigurationData implements ConfigurationDataInterface {

        @Override
        public String[] getPrefs() {
            if (mSelectedDevice != null) {
                return new String[]{
                        DriveAgent.HARDCODED_GUID + AgentPreferences.STRING_SPLIT + AgentPreferences.BLUETOOTH_NAME_TRIGGER + AgentPreferences.STRING_SPLIT + mSelectedDevice.getPrefString(),
                        ParkingAgent.HARDCODED_GUID + AgentPreferences.STRING_SPLIT + AgentPreferences.BLUETOOTH_NAME_TRIGGER + AgentPreferences.STRING_SPLIT + mSelectedDevice.getPrefString(),
                        DriveAgent.HARDCODED_GUID + AgentPreferences.STRING_SPLIT + AgentPreferences.USE_ACTIVITY_DETECTION + AgentPreferences.STRING_SPLIT + "false",
                        ParkingAgent.HARDCODED_GUID + AgentPreferences.STRING_SPLIT + AgentPreferences.USE_ACTIVITY_DETECTION + AgentPreferences.STRING_SPLIT + "false"};
            } else {
                return new String[]{
                        DriveAgent.HARDCODED_GUID + AgentPreferences.STRING_SPLIT + AgentPreferences.USE_ACTIVITY_DETECTION + AgentPreferences.STRING_SPLIT + "true",
                        ParkingAgent.HARDCODED_GUID + AgentPreferences.STRING_SPLIT + AgentPreferences.USE_ACTIVITY_DETECTION + AgentPreferences.STRING_SPLIT + "true"};
            }
        }
    }


    private boolean mIsDeviceSelected = false;
    private static Device mSelectedDevice;

    private ArrayList<Device> mPairedDevicesList;

    private Device TITLE;
    private Device NONE;

    private Timer mWatchdogTimer;

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(receiver);
        if (mWatchdogTimer != null) {
            mWatchdogTimer.cancel();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((LinearLayout) view.findViewById(R.id.onboarding_content)).addView(View.inflate(getActivity(), R.layout.fragment_onboarding_drive, null));
        ((TextView) view.findViewById(R.id.intro)).setText(Html.fromHtml(buildIntroString(R.string.onboarding_drive_intro)));
        view.findViewById(R.id.intro).setOnClickListener(this);
        TITLE = new Device(getString(R.string.choose), null);
        NONE = new Device(getString(R.string.onboarding_none_bt), null);
        mSelectedDevice = null;
        getBluetoothDeviceList();
    }

    @Override
    protected String getLeftButtonText() {
        return String.format(getString(R.string.onboarding_process), getString(R.string.drive_step), getString(R.string.onboarding_total_steps));
    }

    @Override
    protected String getRightButtonText() {
        return (mIsDeviceSelected) ? getString(R.string.finish) : getString(R.string.skip);
    }

    @Override
    protected int getRightButtonBackground() {
        return (mIsDeviceSelected) ? R.drawable.onboarding_button_solid_clickable : R.drawable.onboarding_button_hollow_clickable;
    }

    @Override
    protected int getLeftButtonBackground() {
        return R.drawable.onboarding_transparent;
    }

    @Override
    protected boolean isLeftButtonClickable() {
        return false;
    }

    @Override
    protected boolean isRightButtonClickable() {
        return true;
    }

    @Override
    protected int getRightButtonTextColor() {
        return (mIsDeviceSelected) ? Color.WHITE : R.color.onboarding_button;
    }

    @Override
    protected int getLeftButtonTextColor() {
        return R.color.onboarding_sub_text;
    }

    @Override
    protected void rightClick() {
        if (!mIsDeviceSelected) {
            Usage.logEvent(getActivity(), Usage.Events.ONBOARDING_DRIVING_SKIP, false);
        }

        Usage.logEvent(getActivity(), Usage.Events.ONBOARDING_FINISH, true);

        if (mCallback != null) {
            // Show finish and then schedule a close
            mCallback.updateConfiguration(DriveAgent.HARDCODED_GUID, new DriveAgentConfigurationData());
            showFinished();
            final Handler handler = new Handler();
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            mCallback.goToNext(OnboardingCallbackInterface.Action.FINISH, null);
                        }
                    });
                }
            }, 800);

        }
    }

    @Override
    protected void leftClick() {
        /* Do nothing */
    }

    @Override
    protected void showHelp() {
        Usage.logEvent(getActivity(), Usage.Events.ONBOARDING_DRIVING_LEARN_MORE, false);
        showHelp(getString(R.string.onboarding_drive_help));
    }

    private void getBluetoothDeviceList() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            showNoDevicesFound(true); // have right button say "next" instead of "skip"
            return;
        }


        if (adapter.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

            if (pairedDevices == null || pairedDevices.size() == 0) {
                showNoDevicesFound(true); // have right button say "next" instead of "skip"
                return;
            }


            // We returned one or more devices.  Build list and display

            ArrayList<Device> devices = new ArrayList<Device>(pairedDevices.size());
            devices.add(TITLE);
            devices.add(NONE);

            for (BluetoothDevice d : pairedDevices) {
                devices.add(new Device(d.getName(), d.getAddress()));
            }

            mPairedDevicesList = devices;

            (getView().findViewById(R.id.progress)).setVisibility(View.GONE);


            // Show user the list of Bluetooth devices so they can choose.
            ArrayAdapter<Device> deviceAdapter = new ArrayAdapter<Device>(getActivity(), R.layout.spinner_onboarding_item, mPairedDevicesList);
            deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            (getView().findViewById(R.id.list)).setVisibility(View.VISIBLE);
            (getView().findViewById(R.id.info)).setVisibility(View.GONE);
            ((Spinner) getView().findViewById(R.id.list)).setAdapter(deviceAdapter);
            ((Spinner) getView().findViewById(R.id.list)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    Device item = mPairedDevicesList.get(position);
                    if (item == TITLE) {
                        mIsDeviceSelected = false;
                        mSelectedDevice = null;

                    } else {
                        Usage.logEvent(getActivity(), Usage.Events.ONBOARDING_CHOOSE, false);
                        mIsDeviceSelected = true;
                        mSelectedDevice = (item == NONE) ? null : item;
                    }

                    setupButton(getView(), R.id.button_right);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });


        } else {
            adapter.enable();

            final Handler handler = new Handler();
            mWatchdogTimer = new Timer();
            mWatchdogTimer.schedule(new TimerTask() {
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            bluetoothWatchdog();
                        }
                    });
                }
            }, 6 * 1000);
        }
    }

    private void bluetoothWatchdog() {
        showNoDevicesFound(true);
    }

    private void showNoDevicesFound(boolean changeButtonText) {
        View v = getView();
        if (v != null) {
            (v.findViewById(R.id.progress)).setVisibility(View.GONE);
            (v.findViewById(R.id.info)).setVisibility(View.VISIBLE);
            ((TextView) v.findViewById(R.id.info)).setText(R.string.drive_agent_onboarding_no_bluetooth_devices);
            (v.findViewById(R.id.list)).setVisibility(View.GONE);
            if (changeButtonText) {
                mIsDeviceSelected = true;
                setupButton(v, R.id.button_right);
            }
        }
    }

    private class Device {
        public String name;
        public String address;

        public Device(String name, String address) {
            this.name = name;
            this.address = address;
        }

        public String toString() {
            return name;
        }

        public String getPrefString() {
            return name + AgentPreferences.STRING_SPLIT + address;
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            if (state == BluetoothAdapter.STATE_ON) {
                if (mWatchdogTimer != null) {
                    mWatchdogTimer.cancel();
                }

                getBluetoothDeviceList();
                BluetoothAdapter.getDefaultAdapter().disable();
            }
        }
    };


}
