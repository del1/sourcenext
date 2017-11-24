package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.R;


public class AgentCheckboxWithColorSetting extends AgentUIElement {
	protected boolean mIsEnabled;
	protected boolean mIsChecked;
	protected int mId;
	
	protected String mNameString;
	protected TextView mNameView;
	protected CheckBox mCheckBox;
	protected boolean mEnabled;
	protected int mColor;
	public AgentCheckboxWithColorSetting(AgentConfigurationProvider aca, int name, int color, String prefName) {
		mName = name;
		mEnabled = true;
		mIsChecked = false;
        mIsEnabled = true;
        mPrefName = prefName;
        mId = prefName.hashCode();
        mAgentConfigure = aca;
        mColor = color;
	}
	
	public AgentCheckboxWithColorSetting(AgentConfigurationProvider aca, int name, int color, boolean isChecked, boolean isEnabled, String prefName) {
	    mName = name;
		mEnabled = true;
	    mIsChecked = isChecked;
	    mIsEnabled = isEnabled;
	    mPrefName = prefName;
        mId = prefName.hashCode();
	    mAgentConfigure = aca;
	    mColor = color;
	}
	
	public AgentCheckboxWithColorSetting(AgentConfigurationProvider aca, String name, int color, boolean isChecked, boolean isEnabled, String prefName) {
        mName = -1;
		mEnabled = true;
        mIsChecked = isChecked;
        mIsEnabled = isEnabled;
        mPrefName = prefName;
        mId = prefName.hashCode();
        mAgentConfigure = aca;
        mNameString = name;
        mColor = color;
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

	public int getColor() {
	    return mColor;
	}
	
    @Override
    public View getView(final Context context) {
        View checkboxSettingView = View.inflate(context, R.layout.list_item_config_checkbox_with_color, null);
        Typeface font = Typeface
				.createFromAsset(mAgentConfigure.getActivity().getAssets(),
						"Roboto-Light.ttf");
        
        if (getName() != -1) {
            ((TextView) checkboxSettingView.findViewById(R.id.name)).setText(getName());
        } else if (getNameString() != null) {
            ((TextView) checkboxSettingView.findViewById(R.id.name)).setText(getNameString()); 
        }
        
        ((TextView) checkboxSettingView.findViewById(R.id.name)).setTypeface(font);

        checkboxSettingView.findViewById(R.id.color).setBackgroundColor(getColor());
        
        mNameView = (TextView) checkboxSettingView.findViewById(R.id.name);
        
        mCheckBox = ((CheckBox) checkboxSettingView.findViewById(R.id.checkBox));
        mCheckBox.setId(mId);
        
        mCheckBox.setEnabled(isEditable());
        mCheckBox.setChecked(isChecked());
        mCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        	
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mIsChecked = isChecked;
				mAgentConfigure.updateSetting(mPrefName, String.valueOf(isChecked));
                notifyChecksChanged();
			}
        	
        });
        
        if(mEnabled) {
        	enable();
        } else {
        	disable();
        }
        
        return checkboxSettingView;
    }


    @Override
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
		if(mNameView != null) {
			mNameView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_enabled));
			mCheckBox.setEnabled(true);
		}
		mEnabled = true;
	}
}
