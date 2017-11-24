package com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification;


import android.content.Context;

public interface DelayableNotificationInterface {
    public void notifyDelayAgent(Context context, int triggerType);

    public String getNotifDelayActionString(int triggerType);
    public String getNotifDelayTitle(int triggerType);
    public String getNotifDelayMessage(int triggerType);

    public void delay(Context context, int triggerType);
    public void skip(Context context, int triggerType);
}
