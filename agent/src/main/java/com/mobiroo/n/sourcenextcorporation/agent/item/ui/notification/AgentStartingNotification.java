package com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.agent.service.DelayIntentService;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

import java.util.ArrayList;
import java.util.Collections;

import com.mobiroo.n.sourcenextcorporation.agent.service.PauseIntentService;

public class AgentStartingNotification extends AgentNotification{
    private int mTriggerType;
    private AgentNotificationDetail mNotificationDetail;

	public AgentStartingNotification(Context context, Agent agent, int triggerType, boolean unpause) {
		mNotificationDetail = new AgentNotificationDetail();
		mNotificationActions = new ArrayList<AgentNotificationAction>();
        mAgent = agent;

        mTriggerType = triggerType;

        String message = "";
        String title = "";
        String[] actionLines = new String[0];

        if (mAgent instanceof AgentNotificationInterface) {
            message = ((AgentNotificationInterface) mAgent).getNotifStartMessage(triggerType, unpause);
            title = ((AgentNotificationInterface) mAgent).getNotifStartTitle(triggerType, unpause);
            actionLines = ((AgentNotificationInterface) mAgent).getNotifActionLines(context, triggerType, unpause);
        }

        mMessage = message;
        mTicker = message;
        mTitle = title;

        AgentNotificationDetail detail = getDetails();
        for(String actionLine : actionLines) {
           // detail.addNotificationLineItem(actionLine);
        }
        if (triggerType == Constants.TRIGGER_TYPE_MANUAL) {
           // detail.addNotificationLineItem(context.getResources().getString(R.string.agent_action_manually_started));
        }

        buildActions(context);
        buildClickIntent(context);
	}


	@Override
	public AgentNotificationDetail getDetails() {
		return mNotificationDetail;
	}

    private void buildClickIntent(Context context) {
        Intent intent = new Intent(context,
                MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_LAUNCH_CONFIG, true);
        intent.putExtra(MainActivity.EXTRA_AGENT_GUID, mAgent.getGuid());
        intent.putExtra(MainActivity.EXTRA_FROM_NOTIF, "AgentStartingNotification");
        intent.putExtra(MainActivity.EXTRA_CLEAR_NOTIFICATIONS, false);

        mClickIntent = intent;
    }


    private void buildActions(Context context) {
		mNotificationActions.clear();
        AgentNotificationAction[] actions;

        if ((mAgent instanceof DelayableNotificationInterface) && (mTriggerType != Constants.TRIGGER_TYPE_MANUAL)) {
            actions = new AgentNotificationAction[2];
            actions[0] = generateDelayAction(context);
            actions[1] = generatePauseAction(context);
        } else {
            actions = new AgentNotificationAction[1];
            actions[0] = generatePauseAction(context);
        }
        Collections.addAll(mNotificationActions, actions);
	}
	

	
	private AgentNotificationAction generatePauseAction(Context context) {
		Intent resultIntent = new Intent(context,
				PauseIntentService.class);

		resultIntent.putExtra(MainActivity.EXTRA_AGENT_GUID, mAgent.getGuid());
		resultIntent.putExtra(MainActivity.EXTRA_TRIGGER_TYPE, mTriggerType);

		PendingIntent pendingIntent = PendingIntent.getService(context, mAgent.getId(), resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String pauseActionString = ((AgentNotificationInterface) mAgent).getNotifPauseActionString(mTriggerType);

        int iconId = R.drawable.ic_notification_pause;
        if (!mAgent.needsUIPause() || (mTriggerType == Constants.TRIGGER_TYPE_MANUAL) || (mAgent instanceof DelayableNotificationInterface)) {
            iconId = R.drawable.ic_stop_notification;
        }


		AgentNotificationAction n = new AgentNotificationAction(
				pauseActionString,
                iconId,
                pendingIntent);

		return n;
	}


    private AgentNotificationAction generateDelayAction(Context context) {
        Intent resultIntent = new Intent(context,
                DelayIntentService.class);

        resultIntent.putExtra(MainActivity.EXTRA_AGENT_GUID, mAgent.getGuid());
        resultIntent.putExtra(MainActivity.EXTRA_TRIGGER_TYPE, mTriggerType);

        PendingIntent pendingIntent = PendingIntent.getService(context, mAgent.getId(), resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String delayActionString = ((DelayableNotificationInterface) mAgent).getNotifDelayActionString(mTriggerType);

        int iconId = R.drawable.ic_notification_pause;

        AgentNotificationAction n = new AgentNotificationAction(
                delayActionString,
                iconId,
                pendingIntent);

        return n;
    }

	
}
