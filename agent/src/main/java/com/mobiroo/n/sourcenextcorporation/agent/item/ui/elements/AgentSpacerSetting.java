package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.view.View;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.R;

public class AgentSpacerSetting extends AgentUIElement {
	protected int mText;

	public AgentSpacerSetting(AgentConfigurationProvider aca) {
		mAgentConfigure = aca;
	}
	
	@Override
	public View getView(Context context) {
		return View.inflate(context, R.layout.list_item_spacer, null);
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public SettingType getType() {
		// TODO Auto-generated method stub
		return SettingType.STATIC;
	}

	@Override
	public void disableElement() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enableElement() {
		// TODO Auto-generated method stub
		
	}

}
