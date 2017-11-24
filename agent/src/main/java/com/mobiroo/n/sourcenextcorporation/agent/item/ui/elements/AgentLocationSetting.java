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
import com.mobiroo.n.sourcenextcorporation.agent.activity.choosers.LocationChooserActivity;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.SettingSaver;

public class AgentLocationSetting extends AgentUIElement implements SettingSaver {

    @Override
    public SettingType getType() {
        return SettingType.TIME;
    }

    protected int mName;

    protected TextView mTitleTextView;
    protected TextView mLocationTextView;
    protected String mLocationString;

    protected boolean mIsEnabled;

    public AgentLocationSetting(AgentConfigurationProvider aca, int name, String prefName, String prefValue) {
        mPrefName = prefName;
        mName = name;
        mAgentConfigure = aca;
        mIsEnabled = true;

        mLocationString = prefValue;
    }

    @Override
    public int getName() {
        return mName;
    }


    public void updateText() {
        if(mLocationString.equals("")) {
            mLocationTextView.setText(R.string.location_unset);
            return;
        }

        String[] locationVals = mLocationString.split(AgentPreferences.STRING_SPLIT);
        if(locationVals[0] != null) {
            mLocationTextView.setText(locationVals[0]);
            return;
        }
    }

    @Override
    public View getView(Context context) {
        View timeSettingView = View.inflate(context,
                R.layout.list_item_config_location, null);
        mTitleTextView = (TextView) timeSettingView.findViewById(R.id.title);
        mTitleTextView.setText(getName());

        mLocationTextView = (TextView) timeSettingView.findViewById(R.id.locationText);

        updateText();
        mLocationTextView.setPaintFlags(mLocationTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mLocationTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        Typeface font = Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");
        ((TextView) timeSettingView.findViewById(R.id.title))
                .setTypeface(font);
        mLocationTextView.setTypeface(font);

        if(mIsEnabled) {
            enable();
        } else {
            disable();
        }

        return timeSettingView;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    void showDialog() {
        Intent intent = new Intent( mAgentConfigure.getActivity(), LocationChooserActivity.class);
        mAgentConfigure.startUpdateSettingActivityForResult(intent, getActivityResultKey());
    }

    @Override
    public int getActivityResultKey() {
        return mPrefName.hashCode() & 32767;
    }


    @Override
    public void disableElement() {
        mIsEnabled = false;
        if (mTitleTextView != null) {
            mLocationTextView.setEnabled(false);
            mTitleTextView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_disabled));
        }
    }

    @Override
    public void enableElement() {
        mIsEnabled = true;
        if(mTitleTextView != null) {
            mLocationTextView.setEnabled(true);
            mTitleTextView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_enabled));
        }
    }

    @Override
    public void saveSetting(Intent data) {
        if (data == null) return;

        boolean clear = data.getBooleanExtra("clear", false);
        if(clear) {
            updateSetting("");
            return;
        }

        String locValue = data.getStringExtra("location");
        if (locValue == null) return;
        updateSetting(locValue);

    }

    private void updateSetting(String value) {
        mLocationString = value;
        mAgentConfigure.updateSetting(mPrefName, value);

        updateText();
    }
}
