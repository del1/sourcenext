package com.mobiroo.n.sourcenextcorporation.agent.util.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentNotificationInterface;

public class ShowStartingNotificationTask extends AsyncTask<Void, Void, Void> {
    protected Context mContext;
    protected AgentTaskCollection mTaskCollection;
    protected String mAgentGuid;
    protected int mTriggerType;

    public ShowStartingNotificationTask(Context context, String agentGuid, int triggerType, AgentTaskCollection taskCollection) {
        mContext = context;
        mAgentGuid = agentGuid;
        mTriggerType = triggerType;
        mTaskCollection = taskCollection;
    }


    @Override
    protected void onPreExecute() {
        if (mTaskCollection != null)
            mTaskCollection.addTask(this);
    }

    @Override
    protected Void doInBackground(Void... nothing) {
        try {
            Agent agent = AgentFactory.getAgentFromGuid(mContext, mAgentGuid);
            if ((agent != null) && (agent.isInstalled()) && (agent instanceof AgentNotificationInterface)) {
                // setting unpause = true to avoid showing future meeting name in Meeting Agent
                // also makes sense on some level since this usage of this notification is caused by user interaction
                ((AgentNotificationInterface) agent).notifyStartAgent(mContext, mTriggerType, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (this.isCancelled())
            return;

        if(mTaskCollection != null)
            mTaskCollection.completeTask(this);
    }
}
