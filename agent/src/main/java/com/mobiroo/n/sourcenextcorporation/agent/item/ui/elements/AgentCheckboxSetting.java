package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

import java.util.List;

import static com.mobiroo.n.sourcenextcorporation.agent.R.id.checkBox;


public class AgentCheckboxSetting extends AgentUIElement  {
    protected boolean mIsEnabled;
    protected boolean mIsChecked;
    protected int mId;

    protected String mNameString;
    protected TextView mNameView;
    protected CheckBox mCheckBox;
    protected boolean mEnabled;

    private CheckBox mSelectedCheckBox;

    public AgentCheckboxSetting(AgentConfigurationProvider aca, int name, String prefName) {
        mName = name;
        mEnabled = true;
        mIsChecked = false;
        mIsEnabled = true;
        mPrefName = prefName;
        mId = prefName.hashCode();
        mAgentConfigure = aca;
    }

    public AgentCheckboxSetting(AgentConfigurationProvider aca, int name, boolean isChecked, boolean isEnabled, String prefName) {
        mName = name;
        mEnabled = true;
        mIsChecked = isChecked;
        mIsEnabled = isEnabled;
        mPrefName = prefName;
        mId = prefName.hashCode();
        mAgentConfigure = aca;
    }

    public AgentCheckboxSetting(AgentConfigurationProvider aca, String name, boolean isChecked, boolean isEnabled, String prefName) {
        mName = -1;
        mEnabled = true;
        mIsChecked = isChecked;
        mIsEnabled = isEnabled;
        mPrefName = prefName;
        mId = prefName.hashCode();
        mAgentConfigure = aca;
        mNameString = name;
    }

    @Override
    public int getName() {
        return mName;
    }

    protected String getNameString() {
        return mNameString;
    }

    @Override
    public SettingType getType() {
        return SettingType.BOOLEAN_CHECKBOX;
    }


    public boolean isEditable() {
        return mIsEnabled;
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public View getView(final Context context) {
        View checkboxSettingView = View.inflate(context, R.layout.list_item_config_checkbox, null);
        Typeface font = Typeface
                .createFromAsset(mAgentConfigure.getActivity().getAssets(),
                        "Roboto-Light.ttf");

        if (getName() != -1) {
            ((TextView) checkboxSettingView.findViewById(R.id.name)).setText(String.valueOf(getName()));
        } else if (getNameString() != null) {
            ((TextView) checkboxSettingView.findViewById(R.id.name)).setText(String.valueOf(getNameString()));
        }

        ((TextView) checkboxSettingView.findViewById(R.id.name)).setTypeface(font);

        mNameView = (TextView) checkboxSettingView.findViewById(R.id.name);

        mCheckBox = ((CheckBox) checkboxSettingView.findViewById(checkBox));
        mCheckBox.setId(mId);

        mCheckBox.setEnabled(isEditable());
        mCheckBox.setChecked(isChecked());
        mCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    onChecked(context, mCheckBox, true);
                } else {
                    updateSettings(false);
                }
            }

        });

        if (mEnabled) {
            enable();
        } else {
            disable();
        }

        if (mCheckBox.isChecked()) {
            onChecked(context, mCheckBox, false);
        }

        return checkboxSettingView;
    }

    private void onChecked(Context context, CheckBox checkBox, boolean isUserAction) {
        mSelectedCheckBox = checkBox;

        switch (mPrefName) {
            case AgentPreferences.PHONE_CALL_AUTORESPOND: {

                boolean isGranted = checkPermissionsAndRequestIfNeeded(context, new
                        String[]{Manifest.permission.READ_PHONE_STATE}, Constants
                        .PERMISSIONS_REQUEST_READ_PHONE_STATE);

                if (isGranted && isUserAction) {
                    updateSettings(true);
                }

                break;
            }
            case AgentPreferences.SMS_AUTORESPOND:
            case AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT:
            case AgentPreferences.SMS_READ_ALOUD: {

                boolean isGranted = checkPermissionsAndRequestIfNeeded(context, new
                        String[]{
                        Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
                        Manifest.permission.RECEIVE_SMS
                }, Constants.PERMISSIONS_REQUEST_SMS);

                if (isGranted && isUserAction) {
                    updateSettings(true);
                }

                break;
            }
            case AgentPreferences.SMS_READ_ALOUD_VOICE_RESPONSE: {

                boolean isGranted = checkPermissionsAndRequestIfNeeded(context, new
                                String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.PERMISSIONS_REQUEST_VOICE_RESPONSE);

                if (isGranted && isUserAction) {
                    updateSettings(true);
                }
                break;
            }
            default:
                if(isUserAction) {
                    updateSettings(true);
                }
                break;
        }
    }

    private void updateSettings(boolean isChecked) {
        mIsChecked = isChecked;
        mAgentConfigure.updateSetting(mPrefName, String.valueOf(isChecked));
        notifyChecksChanged();
    }

    public boolean getState() { return isEnabled()&&mIsChecked; }

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public void disableElement() {
        if(mNameView != null) {
            mNameView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_disabled));
            mCheckBox.setEnabled(false);
        }
        mEnabled = false;
    }

    @Override
    public void enableElement() {
        if(mNameView != null && mAgentConfigure.getActivity() != null) {
            mNameView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_enabled));
            mCheckBox.setEnabled(true);
        }
        mEnabled = true;
    }

    /**
     * Checks for permissions and if all permissions are available updates settings otherwise
     * request for missing permissions
     *
     * @param context The Context
     * @param permissions Array of permissions
     * @param requestCode Request code
     */
    private boolean checkPermissionsAndRequestIfNeeded(Context context, String[] permissions,
                                                       int requestCode) {
        List<String> requiredPermissions = Utils.getRequiredPermissions(context, permissions);

        if(!requiredPermissions.isEmpty()) {

            Utils.requestPermissions((Activity) context,
                    requiredPermissions.toArray(new String[requiredPermissions.size()]), requestCode);

            registerPermissionBroadcast(context);

            return false;
        }

        return true;
    }

    private void registerPermissionBroadcast(Context context) {
        LocalBroadcastManager.getInstance(context).registerReceiver(
                mPermissionsResultReceiver,
                new IntentFilter(Constants.ACTION_PERMISSION_RESULT));
    }

    private BroadcastReceiver mPermissionsResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPermissionGranted = intent.getBooleanExtra("is_permissions_granted", false);

            if(isPermissionGranted) {
                updateSettings(true);
            } else {
                mSelectedCheckBox.setChecked(false);
            }

            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }
    };
}
