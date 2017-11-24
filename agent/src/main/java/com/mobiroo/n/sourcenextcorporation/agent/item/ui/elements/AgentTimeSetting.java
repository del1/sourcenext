package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.TimePickerFragment;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AgentTimeSetting extends AgentUIElement implements TimePickerFragment.OnCompleteListener {

	@Override
	public SettingType getType() {
		return SettingType.TIME;
	}

	protected int mName;
	protected String mTime;
	protected TextView mTimeTextView;
	protected TextView mTitleTextView;

	protected boolean mIsEnabled;

	public AgentTimeSetting(AgentConfigurationProvider aca, int name, String prefName, String time) {
		mPrefName = prefName;
		mName = name;
		mAgentConfigure = aca;
		mTime = time;
		mIsEnabled = true;
	}

	@Override
	public int getName() {
		return mName;
	}

	public String getTime() {
		return mTime;
	}
	

	@Override
	public void onComplete(String time) {
		setTime(time);
	}
	
	@SuppressLint("SimpleDateFormat")
	public String getDisplayTime() {
		try {
		    Date dateObj = (new SimpleDateFormat("H:mm")).parse(mTime);
		    return DateFormat.getTimeFormat(mAgentConfigure.getActivity()).format(dateObj);
		} catch (final ParseException e) {
		    e.printStackTrace();
		    Logger.d("getDisplayTime in locale time format failed for " + mTime);
		}

		try {
		    Date dateObj = (new SimpleDateFormat("H:mm")).parse(mTime);
		    return (new SimpleDateFormat("h:mm a")).format(dateObj);
		} catch (Exception e) {
			Logger.d("getDisplayTime in simple time format failed for " + mTime);
		}
		
		return "";
	}

	public void setTime(String timeString) {
		mTime = timeString;
		AgentTimeSetting.this.mTimeTextView.setText(getDisplayTime());
		AgentTimeSetting.this.mAgentConfigure.updateSetting(mPrefName, timeString);
	}
	
	
	@Override
	public View getView(Context context) {
		View timeSettingView = View.inflate(context,
				R.layout.list_item_config_time, null);
		mTimeTextView = (TextView) timeSettingView.findViewById(R.id.timeText);
		mTitleTextView = (TextView) timeSettingView.findViewById(R.id.title);
		mTitleTextView.setText(getName());


        
		mTimeTextView.setText(getDisplayTime());
		mTimeTextView.setPaintFlags(mTimeTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		mTimeTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showDialog();
			}
		});

        Typeface font = Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");  
        ((TextView) timeSettingView.findViewById(R.id.title))
		.setTypeface(font);
        mTimeTextView.setTypeface(font);
		
        if(mIsEnabled)
        {
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
		TimePickerFragment newFragment = new TimePickerFragment();
		Bundle args = new Bundle();
		args.putString("time", getTime());
		newFragment.setArguments(args);
		newFragment.setOnCompleteListener(this);
	    newFragment.show(mAgentConfigure.getActivity().getFragmentManager(), "timePicker");
	}

	@Override
	public void disableElement() {
		mIsEnabled = false;
		if (mTitleTextView != null) {
			mTimeTextView.setEnabled(false);
			mTitleTextView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_disabled));
		}
	}

	@Override
	public void enableElement() {
		mIsEnabled = true;
		if(mTitleTextView != null) {
			mTimeTextView.setEnabled(true);
			mTitleTextView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_enabled));
		}
	}
}
