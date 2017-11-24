package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.content.Context;
import android.content.Intent;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.AgentStatusChangedReceiver;

import java.util.List;

public class AgentDashClockService extends DashClockExtension {
	
	AgentStatusChangedReceiver mReceiver;
	
	protected void onInitialize(boolean isReconnect) {

		mReceiver = new AgentStatusChangedReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				AgentDashClockService.this.onUpdateData(0);
			}
		};
		
		registerReceiver(mReceiver, AgentStatusChangedReceiver.getFilterAllActions());

	}
	@Override
	protected void onUpdateData(int arg0) {
		List<Agent> agents = AgentFactory.getActiveAgents(this);
		
		String title = "";
		String body = "";
		String bodyConcat = "";
		String status = Integer.toString(agents.size()) + getResources().getString(R.string.dashclock_status_trailer);
		
		if(agents.size() > 0) {
			title = Integer.toString(agents.size()) + getResources().getString(R.string.dashclock_title_end);
			bodyConcat = getResources().getString(R.string.dashclock_status_expanded_start);
			for(Agent agent : agents) {
				body = bodyConcat;
				body += agent.getName();
				bodyConcat += ", ";
			}
			
		} else {
			title = getResources().getString(R.string.dashclock_no_agents_running);
			body = "";
		}
		
        publishUpdate(new ExtensionData()
                .visible(true)
                .icon(R.drawable.ic_launcher_nob)
                .status(status)
                .expandedTitle(title)
                .expandedBody(body)
                .clickIntent(new Intent(this, MainActivity.class)));
	}

}
