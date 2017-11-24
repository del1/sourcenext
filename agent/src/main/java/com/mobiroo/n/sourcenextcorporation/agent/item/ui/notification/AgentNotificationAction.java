package com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification;

import android.app.PendingIntent;

public class AgentNotificationAction {
	protected PendingIntent mIntent;
	protected int mIcon;
	protected String mActionName;
	
	AgentNotificationAction(String actionName, int icon, PendingIntent intent) {
		mActionName = actionName;
		mIcon = icon;
		mIntent = intent;
	}
	
	public PendingIntent getIntent() { return mIntent; }
	public int getIconId() { return mIcon; }
	public String getActionName() { return mActionName; }
}
