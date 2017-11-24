package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

public class StartIntentService extends IntentService {

    public StartIntentService() {
		super("StartIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Context context = getApplicationContext();

		String agentGuid = intent.getStringExtra(MainActivity.EXTRA_AGENT_GUID);

        // Trigger app uses agentId as extra name instead of agentGuid
        if (agentGuid == null) {
            agentGuid = intent.getStringExtra("agentId");
        }

		Logger.i("Agent", "start intent received for " + agentGuid);
		if (agentGuid == null)
			return;

		Agent agent = AgentFactory.getAgentFromGuid(context, agentGuid);
		if (agent == null)
			return;

        int triggerType = intent.getIntExtra(MainActivity.EXTRA_TRIGGER_TYPE, Constants.TRIGGER_TYPE_UNKNOWN);

        Logger.i("pending intent with: " + agent.getGuid() + " / "
                + triggerType + " ... received.");

        if (agent.isPaused()) {
            agent.unPause(this);
        } else {
            DbAgent.setActive(this, agent.getGuid(), Constants.TRIGGER_TYPE_MANUAL);
        }
	}

}
