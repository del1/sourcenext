package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.DelayableNotificationInterface;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

public class PauseIntentService extends IntentService {

	public PauseIntentService() {
		super("PauseIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
        if (intent == null) {return;}

		Context context = getApplicationContext();

		String agentGuid = intent.getStringExtra(MainActivity.EXTRA_AGENT_GUID);

        // Trigger app uses agentId as extra name instead of agentGuid
        if (agentGuid == null) {
            agentGuid = intent.getStringExtra("agentId");
        }

		Logger.i("Agent", "pause intent received for " + agentGuid);
		if (agentGuid == null)
			return;

        Agent agent = AgentFactory.getAgentFromGuid(context, agentGuid);

		if (agent != null) {
			int triggerType = intent.getIntExtra(MainActivity.EXTRA_TRIGGER_TYPE, Constants.TRIGGER_TYPE_UNKNOWN);

			Logger.i("pending intent with: " + agent.getGuid() + " / "
                    + triggerType + " ... received.");

			if (agent instanceof DelayableNotificationInterface) {
                ((DelayableNotificationInterface) agent).skip(context, triggerType);
            } else {
                agent.pause(context);
            }
		}

	}

}
