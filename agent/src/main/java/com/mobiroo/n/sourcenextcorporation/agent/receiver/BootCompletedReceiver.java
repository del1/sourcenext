package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.billing.IabClient;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.service.ExpireTrialIntentService;
import com.mobiroo.n.sourcenextcorporation.agent.service.LocationRegistrationService;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;

public class BootCompletedReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
    	Logger.d("BootCompletedReceiver");

    	for (Agent activeAgent:AgentFactory.getActiveAgents(context)) {
    		Logger.d("On boot, stopping active agent: " + activeAgent.getGuid());
    		DbAgent.setInactive(context, activeAgent.getGuid(), Constants.TRIGGER_TYPE_BOOT);
    	}
    	
    	SleepAgentActivityReceiver.clearStill(context, null);
        
        Utils.checkReceivers(context);

        ActivityRecognitionHelper.clearActivityDetectionRunningFlag(context);
        ActivityRecognitionHelper.startActivityRecognitionIfNeeded(context);
        
        MeetingAgent agent = (MeetingAgent) AgentFactory.getAgentFromGuid(context, MeetingAgent.HARDCODED_GUID);
        agent.updateScheduledAlarms(context);

        context.startService(new Intent(context, LocationRegistrationService.class));

        if (IabClient.isUserOnTrial(context)) {
            IabClient.scheduleTrialCancel(context);
        } else if (!IabClient.checkLocalUnlock(context) && !IabClient.grantUnlock(context)) {
            context.startService(new Intent(context, ExpireTrialIntentService.class));
        }
    }

}
