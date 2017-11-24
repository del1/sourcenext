package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.app.Activity;
import android.content.Intent;

public interface AgentConfigurationProvider {
	public Activity getActivity();
	public void updateSetting(String preferenceName, String preferenceValue);
    public void updateSetting(String agentName, String preferenceName, String preferenceValue);
	public void updateSetting(String preferenceName, String preferenceValue, Runnable runnable);
	public void startUpdateSettingActivityForResult(Intent intent, int requestCode);
	
}
