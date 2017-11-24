package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import junit.framework.Assert;

public class AgentStatusChangedReceiver extends BroadcastReceiver {

	public static final String 	ACTION_ACTIVE 		= "com.mobiroo.n.sourcenextcorporation.agent.agent_active";
	public static final String 	ACTION_INACTIVE 	= "com.mobiroo.n.sourcenextcorporation.agent.agent_inactive";
	public static final String	ACTION_INSTALLED 	= "com.mobiroo.n.sourcenextcorporation.agent.agent_installed";
	public static final String	ACTION_UNINSTALLED	= "com.mobiroo.n.sourcenextcorporation.agent.agent_uninstalled";

    public static final String  EXTRA_GUID          = "com.mobiroo.n.sourcenextcorporation.agent.agent_guid";

	public static IntentFilter getFilterAllActions() {
		return getFilterForActions(ACTION_ACTIVE,
									ACTION_INACTIVE,
									ACTION_INSTALLED,
									ACTION_UNINSTALLED);
	}
	
	public static IntentFilter getFilterForActions(String...actions) {
		IntentFilter filter = new IntentFilter();
		for (String action: actions) {
			filter.addAction(action);
		}
		return filter;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Assert.fail("Classes must override onReceive");
	}
	
}
