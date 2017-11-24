package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;

public class CalendarChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d("Calendar data changed");
        MeetingAgent agent = (MeetingAgent) AgentFactory.getAgentFromGuid(context, MeetingAgent.HARDCODED_GUID);
        agent.updateScheduledAlarms(context);
    }

}
