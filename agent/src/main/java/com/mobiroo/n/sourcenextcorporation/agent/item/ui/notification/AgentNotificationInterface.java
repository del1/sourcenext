package com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification;


import android.content.Context;

public interface AgentNotificationInterface {
    public void notifyStartAgent(Context context, int triggerType, boolean unpause);
    public void notifyStopAgent(Context context, int triggerType, long ranForMillis);
    public void notifyPauseAgent(Context context, int triggerType, long ranForMillis);


    public String[] getNotifActionLines(Context context, int triggerType, boolean unpause);
    public String getNotifStartTitle(int triggerType, boolean unpause);
    public String getNotifStartMessage(int triggerType, boolean unpause);
    public String getNotifFirstStartTitle(int triggerType, boolean unpause);
    public String getNotifFirstStartMessage(int triggerType, boolean unpause);
    public String getNotifFirstStartDialogDescription();
    public String getNotifPauseActionString(int triggerType);
}
