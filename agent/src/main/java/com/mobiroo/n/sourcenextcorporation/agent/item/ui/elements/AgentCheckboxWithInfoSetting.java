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

public class AgentCheckboxWithInfoSetting extends AgentCheckboxSetting {

    private int mSubtextOn;
    private int mSubtextOff;
    
    public AgentCheckboxWithInfoSetting(AgentConfigurationProvider aca, int name, int subtextOn, int subtextOff, String prefName) {
        super(aca, name, prefName);
        mSubtextOn = subtextOn;
        mSubtextOff = subtextOff;
    }
    
    public AgentCheckboxWithInfoSetting(AgentConfigurationProvider aca, int name, int subtextOn, int subtextOff, boolean isChecked, boolean isEnabled, String prefName) {
        super(aca, name, isChecked, isEnabled, prefName);
        mSubtextOn = subtextOn;
        mSubtextOff = subtextOff;
    }
    
    public AgentCheckboxWithInfoSetting(AgentConfigurationProvider aca, String name, int subtextOn, int subtextOff, boolean isChecked, boolean isEnabled, String prefName) {
        super(aca, name, isChecked, isEnabled, prefName);
        mSubtextOn = subtextOn;
        mSubtextOff = subtextOff;
    }
    
    @Override
    public View getView(final Context context) {
        View checkboxSettingView = View.inflate(context, R.layout.list_item_config_checkbox_with_subtext, null);
        Typeface font = Typeface
                .createFromAsset(mAgentConfigure.getActivity().getAssets(),
                        "Roboto-Light.ttf");
        
        if (getName() != -1) {
            ((TextView) checkboxSettingView.findViewById(R.id.name)).setText(getName());
        } else if (getNameString() != null) {
            ((TextView) checkboxSettingView.findViewById(R.id.name)).setText(getNameString()); 
        }
        ((TextView) checkboxSettingView.findViewById(R.id.name)).setTypeface(font);
        
        final TextView subtext = (TextView) checkboxSettingView.findViewById(R.id.subtext);
        subtext.setText((isChecked()) ? mSubtextOn : mSubtextOff);
        subtext.setTypeface(font);
        
        final CheckBox check = (CheckBox) checkboxSettingView.findViewById(R.id.checkBox);
        check.setEnabled(isEditable());
        check.setChecked(isChecked());
        check.setOnCheckedChangeListener(
                new OnCheckedChangeListener () {
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        subtext.setText((isChecked) ? mSubtextOn : mSubtextOff);
                        mAgentConfigure.updateSetting(mPrefName, String.valueOf(isChecked));
                    }
                });
         
        return checkboxSettingView;
    }
    
}
