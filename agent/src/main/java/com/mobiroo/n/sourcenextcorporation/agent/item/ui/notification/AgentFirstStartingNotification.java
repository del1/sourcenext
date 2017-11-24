package com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;

import java.util.ArrayList;
import java.util.Collections;

public class AgentFirstStartingNotification extends AgentNotification{
    public static final String NAME = "AgentFirstStartingNotification";

	private int mTriggerType;

	public AgentFirstStartingNotification(Context context, Agent agent, int triggerType, boolean unpause) {
		mNotificationActions = new ArrayList<AgentNotificationAction>();
        mAgent = agent;
        mTriggerType = triggerType;

    	String message = "";
        String title = "";

        if (mAgent instanceof AgentNotificationInterface) {
            message = ((AgentNotificationInterface) mAgent).getNotifFirstStartMessage(triggerType, unpause);
            title = ((AgentNotificationInterface) mAgent).getNotifFirstStartTitle(triggerType, unpause);
        }

        mMessage = message;
        mTicker = message;
        mTitle = title;

    	buildActions(context, triggerType);
    	buildClickIntent(context, mAgent);
	}

    @Override
    public AgentNotificationDetail getDetails() {
        return null;
    }

	
	private void buildActions(Context context, int triggerType) {
		mNotificationActions.clear();

        AgentNotificationAction[] actions = new AgentNotificationAction[1];
        actions[0] = generateLearnMoreAction(context, mAgent);
        Collections.addAll(mNotificationActions, actions);
	}
	
	private void buildClickIntent(Context context, Agent agent) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_LAUNCH_CONFIG, true);
        intent.putExtra(MainActivity.EXTRA_AGENT_GUID, agent.getGuid());
        intent.putExtra(MainActivity.EXTRA_FROM_NOTIF, NAME);
        intent.putExtra(MainActivity.EXTRA_TRIGGER_TYPE, mTriggerType);

        intent.putExtra(MainActivity.EXTRA_CLEAR_NOTIFICATIONS, true);
        intent.putExtra(MainActivity.EXTRA_NOTIF_ID, NotificationFactory.ID_NOTIF_MAIN);
        intent.putExtra(MainActivity.EXTRA_NOTIF_TAG, agent.getGuid());

        mClickIntent = intent;
	}
	

	private AgentNotificationAction generateLearnMoreAction(Context context, Agent agent) {
        buildClickIntent(context, agent);

		Logger.i("pending intent with: " + agent.getGuid() +" ... attached.");
		
		// to do change to use string
		PendingIntent pendingIntent = PendingIntent.getActivity(context, agent.getId(), mClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		AgentNotificationAction n = new AgentNotificationAction(
				context.getResources().getString(R.string.learn_more),
				R.drawable.ic_notification_info, pendingIntent);

		return n;
	}


	
}
