package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.R;

public class AgentIntSliderSetting extends AgentUIElement{
	protected int mCurVal;	
	protected int mMaxVal;
	protected int mId;
	protected TextView mValueText;
	protected TextView mNameText;
	protected SeekBar mSeekBar;
	protected String mFormat;
	protected boolean mEnabled;

	public AgentIntSliderSetting(AgentConfigurationProvider aca, int name, int curVal, int maxVal, String prefName, String format) {
		mAgentConfigure = aca;
		mName = name;
		mCurVal = curVal;
		mPrefName = prefName;
		mMaxVal = maxVal;
		mFormat = format;
		mId = prefName.hashCode();
		mEnabled = true;
	}
	

	@Override
	public int getName() {
		return mName;
	}
	
	public int getCurVal() {
	    return mCurVal;
	}
	
	 @Override
	    public View getView(Context context) {
	        View intRangeSettingView = View.inflate(context, R.layout.list_item_config_int_range, null);
	        
	        mNameText = (TextView) intRangeSettingView.findViewById(R.id.name);
	        mNameText.setText(getName());

	        mValueText = (TextView) intRangeSettingView.findViewById(R.id.seekValue);
			mValueText.setText(String.format(mFormat, Integer.toString(mCurVal)));
	        Typeface font = Typeface
					.createFromAsset(mAgentConfigure.getActivity().getAssets(),
							"Roboto-Light.ttf");

	        mValueText.setTypeface(font);
	        ((TextView) intRangeSettingView.findViewById(R.id.name)).setTypeface(font);

	        mSeekBar = (SeekBar) intRangeSettingView.findViewById(R.id.seekBar);        
	        mSeekBar.setMax(mMaxVal);
	        mSeekBar.setProgress(mCurVal);
	        
	        mSeekBar.setOnSeekBarChangeListener(
	        		new OnSeekBarChangeListener() {

	        			@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							mValueText.setText(String.format(mFormat, Integer.toString(seekBar.getProgress())));
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
							// TODO Auto-generated method stub
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
							// TODO Auto-generated method stub
							mAgentConfigure.updateSetting(mPrefName, Integer.toString(seekBar.getProgress()));
						}
	        			
	        		});
	        
	        mSeekBar.setId(mId);
	        
	        if(mEnabled) {
	        	enableElement();
	        } else {
	        	disableElement();
	        }
	        
	        return intRangeSettingView;
	    }
	@Override
	public boolean isEnabled() {
		return true;
	}


	@Override
	public SettingType getType() {
		return SettingType.INT;
	}


	@Override
	public void disableElement() {
		// TODO Auto-generated method stub
		if(mSeekBar != null) {
			mSeekBar.setEnabled(false);
			mNameText.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_disabled));
			mValueText.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_disabled));
		}
		
		mEnabled = false;
	}

	@Override
	public void enableElement() {
		// TODO Auto-generated method stub
		if(mSeekBar != null) {
			mSeekBar.setEnabled(true);
			mNameText.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_enabled));
			mValueText.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_enabled));
		}
		mEnabled = true;
	}

}
