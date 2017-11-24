package com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.agent.service.StartIntentService;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.service.PauseIntentService;

import java.util.ArrayList;
import java.util.Collections;

public class AgentDelayNotification extends AgentNotification {
    private int mTriggerType;

    public AgentDelayNotification(Context context, Agent agent, int triggerType) {
        mNotificationActions = new ArrayList<AgentNotificationAction>();
        mAgent = agent;

        mTriggerType = triggerType;

        String message = "";
        String title = "";

        if (mAgent instanceof AgentNotificationInterface) {
            message = ((DelayableNotificationInterface) mAgent).getNotifDelayMessage(triggerType);
            title = ((DelayableNotificationInterface) mAgent).getNotifDelayTitle(triggerType);
        }

        mMessage = message;
        mTicker = message;
        mTitle = title;

        buildActions(context);
        buildClickIntent(context);
    }


    @Override
    public AgentNotificationDetail getDetails() {
        return null;
    }


    private void buildClickIntent(Context context) {
        Intent intent = new Intent(context,
                MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_LAUNCH_CONFIG, true);
        intent.putExtra(MainActivity.EXTRA_AGENT_GUID, mAgent.getGuid());
        intent.putExtra(MainActivity.EXTRA_FROM_NOTIF, "AgentDelayNotification");
        intent.putExtra(MainActivity.EXTRA_CLEAR_NOTIFICATIONS, false);

        mClickIntent = intent;
    }


    private void buildActions(Context context) {
        mNotificationActions.clear();
        AgentNotificationAction[] actions;

        actions = new AgentNotificationAction[2];
        actions[0] = generateRestartAction(context);
        actions[1] = generateStopAction(context);

        Collections.addAll(mNotificationActions, actions);
    }


    private AgentNotificationAction generateRestartAction(Context context) {
        Intent resultIntent = new Intent(context,
                StartIntentService.class);

        resultIntent.putExtra(MainActivity.EXTRA_AGENT_GUID, mAgent.getGuid());
        resultIntent.putExtra(MainActivity.EXTRA_TRIGGER_TYPE, mTriggerType);

        // to do change to use string
        PendingIntent pendingIntent = PendingIntent.getService(context, mAgent.getId(), resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        int iconId = R.drawable.ic_play;

        AgentNotificationAction n = new AgentNotificationAction(
                context.getResources().getString(R.string.agent_action_restart),
                iconId,
                pendingIntent);

        return n;
    }


    private AgentNotificationAction generateStopAction(Context context) {
        Intent resultIntent = new Intent(context,
                PauseIntentService.class);

        resultIntent.putExtra(MainActivity.EXTRA_AGENT_GUID, mAgent.getGuid());
        resultIntent.putExtra(MainActivity.EXTRA_TRIGGER_TYPE, mTriggerType);

        // to do change to use string
        PendingIntent pendingIntent = PendingIntent.getService(context, mAgent.getId(), resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AgentNotificationAction n = new AgentNotificationAction(
                context.getResources().getString(R.string.agent_action_skip),
                R.drawable.ic_stop_notification,
                pendingIntent);

        return n;
    }


}
