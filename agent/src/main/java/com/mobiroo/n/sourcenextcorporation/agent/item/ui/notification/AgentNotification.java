package com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification;

import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;

import java.util.ArrayList;

public abstract class AgentNotification {
    protected Agent mAgent;
    protected String mTitle;
    protected String mMessage;
    protected String mTicker;
    protected Intent mClickIntent;
    protected ArrayList<AgentNotificationAction> mNotificationActions;



    public final Agent getAgent() {return mAgent;}
    public final int getAgentWhiteIcon() {return mAgent.getWhiteIconId();}

    public final String getTitle() {
        return mTitle;
    }
    public final String getMessage() {
        return mMessage;
    }
    public final String getTicker() {
        return mTicker;
    }

    public final Intent getStackedIntent() {
        return mClickIntent;
    }
    public final AgentNotificationAction[] getActions() {
        if (mNotificationActions == null) {
            mNotificationActions = new ArrayList<AgentNotificationAction>();
        }
        return mNotificationActions.toArray(new AgentNotificationAction[mNotificationActions.size()]);
    }

	public final boolean isOngoing() {return true;}

    public int getNotifId() { return NotificationFactory.ID_NOTIF_MAIN;}
    public String getNotifTag() { return mAgent.getGuid();}

	public abstract AgentNotificationDetail getDetails();
}
