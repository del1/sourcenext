package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.R;


public class AgentRadioBooleanSetting extends AgentUIElement {
	protected boolean mIsEnabled;
	protected boolean mIsChecked;
	protected int mId;
	
	protected int mTrueText;
	protected int mFalseText;

	protected String mNameString;
	
	protected RadioButton mFalseButton;
	protected RadioButton mTrueButton;
	protected TextView mTitleView;
	
	
	public AgentRadioBooleanSetting(AgentConfigurationProvider aca, int name, int trueText, int falseText, boolean enabled, boolean isChecked, String prefName) {
		mName = name;
		mIsEnabled = enabled;
	    mIsChecked = isChecked;
        mPrefName = prefName;
        mId = prefName.hashCode();
        mAgentConfigure = aca;
        mTrueText = trueText;
        mFalseText = falseText;
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
		return SettingType.BOOLEAN_RADIO;
	}
	
	public boolean isEditable() {
	    return mIsEnabled;
	}
	
	public boolean isChecked(Context context) {
	    return mIsChecked;
	}

    @Override
    public View getView(final Context context) {
        View checkboxSettingView = View.inflate(context, R.layout.list_item_config_radio_binary, null);
        Typeface font = Typeface
				.createFromAsset(mAgentConfigure.getActivity().getAssets(),
						"Roboto-Light.ttf");
        
        if (getName() != -1) {
            ((TextView) checkboxSettingView.findViewById(R.id.name)).setText(getName());
        } else if (getNameString() != null) {
            ((TextView) checkboxSettingView.findViewById(R.id.name)).setText(getNameString()); 
        }
        
        ((TextView) checkboxSettingView.findViewById(R.id.name)).setTypeface(font);
        
        mTitleView = ((TextView) checkboxSettingView.findViewById(R.id.name));
        
        mTrueButton = ((RadioButton) checkboxSettingView.findViewById(R.id.radio_option_true));
        mTrueButton.setTypeface(font);
        mTrueButton.setText(mTrueText);
        mTrueButton.setId(mId);
        mTrueButton.setChecked(isChecked(context));

        mFalseButton = ((RadioButton) checkboxSettingView.findViewById(R.id.radio_option_false));
        mFalseButton.setTypeface(font);
        mFalseButton.setText(mFalseText);
        mFalseButton.setId(mId+1);
        mFalseButton.setChecked(!isChecked(context));        

        mTrueButton.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mIsChecked = true;
				mFalseButton.setChecked(false);
				mAgentConfigure.updateSetting(mPrefName, String.valueOf(mIsChecked));						
			}
        });
        
        mFalseButton.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mIsChecked = false;
				mTrueButton.setChecked(false);
				mAgentConfigure.updateSetting(mPrefName, String.valueOf(mIsChecked));						
			}
        });

        if(mIsEnabled)
        	enable();
        else
        	disable();
        
        return checkboxSettingView;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

	@Override
	public void disableElement() {
		if (mFalseButton != null) {
			mFalseButton.setEnabled(false);
			mTrueButton.setEnabled(false);
			mTitleView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_disabled));
		}
		mIsEnabled = false;
	}

	@Override
	public void enableElement() {
		if (mFalseButton != null) {
			mFalseButton.setEnabled(true);
			mTrueButton.setEnabled(true);
			mTitleView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_enabled));
		}
		mIsEnabled = true;		
	}
}
