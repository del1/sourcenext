package com.mobiroo.n.sourcenextcorporation.agent.action;

import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;

import java.util.HashMap;


public class BatteryAgentAction extends BaseAction {


	@Override
	protected void performActions(int operation, int triggerType, Object extraInfo) {
		if (mAgent == null) {return;}

		HashMap<String, String> agentPrefs = mAgent.getPreferencesMap();

		SettingsButler sb = new SettingsButler(mContext);
		
		if (Boolean.parseBoolean(agentPrefs.get(AgentPreferences.BATTERY_BT))) {
			if (operation == OPERATION_TRIGGER) {
				sb.setBluetoothState(mAgentGuid, "0", SettingsButler.DISABLED);
			} else {
				sb.resetBlutoothState(mAgentGuid, "0");
			}
		}
		
		if (Boolean.parseBoolean(agentPrefs.get(AgentPreferences.BATTERY_WIFI))) {
			if (operation == OPERATION_TRIGGER) {
				sb.setWifiState(mAgentGuid, "0", SettingsButler.DISABLED);
			} else {
				sb.resetWifiState(mAgentGuid, "0");
			}
		}
		
		if (Boolean.parseBoolean(agentPrefs.get(AgentPreferences.BATTERY_MOBILE_DATA))) {
			if (operation == OPERATION_TRIGGER) {
				sb.setMobileDataState(mAgentGuid, "0", SettingsButler.DISABLED);
			} else {
				sb.resetMobileDataState(mAgentGuid, "0");
			}
		}

		if (Boolean.parseBoolean(agentPrefs.get(AgentPreferences.BATTERY_SYNC))) {
			if (operation == OPERATION_TRIGGER) {
				sb.setSyncState(mAgentGuid, "0", SettingsButler.DISABLED);
			} else {
				sb.resetSyncState(mAgentGuid, "0");
			}
		}

		if (Boolean.parseBoolean(agentPrefs.get(AgentPreferences.BATTERY_DISPLAY))) {
			if (operation == OPERATION_TRIGGER) {
				sb.setBrightness(mAgentGuid, "0", Integer.parseInt(agentPrefs.get(AgentPreferences.BATTERY_BRIGHTNESS_LEVEL)));
			} else {
				sb.resetBrightness(mAgentGuid, "0");
			}
		}
		

	}


}
